package ai.portkey.utils;

import ai.portkey.model.ContentPart;
import ai.portkey.model.ImageContentPart;
import ai.portkey.model.Message;
import ai.portkey.model.TextContentPart;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilsTest {

    private byte[] createTestImage(String format, int width, int height) throws IOException {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.fillRect(0, 0, width, height);
        g.dispose();

        var out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    // -- toPngBase64 tests --

    @Test
    void toPngBase64_fromJpeg() throws IOException {
        byte[] jpeg = createTestImage("jpg", 10, 10);

        String base64 = ImageUtils.toPngBase64(jpeg);

        assertNotNull(base64);
        assertFalse(base64.isEmpty());
        byte[] decoded = Base64.getDecoder().decode(base64);
        assertPngSignature(decoded);
    }

    @Test
    void toPngBase64_fromPng() throws IOException {
        byte[] png = createTestImage("png", 10, 10);

        String base64 = ImageUtils.toPngBase64(png);

        assertNotNull(base64);
        byte[] decoded = Base64.getDecoder().decode(base64);
        assertPngSignature(decoded);
    }

    @Test
    void toPngBase64_fromBmp() throws IOException {
        byte[] bmp = createTestImage("bmp", 10, 10);

        String base64 = ImageUtils.toPngBase64(bmp);

        assertNotNull(base64);
        byte[] decoded = Base64.getDecoder().decode(base64);
        assertPngSignature(decoded);
    }

    @Test
    void toPngBase64_fromGif() throws IOException {
        byte[] gif = createTestImage("gif", 10, 10);

        String base64 = ImageUtils.toPngBase64(gif);

        assertNotNull(base64);
        byte[] decoded = Base64.getDecoder().decode(base64);
        assertPngSignature(decoded);
    }

    @Test
    void toPngBase64_nullBytes_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtils.toPngBase64(null));
    }

    @Test
    void toPngBase64_emptyBytes_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtils.toPngBase64(new byte[0]));
    }

    @Test
    void toPngBase64_corruptBytes_throwsIOException() {
        byte[] garbage = {0x00, 0x01, 0x02, 0x03, 0x04};
        assertThrows(IOException.class, () -> ImageUtils.toPngBase64(garbage));
    }

    // -- ContentPart tests --

    @Test
    void toPngContentPart_returnsValidContentPart() throws IOException {
        byte[] jpeg = createTestImage("jpg", 20, 20);

        ImageContentPart part = ImageUtils.toPngContentPart(jpeg);

        assertEquals("image_url", part.type());
        assertNotNull(part.imageUrl());
        assertTrue(part.imageUrl().url().startsWith("data:image/png;base64,"));
        assertNull(part.imageUrl().detail());
    }

    @Test
    void toPngContentPart_withDetail() throws IOException {
        byte[] png = createTestImage("png", 5, 5);

        ImageContentPart part = ImageUtils.toPngContentPart(png, ImageContentPart.Detail.high);

        assertEquals("image_url", part.type());
        assertTrue(part.imageUrl().url().startsWith("data:image/png;base64,"));
        assertEquals(ImageContentPart.Detail.high, part.imageUrl().detail());
    }

    @Test
    void toPngContentPart_roundTrip_decodesToValidPng() throws IOException {
        byte[] original = createTestImage("jpg", 32, 32);

        ImageContentPart part = ImageUtils.toPngContentPart(original);

        String dataUri = part.imageUrl().url();
        String base64 = dataUri.substring("data:image/png;base64,".length());
        byte[] decoded = Base64.getDecoder().decode(base64);

        assertPngSignature(decoded);

        BufferedImage readBack = ImageIO.read(new ByteArrayInputStream(decoded));
        assertNotNull(readBack);
        assertEquals(32, readBack.getWidth());
        assertEquals(32, readBack.getHeight());
    }

    // -- Decompression bomb tests --

    @Test
    void toPngBase64_oversizedImage_throwsIllegalArgument() throws IOException {
        var image = new BufferedImage(ImageUtils.MAX_DIMENSION + 1, 1, BufferedImage.TYPE_INT_RGB);
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] oversized = out.toByteArray();

        var ex = assertThrows(IllegalArgumentException.class, () -> ImageUtils.toPngBase64(oversized));
        assertTrue(ex.getMessage().contains("exceed maximum"));
    }

    @Test
    void toPngBase64_maxDimensionImage_succeeds() throws IOException {
        byte[] atLimit = createTestImage("png", ImageUtils.MAX_DIMENSION, 1);
        String base64 = ImageUtils.toPngBase64(atLimit);
        assertNotNull(base64);
    }

    // -- Resize tests --

    @Test
    void resize_landscapeImage_scalesByWidth() throws IOException {
        byte[] img = createTestImage("jpg", 4000, 3000);

        byte[] resized = ImageUtils.resize(img, 800);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(resized));
        assertEquals(800, result.getWidth());
        assertEquals(600, result.getHeight());
        assertPngSignature(resized);
    }

    @Test
    void resize_portraitImage_scalesByHeight() throws IOException {
        byte[] img = createTestImage("jpg", 3000, 4000);

        byte[] resized = ImageUtils.resize(img, 800);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(resized));
        assertEquals(600, result.getWidth());
        assertEquals(800, result.getHeight());
        assertPngSignature(resized);
    }

    @Test
    void resize_squareImage_scalesBothDimensions() throws IOException {
        byte[] img = createTestImage("png", 2000, 2000);

        byte[] resized = ImageUtils.resize(img, 500);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(resized));
        assertEquals(500, result.getWidth());
        assertEquals(500, result.getHeight());
    }

    @Test
    void resize_alreadySmall_noChange() throws IOException {
        byte[] img = createTestImage("png", 400, 300);

        byte[] resized = ImageUtils.resize(img, 800);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(resized));
        assertEquals(400, result.getWidth());
        assertEquals(300, result.getHeight());
    }

    @Test
    void resize_exactlyAtMax_noChange() throws IOException {
        byte[] img = createTestImage("png", 800, 600);

        byte[] resized = ImageUtils.resize(img, 800);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(resized));
        assertEquals(800, result.getWidth());
        assertEquals(600, result.getHeight());
    }

    @Test
    void resize_wideLandscape_1920to800() throws IOException {
        byte[] img = createTestImage("jpg", 1920, 1080);

        byte[] resized = ImageUtils.resize(img, 800);

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(resized));
        assertEquals(800, result.getWidth());
        assertEquals(450, result.getHeight());
    }

    @Test
    void resize_zeroMaxSize_throwsIllegalArgument() throws IOException {
        byte[] img = createTestImage("png", 100, 100);
        assertThrows(IllegalArgumentException.class, () -> ImageUtils.resize(img, 0));
    }

    @Test
    void resize_negativeMaxSize_throwsIllegalArgument() throws IOException {
        byte[] img = createTestImage("png", 100, 100);
        assertThrows(IllegalArgumentException.class, () -> ImageUtils.resize(img, -1));
    }

    @Test
    void resize_nullBytes_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtils.resize(null, 800));
    }

    @Test
    void resize_corruptBytes_throwsIOException() {
        byte[] garbage = {0x00, 0x01, 0x02, 0x03};
        assertThrows(IOException.class, () -> ImageUtils.resize(garbage, 800));
    }

    // -- Resize + ContentPart --

    @Test
    void toPngContentPart_withMaxSize_resizesAndConverts() throws IOException {
        byte[] img = createTestImage("jpg", 2000, 1000);

        ImageContentPart part = ImageUtils.toPngContentPart(img, 500);

        assertEquals("image_url", part.type());
        assertTrue(part.imageUrl().url().startsWith("data:image/png;base64,"));

        // Decode and verify dimensions
        String base64 = part.imageUrl().url().substring("data:image/png;base64,".length());
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        assertEquals(500, result.getWidth());
        assertEquals(250, result.getHeight());
    }

    @Test
    void toPngContentPart_withMaxSizeAndDetail() throws IOException {
        byte[] img = createTestImage("jpg", 1600, 900);

        ImageContentPart part = ImageUtils.toPngContentPart(img, 400, ImageContentPart.Detail.low);

        assertEquals(ImageContentPart.Detail.low, part.imageUrl().detail());

        String base64 = part.imageUrl().url().substring("data:image/png;base64,".length());
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        assertEquals(400, result.getWidth());
        assertEquals(225, result.getHeight());
    }

    // -- Integration test --

    @Test
    void toPngContentPart_resized_usableInMessage() throws IOException {
        byte[] jpeg = createTestImage("jpg", 3000, 2000);
        ImageContentPart part = ImageUtils.toPngContentPart(jpeg, 800);

        Message msg = Message.user(java.util.List.of(
                ContentPart.text("What is in this image?"),
                part
        ));

        assertEquals("user", msg.getRole());
        var parts = msg.getContentAsParts();
        assertNotNull(parts);
        assertEquals(2, parts.size());
        assertInstanceOf(TextContentPart.class, parts.get(0));
        assertInstanceOf(ImageContentPart.class, parts.get(1));
    }

    // -- Helpers --

    private void assertPngSignature(byte[] data) {
        assertTrue(data.length >= 8, "Data too short for PNG");
        assertEquals((byte) 0x89, data[0]);
        assertEquals((byte) 0x50, data[1]);
        assertEquals((byte) 0x4E, data[2]);
        assertEquals((byte) 0x47, data[3]);
    }
}
