package minigit.objects;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import minigit.exceptions.MiniGitException;
import minigit.hashing.HashUtils;

// A tree is one directory snapshot: a sorted list of (mode, name, hash) entries that point
// at blobs (files) or other trees (subdirectories). The binary layout matches Git's:
// "<mode> <name>\0<20 raw hash bytes>" per entry.
public class Tree extends GitObject {

    public static final String TYPE = "tree";
    public static final String FILE_MODE = "100644";
    public static final String DIRECTORY_MODE = "40000";

    private static final int RAW_HASH_LENGTH = 20;

    // Git sorts directories as if their names ended in "/".
    private static final Comparator<Entry> GIT_ORDER = Comparator.comparing(Entry::sortKey);

    public static class Entry {

        private final String mode;
        private final String name;
        private final String hash;

        public Entry(String mode, String name, String hash) {
            this.mode = mode;
            this.name = name;
            this.hash = hash;
        }

        public String getMode() {
            return mode;
        }

        public String getName() {
            return name;
        }

        public String getHash() {
            return hash;
        }

        public boolean isDirectory() {
            return mode.equals(DIRECTORY_MODE);
        }

        public String getObjectType() {
            return isDirectory() ? TYPE : Blob.TYPE;
        }

        private String sortKey() {
            return isDirectory() ? name + "/" : name;
        }
    }

    private final List<Entry> entries;

    public Tree(List<Entry> entries) {
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(GIT_ORDER);
        this.entries = Collections.unmodifiableList(sorted);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public byte[] getContent() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Entry entry : entries) {
            out.writeBytes((entry.mode + " " + entry.name + "\0").getBytes(StandardCharsets.UTF_8));
            out.writeBytes(HashUtils.fromHex(entry.hash));
        }
        return out.toByteArray();
    }

    public static Tree parse(byte[] content) {
        List<Entry> entries = new ArrayList<>();
        int position = 0;
        while (position < content.length) {
            int space = indexOf(content, (byte) ' ', position);
            int nul = indexOf(content, (byte) 0, space + 1);
            if (space < 0 || nul < 0 || nul + RAW_HASH_LENGTH >= content.length) {
                throw new MiniGitException("tree object is corrupt");
            }
            String mode = new String(content, position, space - position, StandardCharsets.UTF_8);
            String name = new String(content, space + 1, nul - space - 1, StandardCharsets.UTF_8);
            byte[] rawHash = new byte[RAW_HASH_LENGTH];
            System.arraycopy(content, nul + 1, rawHash, 0, RAW_HASH_LENGTH);
            entries.add(new Entry(mode, name, HashUtils.toHex(rawHash)));
            position = nul + 1 + RAW_HASH_LENGTH;
        }
        return new Tree(entries);
    }

    public static Tree read(ObjectStore store, String hash) {
        return parse(store.read(hash, TYPE).getContent());
    }

    // Writes the trees for a set of "dir/file" -> blob hash paths and returns the root tree hash.
    public static String write(Map<String, String> files, ObjectStore store) {
        SortedMap<String, String> blobs = new TreeMap<>();
        SortedMap<String, SortedMap<String, String>> directories = new TreeMap<>();

        for (Map.Entry<String, String> file : files.entrySet()) {
            String path = file.getKey();
            int slash = path.indexOf('/');
            if (slash < 0) {
                blobs.put(path, file.getValue());
            } else {
                directories
                    .computeIfAbsent(path.substring(0, slash), name -> new TreeMap<>())
                    .put(path.substring(slash + 1), file.getValue());
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, String> blob : blobs.entrySet()) {
            entries.add(new Entry(FILE_MODE, blob.getKey(), blob.getValue()));
        }
        for (Map.Entry<String, SortedMap<String, String>> directory : directories.entrySet()) {
            entries.add(new Entry(DIRECTORY_MODE, directory.getKey(), write(directory.getValue(), store)));
        }
        return store.write(new Tree(entries));
    }

    // The inverse of write: every file in the tree as "dir/file" -> blob hash.
    public static SortedMap<String, String> flatten(String treeHash, ObjectStore store) {
        SortedMap<String, String> files = new TreeMap<>();
        flattenInto(treeHash, "", store, files);
        return files;
    }

    private static void flattenInto(String treeHash, String prefix, ObjectStore store, Map<String, String> files) {
        for (Entry entry : read(store, treeHash).getEntries()) {
            String path = prefix + entry.getName();
            if (entry.isDirectory()) {
                flattenInto(entry.getHash(), path + "/", store, files);
            } else {
                files.put(path, entry.getHash());
            }
        }
    }

    private static int indexOf(byte[] data, byte value, int from) {
        for (int i = from; i < data.length; i++) {
            if (data[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
