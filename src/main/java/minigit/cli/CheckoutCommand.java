package minigit.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;
import minigit.objects.Blob;
import minigit.objects.Commit;
import minigit.objects.ObjectStore;
import minigit.objects.Tree;
import minigit.repository.Index;
import minigit.repository.Repository;
import minigit.repository.Status;
import minigit.repository.WorkingTree;

// minigit checkout <branch>        switch to a branch
// minigit checkout <commit>        detach HEAD at a commit
// minigit checkout -b <name>       create a branch at HEAD and switch to it
public class CheckoutCommand implements Command {

    @Override
    public void execute(String[] args) {
        Repository repository = Repository.findFromCurrentDirectory();

        if (args.length == 3 && args[1].equals("-b")) {
            createAndSwitch(repository, args[2]);
        } else if (args.length == 2 && !args[1].startsWith("-")) {
            checkout(repository, args[1]);
        } else {
            throw new MiniGitException("usage: minigit checkout [-b] <branch | commit>");
        }
    }

    private void createAndSwitch(Repository repository, String name) {
        Repository.validateBranchName(name);
        if (repository.branchExists(name)) {
            throw new MiniGitException("a branch named '" + name + "' already exists");
        }
        String head = repository.getHeadCommit();
        if (head != null) {
            repository.writeBranch(name, head);
        }
        repository.setHeadToBranch(name);
        System.out.println("Switched to a new branch '" + name + "'");
    }

    private void checkout(Repository repository, String revision) {
        ObjectStore store = new ObjectStore(repository);
        boolean isBranch = repository.branchExists(revision);

        if (isBranch && revision.equals(repository.getCurrentBranch())) {
            System.out.println("Already on '" + revision + "'");
            return;
        }

        String targetCommit = store.resolveCommit(repository, revision);
        if (!Status.compute(repository).hasNoTrackedChanges()) {
            throw new MiniGitException(
                "you have uncommitted changes; commit them before switching (see \"minigit status\")"
            );
        }

        WorkingTree workingTree = new WorkingTree(repository);
        SortedMap<String, String> currentFiles = Status.headFiles(repository, store);
        SortedMap<String, String> targetFiles = Tree.flatten(Commit.read(store, targetCommit).getTreeHash(), store);

        refuseToOverwriteUntrackedFiles(workingTree, currentFiles, targetFiles);
        updateWorkingTree(repository, store, workingTree, currentFiles, targetFiles);

        Index index = Index.load(repository);
        index.replaceAll(targetFiles);
        index.save();

        if (isBranch) {
            repository.setHeadToBranch(revision);
            System.out.println("Switched to branch '" + revision + "'");
        } else {
            repository.setHeadDetached(targetCommit);
            System.out.println(
                "HEAD is now at " + targetCommit.substring(0, 7) + " " + Commit.read(store, targetCommit).getSummary()
            );
        }
    }

    private void refuseToOverwriteUntrackedFiles(
        WorkingTree workingTree,
        Map<String, String> currentFiles,
        Map<String, String> targetFiles
    ) {
        for (Map.Entry<String, String> target : targetFiles.entrySet()) {
            if (currentFiles.containsKey(target.getKey())) {
                continue;
            }
            Path file = workingTree.toAbsolutePath(target.getKey());
            if (Files.isRegularFile(file)
                && !new Blob(FileUtils.readBytes(file)).getHash().equals(target.getValue())) {
                throw new MiniGitException(
                    "untracked file '" + target.getKey() + "' would be overwritten by checkout; move or remove it first"
                );
            }
        }
    }

    private void updateWorkingTree(
        Repository repository,
        ObjectStore store,
        WorkingTree workingTree,
        Map<String, String> currentFiles,
        Map<String, String> targetFiles
    ) {
        for (String path : currentFiles.keySet()) {
            if (!targetFiles.containsKey(path)) {
                FileUtils.deleteFileAndEmptyParents(workingTree.toAbsolutePath(path), repository.getRootDirectory());
            }
        }
        for (Map.Entry<String, String> target : targetFiles.entrySet()) {
            if (!target.getValue().equals(currentFiles.get(target.getKey()))) {
                byte[] data = store.read(target.getValue(), Blob.TYPE).getContent();
                FileUtils.writeBytes(workingTree.toAbsolutePath(target.getKey()), data);
            }
        }
    }
}
