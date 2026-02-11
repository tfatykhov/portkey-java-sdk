package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Content part for multimodal messages.
 *
 * <p>Sealed hierarchy:
 * <ul>
 *   <li>{@link TextContentPart} - text content</li>
 *   <li>{@link ImageContentPart} - image URL content</li>
 *   <li>{@link FileContentPart} - file/document content (PDF, text, CSV, etc.)</li>
 * </ul>
 *
 * <p>Polymorphic deserialization is handled by {@link MessageContentDeserializer}
 * which reads the {@code type} discriminator field and maps to concrete types.
 * The {@code type()} accessor and each record's constructor ensure the field
 * is present in both serialization and deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface ContentPart permits TextContentPart, ImageContentPart, FileContentPart {

    String type();

    /**
     * Create a text content part.
     */
    static TextContentPart text(String text) {
        return new TextContentPart(text);
    }

    /**
     * Create an image content part from a URL.
     */
    static ImageContentPart imageUrl(String url) {
        return new ImageContentPart(url);
    }

    /**
     * Create an image content part with detail level.
     */
    static ImageContentPart imageUrl(String url, ImageContentPart.Detail detail) {
        return new ImageContentPart(url, detail);
    }

    /**
     * Create an image content part from base64-encoded data.
     *
     * <pre>{@code
     * ContentPart.imageBase64("image/png", base64Data)
     * // produces: "data:image/png;base64,..."
     * }</pre>
     *
     * @param mediaType MIME type (e.g. "image/png", "image/jpeg", "image/webp", "image/gif")
     * @param base64Data base64-encoded image bytes
     */
    static ImageContentPart imageBase64(String mediaType, String base64Data) {
        return new ImageContentPart("data:" + mediaType + ";base64," + base64Data);
    }

    /**
     * Create an image content part from base64-encoded data with detail level.
     */
    static ImageContentPart imageBase64(String mediaType, String base64Data, ImageContentPart.Detail detail) {
        return new ImageContentPart("data:" + mediaType + ";base64," + base64Data, detail);
    }

    /**
     * Create a file content part.
     *
     * <pre>{@code
     * // PDF
     * ContentPart.file("application/pdf", base64PdfData)
     *
     * // Plain text
     * ContentPart.file("text/plain", "This is a plain text file")
     * }</pre>
     *
     * @param mimeType MIME type (e.g. "application/pdf", "text/plain", "text/csv")
     * @param fileData base64-encoded file data, or plain text for text/* types
     */
    static FileContentPart file(String mimeType, String fileData) {
        return new FileContentPart(mimeType, fileData);
    }

    /**
     * Create a PDF file content part from base64-encoded data.
     *
     * @param base64Data base64-encoded PDF bytes
     */
    static FileContentPart pdf(String base64Data) {
        return new FileContentPart("application/pdf", base64Data);
    }

    /**
     * Create a file content part from raw bytes.
     *
     * <pre>{@code
     * byte[] pdfBytes = Files.readAllBytes(Path.of("document.pdf"));
     * ContentPart.fileBytes("application/pdf", pdfBytes)
     * }</pre>
     *
     * @param mimeType MIME type
     * @param data raw file bytes (will be base64-encoded)
     */
    static FileContentPart fileBytes(String mimeType, byte[] data) {
        return new FileContentPart(mimeType, java.util.Base64.getEncoder().encodeToString(data));
    }
}
