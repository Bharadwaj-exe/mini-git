package minigit.exceptions;

public class MiniGitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MiniGitException(String message) {
        super(message);
    }

    public MiniGitException(String message, Throwable cause) {
        super(message, cause);
    }
}
