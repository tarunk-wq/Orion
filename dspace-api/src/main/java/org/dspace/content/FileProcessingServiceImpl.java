package org.dspace.content;

import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamMetadataService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.FileProcessingService;
import org.dspace.content.dto.FileProcessingResult;
import org.dspace.content.dto.OriginalFileProcessingResult;
import org.dspace.content.dto.EmlProcessingResult;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.unconvertedbitstream.UnconvertedBitstream;
import org.dspace.unconvertedbitstream.dao.UnconvertedBitstreamDAO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.net.URLConnection;

/*
 * Implementation of FileProcessingService
 *
 * Contains logic migrated from UploadUtil
 */
@Service
public class FileProcessingServiceImpl implements FileProcessingService {

	private static final String ORG_BUNDLE_NAME = "UNCONVERTED";

	@Autowired
	private BitstreamService bitstreamService;

	@Autowired
	private BundleService bundleService;

	@Autowired
	private BitstreamFormatService bitstreamFormatService;

	@Autowired
	private UnconvertedBitstreamDAO unconvertedBitstreamDAO;

	@Autowired
	private BitstreamMetadataService bitstreamMetadataService;

	/*
	 * Handles EML file logic (this method processes email files and their
	 * attachments)
	 */
	@Override
	public EmlProcessingResult handleEmlFile(Context context, Item dspaceItem, Bundle workingBundle, File tempFile,
			String documentName, String createdDate, String source, String requirementId, String createdBy,
			Map<String, List<String>> unconvertedFileMap) throws Exception {

		// This InputStream will store the email body file (PDF) which will be used
		// later
		InputStream is = null;

		// This will store the original email bitstream (original .eml file)
		Bitstream orgBitstream = null;

		/*
		 * STEP 1: Create ORIGINAL bundle inside item This is where the original email
		 * file will be stored
		 */
		Bundle orgBundle = bundleService.create(context, dspaceItem, ORG_BUNDLE_NAME);

		/*
		 * STEP 2: Create a sub-bundle specific to this email Name format:
		 * documentName_createdDate
		 */
		String emailBundleName = documentName + "_" + createdDate;

		// Try to find if this sub-bundle already exists
		Bundle subBundle = findSubBundleByName(workingBundle, emailBundleName);

		// If not found -> create it, else reuse existing
		if (subBundle == null) {
			workingBundle = bundleService.createSubBundle(context, workingBundle, emailBundleName);
		} else {
			workingBundle = subBundle;
		}

		/*
		 * STEP 3: Loop through all files extracted from EML (original, attachments,
		 * body)
		 */
		for (File f : tempFile.listFiles()) {

			Bitstream b = null; // will hold attachment bitstream
			String fName = f.getName(); // file name of current file

			/*
			 * CASE 1: ORIGINAL email file (.eml)
			 */
			if (fName.contains("original")) {

				// Create bitstream in ORIGINAL bundle
				orgBitstream = bitstreamService.create(context, orgBundle, new FileInputStream(f));

				// Add metadata (name, mime, createdBy, etc.)
				bitstreamMetadataService.addBitstreamMetadata(context, orgBitstream, fName, createdDate, requirementId,
						createdBy, source);

				// Save changes to DB
				bitstreamService.update(context, orgBitstream);
			}

			/*
			 * CASE 2: ATTACHMENTS inside email
			 */
			else if (fName.contains("attachment")) {

				// Create bitstream inside working bundle
				b = bitstreamService.create(context, workingBundle, new FileInputStream(f));

				// Set file name
				b.setName(context, fName);

				// Add metadata
				bitstreamMetadataService.addBitstreamMetadata(context, b, fName, createdDate, requirementId, createdBy,
						source);

				// Save to DB
				bitstreamService.update(context, b);

				/*
				 * If attachment could not be converted (tracked in map) then store its details
				 * in DB
				 */
				if (unconvertedFileMap != null && unconvertedFileMap.containsKey(fName)) {

					// Get details (filename + mime type)
					List<String> details = unconvertedFileMap.get(fName);

					// Insert into unconverted_bitstream table
					insertUnconvertedDocument(context, b.getID(), details);
				}
			}

			/*
			 * CASE 3: EMAIL BODY (PDF version of email content)
			 */
			else if (fName.contains("body")) {

				// Store InputStream (used later for final upload)
				is = new FileInputStream(f);
			}
		}

		/*
		 * STEP 4: Return result using DTO (replaces legacy Object[])
		 */
		EmlProcessingResult result = new EmlProcessingResult();

		// Set extracted values
		result.setInputStream(is);
		result.setOriginalBitstream(orgBitstream);
		result.setWorkingBundle(workingBundle);

		return result;
	}

	/*
	 * Helper method to find a sub-bundle by name inside a parent bundle
	 */
	private Bundle findSubBundleByName(Bundle parent, String name) {

	    // Safety check for input
	    if (parent == null || name == null) {
	        return null;
	    }

	    // Loop through all sub-bundles
	    for (Bundle b : parent.getSubBundles()) {

	        // Check name safely (avoid NullPointerException)
	        if (b.getName() != null && b.getName().equalsIgnoreCase(name)) {
	            return b;
	        }
	    }

	    // Not found
	    return null;
	}

	/*
	 * Insert entry into unconverted_bitstream table (used for files that could not
	 * be converted)
	 */
	private void insertUnconvertedDocument(Context context, UUID bitstreamId, List<String> details)
			throws SQLException {

		// Create new entity object
		UnconvertedBitstream entity = new UnconvertedBitstream();

		// Set values from input
		entity.setBitstreamId(bitstreamId);
		entity.setOriginalFileName(details.get(0));
		entity.setMimeType(details.get(1));

		// Save entity to database using DAO
		unconvertedBitstreamDAO.create(context, entity);
	}

	/*
	 * Handles non-EML files
	 */
	@Override
	public OriginalFileProcessingResult handleOriginalFile(Context context, Item item, File tempFile,
			String createdDate, String source, String requirementId, String createdBy) throws Exception {

		/*
		 * STEP 1: Initialize variables (same as legacy)
		 */
		InputStream is = null;
		Bitstream orgBitstream = null;

		/*
		 * STEP 2: Create ORIGINAL bundle
		 */
		Bundle orgBundle = bundleService.create(context, item, ORG_BUNDLE_NAME);

		/*
		 * STEP 3: Loop through files inside temp directory (exact same as legacy logic)
		 */
		for (File f : tempFile.listFiles()) {

			String fName = f.getName();

			/*
			 * CASE 1: ORIGINAL file
			 */
			if (fName.contains("original")) {

				// Create bitstream in ORIGINAL bundle
				orgBitstream = bitstreamService.create(context, orgBundle, new FileInputStream(f));

				// Add metadata (same helper as EML)
				bitstreamMetadataService.addBitstreamMetadata(context, orgBitstream, fName, createdDate, requirementId,
						createdBy, source);

				// Save to DB
				bitstreamService.update(context, orgBitstream);
			}

			/*
			 * CASE 2: OTHER FILE (used as InputStream)
			 */
			else {

				// This will be returned later
				is = new FileInputStream(f);
			}
		}

		/*
		 * STEP 4: Return DTO (replaces Object[])
		 */
		OriginalFileProcessingResult result = new OriginalFileProcessingResult();
		result.setInputStream(is);
		result.setOriginalBitstream(orgBitstream);

		return result;
	}
}