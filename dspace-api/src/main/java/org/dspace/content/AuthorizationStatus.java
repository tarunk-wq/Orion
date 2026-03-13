package org.dspace.content;

/**
 * Represents authorization results for Single Upload API
 * Replicates UploadBitstream authorization states.
 */
public enum AuthorizationStatus {

    AUTHORIZED,

    MISSING_TOKEN,

    INVALID_TOKEN,

    DEACTIVATED_TOKEN,

    MISSING_SOURCE,

    UNRECOGNIZED_SOURCE
}