package org.dspace.content.dto;

import java.io.InputStream;

import org.dspace.content.Bitstream;

/*
 * DTO for handling non-EML file processing
 *
 * Replaces:
 * Object[] { InputStream, Bitstream }
 *
 * Used in:
 * handleOriginalFile()
 */
public class OriginalFileProcessingResult {

    // InputStream used later in upload flow
    private InputStream inputStream;

    // Original bitstream created from file
    private Bitstream originalBitstream;

    /*
     * GETTERS AND SETTERS
     */

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public Bitstream getOriginalBitstream() {
        return originalBitstream;
    }

    public void setOriginalBitstream(Bitstream originalBitstream) {
        this.originalBitstream = originalBitstream;
    }
}