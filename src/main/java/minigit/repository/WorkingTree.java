package minigit.repository;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;

// The files on disk that MiniGit can see, after applying .minigitignore.
//
// .minigitignore holds one pattern per line ("#" starts a comment):
//   name      ignores any file or directory with that name, at any depth
//   dir/sub   ignores that path (and everything under it) relative to the root
//   *.class   glob patterns are matched against each file or directory name
public class WorkingTree {

    public static final String IGNORE_FILE_NAME = ".minigitignore";

    private static final String[] ALWAYS_IGNORED = {Repository.MINIGIT_DIRECTORY_NAME, ".git"};

    private final Repository repository;
    private final List<String> namePatterns = new ArrayList<>();
    private final List<String> pathPatterns = new ArrayList<>();
    private final List<PathMatcher> globPatterns = new ArrayList<>();

    public WorkingTree(Repository repository) {
        this.repository = repository;
        for (String name : ALWAYS_IGNORED) {
            namePatterns.add(name);
        }
        loadIgnoreFile();
    }

    // Every non-ignored file under the given directory, as sorted repository-relative paths.
    public SortedSet<String> listFiles(Path directory) {
        SortedSet<String> files = new TreeSet<>();
        if (!Files.isDirectory(directory)) {
            return files;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(repository.getRootDirectory()) && isIgnored(toRelativePath(dir))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String relativePath = toRelativePath(file);
                    if (attrs.isRegularFile() && !isIgnored(relativePath)) {
                        files.add(relativePath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new MiniGitException("could not list files in " + directory, e);
        }
        return files;
    }

    public SortedSet<String> listFiles() {
        return listFiles(repository.getRootDirectory());
    }

    public boolean isIgnored(String relativePath) {
        String[] segments = relativePath.split("/");
        for (String segment : segments) {
            if (namePatterns.contains(segment)) {
                return true;
            }
            for (PathMatcher glob : globPatterns) {
                if (glob.matches(Path.of(segment))) {
                    return true;
                }
            }
        }
        for (String pattern : pathPatterns) {
            if (relativePath.equals(pattern) || relativePath.startsWith(pattern + "/")) {
                return true;
            }
        }
        return false;
    }

    // Converts an absolute or working-directory-relative path to "dir/file.txt" form.
    public String toRelativePath(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        if (!absolute.startsWith(repository.getRootDirectory())) {
            throw new MiniGitException("'" + path + "' is outside repository at " + repository.getRootDirectory());
        }
        return repository.getRootDirectory().relativize(absolute).toString().replace('\\', '/');
    }

    public Path toAbsolutePath(String relativePath) {
        return repository.getRootDirectory().resolve(relativePath);
    }

    private void loadIgnoreFile() {
        Path ignoreFile = repository.getRootDirectory().resolve(IGNORE_FILE_NAME);
        if (!Files.isRegularFile(ignoreFile)) {
            return;
        }
        for (String line : FileUtils.readString(ignoreFile).split("\r?\n")) {
            String pattern = line.trim();
            if (pattern.isEmpty() || pattern.startsWith("#")) {
                continue;
            }
            while (pattern.endsWith("/")) {
                pattern = pattern.substring(0, pattern.length() - 1);
            }
            while (pattern.startsWith("/")) {
                pattern = pattern.substring(1);
            }
            if (pattern.isEmpty()) {
                continue;
            }

            if (pattern.contains("*") || pattern.contains("?")) {
                globPatterns.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
            } else if (pattern.contains("/")) {
                pathPatterns.add(pattern);
            } else {
                namePatterns.add(pattern);
            }
        }
    }
}
