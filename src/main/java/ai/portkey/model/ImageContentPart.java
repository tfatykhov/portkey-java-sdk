package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Image URL content part for multimodal messages.
 *
 * @param type always "image_url"
 * @param imageUrl the image URL and detail level
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImageContentPart(
        String type,
        @JsonProperty("image_url") ImageUrl imageUrl
) implements ContentPart {

    public ImageContentPart(String url) {
        this("image_url", new ImageUrl(url, null));
    }

    public ImageContentPart(String url, Detail detail) {
        this("image_url", new ImageUrl(url, detail));
    }

    /**
     * @param url image URL or base64 data URI
     * @param detail resolution level (auto, low, high)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ImageUrl(String url, Detail detail) {}

    public enum Detail {
        auto, low, high
    }
}
