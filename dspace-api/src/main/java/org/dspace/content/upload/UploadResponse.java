package org.dspace.content.upload;

import org.dspace.content.Bitstream;
import java.util.UUID;
import org.dspace.content.UploadStatus;

public class UploadResponse {

    private static final int SUCCESS = 000;
    private static final int FAILURE = 400;
    private static final int NOTFOUND = 404;
    private static final int UNAUTHORIZED = 401;
    private static final int INTERNAL_SERVER_ERROR = 500;

    private UUID documentID;
    private int errorCode;
    private String errorDescription;

    public UploadResponse() {}

    public UploadResponse(Bitstream bitstream, UploadStatus status) {

        switch (status) {

            case SUCCESS:

                if (bitstream != null) {
                    this.documentID = bitstream.getID();
                }

                this.errorCode = SUCCESS;
                this.errorDescription = "Successfully uploaded";
                break;

            case INVALID_FILEFORMAT:

                this.errorCode = FAILURE;
                this.errorDescription = "Invalid file format";
                break;

            case FILE_FORMAT_MISSMATCH:

                this.errorCode = FAILURE;
                this.errorDescription = "File format mismatched";
                break;

            case FAILURE:

                this.errorCode = FAILURE;
                this.errorDescription = "Failed to upload";
                break;

            case NOTFOUND:

                this.errorCode = NOTFOUND;
                this.errorDescription = "Bundle not Found";
                break;

            case UNAUTHORIZED:

                this.errorCode = UNAUTHORIZED;
                this.errorDescription = "Unauthorized user";
                break;

            case DATA_MISSING:

                this.errorCode = UNAUTHORIZED;
                this.errorDescription =
                        "Please contact orion team to get token and source details";
                break;

            case INTERNAL_SERVER_ERROR:

                this.errorCode = INTERNAL_SERVER_ERROR;
                this.errorDescription = "Internal Server Error";
                break;
        }
    }
    
    public UploadResponse(int errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public UUID getDocumentID() {
        return documentID;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}