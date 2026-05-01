package org.dspace.unconvertedbitstream;

import jakarta.persistence.*;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;

import java.util.UUID;

/*
 * Entity for unconverted_bitstream table
 */
@Entity
@Table(name = "unconverted_bitstream")
public class UnconvertedBitstream extends DSpaceObject {

    @Column(name = "bitstream_id")
    private UUID bitstreamId;

    @Column(name = "org_file_name")
    private String originalFileName;

    @Column(name = "mime_type")
    private String mimeType;

    // Required constructor for DSpaceObject
    public UnconvertedBitstream() {
        super();
    }

    // Getters & Setters

    public UUID getBitstreamId() {
        return bitstreamId;
    }

    public void setBitstreamId(UUID bitstreamId) {
        this.bitstreamId = bitstreamId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String getName() {
        return originalFileName;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}