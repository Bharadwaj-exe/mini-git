package minigit.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;
import minigit.hashing.HashUtils;

// The staging area: which blob each tracked path will have in the next commit.
// Stored in .minigit/index as one "<blob hash> <path>" line per file, sorted by path.
public class Index {

    private final Path indexFile;
    private final SortedMap<String, String> entries = new TreeMap<>();

    private Index(Path indexFile) {
        this.indexFile = indexFile;
    }

    public static Index load(Repository repository) {
        Index index = new Index(repository.getIndexFile());
        if (Files.exists(index.indexFile)) {
            for (String line : FileUtils.readString(index.indexFile).split("\n")) {
                if (line.isEmpty()) {
                    continue;
                }
                int space = line.indexOf(' ');
                if (space != HashUtils.HASH_LENGTH) {
                    throw new MiniGitException("index file is corrupt: " + line);
                }
                index.entries.put(line.substring(space + 1), line.substring(0, space));
            }
        }
        return index;
    }

    public void save() {
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            content.append(entry.getValue()).append(' ').append(entry.getKey()).append('\n');
        }
        FileUtils.writeString(indexFile, content.toString());
    }

    public void stage(String path, String blobHash) {
        entries.put(path, blobHash);
    }

    public void remove(String path) {
        entries.remove(path);
    }

    public boolean contains(String path) {
        return entries.containsKey(path);
    }

    public String getHash(String path) {
        return entries.get(path);
    }

    public SortedMap<String, String> getEntries() {
        return Collections.unmodifiableSortedMap(entries);
    }

    public void replaceAll(Map<String, String> newEntries) {
        entries.clear();
        entries.putAll(newEntries);
    }
}
