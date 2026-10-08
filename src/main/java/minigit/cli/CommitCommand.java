package minigit.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import minigit.exceptions.MiniGitException;
import minigit.objects.Commit;
import minigit.objects.ObjectStore;
import minigit.objects.Signature;
import minigit.objects.Tree;
import minigit.repository.Index;
import minigit.repository.Repository;

// minigit commit -m <message> [-m <paragraph>...]
public class CommitCommand implements Command {

    @Override
    public void execute(String[] args) {
        String message = parseMessage(args);

        Repository repository = Repository.findFromCurrentDirectory();
        ObjectStore store = new ObjectStore(repository);
        Index index = Index.load(repository);
        String parent = repository.getHeadCommit();

        if (parent == null && index.getEntries().isEmpty()) {
            throw new MiniGitException("nothing to commit (use \"minigit add\" to track files)");
        }

        String treeHash = Tree.write(index.getEntries(), store);
        if (parent != null && Commit.read(store, parent).getTreeHash().equals(treeHash)) {
            throw new MiniGitException("nothing to commit, no changes staged since the last commit");
        }

        Signature signature = Signature.now();
        List<String> parents = parent == null ? Collections.emptyList() : List.of(parent);
        Commit commit = new Commit(treeHash, parents, signature, signature, message);
        String commitHash = store.write(commit);
        repository.updateHead(commitHash);

        String branch = repository.getCurrentBranch();
        String location = branch != null ? branch : "detached HEAD";
        String rootMarker = parent == null ? " (root-commit)" : "";
        System.out.println(
            "[" + location + rootMarker + " " + commitHash.substring(0, 7) + "] " + commit.getSummary()
        );
    }

    private String parseMessage(String[] args) {
        List<String> paragraphs = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-m") && i + 1 < args.length) {
                paragraphs.add(args[++i].trim());
            } else {
                throw new MiniGitException("usage: minigit commit -m <message>");
            }
        }
        String message = String.join("\n\n", paragraphs);
        if (message.isEmpty()) {
            throw new MiniGitException("aborting commit due to empty commit message (use -m <message>)");
        }
        return message;
    }
}
