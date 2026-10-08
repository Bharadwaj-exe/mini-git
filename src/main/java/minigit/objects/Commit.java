package minigit.objects;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import minigit.exceptions.MiniGitException;

// A commit points at the root tree of a snapshot, its parent commit(s), who made it and why.
// The text layout matches Git's commit format.
public class Commit extends GitObject {

    public static final String TYPE = "commit";

    private final String treeHash;
    private final List<String> parents;
    private final Signature author;
    private final Signature committer;
    private final String message;

    public Commit(String treeHash, List<String> parents, Signature author, Signature committer, String message) {
        this.treeHash = treeHash;
        this.parents = Collections.unmodifiableList(new ArrayList<>(parents));
        this.author = author;
        this.committer = committer;
        this.message = message.endsWith("\n") ? message : message + "\n";
    }

    public String getTreeHash() {
        return treeHash;
    }

    public List<String> getParents() {
        return parents;
    }

    public Signature getAuthor() {
        return author;
    }

    public Signature getCommitter() {
        return committer;
    }

    public String getMessage() {
        return message;
    }

    public String getSummary() {
        return message.split("\n", 2)[0];
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public byte[] getContent() {
        StringBuilder content = new StringBuilder();
        content.append("tree ").append(treeHash).append('\n');
        for (String parent : parents) {
            content.append("parent ").append(parent).append('\n');
        }
        content.append("author ").append(author).append('\n');
        content.append("committer ").append(committer).append('\n');
        content.append('\n');
        content.append(message);
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static Commit parse(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        int blankLine = text.indexOf("\n\n");
        if (blankLine < 0) {
            throw new MiniGitException("commit object is corrupt");
        }

        String treeHash = null;
        List<String> parents = new ArrayList<>();
        Signature author = null;
        Signature committer = null;

        for (String line : text.substring(0, blankLine).split("\n")) {
            int space = line.indexOf(' ');
            String key = line.substring(0, space);
            String value = line.substring(space + 1);
            switch (key) {
                case "tree":
                    treeHash = value;
                    break;
                case "parent":
                    parents.add(value);
                    break;
                case "author":
                    author = Signature.parse(value);
                    break;
                case "committer":
                    committer = Signature.parse(value);
                    break;
                default:
                    break;
            }
        }

        if (treeHash == null || author == null || committer == null) {
            throw new MiniGitException("commit object is missing required headers");
        }
        return new Commit(treeHash, parents, author, committer, text.substring(blankLine + 2));
    }

    public static Commit read(ObjectStore store, String hash) {
        return parse(store.read(hash, TYPE).getContent());
    }
}
