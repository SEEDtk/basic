/**
 *
 */
package org.theseed.basic;

/**
 * This is a subclass of Exception for command-line parsing failures.
 *
 * @author Bruce Parrello
 */
public class ParseFailureException extends Exception {

    /** serialization ID */
    private static final long serialVersionUID = 1312545598054781268L;

    public ParseFailureException() {
        super();
    }

    public ParseFailureException(String message) {
        super(message);
    }

    public ParseFailureException(Throwable cause) {
        super(cause);
    }

    public ParseFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseFailureException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
