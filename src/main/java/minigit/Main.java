package minigit;

import minigit.cli.Command;
import minigit.cli.InitCommand;
public class Main {
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("MiniGit: No command Specified.");
            return;
        }
        String command = args[0];

        if (command.equals("init")) {
            Command initCommand = new InitCommand();
            initCommand.execute(args);
        }

    }
}
