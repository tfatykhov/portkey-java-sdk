package ai.portkey.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thrown when the Portkey API returns a non-2xx response.
 *
 * <p>Provides both raw response body and parsed error fields
 * (message, type, code) from the standard OpenAI/Portkey error format.
 */
public class PortkeyException extends RuntimeException {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int statusCode;
    private final String responseBody;
    private final String errorMessage;
    private final String errorType;
    private final String errorCode;

    public PortkeyException(int statusCode, String responseBody) {
        super("Portkey API error %d: %s".formatted(statusCode, responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;

        // Parse structured error fields
        String msg = null, type = null, code = null;
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(responseBody);
                JsonNode error = root.path("error");
                if (!error.isMissingNode()) {
                    msg = error.path("message").asText(null);
                    type = error.path("type").asText(null);
                    code = error.path("code").asText(null);
                }
            } catch (Exception ignored) {
                // Not JSON or unexpected structure - leave fields null
            }
        }
        this.errorMessage = msg;
        this.errorType = type;
        this.errorCode = code;
    }

    public PortkeyException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
        this.errorMessage = null;
        this.errorType = null;
        this.errorCode = null;
    }

    /** HTTP status code, or -1 for connection/transport errors. */
    public int getStatusCode() { return statusCode; }

    /** Raw response body string. */
    public String getResponseBody() { return responseBody; }

    /** Parsed error message from {@code error.message}, or null. */
    public String getErrorMessage() { return errorMessage; }

    /** Parsed error type from {@code error.type} (e.g. "auth_error", "rate_limit_error"), or null. */
    public String getErrorType() { return errorType; }

    /** Parsed error code from {@code error.code}, or null. */
    public String getErrorCode() { return errorCode; }
}
