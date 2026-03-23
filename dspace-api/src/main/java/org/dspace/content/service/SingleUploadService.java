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
    
    public UploadResponse processRequest(Context context, SingleUploadRequest request, HttpServletRequest servletRequest)
            		throws SQLException;
    
    AuthorizationStatus authorizeRequest(Context context,
            String source,
            String token) throws SQLException;
}