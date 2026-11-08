package com.platform.backend.modules.players.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * The image formats a profile photo can be, recognised by the first bytes of the file and not by what the client says
 * it is. Which of them the server accepts is configuration ({@code app.photos.allowed-content-types}); this enum only
 * knows how to tell them apart.
 */
public enum PhotoFormat {
    JPEG("image/jpeg", new int[]{0xFF, 0xD8, 0xFF}, 0, null, 0),
    PNG("image/png", new int[]{0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, 0, null, 0),
    // A WebP is a RIFF container whose form type, after the 4 bytes of size, is WEBP.
    WEBP("image/webp", new int[]{'R', 'I', 'F', 'F'}, 0, new int[]{'W', 'E', 'B', 'P'}, 8);

    private final String contentType;
    private final int[] signature;
    private final int signatureOffset;
    private final int[] secondSignature;
    private final int secondSignatureOffset;

    PhotoFormat(String contentType, int[] signature, int signatureOffset, int[] secondSignature, int secondSignatureOffset) {
        this.contentType = contentType;
        this.signature = signature;
        this.signatureOffset = signatureOffset;
        this.secondSignature = secondSignature;
        this.secondSignatureOffset = secondSignatureOffset;
    }

    public String contentType() {
        return contentType;
    }

    /** The format the bytes are, or empty when they are none of the known ones. */
    public static Optional<PhotoFormat> detect(byte[] data) {
        if (data == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(format -> format.startsLike(data)).findFirst();
    }

    private boolean startsLike(byte[] data) {
        return hasBytesAt(data, signatureOffset, signature)
                && (secondSignature == null || hasBytesAt(data, secondSignatureOffset, secondSignature));
    }

    private static boolean hasBytesAt(byte[] data, int offset, int[] expected) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
