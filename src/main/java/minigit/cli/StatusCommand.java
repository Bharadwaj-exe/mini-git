package minigit.cli;

import java.util.Map;

import minigit.exceptions.MiniGitException;
import minigit.repository.Repository;
import minigit.repository.Status;

// minigit status
public class StatusCommand implements Command {

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            throw new MiniGitException("usage: minigit status");
        }

        Repository repository = Repository.findFromCurrentDirectory();
        Status status = Status.compute(repository);

        String branch = repository.getCurrentBranch();
        String head = repository.getHeadCommit();
        if (branch != null) {
            System.out.println("On branch " + branch);
        } else {
            System.out.println("HEAD detached at " + head.substring(0, 7));
        }
        if (head == null) {
            System.out.println();
            System.out.println("No commits yet");
        }

        boolean clean = true;

        if (!status.getStaged().isEmpty()) {
            clean = false;
            System.out.println();
            System.out.println("Changes to be committed:");
            printChanges(status.getStaged());
        }

        if (!status.getUnstaged().isEmpty()) {
            clean = false;
            System.out.println();
            System.out.println("Changes not staged for commit:");
            printChanges(status.getUnstaged());
        }

        if (!status.getUntracked().isEmpty()) {
            clean = false;
            System.out.println();
            System.out.println("Untracked files:");
            for (String path : status.getUntracked()) {
                System.out.println("        " + path);
            }
        }

        if (clean) {
            System.out.println();
            System.out.println(head == null
                ? "nothing to commit (create/copy files and use \"minigit add\" to track)"
                : "nothing to commit, working tree clean");
        }
    }

    private void printChanges(Map<String, Status.Change> changes) {
        for (Map.Entry<String, Status.Change> change : changes.entrySet()) {
            String label = change.getValue().getLabel() + ":";
            System.out.println("        " + String.format("%-12s", label) + change.getKey());
        }
    }
}
