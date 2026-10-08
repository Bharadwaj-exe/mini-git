package minigit.repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import minigit.filesystem.FileUtils;
import minigit.objects.Blob;
import minigit.objects.Commit;
import minigit.objects.ObjectStore;
import minigit.objects.Tree;

// Compares the three versions of every file: the last commit (HEAD), the index, and the disk.
public class Status {

    public enum Change {
        NEW_FILE("new file"),
        MODIFIED("modified"),
        DELETED("deleted");

        private final String label;

        Change(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final SortedMap<String, Change> staged = new TreeMap<>();
    private final SortedMap<String, Change> unstaged = new TreeMap<>();
    private final SortedSet<String> untracked = new TreeSet<>();

    private Status() {
    }

    public static Status compute(Repository repository) {
        ObjectStore store = new ObjectStore(repository);
        WorkingTree workingTree = new WorkingTree(repository);
        Map<String, String> indexed = Index.load(repository).getEntries();
        Map<String, String> committed = headFiles(repository, store);

        Status status = new Status();

        // HEAD vs index: what the next commit would change.
        for (Map.Entry<String, String> entry : indexed.entrySet()) {
            String committedHash = committed.get(entry.getKey());
            if (committedHash == null) {
                status.staged.put(entry.getKey(), Change.NEW_FILE);
            } else if (!committedHash.equals(entry.getValue())) {
                status.staged.put(entry.getKey(), Change.MODIFIED);
            }
        }
        for (String path : committed.keySet()) {
            if (!indexed.containsKey(path)) {
                status.staged.put(path, Change.DELETED);
            }
        }

        // Index vs disk: edits that have not been added yet.
        for (Map.Entry<String, String> entry : indexed.entrySet()) {
            Path file = workingTree.toAbsolutePath(entry.getKey());
            if (!Files.isRegularFile(file)) {
                status.unstaged.put(entry.getKey(), Change.DELETED);
            } else if (!new Blob(FileUtils.readBytes(file)).getHash().equals(entry.getValue())) {
                status.unstaged.put(entry.getKey(), Change.MODIFIED);
            }
        }

        for (String path : workingTree.listFiles()) {
            if (!indexed.containsKey(path)) {
                status.untracked.add(path);
            }
        }
        return status;
    }

    // Every file in the commit HEAD points at, or nothing if there are no commits yet.
    public static SortedMap<String, String> headFiles(Repository repository, ObjectStore store) {
        String head = repository.getHeadCommit();
        if (head == null) {
            return new TreeMap<>();
        }
        return Tree.flatten(Commit.read(store, head).getTreeHash(), store);
    }

    public SortedMap<String, Change> getStaged() {
        return staged;
    }

    public SortedMap<String, Change> getUnstaged() {
        return unstaged;
    }

    public SortedSet<String> getUntracked() {
        return untracked;
    }

    // True when the index and the tracked files on disk both match HEAD.
    public boolean hasNoTrackedChanges() {
        return staged.isEmpty() && unstaged.isEmpty();
    }
}
