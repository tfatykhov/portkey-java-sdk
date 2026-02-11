package ai.portkey.model;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility for converting image byte arrays to base64-encoded PNG content parts.
 *
 * <p>Accepts any image format supported by {@link ImageIO} (JPEG, BMP, GIF, TIFF, WBMP, PNG)
 * and converts to PNG before base64 encoding.
 *
 * <pre>{@code
 * byte[] jpegBytes = Files.readAllBytes(Path.of("photo.jpg"));
 * ContentPart part = ImageUtils.toPngContentPart(jpegBytes);
 * Message msg = Message.user(List.of(ContentPart.text("Describe this"), part));
 * }</pre>
 */
public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Convert image bytes (any supported format) to a PNG base64 string.
     *
     * @param imageBytes raw image bytes (JPEG, PNG, BMP, GIF, etc.)
     * @return base64-encoded PNG string
     * @throws IOException if the bytes cannot be read as an image or PNG encoding fails
     * @throws IllegalArgumentException if imageBytes is null or empty
     */
    public static String toPngBase64(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes must not be null or empty");
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Unsupported or corrupt image format");
        }

        var out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("No PNG writer available");
        }

        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /**
     * Convert image bytes to an {@link ImageContentPart} with PNG base64 data URI.
     *
     * @param imageBytes raw image bytes (JPEG, PNG, BMP, GIF, etc.)
     * @return an image content part ready to use in a {@link Message}
     * @throws IOException if the bytes cannot be read as an image or PNG encoding fails
     * @throws IllegalArgumentException if imageBytes is null or empty
     */
    public static ImageContentPart toPngContentPart(byte[] imageBytes) throws IOException {
        return ContentPart.imageBase64("image/png", toPngBase64(imageBytes));
    }

    /**
     * Convert image bytes to an {@link ImageContentPart} with PNG base64 data URI and detail level.
     *
     * @param imageBytes raw image bytes (JPEG, PNG, BMP, GIF, etc.)
     * @param detail the detail level for the image (low, high, auto)
     * @return an image content part ready to use in a {@link Message}
     * @throws IOException if the bytes cannot be read as an image or PNG encoding fails
     * @throws IllegalArgumentException if imageBytes is null or empty
     */
    public static ImageContentPart toPngContentPart(byte[] imageBytes, ImageContentPart.Detail detail) throws IOException {
        return ContentPart.imageBase64("image/png", toPngBase64(imageBytes), detail);
    }
}
