package ai.portkey.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom deserializer for {@link Message#getContent()}.
 *
 * <p>The OpenAI/Portkey API uses a polymorphic {@code content} field:
 * <ul>
 *   <li>A JSON string for plain text messages</li>
 *   <li>A JSON array of typed objects for multimodal messages (text + images)</li>
 *   <li>JSON null when content is absent (e.g. tool-call-only assistant messages)</li>
 * </ul>
 *
 * <p>Without this deserializer, Jackson maps the array case to
 * {@code List<LinkedHashMap>} since the field is typed as {@code Object}.
 * This deserializer ensures array elements are properly deserialized as
 * {@link ContentPart} instances using the {@code type} discriminator.
 *
 * <p>Type resolution is handled manually here (rather than relying on
 * {@code @JsonTypeInfo}) because Jackson's type-info mechanisms conflict
 * with Java records: they attempt to set the discriminator property via
 * a setter after construction, which fails on immutable records.
 */
class MessageContentDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return switch (p.currentToken()) {
            case VALUE_STRING -> p.getText();
            case VALUE_NULL -> null;
            case START_ARRAY -> deserializeContentParts(p, ctxt);
            default -> ctxt.handleUnexpectedToken(Object.class, p);
        };
    }

    private List<ContentPart> deserializeContentParts(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        var mapper = (ObjectMapper) p.getCodec();
        var parts = new ArrayList<ContentPart>();

        while (p.nextToken() != JsonToken.END_ARRAY) {
            var node = (JsonNode) mapper.readTree(p);
            parts.add(deserializeContentPart(node, mapper, ctxt));
        }

        return List.copyOf(parts);
    }

    private ContentPart deserializeContentPart(JsonNode node, ObjectMapper mapper,
                                               DeserializationContext ctxt) throws IOException {
        var typeNode = node.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            ctxt.reportInputMismatch(ContentPart.class,
                    "ContentPart missing required 'type' discriminator field");
            return null; // unreachable - reportInputMismatch throws
        }

        return switch (typeNode.textValue()) {
            case "text" -> mapper.treeToValue(node, TextContentPart.class);
            case "image_url" -> mapper.treeToValue(node, ImageContentPart.class);
            case "file" -> mapper.treeToValue(node, FileContentPart.class);
            default -> {
                ctxt.reportInputMismatch(ContentPart.class,
                        "Unknown ContentPart type: '%s'", typeNode.textValue());
                yield null; // unreachable
            }
        };
    }
}
