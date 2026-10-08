package minigit.repository;

import java.nio.file.Files;
import java.nio.file.Path;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;

public class Repository {

    public static final String MINIGIT_DIRECTORY_NAME = ".minigit";
    public static final String DEFAULT_BRANCH = "main";

    private final Path rootDirectory;
    private final Path miniGitDirectory;

    public Repository(Path rootDirectory) {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        this.miniGitDirectory = this.rootDirectory.resolve(MINIGIT_DIRECTORY_NAME);
    }

    // Walks up from the given directory until it finds one containing .minigit.
    public static Repository find(Path startDirectory) {
        Path current = startDirectory.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve(MINIGIT_DIRECTORY_NAME))) {
                return new Repository(current);
            }
            current = current.getParent();
        }
        throw new MiniGitException(
            "not a minigit repository (or any of the parent directories): " + MINIGIT_DIRECTORY_NAME
        );
    }

    public static Repository findFromCurrentDirectory() {
        return find(Path.of(""));
    }

    public boolean exists() {
        return Files.isDirectory(miniGitDirectory);
    }

    public void init() {
        if (exists()) {
            throw new MiniGitException("repository already exists in " + miniGitDirectory);
        }
        FileUtils.createDirectories(getObjectsDirectory());
        FileUtils.createDirectories(getHeadsDirectory());
        FileUtils.writeString(getHeadFile(), "ref: refs/heads/" + DEFAULT_BRANCH + "\n");
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public Path getMiniGitDirectory() {
        return miniGitDirectory;
    }

    public Path getObjectsDirectory() {
        return miniGitDirectory.resolve("objects");
    }

    public Path getHeadsDirectory() {
        return miniGitDirectory.resolve("refs").resolve("heads");
    }

    public Path getHeadFile() {
        return miniGitDirectory.resolve("HEAD");
    }
}
