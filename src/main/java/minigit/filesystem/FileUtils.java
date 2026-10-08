package minigit.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import minigit.exceptions.MiniGitException;

public final class FileUtils {

    private FileUtils() {
    }

    public static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new MiniGitException("could not read " + path, e);
        }
    }

    public static void writeBytes(Path path, byte[] data) {
        Path parent = path.getParent();
        if (parent != null) {
            createDirectories(parent);
        }
        try {
            Files.write(path, data);
        } catch (IOException e) {
            throw new MiniGitException("could not write " + path, e);
        }
    }

    public static String readString(Path path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

    public static void writeString(Path path, String content) {
        writeBytes(path, content.getBytes(StandardCharsets.UTF_8));
    }

    // Deletes a file, then any parent directories it leaves empty, stopping at root.
    public static void deleteFileAndEmptyParents(Path file, Path root) {
        try {
            Files.deleteIfExists(file);
            Path parent = file.getParent();
            while (parent != null && !parent.equals(root) && isEmptyDirectory(parent)) {
                Files.delete(parent);
                parent = parent.getParent();
            }
        } catch (IOException e) {
            throw new MiniGitException("could not delete " + file, e);
        }
    }

    private static boolean isEmptyDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> children = Files.list(directory)) {
            return children.findAny().isEmpty();
        }
    }

    public static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new MiniGitException("could not create directory " + path, e);
        }
    }
}
