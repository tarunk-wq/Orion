package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.content.dto.SingleUploadRequest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.upload.UploadResponse;
import org.dspace.content.service.BundleMapService;
import org.dspace.content.service.SingleUploadService;
import org.dspace.core.Context;
import org.dspace.app.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SingleUploadRestController
 *
 * Phase 1:
 *  - Context creation
 *  - Basic authorization check
 *  - Input normalization
 *  - Required field validation
 *  - Mapping validation
 *  - Base64 validation
 *
 * item,bundle,bitstream logic (item creation, bundle creation, bitstream)
 * will be implemented in Phase 2.
 */
@RestController
@RequestMapping("/api/custom/single-upload")
public class SingleUploadRestController {

    private static final Logger log =
            LogManager.getLogger(SingleUploadRestController.class);

    @Autowired
    private BundleMapService bundleMapService;
    
    @Autowired
    private SingleUploadService singleUploadService;

    @PostMapping
    public ResponseEntity<?> singleUpload(
            @RequestBody SingleUploadRequest requestBody,
            HttpServletRequest request) throws SQLException {

        Context context = null;

        try {

            // Create DSpace transaction context
            context = ContextUtil.obtainContext(request);

            // Disable authorization temporarily (same as UploadBitstream)
            context.turnOffAuthorisationSystem();

            log.info("Single Upload API invoked");

            // Pass request to service layer
            UploadResponse response = singleUploadService.processRequest(context, requestBody, request);
            
            if (response != null) {
                return ResponseEntity.status(HttpStatus.valueOf(response.getErrorCode())).body(response);
            }
            
            context.complete();

            return ResponseEntity.ok("Request received");

        } catch (Exception e) {

            if (context != null && context.isValid()) {
                context.abort();
            }

            log.error("Error processing upload request", e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error");

        } finally {

            if (context != null) {
                context.restoreAuthSystemState();
            }
        }
    }
}