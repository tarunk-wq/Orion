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
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.cbo2cho.dao.CboDAO;
import org.dspace.content.dao.SourceTokenDAO;
import org.dspace.content.dto.FileProcessingResult;
import org.dspace.content.dto.SingleUploadRequest;
import org.dspace.content.exception.InvalidFileFormatException;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleMapService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.FileFormatService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.PdfConverterService;
import org.dspace.content.service.SingleUploadService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.upload.UploadConstants;
import org.dspace.content.upload.UploadResponse;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.item2ackno.Item2AckNo;
import org.dspace.item2ackno.dao.AckDAO;
import org.dspace.item2agency.Item2Agency;
import org.dspace.item2agency.dao.AgencyDAO;
import org.dspace.item2agent.Item2Agent;
import org.dspace.item2agent.dao.AgentDAO;
import org.dspace.item2agentagencypan.Item2AgentAgencyPan;
import org.dspace.item2agentagencypan.dao.AgencyPanDAO;
import org.dspace.item2corporate.Item2Corporate;
import org.dspace.item2corporate.dao.CorporateDAO;
import org.dspace.item2pan.Item2Pan;
import org.dspace.item2pan.dao.PanDAO;
import org.dspace.item2pran.Item2Pran;
import org.dspace.item2pran.dao.PranDAO;
import org.dspace.item2primary.Item2Primary;
import org.dspace.item2primary.dao.PrimaryDAO;
import org.dspace.primarytype.dao.PrimaryTypeDAO;
import org.dspace.services.ConfigurationService;
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
	private ConfigurationService configurationService;

	@Autowired
	private AckDAO ackDAO;

	@Autowired
	private PranDAO pranDAO;

	@Autowired
	private CorporateDAO corporateDAO;

	@Autowired
	private AgentDAO agentDAO;

	@Autowired
	private AgencyDAO agencyDAO;

	@Autowired
	private PrimaryDAO primaryDAO;
	
	@Autowired
	private CboDAO cboDAO;

	@Autowired
	private InstallItemService installItemService;

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

	// Fetch collection UUIDs from config
	String defaultCollectionId = configurationService.getProperty("dspace.default.collection.id");
	String notsrCollectionId = configurationService.getProperty("dspace.other.primtype.collection.id");

	// Convert to UUID
	UUID defaultCollectionUUID = UUID.fromString(defaultCollectionId);
	UUID notsrCollectionUUID = UUID.fromString(notsrCollectionId);

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
			HttpServletRequest servletRequest) throws SQLException, AuthorizeException {

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
					if (request.getLegacyType() == null || request.getLegacyType().isEmpty()) {
						dspaceItem = findExistingItem(context, request);
					}
				} catch (Exception e) {
					throw new RuntimeException("Error while finding existing item", e);
				}

				if (dspaceItem == null) {
					dspaceItem = createItem(context, request);
				}

				// updateotherfields() foes here
				// 1. Parse createdDate (from request)
				String parsedCreatedDate = parseCreatedDate(request.getCreatedDate());

				// 2. Generate system item creation date
				String itemCreationDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

				// 3. Update metadata + validations
				updateOtherFields(context, dspaceItem, request, parsedCreatedDate, itemCreationDate);

				// 4. Logging (keep it simple for now)
				log.info("Adding bitstream to item(id=" + dspaceItem.getID() + ").");
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

			searchQuery = "dc.primary_keyword:\"" + searchValue + "\" AND dc.primary.type_keyword:\"" + searchValue2
					+ "\"";

			selectQuery = "Select * from item2primary where primary_id = ? and primary_type = '" + searchValue2 + "'";
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

		// Apply filter so that only DSpace Items are returned (not
		// collections/communities)
		// this method expects a String, so we convert the int constant
		query.setDSpaceObjectFilter(String.valueOf(Constants.ITEM));

		// Limit results to only 1 item (since identifiers are unique, only one match is
		// needed)
		query.setMaxResults(1);

		// Create DSpace object
		DSpace dspace = new DSpace();

		// Get SearchService bean from DSpace ServiceManager
		// This service is responsible for executing Solr (Discovery) queries
		SearchService searcher = dspace.getServiceManager().getServiceByName(SearchService.class.getName(),
				SearchService.class);

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
			List<Integer> itemIds = jdbcTemplate.query(selectQuery, ps -> ps.setString(1, searchValue),
					(rs, rowNum) -> rs.getInt("item_id"));

			// Check if database returned any matching rows
			if (!itemIds.isEmpty()) {

				// Extract item_id
				Integer itemId = itemIds.get(0);

				// Fetch Item using native query
				List<Item> items = itemService.findByCustomNativeQuery(context,
						"SELECT * FROM item WHERE item_id = " + itemId);

				if (!items.isEmpty()) {
					item = items.get(0);
				}
			}
		}

		// Return the found Item (or null if not found in both Solr and DB)
		return item;
	}

	// createitem() implementation from here
	/**
	 * Creates a new Item if no existing item is found.
	 * 
	 * This method replicates legacy UploadUtil.createItem() logic but follows
	 * Spring architecture (no tempContext, no DatabaseManager).
	 */
	private Item createItem(Context context, SingleUploadRequest request) throws SQLException {

		// 1. Extract fields from DTO
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

		String corpName = request.getCorporateName();
		String subscrName = request.getSubscriberName();
		String agencyName = request.getAgencyName();
		String agentName = request.getAgentName();

		String legacyType = request.getLegacyType();

		// 2. Local variables
		Item item = null;
		String ackNo = "";
		String itemType = "";

		// 3. Collection resolution
		Collection collection = null;

		// Flag to check standalone primary case
		boolean isStandAlonePrimary = false;

		// Check if ALL identifiers are empty
		boolean allIdentifiersEmpty = corporateAckNo.isEmpty() && corporateSubAckNo.isEmpty()
				&& retailSubAckNo.isEmpty() && agencyAckNo.isEmpty() && agentAckNo.isEmpty() && choNo.isEmpty()
				&& pran.isEmpty() && pan.isEmpty() && agencyID.isEmpty() && agentID.isEmpty();

		// Standalone primary condition (same as legacy)
		if (allIdentifiersEmpty && !primaryType.isEmpty()) {

			// This means item is created ONLY based on primary
			isStandAlonePrimary = true;

			// Fetch NOTSR collection
			collection = collectionService.find(context, notsrCollectionUUID);

		} else {

			// Default collection for normal items
			collection = collectionService.find(context, defaultCollectionUUID);
		}

		// Safety check (same as legacy "if(col != null)")
		if (collection == null) {
			throw new RuntimeException("Collection not found for item creation"); // implemented hard validation instead
																					// of soft
		} // check like in legacy, now it will throw error if collection is not found
			// continue workflow as legacy

		// 4. Create Workspace Item part
		WorkspaceItem wsItem = null;

		try {
			// 4. Create Workspace Item

			// Create workspace item in selected collection
			wsItem = workspaceItemService.create(context, collection, true);

			// Extract actual Item object from workspace item
			item = wsItem.getItem();

			// Log for debugging (helps track item creation)
			log.info("Workspace item created with ID: {}", item.getID());

			// 5. Determine Item Type

			// Case 1: Corporate Ack No
			if (!corporateAckNo.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_1;
				ackNo = corporateAckNo;
			}

			// Case 2: Corporate Sub Ack No
			else if (!corporateSubAckNo.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_2;
				ackNo = corporateSubAckNo;
			}

			// Case 3: Retail Sub Ack No
			else if (!retailSubAckNo.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_3;
				ackNo = retailSubAckNo;
			}

			// Case 4: Agency Ack No
			else if (!agencyAckNo.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_4;
				ackNo = agencyAckNo;
			}

			// Case 5: Agent Ack No
			else if (!agentAckNo.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_5;
				ackNo = agentAckNo;
			}

			// Case 6: Individual Agent Ack No
			else if (!individualAgentAckNo.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_6;
				ackNo = individualAgentAckNo;
			}

			// Case 7: Legacy Type
			else if (!legacyType.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_9;

				// Add legacy type metadata
				itemService.addMetadata(context, item, "dc", "legacy", "type", null, legacyType);
			}

			// Case 8: Corporate Contribution Ack No
			else if (!corporateContributionAckNo.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_8;
				ackNo = corporateContributionAckNo;
			}

			// Case 9: Subscriber Name
			else if (!subscrName.isEmpty()) {

				if (!choNo.isEmpty()) {
					itemType = UploadConstants.ITEM_TYPE_2;
				} else {
					itemType = UploadConstants.ITEM_TYPE_3;
				}
			}

			// Case 10: Corporate Name
			else if (!corpName.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_1;
			}

			// Case 11: Agent Name
			else if (!agentName.isEmpty()) {

				if (!agencyID.isEmpty()) {
					itemType = UploadConstants.ITEM_TYPE_5;
				} else {
					itemType = UploadConstants.ITEM_TYPE_6;
				}
			}

			// Case 12: Agency Name
			else if (!agencyName.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_4;
			}

			// 6. Handle Identifier Inserts

			// PAN Handling

			// Only execute if NOT standalone primary (same as legacy)
			if (!isStandAlonePrimary) {

				// Case 1: PAN exists
				if (!pan.isEmpty()) {

					// Insert into item2pan table(adding entry into item2pan table)
					panDAO.create(context, pan, item);

					// Add metadata: dc.pan
					itemService.addMetadata(context, item, "dc", "pan", null, null, pan);
				}

				// Case 2: Agency or Agent PAN
				else if (!agencyOrAgentPAN.isEmpty()) {

					// Insert into item2agent_agency_pan table
					agencyPanDAO.create(context, agencyOrAgentPAN, item);

					// Add metadata: dc.pan.ag
					itemService.addMetadata(context, item, "dc", "pan", "ag", null, agencyOrAgentPAN);
				}
			}

			// 7. ACK Handling

			if (!ackNo.isEmpty()) {

				// Insert into item2ackno table
				ackDAO.create(context, ackNo, item);

				// Add metadata: dc.ack.no
				itemService.addMetadata(context, item, "dc", "ack", "no", null, ackNo);
			}

			// 8. PRAN + CHO Handling

			else if (!choNo.isEmpty()) {

				// Case: CHO + PRAN
				if (!pran.isEmpty()) {

					// Override item type (same as legacy)
					itemType = UploadConstants.ITEM_TYPE_2;

					// Insert into item2pran
					pranDAO.create(context, pran, item);

					// Metadata: dc.pran
					itemService.addMetadata(context, item, "dc", "pran", null, null, pran);

					// Metadata: dc.cho
					itemService.addMetadata(context, item, "dc", "cho", null, null, choNo);
				}

				// Case: Only CHO
				else {

					itemType = UploadConstants.ITEM_TYPE_1;

					// Insert into item2corporate
					corporateDAO.create(context, choNo, item);

					// Metadata: dc.cho
					itemService.addMetadata(context, item, "dc", "cho", null, null, choNo);

					// Metadata: dc.cho.sta
					itemService.addMetadata(context, item, "dc", "cho", "sta", null, choNo);
				}
			}

			// Case: Only PRAN
			else if (!pran.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_3;

				// Insert into item2pran
				pranDAO.create(context, pran, item);

				// Metadata: dc.pran
				itemService.addMetadata(context, item, "dc", "pran", null, null, pran);
			}

			// 9. Agency + Agent Handling

			else if (!agencyID.isEmpty()) {

				// Case: Agency + Agent
				if (!agentID.isEmpty()) {

					itemType = UploadConstants.ITEM_TYPE_5;

					// Insert into item2agent
					agentDAO.create(context, agentID, item);

					// Metadata: dc.agency
					itemService.addMetadata(context, item, "dc", "agency", null, null, agencyID);

					// Metadata: dc.agent.id
					itemService.addMetadata(context, item, "dc", "agent", "id", null, agentID);
				}

				// Case: Only Agency
				else {

					itemType = UploadConstants.ITEM_TYPE_4;

					// Insert into item2agency
					agencyDAO.create(context, agencyID, item);

					// Metadata: dc.agency
					itemService.addMetadata(context, item, "dc", "agency", null, null, agencyID);

					// Metadata: dc.agency.sta
					itemService.addMetadata(context, item, "dc", "agency", "sta", null, agencyID);
				}
			}

			// Case: Only Agent
			else if (!agentID.isEmpty()) {

				itemType = UploadConstants.ITEM_TYPE_6;

				// Insert into item2agent
				agentDAO.create(context, agentID, item);

				// Metadata: dc.agency
				itemService.addMetadata(context, item, "dc", "agency", null, null, agencyID);

				// Metadata: dc.agent.id
				itemService.addMetadata(context, item, "dc", "agent", "id", null, agentID);
			}

			// 10. Standalone Primary Handling

			else if (isStandAlonePrimary) {

				itemType = UploadConstants.ITEM_TYPE_7;

				// Insert into item2primary table
				primaryDAO.create(context, primary, primaryType, item);

				// Metadata: dc.primary
				itemService.addMetadata(context, item, "dc", "primary", null, null, primary);

				// Metadata: dc.primary.type
				itemService.addMetadata(context, item, "dc", "primary", "type", null, primaryType);
			}

			// 11. Add Item Type Metadata

			if (!itemType.isEmpty()) {

				itemService.addMetadata(context, item, "dc", "item", "type", null, itemType);
			}

			// 12. Install Item (FINAL STEP)

			// Convert workspace item into final archived item
			item = installItemService.installItem(context, wsItem);

			// Log final success
			log.info("Item successfully created and installed with ID: {}", item.getID());

			// 9. Return Item
			return item;

		} catch (Exception e) {

			log.error("Error while creating item, attempting recovery...", e);

			Item existingItem = null;

			// 1. ACK Recovery
			if (!ackNo.isEmpty() && ackDAO.existsByAckNo(context, ackNo)) {

				log.info("Recovering existing item using ACK: {}", ackNo);
				existingItem = findItemByAck(context, ackNo);
			}

			// 2. CHO Recovery
			if (existingItem == null && !choNo.isEmpty() && corporateDAO.existsByChoNo(context, choNo)) {

				log.info("Recovering existing item using CHO: {}", choNo);
				existingItem = findItemByCorporate(context, choNo);
			}

			// 3. PAN Recovery (SPECIAL CASE)
			if (existingItem == null && !pan.isEmpty() && panDAO.existsByPan(context, pan)) {

				Item panItem = findItemByPan(context, pan);

				// PAN conflict check
				if (panItem != null && !panItem.getID().equals(item.getID())) {

					context.abort();

					UploadResponse uploadResp = new UploadResponse(HttpStatus.BAD_REQUEST.value(),
							"Already registered PAN");

					throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(uploadResp)
							.type("application/json").build());
				}

				log.info("Recovering existing item using PAN: {}", pan);
				existingItem = panItem;
			}

			// 4. Agency PAN Recovery (SPECIAL CASE)
			if (existingItem == null && agencyOrAgentPAN != null && !agencyOrAgentPAN.isEmpty()
					&& agencyPanDAO.existsByPan(context, agencyOrAgentPAN)) {

				Item agencyPanItem = findItemByAgencyPan(context, agencyOrAgentPAN);

				UUID currentItemId = item != null ? item.getID() : null;

				// SAME LOGIC AS PAN 
				if (agencyPanItem != null && currentItemId != null && !agencyPanItem.getID().equals(currentItemId)) {

					context.abort();

					UploadResponse uploadResp = new UploadResponse(HttpStatus.BAD_REQUEST.value(),
							"Already registered PAN");

					throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(uploadResp)
							.type("application/json").build());
				}

				log.info("Recovering existing item using Agency PAN: {}", agencyOrAgentPAN);
				existingItem = agencyPanItem;
			}

			// 5. PRAN Recovery
			if (existingItem == null && !pran.isEmpty() && pranDAO.existsByPran(context, pran)) {

				log.info("Recovering existing item using PRAN: {}", pran);
				existingItem = findItemByPran(context, pran);
			}

			// 6. Agency Recovery
			if (existingItem == null && !agencyID.isEmpty() && agencyDAO.existsByAgencyId(context, agencyID)) {

				log.info("Recovering existing item using Agency ID: {}", agencyID);
				existingItem = findItemByAgency(context, agencyID);
			}

			// 7. Agent Recovery
			if (existingItem == null && !agentID.isEmpty() && agentDAO.existsByAgentId(context, agentID)) {

				log.info("Recovering existing item using Agent ID: {}", agentID);
				existingItem = findItemByAgent(context, agentID);
			}

			// 8. Primary Recovery
			if (existingItem == null && isStandAlonePrimary && primaryDAO.exists(context, primary, primaryType)) {

				log.info("Recovering existing item using Primary: {}", primary);
				existingItem = findItemByPrimary(context, primary, primaryType);
			}

			// FINAL DECISION
			if (existingItem != null) {

				return itemService.find(context, existingItem.getID());
			}

			// Nothing worked
			context.abort();

			UploadResponse uploadResp = new UploadResponse(null, UploadStatus.FAILURE);

			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity(uploadResp).type("application/json").build());
		}
	}

	private Item findItemByAck(Context context, String ackNo) throws SQLException {

		// Fetch mapping entity using DAO
		Item2AckNo entity = ackDAO.findByAckNo(context, ackNo);

		// Return associated item if found
		return entity != null ? entity.getItem() : null;
	}

	private Item findItemByPan(Context context, String pan) throws SQLException {

		Item2Pan entity = panDAO.findByPan(context, pan);

		return entity != null ? entity.getItem() : null;
	}

	private Item findItemByAgencyPan(Context context, String pan) throws SQLException {

		Item2AgentAgencyPan entity = agencyPanDAO.findByPan(context, pan);

		return entity != null ? entity.getItem() : null;
	}

	private Item findItemByPran(Context context, String pran) throws SQLException {

		Item2Pran entity = pranDAO.findByPran(context, pran);

		return entity != null ? entity.getItem() : null;
	}

	private Item findItemByCorporate(Context context, String choNo) throws SQLException {

		Item2Corporate entity = corporateDAO.findByChoNo(context, choNo);

		return entity != null ? entity.getItem() : null;
	}

	private Item findItemByAgency(Context context, String agencyID) throws SQLException {

		Item2Agency entity = agencyDAO.findByAgencyId(context, agencyID);

		return entity != null ? entity.getItem() : null;
	}

	private Item findItemByAgent(Context context, String agentID) throws SQLException {

		Item2Agent entity = agentDAO.findByAgentId(context, agentID);

		return entity != null ? entity.getItem() : null;
	}

	private Item findItemByPrimary(Context context, String primary, String primaryType) throws SQLException {

		Item2Primary entity = primaryDAO.find(context, primary, primaryType);

		return entity != null ? entity.getItem() : null;
	}

	private String parseCreatedDate(String createdDate) {

		try {

			// 1. If empty → use current timestamp
			if (createdDate == null || createdDate.trim().isEmpty()) {

				return new Timestamp(System.currentTimeMillis()).toString();
			}

			// 2. Parse input format (legacy format)
			SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yy hh:mm:ss.SSSSSS aa");

			Date parsedDate = inputFormat.parse(createdDate);

			// 3. Convert to standard format
			SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

			return outputFormat.format(parsedDate);

		} catch (ParseException e) {

			// 4. Handle invalid date input
			throw new WebApplicationException(
					Response.status(Response.Status.BAD_REQUEST).entity(new UploadResponse(Response.Status.BAD_REQUEST.getStatusCode(), "Invalid createdDate format")).build());
		}
	}

	private void updateOtherFields(Context context, Item item, SingleUploadRequest request, String parsedCreatedDate,
			String itemCreationDate) throws SQLException, AuthorizeException {

		// PRIMARY LOGIC (Exact legacy behavior in Spring style)

		String itemType = itemService.getMetadataFirstValue(item, "dc", "item", "type", Item.ANY);

		String primary = request.getPrimary();
		String primaryType = request.getPrimaryType();

		// Don't enter here for standalone primary (ITEM_TYPE_7)
		if (primary != null && !primary.isEmpty() && primaryType != null && !primaryType.isEmpty() && itemType != null
				&& !itemType.equalsIgnoreCase(UploadConstants.ITEM_TYPE_7)) {

			// Replace SQL query with DAO
			Item2Primary existing = primaryDAO.find(context, primary, primaryType);

			if (existing == null) {

				// Equivalent to updateField(PRIMARY_NO,...)
				primaryDAO.create(context, primary, primaryType, item);

				// Metadata handling (same as legacy updateField)
				addPrimaryMetadata(context, item, primary, primaryType);

			} else {

				UUID existingItemId = existing.getItem().getID();

				// If item related to primary not same -> THROW ERROR
				if (!existingItemId.equals(item.getID())) {

					throw new WebApplicationException(
					        Response.status(Response.Status.BAD_REQUEST)
					                .entity(new UploadResponse(
					                        Response.Status.BAD_REQUEST.getStatusCode(),
					                        primaryType + "-" + primary +
					                                " already exists in different " + itemType))
					                .type("application/json")
					                .build()
					);
				}
			}
		}

		// SWITCH CASE (LEGACY EXACT + SPRING STYLE)

		switch (itemType) {

		case UploadConstants.ITEM_TYPE_1:
			updateField(context, item, UploadConstants.PAN, request.getPan(), null, null);
			updateField(context, item, UploadConstants.CORP_NAME, request.getCorpName(), null, null);
			updateField(context, item, UploadConstants.CHO_NO_STA, request.getChoNo(), null, null);
			updateField(context, item, UploadConstants.CBO_DETAILS, request.getCboNo(), request.getCboName(),request.getChoNo());
			break;

		case UploadConstants.ITEM_TYPE_2:
			updateField(context, item, UploadConstants.PAN, request.getPan(), null, null);
			updateField(context, item, UploadConstants.SUBSC_NAME, request.getSubscrName(), null, null);
			updateField(context, item, UploadConstants.EMAIL, request.getEmail(), null, null);
			updateField(context, item, UploadConstants.CONTACT_NO, request.getContactNo(), null, null);
			updateField(context, item, UploadConstants.AADHAAR, request.getAadhaar(), null, null);
			updateField(context, item, UploadConstants.CHO_NO, request.getChoNo(), null, null);
			updateField(context, item, UploadConstants.CBO_DETAILS, request.getCboNo(), request.getCboName(),request.getChoNo());
			updateField(context, item, UploadConstants.PRAN, request.getPran(), null, null);
			break;

		case UploadConstants.ITEM_TYPE_3:
			updateField(context, item, UploadConstants.PAN, request.getPan(), null, null);
			updateField(context, item, UploadConstants.SUBSC_NAME, request.getSubscrName(), null, null);
			updateField(context, item, UploadConstants.EMAIL, request.getEmail(), null, null);
			updateField(context, item, UploadConstants.CONTACT_NO, request.getContactNo(), null, null);
			updateField(context, item, UploadConstants.AADHAAR, request.getAadhaar(), null, null);
			updateField(context, item, UploadConstants.PRAN, request.getPran(), null, null);
			break;

		case UploadConstants.ITEM_TYPE_4:
			updateField(context, item, UploadConstants.AGENCY_N_AGENT_PAN, request.getPan(), null, null);
			updateField(context, item, UploadConstants.AGENCY_NAME, request.getAgencyName(), null, null);
			updateField(context, item, UploadConstants.AGENCY_ID_STA, request.getAgencyID(), null, null);
			break;

		case UploadConstants.ITEM_TYPE_5:
			updateField(context, item, UploadConstants.AGENCY_N_AGENT_PAN, request.getPan(), null, null);
			updateField(context, item, UploadConstants.AGENT_NAME, request.getAgentName(), null, null);
			updateField(context, item, UploadConstants.EMAIL, request.getEmail(), null, null);
			updateField(context, item, UploadConstants.CONTACT_NO, request.getContactNo(), null, null);
			updateField(context, item, UploadConstants.AADHAAR, request.getAadhaar(), null, null);
			updateField(context, item, UploadConstants.AGENCY_ID, request.getAgencyID(), null, null);
			updateField(context, item, UploadConstants.AGENT_ID, request.getAgentID(), null, null);
			break;

		case UploadConstants.ITEM_TYPE_6:
			updateField(context, item, UploadConstants.AGENCY_N_AGENT_PAN, request.getPan(), null, null);
			updateField(context, item, UploadConstants.AGENT_NAME, request.getAgentName(), null, null);
			updateField(context, item, UploadConstants.EMAIL, request.getEmail(), null, null);
			updateField(context, item, UploadConstants.CONTACT_NO, request.getContactNo(), null, null);
			updateField(context, item, UploadConstants.AADHAAR, request.getAadhaar(), null, null);
			updateField(context, item, UploadConstants.AGENT_ID, request.getAgentID(), null, null);
			break;

		case UploadConstants.ITEM_TYPE_8:
			updateField(context, item, UploadConstants.CREATED_BY, request.getCreatedBy(), null, null);
			updateField(context, item, UploadConstants.CREATED_DATE, parsedCreatedDate, null, null);
			break;

		case UploadConstants.ITEM_TYPE_9:
			updateField(context, item, UploadConstants.DOCUMENT_NAME, request.getDocumentName(), null, null);
			updateField(context, item, UploadConstants.CREATED_BY, request.getCreatedBy(), null, null);
			updateField(context, item, UploadConstants.CREATED_DATE, parsedCreatedDate, null, null);
			break;
		}

		// Spring replacement for item.update()
		itemService.update(context, item);
	}

	/*
	 * This method handles adding PRIMARY metadata to the item.
	 * It follows legacy behavior:
	 * 1. Add primary type (dc.primary.type) only if it does not already exist (avoid duplicates)
	 * 2. Always append primary value (dc.primary) since it supports multiple values
	 */
	private void addPrimaryMetadata(Context context, Item item, String primary, String primaryType)
	        throws SQLException {

	    // Fetch all existing "dc.primary.type" metadata values from the item
	    List<MetadataValue> existingTypes = itemService.getMetadata(item, "dc", "primary", "type", Item.ANY);

	    // Check if the given primaryType already exists (case-insensitive comparison)
	    // existingTypes.stream().anyMatch(condition) -> check if ANY item in the list satisfies this condition
	    //this is equivalent to the loop + duplicate check in legacy
	    boolean exists = existingTypes.stream().anyMatch(m -> m.getValue() != null && m.getValue().equalsIgnoreCase(primaryType));
	    
	    // If primaryType is not already present, add it
	    // (prevents duplicate values in metadata), this is skipped above boolean returns true
	    if (!exists) {
	        itemService.addMetadata(context, item, "dc", "primary", "type", null, primaryType);
	    }

	    // Add primary value to "dc.primary"
	    // This is always appended (multi-valued field), not replaced
	    itemService.addMetadata(context, item, "dc", "primary", null, null, primary);
	}

	private void updateField(Context context, Item item, String field, String value1, String value2, String value3)
			throws SQLException {

		// STEP 1: Skip empty, exists method if any either are true
		if (value1 == null || value1.trim().isEmpty())
			return;

		switch (field) {

		// PAN
		case UploadConstants.PAN:
			if (itemService.getMetadataFirstValue(item, "dc", "pan", null, Item.ANY) == null) {
				panDAO.create(context, value1, item);
				itemService.addMetadata(context, item, "dc", "pan", null, null, value1);
			}
			break;

		// AGENCY/AGENT PAN
		case UploadConstants.AGENCY_N_AGENT_PAN:
			if (itemService.getMetadataFirstValue(item, "dc", "pan", "ag", Item.ANY) == null) {
				agencyPanDAO.create(context, value1, item);
				itemService.addMetadata(context, item, "dc", "pan", "ag", null, value1);
			}
			break;

		// PRAN (with DB uniqueness check)
		case UploadConstants.PRAN:
			if (itemService.getMetadataFirstValue(item, "dc", "pran", null, Item.ANY) == null
					&& !pranDAO.existsByPran(context, value1)) {

				pranDAO.create(context, value1, item);
				itemService.addMetadata(context, item, "dc", "pran", null, null, value1);
			}
			break;

		// CHO STANDALONE
		case UploadConstants.CHO_NO_STA:
			if (itemService.getMetadataFirstValue(item, "dc", "cho", null, Item.ANY) == null
					&& !corporateDAO.existsByChoNo(context, value1)) {

				corporateDAO.create(context, value1, item);

				itemService.addMetadata(context, item, "dc", "cho", null, null, value1);
				itemService.addMetadata(context, item, "dc", "cho", "sta", null, value1);
			}
			break;

		// CHO NORMAL
		case UploadConstants.CHO_NO:
			if (itemService.getMetadataFirstValue(item, "dc", "cho", null, Item.ANY) == null) {
				itemService.addMetadata(context, item, "dc", "cho", null, null, value1);
			}
			break;

		// AGENCY ID STANDALONE
		case UploadConstants.AGENCY_ID_STA:
			if (itemService.getMetadataFirstValue(item, "dc", "agency", null, Item.ANY) == null
					&& !agencyDAO.existsByAgencyId(context, value1)) {

				agencyDAO.create(context, value1, item);

				itemService.addMetadata(context, item, "dc", "agency", null, null, value1);
				itemService.addMetadata(context, item, "dc", "agency", "sta", null, value1);
			}
			break;

		// AGENCY ID NORMAL
		case UploadConstants.AGENCY_ID:
			if (itemService.getMetadataFirstValue(item, "dc", "agency", null, Item.ANY) == null) {
				itemService.addMetadata(context, item, "dc", "agency", null, null, value1);
			}
			break;

		// AGENT ID
		case UploadConstants.AGENT_ID:
			if (itemService.getMetadataFirstValue(item, "dc", "agent", "id", Item.ANY) == null
					&& !agentDAO.existsByAgentId(context, value1)) {

				agentDAO.create(context, value1, item);
				itemService.addMetadata(context, item, "dc", "agent", "id", null, value1);
			}
			break;

		// CBO
		case UploadConstants.CBO_DETAILS:
			if (itemService.getMetadataFirstValue(item, "dc", "cbo", null, Item.ANY) == null) {

				if (!cboDAO.existsByCboNo(context, value1)) {
					cboDAO.create(context, value1, value2, value3);
				}

				itemService.addMetadata(context, item, "dc", "cbo", null, null, value1);

				if (value2 != null && !value2.isEmpty()) {
					itemService.addMetadata(context, item, "dc", "cbo", "name", null, value2);
				}
			}
			break;

		// SIMPLE FIELDS

		case UploadConstants.CORP_NAME:
			addIfMissing(context, item, "dc", "corp", "name", value1);
			break;

		case UploadConstants.SUBSC_NAME:
			addIfMissing(context, item, "dc", "subscriber", "name", value1);
			break;

		case UploadConstants.AGENCY_NAME:
			addIfMissing(context, item, "dc", "agency", "name", value1);
			break;

		case UploadConstants.AGENT_NAME:
			addIfMissing(context, item, "dc", "agent", "name", value1);
			break;

		case UploadConstants.EMAIL:
			addIfMissing(context, item, "dc", "email", null, value1);
			break;

		case UploadConstants.CONTACT_NO:
			addIfMissing(context, item, "dc", "contactno", null, value1);
			break;

		case UploadConstants.AADHAAR:
			addIfMissing(context, item, "dc", "aadhaar", null, value1);
			break;

		case UploadConstants.CREATED_BY:
			addIfMissing(context, item, "dc", "createdBy", null, value1);
			break;

		case UploadConstants.CREATED_DATE:
			addIfMissing(context, item, "dc", "createdDate", null, value1);
			break;

		case UploadConstants.DOCUMENT_NAME:
			addIfMissing(context, item, "dc", "document", "name", value1);
			break;
		}
	}

	private void addIfMissing(Context context, Item item, String schema, String element, String qualifier, String value)
			throws SQLException {

		if (value != null && !value.trim().isEmpty() && 
			itemService.getMetadataFirstValue(item, schema, element, qualifier, Item.ANY) == null) {
			itemService.addMetadata(context, item, schema, element, qualifier, null, value);
		}
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