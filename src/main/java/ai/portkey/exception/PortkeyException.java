package ai.portkey.exception;

/**
 * Thrown when the Portkey API returns a non-2xx response.
 */
public class PortkeyException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public PortkeyException(int statusCode, String responseBody) {
        super("Portkey API error " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public PortkeyException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    public int getStatusCode() { return statusCode; }
    public String getResponseBody() { return responseBody; }
}
