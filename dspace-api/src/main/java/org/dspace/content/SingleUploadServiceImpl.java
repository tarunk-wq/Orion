package org.dspace.content;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.dao.SourceTokenDAO;
import org.dspace.content.dto.FileProcessingResult;
import org.dspace.content.dto.SingleUploadRequest;
import org.dspace.content.exception.InvalidFileFormatException;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.content.service.BundleMapService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.FileFormatService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.PdfConverterService;
import org.dspace.content.service.SingleUploadService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.upload.UploadConstants;
import org.dspace.content.upload.UploadResponse;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.item2agentagencypan.dao.AgencyPanDAO;
import org.dspace.item2pan.dao.PanDAO;
import org.dspace.primarytype.dao.PrimaryTypeDAO;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SingleUploadServiceImpl
 *
 * This class contains the business logic for the Single Upload API.
 *
 * The controller performs: - Authorization - Input validation
 *
 * This service performs: - bundle_map validation - Item creation - Bundle
 * hierarchy creation - Bitstream creation
 *
 * The design follows the same flow as UploadBitstream (old tech) but
 * implemented using Spring service architecture.
 */
public class SingleUploadServiceImpl implements SingleUploadService {

	private static final Logger log = LogManager.getLogger(SingleUploadServiceImpl.class);

	// Service used to check bundle_map table.
	@Autowired
	private BundleMapService bundleMapService;

	// Service used for creating and updating Items.
	@Autowired
	private ItemService itemService;

	@Autowired
	private FileFormatService fileFormatService;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PdfConverterService pdfConverterService;
	
	@Autowired
	private PanDAO panDAO;

	@Autowired
	private AgencyPanDAO agencyPanDAO;

	@Autowired
	private PrimaryTypeDAO primaryTypeDAO;

	@Autowired
	private BitstreamService bitstreamService;

	@Autowired
	private WorkspaceItemService workspaceItemService;

	// Service used for creating Bundles.
	@Autowired
	private BundleService bundleService;

	// Service used for retrieving collections.
	@Autowired
	private CollectionService collectionService;

	@Autowired
	private SourceTokenDAO sourceTokenDAO;

	private static final String REGEX = ".*[#$%&!^>=<:;`~*+'\"?()/].*";

	private static final String REGEX_ALLOW_BRACKETS = ".*[#$%>!?+:;^`~*\"'=<].*";

	private static final String SIMPLE_REGEX = ".*[#$%><!~].*";

	/**
	 * Temporary fixed collection UUID, i need to see if this is to be used layer
	 * atfer fixing findbundleitem method
	 *
	 * All new items created by the API will be placed in this collection will
	 * replace this later with configuration if needed
	 */
	// private static final UUID FIXED_COLLECTION_UUID = UUID.fromString(" ");
	// commented out for now due to unsure collection logic

	@Override
	public UploadResponse processRequest(Context context, SingleUploadRequest request,
			HttpServletRequest servletRequest) throws SQLException {

		// Normalize inputs
		normalizeInputs(request);

		// Extract authorization fields
		String source = request.getSource();
		String token = servletRequest.getHeader("Token");

		// Authorization check
		AuthorizationStatus status = authorizeRequest(context, source, token);

		UploadResponse uploadResp = null;

		switch (status) {

		case MISSING_TOKEN:
			uploadResp = new UploadResponse(null, UploadStatus.DATA_MISSING);
			break;

		case INVALID_TOKEN:
			uploadResp = new UploadResponse(null, UploadStatus.UNAUTHORIZED);
			break;

		case DEACTIVATED_TOKEN:
			uploadResp = new UploadResponse(null, UploadStatus.UNAUTHORIZED);
			break;

		case MISSING_SOURCE:
			uploadResp = new UploadResponse(null, UploadStatus.DATA_MISSING);
			break;

		case UNRECOGNIZED_SOURCE:
			uploadResp = new UploadResponse(null, UploadStatus.DATA_MISSING);
			break;

		case AUTHORIZED:
			break;
		}

		if (uploadResp != null) {

			log.warn("Authorization failed: {}", uploadResp.getErrorDescription());
			context.abort();

			return uploadResp;
		}

		/*
		 * UploadBitstream logic starts here
		 */

		boolean isDocUploaded = request.getFile() != null && !request.getFile().trim().isEmpty();

		if (isDocUploaded) {

			String base64EncodedString = request.getFile();
			String docType = request.getDocumentType();
			String fileType = request.getFileType();
			String documentName = request.getDocumentName();

			if (!base64EncodedString.isEmpty() && !docType.isEmpty() && !fileType.isEmpty()
					&& !documentName.isEmpty()) {

				String msg;

				if (!(msg = validInputs(context, request)).isEmpty()) {

					context.abort();

					return new UploadResponse(HttpStatus.BAD_REQUEST.value(), msg);
				}

				// UploadBitstream logic continued, all helpers should throw exception and not
				// return response

				byte[] decodedBytes;

				try {
					decodedBytes = decodeFile(base64EncodedString);
				} catch (IllegalArgumentException e) {

					context.abort();

					return new UploadResponse(HttpStatus.BAD_REQUEST.value(), "Invalid Base64 file");
				}

				String mimeType;

				try {
					mimeType = validateFileFormat(decodedBytes);
				} catch (InvalidFileFormatException e) {

					context.abort();
					return new UploadResponse(null, UploadStatus.INVALID_FILEFORMAT);

				} catch (IOException e) {

					context.abort();
					return new UploadResponse(null, UploadStatus.INTERNAL_SERVER_ERROR);
				}

				String cleanDocumentName = validateFileExtension(documentName, fileType);

				if (cleanDocumentName == null) {
					context.abort();
					return new UploadResponse(null, UploadStatus.FILE_FORMAT_MISSMATCH);
				}

				documentName = cleanDocumentName;

				InputStream inputStream = null;
				Map<String, List<String>> unconvertedFileMap = new HashMap<>();

				// Call processFile() -> this replaces legacy convertToPDF() logic
				FileProcessingResult fileResult;

				try {
					fileResult = processFile(mimeType, documentName, fileType, decodedBytes);
				} catch (IOException e) {
					throw new RuntimeException("Error processing file", e);
				}

				// Check if processing was successful
				// (Same as: if(objArr != null) in legacy code)
				if (fileResult != null) {

					// Check whether file was converted to PDF or not
					boolean isConverted = fileResult.isConverted();

					// If file was converted (i.e., NOT originally a PDF)
					if (isConverted) {

						// Get file type flags (these tell what type of file it originally was)
						boolean isEml = fileResult.isEml(); // email file
						boolean isDoc = fileResult.isDoc(); // office document
						boolean isHtml = fileResult.isHtml(); // html file
						boolean isTxt = fileResult.isTxt(); // text/csv file
						boolean isImg = fileResult.isImg(); // image file

						// Get the temporary file where converted PDF is stored
						File tempFile = fileResult.getTempFile();

						/*
						 * If file is NOT any special type (doc, eml, html, txt, img), then it means it
						 * is a normal converted PDF file.
						 *
						 * So we create InputStream from the temp file -> This will later be used to
						 * create Bitstream
						 */
						if (!isDoc && !isEml && !isHtml && !isTxt && !isImg) {

							try {
								inputStream = new FileInputStream(tempFile);
							} catch (FileNotFoundException e) {
								throw new RuntimeException("Temp file not found", e);
							}
						}

						/*
						 * Special case: EML files (email files)
						 *
						 * Instead of using InputStream, we store metadata (attachments, content, etc.)
						 */
						else if (isEml) {

							unconvertedFileMap = fileResult.getUnconvertedFileMap();
						}

					} else {
						/*
						 * If NOT converted -> file was already a PDF
						 *
						 * So we directly use original input stream (no temp file needed)
						 */
						inputStream = fileResult.getInputStream();
					}

				} else {
					/*
					 * If processFile() returned null → means file format is invalid / conversion
					 * failed
					 *
					 * So we abort the transaction and return error
					 */
					context.abort();
					return new UploadResponse(null, UploadStatus.FILE_FORMAT_MISSMATCH);
				}

				Item dspaceItem = null;

				try {
					dspaceItem = findExistingItem(context, request);
				} catch (Exception e) {
					throw new RuntimeException("Error while finding existing item", e);
				}
			}
		}

		return null;
	}

	private byte[] decodeFile(String base64File) {
		return Base64.getDecoder().decode(base64File);
	}

	private String validateFileFormat(byte[] decodedBytes) throws IOException, InvalidFileFormatException {

		// Directly call service
		// If invalid -> exception will be thrown automatically
		return fileFormatService.checkIfValidFile(decodedBytes);
	}

	private String validateFileExtension(String documentName, String fileType) {

		String extension = FilenameUtils.getExtension(documentName);
		String cleanName = FilenameUtils.removeExtension(documentName);

		if (extension == null || extension.isEmpty() || !extension.equalsIgnoreCase(fileType)) {
			return null;
		}

		return cleanName;
	}

	public FileProcessingResult processFile(String fileMimeType, String docName, String fileExtension,
			byte[] decodedBytes) throws IOException {

		// This DTO will store all results instead of using Object[]
		FileProcessingResult result = new FileProcessingResult();

		// Temporary file where converted PDF will be stored
		File tempFile = null;

		// Flags to identify what type of file we received
		boolean isConverted = false; // whether conversion happened
		boolean isEml = false; // email file
		boolean isDoc = false; // office document
		boolean isHtml = false; // html file
		boolean isTxt = false; // text file
		boolean isImg = false; // image file

		// Allowed image extensions (same as legacy logic)
		List<String> imageExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp");

		// Used only in email case (stores attachments that could not be converted)
		Map<String, List<String>> unconvertedFileMap = new HashMap<>();

		// InputStream used later for Bitstream creation
		InputStream inputStream = null;

		// HTML CASE
		// If MIME type and extension both match HTML
		if (fileFormatService.getHtmlFormats().contains(fileMimeType)
				&& (fileExtension.equalsIgnoreCase("htm") || fileExtension.equalsIgnoreCase("html"))) {

			// Create temp directory for conversion
			tempFile = new File(System.getProperty("java.io.tmpdir"), "HtmlToPDF_" + System.currentTimeMillis());

			// Create directory if it doesn't exist
			if (!tempFile.exists()) {
				tempFile.mkdir();
			}

			// Convert HTML to PDF (legacy logic)
			tempFile = pdfConverterService.convertHtmlToPdf(decodedBytes, tempFile, docName);

			isHtml = true;
			isConverted = true;
		}

		// TXT CASE
		else if (fileFormatService.getTxtFormats().contains(fileMimeType)
				&& (fileExtension.equalsIgnoreCase("txt") || fileExtension.equalsIgnoreCase("csv"))) {

			tempFile = new File(System.getProperty("java.io.tmpdir"), "TxtToPDF_" + System.currentTimeMillis());

			if (!tempFile.exists()) {
				tempFile.mkdir();
			}

			// Convert TXT to PDF
			tempFile = pdfConverterService.convertTextToPdf(decodedBytes, tempFile, docName);

			isTxt = true;
			isConverted = true;
		}

		// IMAGE CASE
		else if (fileFormatService.getImageFormats().contains(fileMimeType)
				&& imageExtensions.contains(fileExtension)) {

			tempFile = new File(System.getProperty("java.io.tmpdir"), "ImagetoPdf_" + System.currentTimeMillis());

			if (!tempFile.exists()) {
				tempFile.mkdir();
			}

			// Convert Image to PDF
			tempFile = pdfConverterService.convertImageToPdf(decodedBytes, tempFile, docName);

			isImg = true;
			isConverted = true;
		}

		// TIFF CASE (SPECIAL CASE)
		else if (fileFormatService.getTiffFormats().contains(fileMimeType)
				&& (fileExtension.equalsIgnoreCase("tiff") || fileExtension.equalsIgnoreCase("tif"))) {

			// Step 1: create temp directory
			tempFile = new File(System.getProperty("java.io.tmpdir"), "TiffToPDF_" + System.currentTimeMillis());
			if (!tempFile.exists()) {
			    tempFile.mkdir();
			}

			// Create output PDF file
			File outputPdf = new File(tempFile, docName + ".pdf");

			// Step 2: convert TIFF directly
			tempFile = pdfConverterService.convertTiffToPdf(decodedBytes, outputPdf);

			isConverted = true;
		}

		// EML CASE (EMAIL FILE)
		else if (fileFormatService.getEmailFormats().contains(fileMimeType)
				&& (fileExtension.equalsIgnoreCase("eml") || fileExtension.equalsIgnoreCase("msg"))) {

			tempFile = new File(System.getProperty("java.io.tmpdir"),
					"EmailToPDF_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId());

			if (!tempFile.exists()) {
				tempFile.mkdir();
			}

			// Convert email to PDF and extract metadata/attachments
			unconvertedFileMap = pdfConverterService.convertEmailToPdf(decodedBytes, tempFile, docName);

			isEml = true;
			isConverted = true;
		}

		// OFFICE FILE CASE
		else if (fileFormatService.getOfficeFormats().contains(fileMimeType)
				&& (fileExtension.equalsIgnoreCase("doc") || fileExtension.equalsIgnoreCase("xls")
						|| fileExtension.equalsIgnoreCase("ppt") || fileExtension.equalsIgnoreCase("docx")
						|| fileExtension.equalsIgnoreCase("xlsx") || fileExtension.equalsIgnoreCase("pptx"))) {

			tempFile = new File(System.getProperty("java.io.tmpdir"), "OfficeToPDF_" + System.currentTimeMillis());

			if (!tempFile.exists()) {
				tempFile.mkdir();
			}

			// Convert Office file to PDF
			tempFile = pdfConverterService.convertOfficeToPdf(decodedBytes, tempFile, docName);

			isDoc = true;
			isConverted = true;
		}

		// PDF CASE (NO CONVERSION NEEDED)
		else if (fileFormatService.getPdfFormats().contains(fileMimeType) && fileExtension.equalsIgnoreCase("pdf")) {

			// Directly create InputStream (no conversion)
			inputStream = new ByteArrayInputStream(decodedBytes);
		}

		// DEFAULT CASE (INVALID FILE)
		else {
			// Return null same as legacy logic
			return null;
		}

		// FINAL: SET VALUES IN DTO

		result.setConverted(isConverted);
		result.setEml(isEml);
		result.setDoc(isDoc);
		result.setHtml(isHtml);
		result.setTxt(isTxt);
		result.setImg(isImg);

		result.setTempFile(tempFile);
		result.setUnconvertedFileMap(unconvertedFileMap);

		// Only set InputStream if it exists (PDF case)
		if (inputStream != null) {
			result.setInputStream(inputStream);
		}

		return result;
	}

	private Item findExistingItem(Context context, SingleUploadRequest request)
			throws AuthorizeException, IOException, SearchServiceException, SQLException {

		Item item = null;
		String searchWith = null;

		// Extract all fields from request (clean mapping)
		String corporateAckNo = request.getCorporateAckNo();
		String corporateSubAckNo = request.getCorporateSubAckNo();
		String corporateContributionAckNo = request.getCorporateContributionAckNo();
		String retailSubAckNo = request.getRetailSubAckNo();
		String agencyAckNo = request.getAgencyAckNo();
		String agentAckNo = request.getAgentAckNo();
		String individualAgentAckNo = request.getIndividualAgentAckNo();
		String choNo = request.getChoNo();
		String pran = request.getPran();
		String pan = request.getPan();
		String agencyOrAgentPAN = request.getAgencyOrAgentPAN();
		String agencyID = request.getAgencyID();
		String agentID = request.getAgentID();
		String primary = request.getPrimary();
		String primaryType = request.getPrimaryType();

		// SAME ORDER AS LEGACY

		if (corporateAckNo != null && !corporateAckNo.isEmpty()) {
			searchWith = UploadConstants.ACK_NO;
			item = searchItem(context, searchWith, corporateAckNo, "");
		}

		else if (corporateSubAckNo != null && !corporateSubAckNo.isEmpty()) {
			searchWith = UploadConstants.ACK_NO;
			item = searchItem(context, searchWith, corporateSubAckNo, "");
		}

		else if (corporateContributionAckNo != null && !corporateContributionAckNo.isEmpty()) {
			searchWith = UploadConstants.ACK_NO;
			item = searchItem(context, searchWith, corporateContributionAckNo, "");
		}

		else if (retailSubAckNo != null && !retailSubAckNo.isEmpty()) {
			searchWith = UploadConstants.ACK_NO;
			item = searchItem(context, searchWith, retailSubAckNo, "");
		}

		else if (agencyAckNo != null && !agencyAckNo.isEmpty()) {
			searchWith = UploadConstants.ACK_NO;
			item = searchItem(context, searchWith, agencyAckNo, "");
		}

		else if (agentAckNo != null && !agentAckNo.isEmpty()) {
			searchWith = UploadConstants.ACK_NO;
			item = searchItem(context, searchWith, agentAckNo, "");
		}

		else if (individualAgentAckNo != null && !individualAgentAckNo.isEmpty()) {
			searchWith = UploadConstants.ACK_NO;
			item = searchItem(context, searchWith, individualAgentAckNo, "");
		}

		else if (pan != null && !pan.isEmpty()) {
			searchWith = UploadConstants.PAN;
			item = searchItem(context, searchWith, pan, "");
		}

		else if (agencyOrAgentPAN != null && !agencyOrAgentPAN.isEmpty()) {
			searchWith = UploadConstants.AGENCY_N_AGENT_PAN;
			item = searchItem(context, searchWith, agencyOrAgentPAN, "");
		}

		else if (pran != null && !pran.isEmpty()) {
			searchWith = UploadConstants.PRAN;
			item = searchItem(context, searchWith, pran, "");
		}

		else if (choNo != null && !choNo.isEmpty()) {
			searchWith = UploadConstants.CHO_NO;
			item = searchItem(context, searchWith, choNo, "");
		}

		else if (agentID != null && !agentID.isEmpty()) {
			searchWith = UploadConstants.AGENT_ID;
			item = searchItem(context, searchWith, agentID, "");
		}

		else if (agencyID != null && !agencyID.isEmpty()) {
			searchWith = UploadConstants.AGENCY_ID;
			item = searchItem(context, searchWith, agencyID, "");
		}

		return item;
	}

	private Item searchItem(Context context, String searchWith, String searchValue, String searchValue2)
			throws SearchServiceException, SQLException {

		Item item = null;

		String searchQuery = null;
		String selectQuery = null;

			// SAME SWITCH LOGIC 
		switch (searchWith) {

		case UploadConstants.ACK_NO:
			searchQuery = "dc.ack.no_keyword:\"" + searchValue + "\"";
			selectQuery = "Select * from item2ackno where ack_no = ?";
			break;

		case UploadConstants.PRAN:
			searchQuery = "dc.pran_keyword:\"" + searchValue + "\"";
			selectQuery = "Select * from item2pran where pran = ?";
			break;

		case UploadConstants.PAN:
			searchQuery = "dc.pan_keyword:\"" + searchValue + "\"";
			selectQuery = "Select * from item2pan where pan = ?";
			break;

		case UploadConstants.AGENCY_N_AGENT_PAN:
			searchQuery = "dc.pan.ag_keyword:\"" + searchValue + "\"";
			selectQuery = "Select * from item2agent_agency_pan where pan = ?";
			break;

		case UploadConstants.PRIMARY_NO:

		    searchQuery = "dc.primary_keyword:\"" + searchValue + "\" AND dc.primary.type_keyword:\""
		            + searchValue2 + "\"";

		    selectQuery = "Select * from item2primary where primary_id = ? and primary_type = '"
		            + searchValue2 + "'";
		    break;
			
		case UploadConstants.CHO_NO:
			searchQuery = "dc.cho.sta_keyword:\"" + searchValue + "\"";
			selectQuery = "Select * from item2corporate where cho_no = ?";
			break;

		case UploadConstants.AGENCY_ID:
			searchQuery = "dc.agency.sta_keyword:\"" + searchValue + "\"";
			selectQuery = "Select * from item2agency where agency_id = ?";
			break;

		case UploadConstants.AGENT_ID:
			searchQuery = "dc.agent.id_keyword:\"" + searchValue + "\"";
			selectQuery = "Select * from item2agent where agent_id = ?";
			break;
		}

		// SEARCH USING DISCOVERY (SOLR)
		// Create a new DiscoverQuery object (used to query Solr index)
		DiscoverQuery query = new DiscoverQuery();

		// Set the actual search query string (e.g., dc.pan_keyword:"ABCDE1234F")
		query.setQuery(searchQuery);

		// Apply filter so that only DSpace Items are returned (not collections/communities)
		// this method expects a String, so we convert the int constant
		query.setDSpaceObjectFilter(String.valueOf(Constants.ITEM));

		// Limit results to only 1 item (since identifiers are unique, only one match is needed)
		query.setMaxResults(1);

		// Create DSpace object 
		DSpace dspace = new DSpace();

		// Get SearchService bean from DSpace ServiceManager
		// This service is responsible for executing Solr (Discovery) queries
		SearchService searcher = dspace.getServiceManager().getServiceByName(SearchService.class.getName(), SearchService.class);

		// Execute the search query using the current DSpace context
		// This sends the query to Solr and retrieves results
		DiscoverResult resp = searcher.search(context, query);

		// Check if any results were found in Solr
		if (resp.getTotalSearchResults() > 0) {

		    // If results exist:
		    // getIndexableObjects() -> returns list of wrapped objects
		    // get(0) -> get first result (since maxResults = 1)
		    // getIndexedObject() -> extract actual DSpace object
		    // Cast to Item because we know we filtered only Items
		    item = (Item) resp.getIndexableObjects().get(0).getIndexedObject();

		} else {

		    // If item was NOT found in Solr:
		    // This usually happens if item is not indexed yet or index is out-of-date

		    // Log message for debugging (helps track fallback cases)
		    log.info("Finding entry in database for : " + searchValue);

		    // Execute SQL query using JdbcTemplate (Spring way, replaces DatabaseManager)
		    // ps -> sets parameter value in query 
		    // rs -> extracts "item_id" column from result set
		    List<Integer> itemIds = jdbcTemplate.query(selectQuery, ps -> ps.setString(1, searchValue), (rs, rowNum) -> rs.getInt("item_id"));

		    // Check if database returned any matching rows
		    if (!itemIds.isEmpty()) {

		        // Extract item_id
		        Integer itemId = itemIds.get(0);

		        // Fetch Item using native query 
		        List<Item> items = itemService.findByCustomNativeQuery(context, "SELECT * FROM item WHERE item_id = " + itemId);

		        if (!items.isEmpty()) {
		            item = items.get(0);
		        }
		    }
		}

		// Return the found Item (or null if not found in both Solr and DB)
		return item;
	}

	/**
	 * Normalize incoming request fields (replicates UploadUtil.getTrimmedValue())
	 */
	private void normalizeInputs(SingleUploadRequest request) {

		request.setBundle(trim(request.getBundle()));
		request.setFile(trim(request.getFile()));

		if (request.getSource() != null) {
			request.setSource(request.getSource().trim());
		}

		if (request.getMetadata() != null) {

			String title = request.getMetadata().get("dc.title");

			if (title != null) {
				request.getMetadata().put("dc.title", title.trim());
			}
		}

		// UploadBitstream normalization

		request.setCorporateAckNo(trim(request.getCorporateAckNo()).toUpperCase(Locale.ROOT));
		request.setCorporateSubAckNo(trim(request.getCorporateSubAckNo()).toUpperCase(Locale.ROOT));
		request.setCorporateContributionAckNo(trim(request.getCorporateContributionAckNo()).toUpperCase(Locale.ROOT));

		request.setRetailSubAckNo(trim(request.getRetailSubAckNo()).toUpperCase(Locale.ROOT));
		request.setAgencyAckNo(trim(request.getAgencyAckNo()).toUpperCase(Locale.ROOT));
		request.setAgentAckNo(trim(request.getAgentAckNo()).toUpperCase(Locale.ROOT));
		request.setIndividualAgentAckNo(trim(request.getIndividualAgentAckNo()).toUpperCase(Locale.ROOT));

		request.setCorporateName(trim(request.getCorporateName()).toUpperCase(Locale.ROOT));
		request.setSubscriberName(trim(request.getSubscriberName()).toUpperCase(Locale.ROOT));
		request.setAgencyName(trim(request.getAgencyName()).toUpperCase(Locale.ROOT));
		request.setAgentName(trim(request.getAgentName()).toUpperCase(Locale.ROOT));

		request.setAadhaar(trim(request.getAadhaar()).toUpperCase(Locale.ROOT));

		request.setEmail(trim(request.getEmail()).toLowerCase(Locale.ROOT));

		request.setContactNo(trim(request.getContactNo()));

		request.setPan(trim(request.getPan()).toUpperCase(Locale.ROOT));
		request.setAgencyOrAgentPAN(trim(request.getAgencyOrAgentPAN()).toUpperCase(Locale.ROOT));
		request.setPran(trim(request.getPran()).toUpperCase(Locale.ROOT));

		request.setChoNo(trim(request.getChoNo()).toUpperCase(Locale.ROOT));
		request.setCboNo(trim(request.getCboNo()).toUpperCase(Locale.ROOT));
		request.setCboName(trim(request.getCboName()).toUpperCase(Locale.ROOT));

		request.setAgencyID(trim(request.getAgencyID()).toUpperCase(Locale.ROOT));
		request.setAgentID(trim(request.getAgentID()).toUpperCase(Locale.ROOT));

		request.setPrimary(trim(request.getPrimary()));
		request.setPrimaryType(trim(request.getPrimaryType()));

		request.setDocumentType(trim(request.getDocumentType()));

		request.setDocumentName(trim(request.getDocumentName()));

		request.setFileType(trim(request.getFileType()).toLowerCase(Locale.ROOT));

		request.setLegacyType(trim(request.getLegacyType()).toUpperCase(Locale.ROOT));

		request.setCreatedBy(trim(request.getCreatedBy()));
		request.setCreatedDate(trim(request.getCreatedDate()));
		request.setRequirementId(trim(request.getRequirementId()));
	}

	/**
	 * Helper method used for trimming strings
	 */
	private String trim(String value) {

		String trimmedValue = "";

		if (value != null) {
			trimmedValue = value.trim();
		}

		return trimmedValue;
	}

	private String validInputs(Context context, SingleUploadRequest req) throws SQLException {

		String msg = "";

		String corporateAckNo = req.getCorporateAckNo();
		String corporateSubAckNo = req.getCorporateSubAckNo();
		String corporateContributionAckNo = req.getCorporateContributionAckNo();

		String legacyType = req.getLegacyType();
		String retailSubAckNo = req.getRetailSubAckNo();
		String agencyAckNo = req.getAgencyAckNo();
		String agentAckNo = req.getAgentAckNo();
		String individualAgentAckNo = req.getIndividualAgentAckNo();

		String choNo = req.getChoNo();
		String pran = req.getPran();
		String pan = req.getPan();
		String agencyOrAgentPAN = req.getAgencyOrAgentPAN();

		String agencyID = req.getAgencyID();
		String agentID = req.getAgentID();

		String primary = req.getPrimary();
		String primaryType = req.getPrimaryType();

		String docType = req.getDocumentType();
		String documentName = req.getDocumentName();

		String corpName = req.getCorporateName();
		String subscrName = req.getSubscriberName();
		String agencyName = req.getAgencyName();
		String agentName = req.getAgentName();

		String creationDate = req.getCreatedDate();

		String[] otherParams = { req.getCboNo(), req.getCboName(), req.getAadhaar(), req.getEmail(), req.getContactNo(),
				req.getFileType(), req.getCreatedBy(), req.getRequirementId() };

		if (corporateAckNo.isEmpty() && corporateSubAckNo.isEmpty() && retailSubAckNo.isEmpty() && agencyAckNo.isEmpty()
				&& legacyType.isEmpty() && agentAckNo.isEmpty() && choNo.isEmpty() && pran.isEmpty()
				&& agencyID.isEmpty() && agentID.isEmpty() && primary.isEmpty()
				&& corporateContributionAckNo.isEmpty()) {

			if (!pan.isEmpty()) {

				if (!panExists(context, pan) && corpName.isEmpty() && subscrName.isEmpty()) {
					msg = "With new PAN corresponding name is required";
				}

			} else if (!agencyOrAgentPAN.isEmpty()) {

				if (!agencyPanExists(context, pan) && agencyName.isEmpty() && agentName.isEmpty()) {
					msg = "With new PAN corresponding name is required";
				}

			} else {
				msg = "All key fileds of upload API are missing";
			}
		}

		else if (!corporateSubAckNo.isEmpty() && choNo.isEmpty()) {
			msg = "For corporate subscribers CHO no. is mandatory";
		}

		else if (!agentAckNo.isEmpty() && agencyID.isEmpty()) {
			msg = "For onboarding Agents belongs to agencies, agency ID is mandatory";
		}

		else if ((!primary.isEmpty() && primaryType.isEmpty()) || (primary.isEmpty() && !primaryType.isEmpty())) {

			msg = "Primary with Primary Type is mandatory";
		}

		else if (!isSupportedInput(corporateAckNo) || !isSupportedInput(corporateSubAckNo)
				|| !isSupportedInput(retailSubAckNo) || !isSupportedInput(agencyAckNo) || !isSupportedInput(agentAckNo)
				|| !isSupportedInput(individualAgentAckNo) || !isSupportedInput(choNo) || !isSupportedInput(pan)
				|| !isSupportedInput(agencyOrAgentPAN) || !isSupportedInput(pran) || !isSupportedInput(agencyID)
				|| !isSupportedInput(agentID) || !isSupportedInput(primary) || !isSupportedInput(primaryType)
				|| !isSupportedInputs(otherParams)) {

			msg = "Unsupported special characters #$%&!^>=<:;`~*+'\"?()/ provided in the input field value";
		}

		else if (!isSupportedInput(docType, REGEX_ALLOW_BRACKETS)
				|| !isSupportedInput(documentName, REGEX_ALLOW_BRACKETS)
				|| !isSupportedInput(subscrName, REGEX_ALLOW_BRACKETS)
				|| !isSupportedInput(agencyName, REGEX_ALLOW_BRACKETS)
				|| !isSupportedInput(agentName, REGEX_ALLOW_BRACKETS)) {

			msg = "Unsupported special characters #$%>!?+:;^`~*\"'=< provided in the input field value";
		}

		else if (!isSupportedInput(creationDate, SIMPLE_REGEX) || !isSupportedInput(corpName, SIMPLE_REGEX)) {

			msg = "Unsupported special characters #$%><!~ provided in the input field value";
		}

		else if (!primary.isEmpty() && !primaryType.isEmpty()) {

			if (!primaryTypeExists(context, primaryType)) {
				msg = "Invalid primary type";
			}
		}

		return msg;
	}

	private boolean isSupportedInputs(String... values) {

		for (String v : values) {
			if (!isSupportedInput(v)) {
				return false;
			}
		}

		return true;
	}

	private boolean isSupportedInput(String value) {

		return isSupportedInput(value, REGEX);
	}

	private boolean isSupportedInput(String value, String regex) {

		if (value == null) {
			return true;
		}

		if (value.matches(regex)) {
			return false;
		}

		return true;
	}

	private boolean panExists(Context context, String pan) throws SQLException {

		return panDAO.existsByPan(context, pan);

	}

	private boolean agencyPanExists(Context context, String pan) throws SQLException {

		return agencyPanDAO.existsByPan(context, pan);

	}

	private boolean primaryTypeExists(Context context, String primaryType) throws SQLException {

		return primaryTypeDAO.existsByPrimaryType(context, primaryType);

	}

	@Override
	public AuthorizationStatus authorizeRequest(Context context, String source, String token) throws SQLException {

		// 1️ Check source FIRST
		if (source == null || source.trim().isEmpty()) {
			return AuthorizationStatus.MISSING_SOURCE;
		}

		// 2️ Fetch source from DB
		SourceToken tokenRow = sourceTokenDAO.findBySource(context, source);

		// 3 row null check
		if (tokenRow == null) {
			return AuthorizationStatus.UNRECOGNIZED_SOURCE;
		}

		// 4 Check if active
		if (!tokenRow.isActive()) {
			return AuthorizationStatus.DEACTIVATED_TOKEN;
		}

		// 5 Only now check token
		if (token == null || token.trim().isEmpty()) {
			return AuthorizationStatus.MISSING_TOKEN;
		}

		// 6 Compare token
		if (!token.equals(tokenRow.getToken())) {
			return AuthorizationStatus.INVALID_TOKEN;
		}

		// 7 Success
		return AuthorizationStatus.AUTHORIZED;
	}

}