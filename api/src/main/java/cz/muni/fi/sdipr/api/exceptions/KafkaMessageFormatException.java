package cz.muni.fi.sdipr.api.exceptions;

/**
 * Exception for indicating wrong Kafka message format
 * @author milossilhar
 */
public class KafkaMessageFormatException extends RuntimeException {

    public KafkaMessageFormatException() {
        super();
    }

    public KafkaMessageFormatException(String message) {
        super(message);
    }

    public KafkaMessageFormatException(Throwable cause) {
        super(cause);
    }

    public KafkaMessageFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
