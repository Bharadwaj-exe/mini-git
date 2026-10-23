package minigit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import minigit.exceptions.MiniGitException;
import minigit.hashing.HashUtils;
import minigit.objects.Blob;
import minigit.objects.ObjectStore;
import minigit.objects.StoredObject;
import minigit.objects.Tree;
import minigit.repository.Repository;

// Expected hashes below were produced by real Git (`git hash-object`, `git write-tree`),
// so these tests pin MiniGit to Git's exact object format.
class ObjectFormatTest {

    @TempDir
    Path dir;

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void sha1MatchesKnownDigest() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", HashUtils.sha1(bytes("abc")));
    }

    @Test
    void hexRoundTrips() {
        byte[] data = {0, 1, 15, 16, (byte) 0xab, (byte) 0xff};
        assertArrayEquals(data, HashUtils.fromHex(HashUtils.toHex(data)));
    }

    @Test
    void blobHashMatchesGit() {
        // printf 'hello\n' | git hash-object --stdin
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", new Blob(bytes("hello\n")).getHash());
        // git hash-object /dev/null
        assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", new Blob(new byte[0]).getHash());
    }

    @Test
    void emptyTreeHashMatchesGit() {
        // git hash-object -t tree /dev/null
        assertEquals("4b825dc642cb6eb9a060e54bf8d69288fbee4904", new Tree(List.of()).getHash());
    }

    @Test
    void objectStoreRoundTripsAndResolvesPrefixes() {
        Repository repository = new Repository(dir);
        repository.init();
        ObjectStore store = new ObjectStore(repository);

        String hash = store.write(new Blob(bytes("hello\n")));
        assertTrue(store.contains(hash));

        StoredObject read = store.read(hash.substring(0, 7));
        assertEquals(hash, read.getHash());
        assertEquals("blob", read.getType());
        assertArrayEquals(bytes("hello\n"), read.getContent());
        assertThrows(MiniGitException.class, () -> store.read(hash, "tree"));
    }

    @Test
    void nestedTreeWritesAndFlattens() {
        Repository repository = new Repository(dir);
        repository.init();
        ObjectStore store = new ObjectStore(repository);

        String a = store.write(new Blob(bytes("a\n")));
        String b = store.write(new Blob(bytes("b\n")));
        Map<String, String> files = new TreeMap<>(Map.of("a.txt", a, "src/b.txt", b));

        String root = Tree.write(files, store);
        SortedMap<String, String> flattened = Tree.flatten(root, store);
        assertEquals(files, flattened);
    }
}
