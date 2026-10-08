package minigit.objects;

// An object read back from the object store: its type and its content without the header.
public class StoredObject {

    private final String hash;
    private final String type;
    private final byte[] content;

    public StoredObject(String hash, String type, byte[] content) {
        this.hash = hash;
        this.type = type;
        this.content = content;
    }

    public String getHash() {
        return hash;
    }

    public String getType() {
        return type;
    }

    public byte[] getContent() {
        return content;
    }
}
