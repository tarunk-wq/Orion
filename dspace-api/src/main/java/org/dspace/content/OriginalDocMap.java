package org.dspace.content;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import org.dspace.core.Constants;

/*
 * Entity to store mapping between:
 * ORIGINAL bitstream and CONVERTED bitstream
 *
 * Legacy equivalent:
 * original_bitstream_map table
 *
 */
@Entity
@Table(name = "original_bitstream_map")
public class OriginalDocMap extends DSpaceObject {

    /*
     * Converted Bitstream
     * Maps to column: bitstream_id
     *
     * This is the final converted file (e.g., PDF)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bitstream_id")
    private Bitstream convertedBitstream;

    /*
     * Original Bitstream
     * Maps to column: org_bitstream_id
     *
     * This is the source file (e.g., EML, DOC, TXT)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_bitstream_id")
    private Bitstream originalBitstream;

    // Required constructor for DSpaceObject
    protected OriginalDocMap() {
        super();
    }

    // GETTERS / SETTERS

    public Bitstream getConvertedBitstream() {
        return convertedBitstream;
    }

    public void setConvertedBitstream(Bitstream convertedBitstream) {
        this.convertedBitstream = convertedBitstream;
    }

    public Bitstream getOriginalBitstream() {
        return originalBitstream;
    }

    public void setOriginalBitstream(Bitstream originalBitstream) {
        this.originalBitstream = originalBitstream;
    }

    @Override
    public String getName() {
        return "OriginalDocMap";
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}