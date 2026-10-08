package minigit.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;

public class Repository {

    public static final String MINIGIT_DIRECTORY_NAME = ".minigit";
    public static final String DEFAULT_BRANCH = "main";

    private static final String HEAD_REF_PREFIX = "ref: refs/heads/";
    private static final Pattern VALID_BRANCH_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9._-]*");

    private final Path rootDirectory;
    private final Path miniGitDirectory;

    public Repository(Path rootDirectory) {
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        this.miniGitDirectory = this.rootDirectory.resolve(MINIGIT_DIRECTORY_NAME);
    }

    // Walks up from the given directory until it finds one containing .minigit.
    public static Repository find(Path startDirectory) {
        Path current = startDirectory.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve(MINIGIT_DIRECTORY_NAME))) {
                return new Repository(current);
            }
            current = current.getParent();
        }
        throw new MiniGitException(
            "not a minigit repository (or any of the parent directories): " + MINIGIT_DIRECTORY_NAME
        );
    }

    public static Repository findFromCurrentDirectory() {
        return find(Path.of(""));
    }

    public boolean exists() {
        return Files.isDirectory(miniGitDirectory);
    }

    public void init() {
        if (exists()) {
            throw new MiniGitException("repository already exists in " + miniGitDirectory);
        }
        FileUtils.createDirectories(getObjectsDirectory());
        FileUtils.createDirectories(getHeadsDirectory());
        FileUtils.writeString(getHeadFile(), HEAD_REF_PREFIX + DEFAULT_BRANCH + "\n");
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public Path getMiniGitDirectory() {
        return miniGitDirectory;
    }

    public Path getObjectsDirectory() {
        return miniGitDirectory.resolve("objects");
    }

    public Path getHeadsDirectory() {
        return miniGitDirectory.resolve("refs").resolve("heads");
    }

    public Path getHeadFile() {
        return miniGitDirectory.resolve("HEAD");
    }

    public Path getIndexFile() {
        return miniGitDirectory.resolve("index");
    }

    // The branch HEAD points at, or null when HEAD is detached at a commit.
    public String getCurrentBranch() {
        String head = FileUtils.readString(getHeadFile()).trim();
        if (head.startsWith(HEAD_REF_PREFIX)) {
            return head.substring(HEAD_REF_PREFIX.length());
        }
        return null;
    }

    // The commit HEAD resolves to, or null on a branch that has no commits yet.
    public String getHeadCommit() {
        String branch = getCurrentBranch();
        if (branch != null) {
            return readBranch(branch);
        }
        return FileUtils.readString(getHeadFile()).trim();
    }

    // Moves the current branch (or the detached HEAD) to a new commit.
    public void updateHead(String commitHash) {
        String branch = getCurrentBranch();
        if (branch != null) {
            writeBranch(branch, commitHash);
        } else {
            FileUtils.writeString(getHeadFile(), commitHash + "\n");
        }
    }

    public String readBranch(String name) {
        Path file = getHeadsDirectory().resolve(name);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return FileUtils.readString(file).trim();
    }

    public void writeBranch(String name, String commitHash) {
        FileUtils.writeString(getHeadsDirectory().resolve(name), commitHash + "\n");
    }

    public boolean branchExists(String name) {
        return readBranch(name) != null;
    }

    public void deleteBranch(String name) {
        try {
            Files.delete(getHeadsDirectory().resolve(name));
        } catch (IOException e) {
            throw new MiniGitException("could not delete branch '" + name + "'", e);
        }
    }

    public List<String> listBranches() {
        List<String> branches = new ArrayList<>();
        if (!Files.isDirectory(getHeadsDirectory())) {
            return branches;
        }
        try (Stream<Path> files = Files.list(getHeadsDirectory())) {
            files.filter(Files::isRegularFile)
                .map(file -> file.getFileName().toString())
                .sorted()
                .forEach(branches::add);
        } catch (IOException e) {
            throw new MiniGitException("could not list branches", e);
        }
        return branches;
    }

    public void setHeadToBranch(String name) {
        FileUtils.writeString(getHeadFile(), HEAD_REF_PREFIX + name + "\n");
    }

    public void setHeadDetached(String commitHash) {
        FileUtils.writeString(getHeadFile(), commitHash + "\n");
    }

    public static void validateBranchName(String name) {
        if (!VALID_BRANCH_NAME.matcher(name).matches()) {
            throw new MiniGitException("'" + name + "' is not a valid branch name");
        }
    }
}
