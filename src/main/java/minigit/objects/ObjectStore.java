package minigit.objects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;
import minigit.hashing.HashUtils;
import minigit.repository.Repository;

// Content-addressed storage: each object lives at objects/<first 2 hash chars>/<remaining 38>,
// zlib-compressed, the same loose-object layout Git uses.
public class ObjectStore {

    private static final int MIN_PREFIX_LENGTH = 4;

    private final Path objectsDirectory;

    public ObjectStore(Repository repository) {
        this.objectsDirectory = repository.getObjectsDirectory();
    }

    public String write(GitObject object) {
        byte[] serialized = object.serialize();
        String hash = HashUtils.sha1(serialized);
        Path path = objectPath(hash);
        if (!Files.exists(path)) {
            FileUtils.writeBytes(path, compress(serialized));
        }
        return hash;
    }

    public boolean contains(String hash) {
        return Files.exists(objectPath(hash));
    }

    public StoredObject read(String hashOrPrefix) {
        String hash = resolve(hashOrPrefix);
        byte[] data = decompress(FileUtils.readBytes(objectPath(hash)));

        int space = indexOf(data, (byte) ' ', 0);
        int nul = indexOf(data, (byte) 0, space + 1);
        if (space < 0 || nul < 0) {
            throw new MiniGitException("object " + hash + " is corrupt");
        }

        String type = new String(data, 0, space, StandardCharsets.UTF_8);
        int length = Integer.parseInt(new String(data, space + 1, nul - space - 1, StandardCharsets.UTF_8));
        if (data.length - nul - 1 != length) {
            throw new MiniGitException("object " + hash + " has the wrong length");
        }

        byte[] content = new byte[length];
        System.arraycopy(data, nul + 1, content, 0, length);
        return new StoredObject(hash, type, content);
    }

    public StoredObject read(String hashOrPrefix, String expectedType) {
        StoredObject object = read(hashOrPrefix);
        if (!object.getType().equals(expectedType)) {
            throw new MiniGitException(
                "object " + object.getHash() + " is a " + object.getType() + ", not a " + expectedType
            );
        }
        return object;
    }

    // Expands an abbreviated hash (at least 4 characters) to the full 40-character hash.
    public String resolve(String hashOrPrefix) {
        String prefix = hashOrPrefix.toLowerCase();
        if (!HashUtils.isHexPrefix(prefix) || prefix.length() < MIN_PREFIX_LENGTH) {
            throw new MiniGitException("not a valid object name: " + hashOrPrefix);
        }
        if (prefix.length() == HashUtils.HASH_LENGTH) {
            if (!contains(prefix)) {
                throw new MiniGitException("object not found: " + hashOrPrefix);
            }
            return prefix;
        }

        Path directory = objectsDirectory.resolve(prefix.substring(0, 2));
        List<String> matches = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
                for (Path file : files) {
                    String hash = prefix.substring(0, 2) + file.getFileName();
                    if (hash.startsWith(prefix)) {
                        matches.add(hash);
                    }
                }
            } catch (IOException e) {
                throw new MiniGitException("could not read " + directory, e);
            }
        }

        if (matches.isEmpty()) {
            throw new MiniGitException("object not found: " + hashOrPrefix);
        }
        if (matches.size() > 1) {
            throw new MiniGitException("short object name " + hashOrPrefix + " is ambiguous");
        }
        return matches.get(0);
    }

    private Path objectPath(String hash) {
        return objectsDirectory.resolve(hash.substring(0, 2)).resolve(hash.substring(2));
    }

    private static byte[] compress(byte[] data) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (OutputStream out = new DeflaterOutputStream(buffer)) {
            out.write(data);
        } catch (IOException e) {
            throw new MiniGitException("could not compress object", e);
        }
        return buffer.toByteArray();
    }

    private static byte[] decompress(byte[] data) {
        try (InputStream in = new InflaterInputStream(new ByteArrayInputStream(data))) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new MiniGitException("could not decompress object", e);
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
