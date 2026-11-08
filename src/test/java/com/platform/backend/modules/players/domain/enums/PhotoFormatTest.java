package com.platform.backend.modules.players.domain.enums;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoFormatTest {

    /** Bytes given as ints so the ones above 127 (0xFF, 0x89) read like their hex values. */
    private static byte[] bytes(int... values) {
        byte[] data = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            data[i] = (byte) values[i];
        }
        return data;
    }

    private static byte[] riff(String formType) {
        return ("RIFF" + "\u0000\u0000\u0000\u0000" + formType + "VP8 ").getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void detect_whenTheBytesStartLikeAJpeg_thenJpeg() {
        assertThat(PhotoFormat.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10))).contains(PhotoFormat.JPEG);
    }

    @Test
    void detect_whenTheBytesStartLikeAPng_thenPng() {
        assertThat(PhotoFormat.detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00))).contains(PhotoFormat.PNG);
    }

    @Test
    void detect_whenTheBytesAreARiffContainerOfTypeWebp_thenWebp() {
        assertThat(PhotoFormat.detect(riff("WEBP"))).contains(PhotoFormat.WEBP);
    }

    @Test
    void detect_whenTheBytesAreARiffContainerOfAnotherType_thenNothing() {
        // A WAV audio file is RIFF too: the form type after the size is what tells them apart.
        assertThat(PhotoFormat.detect(riff("WAVE"))).isEmpty();
    }

    @Test
    void detect_whenTheBytesAreText_thenNothing() {
        assertThat(PhotoFormat.detect("<html>not an image</html>".getBytes(StandardCharsets.UTF_8))).isEmpty();
    }

    @Test
    void detect_whenThereAreFewerBytesThanTheSignature_thenNothingAndNoError() {
        assertThat(PhotoFormat.detect(bytes(0xFF, 0xD8))).isEmpty();
        assertThat(PhotoFormat.detect(bytes(0x89, 'P', 'N', 'G'))).isEmpty();
        assertThat(PhotoFormat.detect(new byte[0])).isEmpty();
    }

    @Test
    void detect_whenThereAreNoBytes_thenNothing() {
        assertThat(PhotoFormat.detect(null)).isEmpty();
    }

    @Test
    void contentType_whenAsked_thenTheMediaTypeOfEachFormat() {
        assertThat(PhotoFormat.JPEG.contentType()).isEqualTo("image/jpeg");
        assertThat(PhotoFormat.PNG.contentType()).isEqualTo("image/png");
        assertThat(PhotoFormat.WEBP.contentType()).isEqualTo("image/webp");
    }
}
