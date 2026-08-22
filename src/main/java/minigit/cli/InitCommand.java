package minigit.cli;

import java.nio.file.Path;
import minigit.repository.Repository;


public class InitCommand implements Command {
    @Override
    public void execute(String[] args) {

        Path currentDirectory = Path.of("").toAbsolutePath();

        Repository repository = new Repository(currentDirectory);
        
        System.out.println(
            "Repository: " + repository.getRootDirectory()
        );

        System.out.println(
            "MiniGit directory: " + repository.getMiniGitDirectory()
        );

    }
}
    

