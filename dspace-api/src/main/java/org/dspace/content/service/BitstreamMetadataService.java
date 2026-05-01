package org.dspace.content.service;

import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.net.URLConnection;

/*
 * Service class to handle Bitstream metadata operations
 *
 * This logic was moved from FileProcessingServiceImpl
 * so that it can be reused in multiple places (EML + non-EML flow)
 */
@Service
public class BitstreamMetadataService {

	// Service used to update metadata fields in bitstream
	@Autowired
	private BitstreamService bitstreamService;

	// Service used to set file format (MIME type)
	@Autowired
	private BitstreamFormatService bitstreamFormatService;

	/*
	 * Default method (used when masked flag is not required)
	 *
	 * Calls main method with isMaskedDoc = false
	 */
	public void addBitstreamMetadata(Context context, Bitstream bitstream, String name, String createdDate,
			String requirementId, String createdBy, String source) throws SQLException {

		addBitstreamMetadata(context, bitstream, name, createdDate, requirementId, createdBy, source, false);
	}

	/*
	 * Main method to add metadata to a Bitstream
	 *
	 * This method sets: - file name - MIME type (format) - createdBy - createdDate
	 * - requirementId - source - masked flag (optional)
	 */
	public void addBitstreamMetadata(Context context, Bitstream bitstream, String name, String createdDate,
			String requirementId, String createdBy, String source, boolean isMaskedDoc) throws SQLException {

		/*
		 *  Set bitstream name (this is the file name shown in DSpace)
		 */
		bitstream.setName(context, name);

		/*
		 *  Detect MIME type from file name (used to identify file format like
		 * PDF, DOC, etc.)
		 */
		String mimeType = getMimeType(name);

		if (mimeType == null) {
			// If MIME type is not detected -> set as unknown
			bitstreamService.setFormat(context, bitstream, bitstreamFormatService.findUnknown(context));
		} else {
			// Set correct MIME type based on file name
			bitstreamService.setFormat(context, bitstream, bitstreamFormatService.findByMIMEType(context, mimeType));
		}

		/*
		 *  Add metadata fields
		 */

		// Add createdBy (who uploaded the file)
		if (createdBy != null && !createdBy.isEmpty()) {
			bitstreamService.setMetadataSingleValue(context, bitstream, "dc", "createdBy", null, null, createdBy);
		}

		// Add createdDate (when file was created)
		if (createdDate != null) {
			bitstreamService.setMetadataSingleValue(context, bitstream, "dc", "createdDate", null, null, createdDate);
		}

		// Add requirementId (custom business field)
		if (requirementId != null && !requirementId.isEmpty()) {
			bitstreamService.setMetadataSingleValue(context, bitstream, "dc", "requirement", "id", null, requirementId);
		}

		// Set source (where file came from)
		if (source != null && !source.isEmpty()) {
			bitstream.setSource(context, source);
		}

		// Add masked flag (only if document is masked)
		if (isMaskedDoc) {
			bitstreamService.setMetadataSingleValue(context, bitstream, "dc", "masked", "doc", null,
					String.valueOf(isMaskedDoc));
		}
	}

	/*
	 * Helper method to detect MIME type from file name
	 *
	 * Uses Java built-in method
	 */
	private String getMimeType(String name) {
		if (name == null) {
			return null;
		}
		return URLConnection.guessContentTypeFromName(name);
	}
}