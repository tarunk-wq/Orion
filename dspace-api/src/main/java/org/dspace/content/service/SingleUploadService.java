package org.dspace.content.service;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.AuthorizationStatus;
import org.dspace.content.dto.SingleUploadRequest;
import org.dspace.content.upload.UploadResponse;
import org.dspace.core.Context;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service is responsible for executing the Single Upload workflow
 *
 * This service follows the style of UploadBitstream
 * but implemented using Spring-style service architecture
 */
public interface SingleUploadService {

    /**
     * Executes the Single Upload process-
     *
     * Steps performed:
     * 1. Validate bundle mapping
     * 2. Create Item
     * 3. Create or fetch parent bundle
     * 4. Create or fetch child bundle
     * 5. Decode Base64 file
     * 6. Create bitstream
     * 7. Attach bitstream to child bundle
     */
    void handleSingleUpload(
            Context context,
            String bundle,
            String base64File,
            String title)
            throws SQLException, AuthorizeException, IOException;
    
    public UploadResponse processRequest(Context context, SingleUploadRequest request, HttpServletRequest servletRequest)
            		throws SQLException;
    
    AuthorizationStatus authorizeRequest(Context context,
            String source,
            String token) throws SQLException;
}