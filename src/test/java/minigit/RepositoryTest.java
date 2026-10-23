package minigit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import minigit.exceptions.MiniGitException;
import minigit.repository.Repository;

class RepositoryTest {

    @TempDir
    Path dir;

    @Test
    void initCreatesLayoutAndStartsOnMain() throws Exception {
        Repository repository = new Repository(dir);
        repository.init();

        assertTrue(Files.isDirectory(dir.resolve(".minigit/objects")));
        assertTrue(Files.isDirectory(dir.resolve(".minigit/refs/heads")));
        assertEquals("ref: refs/heads/main\n", Files.readString(dir.resolve(".minigit/HEAD")));
        assertEquals("main", repository.getCurrentBranch());
        assertNull(repository.getHeadCommit());
    }

    @Test
    void initTwiceFails() {
        Repository repository = new Repository(dir);
        repository.init();
        assertThrows(MiniGitException.class, repository::init);
    }

    @Test
    void findWalksUpToTheRepositoryRoot() throws Exception {
        new Repository(dir).init();
        Path nested = Files.createDirectories(dir.resolve("a/b/c"));
        assertEquals(dir.toAbsolutePath().normalize(), Repository.find(nested).getRootDirectory());
    }

    @Test
    void findOutsideARepositoryFails() {
        assertThrows(MiniGitException.class, () -> Repository.find(dir));
    }

    @Test
    void branchNamesAreValidated() {
        Repository.validateBranchName("feature-1");
        Repository.validateBranchName("release_2.0");
        assertThrows(MiniGitException.class, () -> Repository.validateBranchName("-bad"));
        assertThrows(MiniGitException.class, () -> Repository.validateBranchName("has space"));
        assertThrows(MiniGitException.class, () -> Repository.validateBranchName("a/b"));
    }
}
