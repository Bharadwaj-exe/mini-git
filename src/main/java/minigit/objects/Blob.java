package minigit.objects;

// A blob is the raw contents of a single file.
public class Blob extends GitObject {

    public static final String TYPE = "blob";

    private final byte[] data;

    public Blob(byte[] data) {
        this.data = data;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public byte[] getContent() {
        return data;
    }
}
