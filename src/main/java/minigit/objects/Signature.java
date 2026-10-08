package minigit.objects;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import minigit.exceptions.MiniGitException;
import minigit.filesystem.FileUtils;

// Who made a commit and when, written as "Name <email> <epoch seconds> <+hhmm>".
public class Signature {

    private static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);

    private final String name;
    private final String email;
    private final long epochSeconds;
    private final ZoneOffset offset;

    public Signature(String name, String email, long epochSeconds, ZoneOffset offset) {
        this.name = name;
        this.email = email;
        this.epochSeconds = epochSeconds;
        this.offset = offset;
    }

    // The current user, taken from MINIGIT_AUTHOR_NAME / MINIGIT_AUTHOR_EMAIL, then the
    // [user] section of ~/.gitconfig, then the operating system user name.
    public static Signature now() {
        String name = System.getenv("MINIGIT_AUTHOR_NAME");
        String email = System.getenv("MINIGIT_AUTHOR_EMAIL");

        if (name == null || email == null) {
            String[] gitIdentity = readGitConfigIdentity();
            if (name == null) {
                name = gitIdentity[0];
            }
            if (email == null) {
                email = gitIdentity[1];
            }
        }
        if (name == null) {
            name = System.getProperty("user.name", "unknown");
        }
        if (email == null) {
            email = name.replace(' ', '.').toLowerCase() + "@localhost";
        }

        ZonedDateTime now = ZonedDateTime.now();
        return new Signature(name, email, now.toEpochSecond(), now.getOffset());
    }

    public static Signature parse(String value) {
        int open = value.lastIndexOf('<');
        int close = value.lastIndexOf('>');
        if (open < 0 || close < open) {
            throw new MiniGitException("malformed signature: " + value);
        }
        String[] time = value.substring(close + 1).trim().split(" ");
        if (time.length != 2) {
            throw new MiniGitException("malformed signature: " + value);
        }
        return new Signature(
            value.substring(0, open).trim(),
            value.substring(open + 1, close),
            Long.parseLong(time[0]),
            ZoneOffset.of(time[1].substring(0, 3) + ":" + time[1].substring(3))
        );
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String formatDate() {
        return DISPLAY_FORMAT.format(Instant.ofEpochSecond(epochSeconds).atOffset(offset));
    }

    @Override
    public String toString() {
        return name + " <" + email + "> " + epochSeconds + " " + formatOffset();
    }

    private String formatOffset() {
        int totalMinutes = offset.getTotalSeconds() / 60;
        char sign = totalMinutes < 0 ? '-' : '+';
        totalMinutes = Math.abs(totalMinutes);
        return String.format("%c%02d%02d", sign, totalMinutes / 60, totalMinutes % 60);
    }

    private static String[] readGitConfigIdentity() {
        String[] identity = new String[2];
        Path gitConfig = Path.of(System.getProperty("user.home"), ".gitconfig");
        if (!Files.isRegularFile(gitConfig)) {
            return identity;
        }

        boolean inUserSection = false;
        for (String line : FileUtils.readString(gitConfig).split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[")) {
                inUserSection = trimmed.equalsIgnoreCase("[user]");
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (!inUserSection || equals < 0) {
                continue;
            }
            String key = trimmed.substring(0, equals).trim().toLowerCase();
            String value = trimmed.substring(equals + 1).trim();
            if (key.equals("name")) {
                identity[0] = value;
            } else if (key.equals("email")) {
                identity[1] = value;
            }
        }
        return identity;
    }
}
