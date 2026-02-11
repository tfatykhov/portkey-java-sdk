package ai.portkey.utils;

import ai.portkey.model.ContentPart;
import ai.portkey.model.ImageContentPart;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

/**
 * Utility for converting and resizing image byte arrays to base64-encoded PNG content parts.
 *
 * <p>Accepts any image format supported by {@link ImageIO} (JPEG, BMP, GIF, TIFF, WBMP, PNG)
 * and converts to PNG before base64 encoding.
 *
 * <p>Includes decompression bomb protection: images exceeding {@link #MAX_DIMENSION}
 * pixels in either dimension are rejected to prevent OOM from malicious inputs.
 *
 * <pre>{@code
 * byte[] jpegBytes = Files.readAllBytes(Path.of("photo.jpg"));
 *
 * // Convert to PNG base64
 * ContentPart part = ImageUtils.toPngContentPart(jpegBytes);
 *
 * // Resize to max 800px (proportional) then convert
 * ContentPart resized = ImageUtils.toPngContentPart(jpegBytes, 800);
 *
 * // Resize only (get raw PNG bytes)
 * byte[] resizedPng = ImageUtils.resize(jpegBytes, 800);
 * }</pre>
 */
public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Maximum allowed dimension (width or height) in pixels.
     * Prevents decompression bombs - a 16384x16384 RGBA image is ~1 GB in memory.
     */
    public static final int MAX_DIMENSION = 8192;

    // -- Resize --

    /**
     * Resize an image proportionally so that neither width nor height exceeds {@code maxSize}.
     *
     * <p>If both dimensions are already within {@code maxSize}, the image is returned as-is
     * (re-encoded as PNG). The aspect ratio is always preserved.
     *
     * <pre>{@code
     * // 4000x3000 image resized to max 800 -> 800x600
     * // 600x800 image resized to max 800 -> 600x800 (no change)
     * // 1920x1080 resized to max 800 -> 800x450
     * byte[] resized = ImageUtils.resize(jpegBytes, 800);
     * }</pre>
     *
     * @param imageBytes raw image bytes (any ImageIO-supported format)
     * @param maxSize maximum pixels for the larger dimension
     * @return PNG-encoded resized image bytes
     * @throws IOException if the image cannot be read or encoded
     * @throws IllegalArgumentException if imageBytes is null/empty or maxSize is not positive
     */
    public static byte[] resize(byte[] imageBytes, int maxSize) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes must not be null or empty");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive, got: " + maxSize);
        }

        checkDimensions(imageBytes);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Unsupported or corrupt image format");
        }

        BufferedImage result = resizeImage(image, maxSize);
        return encodePng(result);
    }

    // -- PNG base64 --

    /**
     * Convert image bytes (any supported format) to a PNG base64 string.
     *
     * @param imageBytes raw image bytes (JPEG, PNG, BMP, GIF, etc.)
     * @return base64-encoded PNG string
     * @throws IOException if the bytes cannot be read as an image or PNG encoding fails
     * @throws IllegalArgumentException if imageBytes is null, empty, or exceeds dimension limits
     */
    public static String toPngBase64(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes must not be null or empty");
        }

        checkDimensions(imageBytes);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Unsupported or corrupt image format");
        }

        return Base64.getEncoder().encodeToString(encodePng(image));
    }

    /**
     * Resize an image to max dimension, then convert to PNG base64 string.
     *
     * @param imageBytes raw image bytes
     * @param maxSize maximum pixels for the larger dimension
     * @return base64-encoded PNG string of the resized image
     * @throws IOException if the image cannot be read or encoded
     * @throws IllegalArgumentException if imageBytes is null/empty or maxSize is not positive
     */
    public static String toPngBase64(byte[] imageBytes, int maxSize) throws IOException {
        return Base64.getEncoder().encodeToString(resize(imageBytes, maxSize));
    }

    // -- ContentPart factories --

    /**
     * Convert image bytes to an {@link ImageContentPart} with PNG base64 data URI.
     *
     * @param imageBytes raw image bytes (JPEG, PNG, BMP, GIF, etc.)
     * @return an image content part ready to use in a message
     * @throws IOException if the bytes cannot be read as an image or PNG encoding fails
     * @throws IllegalArgumentException if imageBytes is null, empty, or exceeds dimension limits
     */
    public static ImageContentPart toPngContentPart(byte[] imageBytes) throws IOException {
        return ContentPart.imageBase64("image/png", toPngBase64(imageBytes));
    }

    /**
     * Resize an image to max dimension, then convert to an {@link ImageContentPart}.
     *
     * @param imageBytes raw image bytes
     * @param maxSize maximum pixels for the larger dimension
     * @return an image content part ready to use in a message
     * @throws IOException if the image cannot be read or encoded
     * @throws IllegalArgumentException if imageBytes is null/empty or maxSize is not positive
     */
    public static ImageContentPart toPngContentPart(byte[] imageBytes, int maxSize) throws IOException {
        return ContentPart.imageBase64("image/png", toPngBase64(imageBytes, maxSize));
    }

    /**
     * Convert image bytes to an {@link ImageContentPart} with detail level.
     *
     * @param imageBytes raw image bytes
     * @param detail the detail level for the image (low, high, auto)
     * @return an image content part ready to use in a message
     * @throws IOException if the bytes cannot be read as an image or PNG encoding fails
     * @throws IllegalArgumentException if imageBytes is null, empty, or exceeds dimension limits
     */
    public static ImageContentPart toPngContentPart(byte[] imageBytes, ImageContentPart.Detail detail) throws IOException {
        return ContentPart.imageBase64("image/png", toPngBase64(imageBytes), detail);
    }

    /**
     * Resize an image to max dimension, then convert to an {@link ImageContentPart} with detail level.
     *
     * @param imageBytes raw image bytes
     * @param maxSize maximum pixels for the larger dimension
     * @param detail the detail level for the image (low, high, auto)
     * @return an image content part ready to use in a message
     * @throws IOException if the image cannot be read or encoded
     * @throws IllegalArgumentException if imageBytes is null/empty or maxSize is not positive
     */
    public static ImageContentPart toPngContentPart(byte[] imageBytes, int maxSize, ImageContentPart.Detail detail) throws IOException {
        return ContentPart.imageBase64("image/png", toPngBase64(imageBytes, maxSize), detail);
    }

    // -- Internal --

    /**
     * Resize a BufferedImage proportionally so the larger dimension fits within maxSize.
     * Returns the original image if already within bounds.
     */
    private static BufferedImage resizeImage(BufferedImage original, int maxSize) {
        int w = original.getWidth();
        int h = original.getHeight();

        if (w <= maxSize && h <= maxSize) {
            return original;
        }

        double scale = Math.min((double) maxSize / w, (double) maxSize / h);
        int newW = Math.max(1, (int) Math.round(w * scale));
        int newH = Math.max(1, (int) Math.round(h * scale));

        var resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        var g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();

        return resized;
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        var out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("No PNG writer available");
        }
        return out.toByteArray();
    }

    /**
     * Check image dimensions without fully decoding pixel data.
     * Rejects images exceeding {@link #MAX_DIMENSION} in either axis.
     */
    private static void checkDimensions(byte[] imageBytes) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (iis == null) {
                throw new IOException("Unsupported or corrupt image format");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported or corrupt image format");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new IllegalArgumentException(
                            "Image dimensions %dx%d exceed maximum %dx%d".formatted(
                                    width, height, MAX_DIMENSION, MAX_DIMENSION));
                }
            } finally {
                reader.dispose();
            }
        }
    }
}
