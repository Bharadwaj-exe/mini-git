package minigit.cli;

import minigit.exceptions.MiniGitException;
import minigit.objects.Commit;
import minigit.objects.ObjectStore;
import minigit.repository.Repository;

// minigit log [--oneline]
// Walks first parents back from HEAD.
public class LogCommand implements Command {

    @Override
    public void execute(String[] args) {
        boolean oneline = false;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--oneline")) {
                oneline = true;
            } else {
                throw new MiniGitException("usage: minigit log [--oneline]");
            }
        }

        Repository repository = Repository.findFromCurrentDirectory();
        ObjectStore store = new ObjectStore(repository);
        String hash = repository.getHeadCommit();
        if (hash == null) {
            String branch = repository.getCurrentBranch();
            throw new MiniGitException("your current branch '" + branch + "' does not have any commits yet");
        }

        boolean first = true;
        while (hash != null) {
            Commit commit = Commit.read(store, hash);
            if (oneline) {
                System.out.println(hash.substring(0, 7) + " " + commit.getSummary());
            } else {
                if (!first) {
                    System.out.println();
                }
                printFull(hash, commit);
            }
            first = false;
            hash = commit.getParents().isEmpty() ? null : commit.getParents().get(0);
        }
    }

    private void printFull(String hash, Commit commit) {
        System.out.println("commit " + hash);
        System.out.println("Author: " + commit.getAuthor().getName() + " <" + commit.getAuthor().getEmail() + ">");
        System.out.println("Date:   " + commit.getAuthor().formatDate());
        System.out.println();
        for (String line : commit.getMessage().split("\n")) {
            System.out.println("    " + line);
        }
    }
}
