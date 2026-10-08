package minigit.cli;

import java.nio.file.Path;

import minigit.repository.Repository;

public class InitCommand implements Command {

    @Override
    public void execute(String[] args) {
        Path currentDirectory = Path.of("").toAbsolutePath();
        Repository repository = new Repository(currentDirectory);

        repository.init();

        System.out.println(
            "Initialized empty MiniGit repository in " + repository.getMiniGitDirectory()
        );
    }
}
