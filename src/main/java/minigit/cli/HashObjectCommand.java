package minigit.cli;

import java.nio.file.Files;
import java.nio.file.Path;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;
import minigit.objects.Blob;
import minigit.objects.ObjectStore;
import minigit.repository.Repository;

// minigit hash-object [-w] <file>
public class HashObjectCommand implements Command {

    @Override
    public void execute(String[] args) {
        boolean write = false;
        String fileName = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-w")) {
                write = true;
            } else {
                fileName = args[i];
            }
        }
        if (fileName == null) {
            throw new MiniGitException("usage: minigit hash-object [-w] <file>");
        }

        Path file = Path.of(fileName);
        if (!Files.isRegularFile(file)) {
            throw new MiniGitException("could not open '" + fileName + "' for reading");
        }

        Blob blob = new Blob(FileUtils.readBytes(file));
        if (write) {
            ObjectStore store = new ObjectStore(Repository.findFromCurrentDirectory());
            System.out.println(store.write(blob));
        } else {
            System.out.println(blob.getHash());
        }
    }
}
