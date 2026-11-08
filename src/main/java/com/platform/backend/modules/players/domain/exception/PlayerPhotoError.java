package com.platform.backend.modules.players.domain.exception;

/**
 * Why a profile photo was refused. The constant's name is the stable code clients receive as the reason of the
 * {@code file} field of the 400, so they can show it in their own language.
 */
public enum PlayerPhotoError {
    /** No file came in the request, or it has no bytes. */
    EMPTY_FILE,

    /** The file is bigger than {@code app.photos.max-bytes}. */
    FILE_TOO_LARGE,

    /** The declared type is not an allowed one, or the first bytes of the file are not that kind of image. */
    UNSUPPORTED_TYPE
}
