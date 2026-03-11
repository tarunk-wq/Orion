package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.SingleUploadRequest;
import org.dspace.app.rest.utils.ContextUtil;
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
            HttpServletRequest request)
            throws SQLException {

        Context context = null;

        try {

            //Create DSpace context (transaction object)
            context = ContextUtil.obtainContext(request);

            // Disable default DSpace authorization temporarily
            context.turnOffAuthorisationSystem();

            log.info("Single Upload API invoked");

            String source = request.getParameter("source");
            String token = request.getHeader("Token"); //read token coming from api request in the header

            // Validate source
            boolean validSource = singleUploadService.validateSource(context, source);

            if (!validSource) {
                context.abort();
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid source");
            }

            // Validate token
            boolean validToken = singleUploadService.validateToken(context, source, token);

            if (!validToken) {
                context.abort();
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid token");
            }

            //Normalize Input (trim spaces)
            String bundle = requestBody.getBundle() == null
                    ? null
                    : requestBody.getBundle().trim();

            String mapping = requestBody.getMapping() == null
                    ? null
                    : requestBody.getMapping().trim();

            String base64File = requestBody.getFile() == null
                    ? null
                    : requestBody.getFile().trim();

            String title = null;

            if (requestBody.getMetadata() != null) {
                title = requestBody.getMetadata().get("dc.title");
                if (title != null) {
                    title = title.trim();
                }
            }

            //Required Field Validation

            if (bundle == null || bundle.isEmpty()) {
                context.abort();
                throw new UnprocessableEntityException("Bundle is required");
            }

            if (mapping == null || mapping.isEmpty()) {
                context.abort();
                throw new UnprocessableEntityException("Mapping is required");
            }

            if (base64File == null || base64File.isEmpty()) {
                context.abort();
                throw new UnprocessableEntityException("File is required");
            }

            if (title == null || title.isEmpty()) {
                context.abort();
                throw new UnprocessableEntityException("dc.title is required");
            }

            //Validate mapping format (must be Parent|Child)

            String[] parts = mapping.split("\\|");

            if (parts.length != 2) {
                context.abort();
                throw new UnprocessableEntityException(
                        "Mapping must be in Parent|Child format");
            }

            String parent = parts[0].trim();
            String child = parts[1].trim();

            //Validate mapping exists in bundle_map table

            boolean exists =
                    bundleMapService.isValidMapping(context, bundle, parent, child);

            if (!exists) {
                context.abort();
                throw new UnprocessableEntityException(
                        "Mapping does not exist in bundle_map table");
            }

            //Validate Base64 format

            try {
                Base64.getDecoder().decode(base64File);
            } catch (IllegalArgumentException e) {
                context.abort();
                throw new UnprocessableEntityException("Invalid Base64 file format");
            }

            //All validations passed → execute upload workflow

            singleUploadService.handleSingleUpload(context, bundle, mapping, base64File, title);

            //Commit database transaction
            context.complete();

            log.info("Single upload completed successfully.");

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("Upload successful");

        } catch (UnprocessableEntityException e) {

            log.error("Validation error: " + e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (Exception e) {

            if (context != null && context.isValid()) {
                context.abort();
            }

            log.error("Unexpected error occurred.", e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error");

        } finally {

            //Always restore authorization system
            if (context != null) {
                context.restoreAuthSystemState();
            }
        }
    }
}