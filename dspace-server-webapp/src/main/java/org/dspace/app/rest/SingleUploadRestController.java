package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.content.UploadStatus;
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
 */

@RestController
@RequestMapping("/api/custom/single-upload")
public class SingleUploadRestController {

	private static final Logger log = LogManager.getLogger(SingleUploadRestController.class);

	@Autowired
	private BundleMapService bundleMapService;

	@Autowired
	private SingleUploadService singleUploadService;

	@PostMapping
	public ResponseEntity<?> singleUpload(@RequestBody SingleUploadRequest requestBody, HttpServletRequest request)
			throws SQLException {

		log.info("API HIT: SINGLE UPLOAD");
	    log.info("Request Body: {}", requestBody);
	    System.out.println("SYSTEM OUT HIT");
		Context context = null;

		try {

			// Create DSpace transaction context
			context = ContextUtil.obtainContext(request);

			// Disable authorization temporarily (same as UploadBitstream)
			context.turnOffAuthorisationSystem();

			log.info("Single Upload API invoked");

			// Pass request to service layer
			UploadResponse response = singleUploadService.processRequest(context, requestBody, request);

			return ResponseEntity.status(HttpStatus.valueOf(response.getErrorCode())).body(response);

		} catch (Exception e) {

			
			 //Abort transaction 
			 
			if (context != null && context.isValid()) {
				context.abort();
			}

			
			 //Log based on flow 
			 
			if (requestBody != null && requestBody.getFile() != null && !requestBody.getFile().isEmpty()) {
				log.error("Could not create bitstream in item.", e);
			} else {
				log.error("Could not find item with provided data.", e);
			}

			
			 //Return structured response (NOT string)
			 
			UploadResponse errorResponse = new UploadResponse(null, UploadStatus.INTERNAL_SERVER_ERROR);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

		} finally {

			if (context != null) {
				context.restoreAuthSystemState();
			}
		}
	}
}

