package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
}
