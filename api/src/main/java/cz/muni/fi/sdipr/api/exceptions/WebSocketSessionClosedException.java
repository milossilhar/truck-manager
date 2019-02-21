package cz.muni.fi.sdipr.api.exceptions;

/**
 * Exception for indication that WebSocket session was closed before WAMP session was closed
 *
 * @author milossilhar
 */
public class WebSocketSessionClosedException extends RuntimeException {

    public WebSocketSessionClosedException() {
        super();
    }

    public WebSocketSessionClosedException(String message) {
        super(message);
    }

    public WebSocketSessionClosedException(Throwable cause) {
        super(cause);
    }

    public WebSocketSessionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
