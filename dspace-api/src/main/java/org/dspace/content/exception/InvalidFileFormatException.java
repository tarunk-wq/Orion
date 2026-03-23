package org.dspace.content.exception;

/**
 * Custom exception for invalid file formats.
 *
 * This is thrown when:
 * - File MIME type is not in allowed formats
 * - File validation fails in FileFormatService
 *
 * Replaces legacy InvaidFileFormatException
 */
public class InvalidFileFormatException extends Exception {

    /**
     * Constructor with message
     */
    public InvalidFileFormatException(String message) {
        super(message);
    }

    /**
     * Constructor with message + cause
     */
    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}