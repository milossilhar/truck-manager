package cz.muni.fi.sdipr.api.exceptions;

/**
 * Exception for indicating wrong WAMP message format
 * @author milossilhar
 */
public class WampMessageFormatException extends RuntimeException {

    public WampMessageFormatException() {
        super();
    }

    public WampMessageFormatException(String message) {
        super(message);
    }

    public WampMessageFormatException(Throwable cause) {
        super(cause);
    }

    public WampMessageFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
