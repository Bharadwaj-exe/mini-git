package minigit.cli;

import java.io.IOException;

import minigit.exceptions.MiniGitException;
import minigit.objects.ObjectStore;
import minigit.objects.StoredObject;
import minigit.repository.Repository;

// minigit cat-file (-t | -s | -p) <object>
public class CatFileCommand implements Command {

    @Override
    public void execute(String[] args) {
        if (args.length != 3) {
            throw new MiniGitException("usage: minigit cat-file (-t | -s | -p) <object>");
        }

        ObjectStore store = new ObjectStore(Repository.findFromCurrentDirectory());
        StoredObject object = store.read(args[2]);

        switch (args[1]) {
            case "-t":
                System.out.println(object.getType());
                break;
            case "-s":
                System.out.println(object.getContent().length);
                break;
            case "-p":
                printContent(object);
                break;
            default:
                throw new MiniGitException("unknown option " + args[1]);
        }
    }

    private void printContent(StoredObject object) {
        try {
            System.out.write(object.getContent());
            System.out.flush();
        } catch (IOException e) {
            throw new MiniGitException("could not write output", e);
        }
    }
}
