package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Text content part for multimodal messages.
 *
 * @param type always "text"
 * @param text the text content
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextContentPart(
        String type,
        String text
) implements ContentPart {

    public TextContentPart(String text) {
        this("text", text);
    }
}
