package net.robinfriedli.jxp.exceptions;

public class PersistException extends RuntimeException {

    public PersistException() {
        super();
    }

    public PersistException(String message) {
        super(message);
    }

    public PersistException(Throwable cause) {
        super(cause);
    }

    public PersistException(String message, Throwable cause) {
        super(message, cause);
    }

}
