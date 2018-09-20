package net.robinfriedli.jxp.exceptions;

public class CommitException extends Exception {

    public CommitException() {
        super();
    }

    public CommitException(String message) {
        super(message);
    }

    public CommitException(String message, Throwable cause) {
        super(message, cause);
    }

}
