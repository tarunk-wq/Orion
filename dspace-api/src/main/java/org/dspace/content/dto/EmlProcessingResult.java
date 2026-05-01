package org.dspace.content.dto;

import java.io.InputStream;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;

/*
 * This class replaces the legacy Object[] used in handleEMLFile()
 *
 * Instead of:
 * objects[0], objects[1], objects[2]
 *
 * We use named fields for better readability and type safety
 */
public class EmlProcessingResult {

    // InputStream used to create final bitstream
    private InputStream inputStream;

    // Original bitstream created from EML file
    private Bitstream originalBitstream;

    // Updated working bundle (may change during EML processing)
    private Bundle workingBundle;

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

    public Bundle getWorkingBundle() {
        return workingBundle;
    }

    public void setWorkingBundle(Bundle workingBundle) {
        this.workingBundle = workingBundle;
    }
}