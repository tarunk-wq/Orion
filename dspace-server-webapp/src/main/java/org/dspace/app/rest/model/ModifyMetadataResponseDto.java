package org.dspace.app.rest.model;

import java.util.List;


public class ModifyMetadataResponseDto {

    private List<UserMetadataDto> metadata;
    private boolean uniqueMetadataFlag;

    public ModifyMetadataResponseDto(List<UserMetadataDto> metadata, boolean uniqueMetadataFlag) {
        this.metadata = metadata;
        this.uniqueMetadataFlag = uniqueMetadataFlag;
    }

    public List<UserMetadataDto> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<UserMetadataDto> metadata) {
        this.metadata = metadata;
    }

    public boolean isCommunityFlag() {
        return uniqueMetadataFlag;
    }

    public void setCommunityFlag(boolean uniqueMetadataFlag) {
        this.uniqueMetadataFlag = uniqueMetadataFlag;
    }
}