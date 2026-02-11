package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Content part for multimodal messages.
 *
 * <p>Sealed hierarchy:
 * <ul>
 *   <li>{@link TextContentPart} - text content</li>
 *   <li>{@link ImageContentPart} - image URL content</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextContentPart.class, name = "text"),
        @JsonSubTypes.Type(value = ImageContentPart.class, name = "image_url")
})
public sealed interface ContentPart permits TextContentPart, ImageContentPart {

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
}
