package ai.portkey.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PortkeyException} structured error parsing.
 */
class PortkeyExceptionTest {

    @Test
    void parsesStandardErrorFormat() {
        var ex = new PortkeyException(400,
                """
                {"error":{"message":"Invalid model","type":"invalid_request_error","code":"model_not_found"}}
                """);

        assertEquals(400, ex.getStatusCode());
        assertEquals("Invalid model", ex.getErrorMessage());
        assertEquals("invalid_request_error", ex.getErrorType());
        assertEquals("model_not_found", ex.getErrorCode());
        assertNotNull(ex.getResponseBody());
        assertTrue(ex.getMessage().contains("400"));
    }

    @Test
    void parsesErrorWithMissingFields() {
        var ex = new PortkeyException(500,
                """
                {"error":{"message":"Something broke"}}
                """);

        assertEquals(500, ex.getStatusCode());
        assertEquals("Something broke", ex.getErrorMessage());
        assertNull(ex.getErrorType());
        assertNull(ex.getErrorCode());
    }

    @Test
    void handlesNonJsonBody() {
        var ex = new PortkeyException(502, "Bad Gateway");

        assertEquals(502, ex.getStatusCode());
        assertEquals("Bad Gateway", ex.getResponseBody());
        assertNull(ex.getErrorMessage());
        assertNull(ex.getErrorType());
        assertNull(ex.getErrorCode());
    }

    @Test
    void handlesEmptyBody() {
        var ex = new PortkeyException(504, "");

        assertEquals(504, ex.getStatusCode());
        assertNull(ex.getErrorMessage());
        assertNull(ex.getErrorType());
        assertNull(ex.getErrorCode());
    }

    @Test
    void handlesNullBody() {
        var ex = new PortkeyException(500, null);

        assertEquals(500, ex.getStatusCode());
        assertNull(ex.getResponseBody());
        assertNull(ex.getErrorMessage());
    }

    @Test
    void handlesBlankBody() {
        var ex = new PortkeyException(500, "   ");

        assertEquals(500, ex.getStatusCode());
        assertNull(ex.getErrorMessage());
    }

    @Test
    void handlesJsonWithoutErrorKey() {
        var ex = new PortkeyException(400, """
                {"detail":"Not found","status":404}
                """);

        assertEquals(400, ex.getStatusCode());
        assertNull(ex.getErrorMessage());
        assertNull(ex.getErrorType());
    }

    @Test
    void handlesNestedJsonStructure() {
        var ex = new PortkeyException(422, """
                {"error":{"message":"Validation failed","type":"validation_error","code":"invalid_params","param":"temperature","details":{"min":0,"max":2}}}
                """);

        assertEquals(422, ex.getStatusCode());
        assertEquals("Validation failed", ex.getErrorMessage());
        assertEquals("validation_error", ex.getErrorType());
        assertEquals("invalid_params", ex.getErrorCode());
    }

    @Test
    void causeConstructorSetsDefaults() {
        var cause = new RuntimeException("connection refused");
        var ex = new PortkeyException("Request failed: connection refused", cause);

        assertEquals(-1, ex.getStatusCode());
        assertNull(ex.getResponseBody());
        assertNull(ex.getErrorMessage());
        assertNull(ex.getErrorType());
        assertNull(ex.getErrorCode());
        assertEquals(cause, ex.getCause());
        assertTrue(ex.getMessage().contains("connection refused"));
    }

    @Test
    void exceptionMessageIncludesStatusAndBody() {
        var ex = new PortkeyException(429, """
                {"error":{"message":"Rate limit exceeded"}}
                """);

        assertTrue(ex.getMessage().contains("429"));
        assertTrue(ex.getMessage().contains("Rate limit"));
    }
}
