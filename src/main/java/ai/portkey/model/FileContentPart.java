package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * File content part for document-based multimodal messages (PDFs, text files, etc.).
 *
 * <pre>{@code
 * // PDF from base64
 * ContentPart.file("application/pdf", base64PdfData)
 *
 * // Plain text file
 * ContentPart.file("text/plain", "This is a plain text file")
 *
 * // From byte array
 * byte[] pdfBytes = Files.readAllBytes(Path.of("document.pdf"));
 * ContentPart.fileBytes("application/pdf", pdfBytes)
 * }</pre>
 *
 * @param type always "file"
 * @param file the file metadata and data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileContentPart(
        String type,
        @JsonProperty("file") FileData file
) implements ContentPart {

    public FileContentPart(String mimeType, String fileData) {
        this("file", new FileData(mimeType, fileData));
    }

    /**
     * @param mimeType MIME type (e.g. "application/pdf", "text/plain", "text/csv")
     * @param fileData base64-encoded file data, or plain text for text/* types
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FileData(
            @JsonProperty("mime_type") String mimeType,
            @JsonProperty("file_data") String fileData
    ) {}
}
