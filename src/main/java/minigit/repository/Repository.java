package minigit.repository;
import java.nio.file.Path;

public class Repository {

    private final Path rootDirectory;
    private final Path miniGitDirectory;

    public Repository(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.miniGitDirectory = rootDirectory.resolve(".minigit");

    }
    
    public Path getRootDirectory() {
        return rootDirectory;
    }

    public Path getMiniGitDirectory() {
        return miniGitDirectory;
    }
}
  

