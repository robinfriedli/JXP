package net.robinfriedli.jxp.exceptions;

/**
 * Thrown to indicate that a type could not be converted
 */
public class ConversionException extends RuntimeException {

    public ConversionException() {
        super();
    }

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversionException(Throwable cause) {
        super(cause);
    }
}
