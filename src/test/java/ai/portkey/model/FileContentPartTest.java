package ai.portkey.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FileContentPart} - PDF and file content support.
 */
class FileContentPartTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // -- factory methods --

    @Test
    void fileFactoryMethod() {
        var part = ContentPart.file("application/pdf", "base64data");
        assertInstanceOf(FileContentPart.class, part);
        assertEquals("file", part.type());
        assertEquals("application/pdf", part.file().mimeType());
        assertEquals("base64data", part.file().fileData());
    }

    @Test
    void pdfFactoryMethod() {
        var part = ContentPart.pdf("base64pdfdata");
        assertInstanceOf(FileContentPart.class, part);
        assertEquals("file", part.type());
        assertEquals("application/pdf", part.file().mimeType());
        assertEquals("base64pdfdata", part.file().fileData());
    }

    @Test
    void fileBytesFactoryMethod() {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        var part = ContentPart.fileBytes("text/plain", data);
        assertInstanceOf(FileContentPart.class, part);
        assertEquals("file", part.type());
        assertEquals("text/plain", part.file().mimeType());

        // Verify base64 encoding
        var decoded = Base64.getDecoder().decode(part.file().fileData());
        assertEquals("hello world", new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    void plainTextFile() {
        var part = ContentPart.file("text/plain", "This is a plain text file");
        assertEquals("text/plain", part.file().mimeType());
        assertEquals("This is a plain text file", part.file().fileData());
    }

    @Test
    void csvFile() {
        var part = ContentPart.file("text/csv", "name,age\nAlice,30\nBob,25");
        assertEquals("text/csv", part.file().mimeType());
    }

    // -- serialization --

    @Test
    void serializesToCorrectJson() throws Exception {
        var part = ContentPart.file("application/pdf", "JVBERi0xLjQ=");
        var json = mapper.writeValueAsString(part);

        assertTrue(json.contains("\"type\":\"file\""));
        assertTrue(json.contains("\"mime_type\":\"application/pdf\""));
        assertTrue(json.contains("\"file_data\":\"JVBERi0xLjQ=\""));
    }

    @Test
    void serializesInMessage() throws Exception {
        var msg = Message.user(List.of(
                ContentPart.text("What are the key findings in this document?"),
                ContentPart.pdf("JVBERi0xLjQ=")
        ));

        var json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"type\":\"text\""));
        assertTrue(json.contains("\"type\":\"file\""));
        assertTrue(json.contains("\"mime_type\":\"application/pdf\""));
        assertTrue(json.contains("\"file_data\":\"JVBERi0xLjQ=\""));
    }

    // -- deserialization (round-trip) --

    @Test
    void roundTripPdfMessage() throws Exception {
        var original = Message.user(List.of(
                ContentPart.text("Summarize this PDF"),
                ContentPart.pdf("JVBERi0xLjQ=")
        ));

        var json = mapper.writeValueAsString(original);
        var deserialized = mapper.readValue(json, Message.class);

        assertEquals("user", deserialized.getRole());
        var parts = deserialized.getContentAsParts();
        assertNotNull(parts);
        assertEquals(2, parts.size());

        assertInstanceOf(TextContentPart.class, parts.get(0));
        assertEquals("Summarize this PDF", ((TextContentPart) parts.get(0)).text());

        assertInstanceOf(FileContentPart.class, parts.get(1));
        var filePart = (FileContentPart) parts.get(1);
        assertEquals("file", filePart.type());
        assertEquals("application/pdf", filePart.file().mimeType());
        assertEquals("JVBERi0xLjQ=", filePart.file().fileData());
    }

    @Test
    void roundTripPlainTextFile() throws Exception {
        var original = Message.user(List.of(
                ContentPart.text("What does this file say?"),
                ContentPart.file("text/plain", "Hello from a text file")
        ));

        var json = mapper.writeValueAsString(original);
        var deserialized = mapper.readValue(json, Message.class);

        var parts = deserialized.getContentAsParts();
        assertNotNull(parts);
        assertEquals(2, parts.size());

        var filePart = (FileContentPart) parts.get(1);
        assertEquals("text/plain", filePart.file().mimeType());
        assertEquals("Hello from a text file", filePart.file().fileData());
    }

    @Test
    void deserializeFromApiJson() throws Exception {
        var json = """
                {
                  "role": "user",
                  "content": [
                    {"type": "text", "text": "Analyze this document"},
                    {"type": "file", "file": {"mime_type": "application/pdf", "file_data": "JVBERi0xLjQ="}}
                  ]
                }
                """;

        var msg = mapper.readValue(json, Message.class);

        var parts = msg.getContentAsParts();
        assertNotNull(parts);
        assertEquals(2, parts.size());

        assertInstanceOf(TextContentPart.class, parts.get(0));
        assertInstanceOf(FileContentPart.class, parts.get(1));

        var filePart = (FileContentPart) parts.get(1);
        assertEquals("application/pdf", filePart.file().mimeType());
        assertEquals("JVBERi0xLjQ=", filePart.file().fileData());
    }

    @Test
    void mixedContentPartsWithFile() throws Exception {
        var json = """
                {
                  "role": "user",
                  "content": [
                    {"type": "text", "text": "Compare these"},
                    {"type": "image_url", "image_url": {"url": "https://example.com/chart.png"}},
                    {"type": "file", "file": {"mime_type": "application/pdf", "file_data": "JVBERi0="}}
                  ]
                }
                """;

        var msg = mapper.readValue(json, Message.class);
        var parts = msg.getContentAsParts();
        assertNotNull(parts);
        assertEquals(3, parts.size());

        assertInstanceOf(TextContentPart.class, parts.get(0));
        assertInstanceOf(ImageContentPart.class, parts.get(1));
        assertInstanceOf(FileContentPart.class, parts.get(2));
    }

    // -- pattern matching --

    @Test
    void patternMatchingWorksForFile() {
        ContentPart part = ContentPart.pdf("data");
        var result = switch (part) {
            case TextContentPart t -> "text";
            case ImageContentPart i -> "image";
            case FileContentPart f -> "file:" + f.file().mimeType();
        };
        assertEquals("file:application/pdf", result);
    }
}
