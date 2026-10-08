package minigit.cli;

import minigit.exceptions.MiniGitException;
import minigit.objects.ObjectStore;
import minigit.repository.Repository;

// minigit branch                      list branches
// minigit branch <name> [<start>]     create a branch at HEAD or at <start>
// minigit branch -d <name>            delete a branch
public class BranchCommand implements Command {

    @Override
    public void execute(String[] args) {
        Repository repository = Repository.findFromCurrentDirectory();

        if (args.length == 1) {
            listBranches(repository);
        } else if (args[1].equals("-d") && args.length == 3) {
            deleteBranch(repository, args[2]);
        } else if (!args[1].startsWith("-") && args.length <= 3) {
            createBranch(repository, args[1], args.length == 3 ? args[2] : null);
        } else {
            throw new MiniGitException("usage: minigit branch [-d] [<name>] [<start>]");
        }
    }

    private void listBranches(Repository repository) {
        String current = repository.getCurrentBranch();
        if (current == null) {
            System.out.println("* (HEAD detached at " + repository.getHeadCommit().substring(0, 7) + ")");
        }
        for (String branch : repository.listBranches()) {
            System.out.println((branch.equals(current) ? "* " : "  ") + branch);
        }
    }

    private void createBranch(Repository repository, String name, String startPoint) {
        Repository.validateBranchName(name);
        if (repository.branchExists(name)) {
            throw new MiniGitException("a branch named '" + name + "' already exists");
        }

        String commit;
        if (startPoint != null) {
            commit = new ObjectStore(repository).resolveCommit(repository, startPoint);
        } else {
            commit = repository.getHeadCommit();
            if (commit == null) {
                throw new MiniGitException("cannot create a branch before the first commit");
            }
        }
        repository.writeBranch(name, commit);
    }

    private void deleteBranch(Repository repository, String name) {
        if (!repository.branchExists(name)) {
            throw new MiniGitException("branch '" + name + "' not found");
        }
        if (name.equals(repository.getCurrentBranch())) {
            throw new MiniGitException("cannot delete branch '" + name + "' because it is checked out");
        }
        String commit = repository.readBranch(name);
        repository.deleteBranch(name);
        System.out.println("Deleted branch " + name + " (was " + commit.substring(0, 7) + ").");
    }
}
