package minigit.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;
import minigit.objects.Blob;
import minigit.objects.ObjectStore;
import minigit.repository.Index;
import minigit.repository.Repository;
import minigit.repository.WorkingTree;

// minigit add <path>...
// Stages files (or every file under a directory). Tracked files that were deleted
// from disk are staged as deletions.
public class AddCommand implements Command {

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            throw new MiniGitException("usage: minigit add <path>...");
        }

        Repository repository = Repository.findFromCurrentDirectory();
        ObjectStore store = new ObjectStore(repository);
        WorkingTree workingTree = new WorkingTree(repository);
        Index index = Index.load(repository);

        for (int i = 1; i < args.length; i++) {
            Path path = Path.of(args[i]).toAbsolutePath().normalize();
            String relativePath = workingTree.toRelativePath(path);

            if (Files.isDirectory(path)) {
                for (String file : workingTree.listFiles(path)) {
                    stageFile(file, workingTree, store, index);
                }
                for (String tracked : trackedUnder(relativePath, index)) {
                    if (!Files.exists(workingTree.toAbsolutePath(tracked))) {
                        index.remove(tracked);
                    }
                }
            } else if (Files.isRegularFile(path)) {
                if (workingTree.isIgnored(relativePath)) {
                    throw new MiniGitException("'" + args[i] + "' is ignored by " + WorkingTree.IGNORE_FILE_NAME);
                }
                stageFile(relativePath, workingTree, store, index);
            } else if (index.contains(relativePath)) {
                index.remove(relativePath);
            } else {
                throw new MiniGitException("pathspec '" + args[i] + "' did not match any files");
            }
        }

        index.save();
    }

    private void stageFile(String relativePath, WorkingTree workingTree, ObjectStore store, Index index) {
        byte[] data = FileUtils.readBytes(workingTree.toAbsolutePath(relativePath));
        index.stage(relativePath, store.write(new Blob(data)));
    }

    private List<String> trackedUnder(String directory, Index index) {
        List<String> tracked = new ArrayList<>();
        for (String path : index.getEntries().keySet()) {
            if (directory.isEmpty() || path.startsWith(directory + "/")) {
                tracked.add(path);
            }
        }
        return tracked;
    }
}
