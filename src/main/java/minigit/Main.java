package minigit;

import java.util.LinkedHashMap;
import java.util.Map;

import minigit.cli.CatFileCommand;
import minigit.cli.Command;
import minigit.cli.HashObjectCommand;
import minigit.cli.InitCommand;
import minigit.exceptions.MiniGitException;

public class Main {

    private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

    static {
        COMMANDS.put("init", new InitCommand());
        COMMANDS.put("hash-object", new HashObjectCommand());
        COMMANDS.put("cat-file", new CatFileCommand());
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("MiniGit: No command Specified.");
            printUsage();
            return;
        }

        Command command = COMMANDS.get(args[0]);
        if (command == null) {
            System.out.println("MiniGit: '" + args[0] + "' is not a minigit command.");
            printUsage();
            System.exit(1);
        }

        try {
            command.execute(args);
        } catch (MiniGitException e) {
            System.err.println("fatal: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("usage: minigit <command> [<args>]");
        System.out.println("commands: " + String.join(", ", COMMANDS.keySet()));
    }
}
