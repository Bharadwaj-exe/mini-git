package minigit.objects;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import minigit.hashing.HashUtils;

// Every object is stored as "<type> <content length>\0<content>", exactly like Git,
// so a MiniGit blob gets the same hash Git would give it.
public abstract class GitObject {

    public abstract String getType();

    public abstract byte[] getContent();

    public byte[] serialize() {
        byte[] content = getContent();
        byte[] header = (getType() + " " + content.length + "\0").getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream out = new ByteArrayOutputStream(header.length + content.length);
        out.writeBytes(header);
        out.writeBytes(content);
        return out.toByteArray();
    }

    public String getHash() {
        return HashUtils.sha1(serialize());
    }
}
