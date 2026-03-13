package org.dspace.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.dao.AgencyPanDAO;
import org.dspace.content.dao.PanDAO;
import org.dspace.content.dao.PrimaryTypeDAO;
import org.dspace.content.dao.SourceTokenDAO;
import org.dspace.content.SourceToken;
import org.dspace.content.service.BundleMapService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.dto.SingleUploadRequest;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SingleUploadService;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.upload.UploadResponse;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

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

	private static final Logger log =
            LogManager.getLogger(SingleUploadServiceImpl.class);
	
	// Service used to check bundle_map table.
	@Autowired
	private BundleMapService bundleMapService;

	// Service used for creating and updating Items.
	@Autowired
	private ItemService itemService;

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

	private static final String REGEX =
	        ".*[#$%&!^>=<:;`~*+'\"?()/].*";

	private static final String REGEX_ALLOW_BRACKETS =
	        ".*[#$%>!?+:;^`~*\"'=<].*";

	private static final String SIMPLE_REGEX =
	        ".*[#$%><!~].*";
	
	/**
	 * Temporary fixed collection UUID, i need to see if this is to be used layer
	 * atfer fixing findbundleitem method
	 *
	 * All new items created by the API will be placed in this collection will
	 * replace this later with configuration if needed
	 */
	//private static final UUID FIXED_COLLECTION_UUID = UUID.fromString(" "); commented out for now due to unsure collection logic

	@Override
	public UploadResponse processRequest(Context context, SingleUploadRequest request, HttpServletRequest servletRequest)
	        throws SQLException {

	    // Normalize inputs
	    normalizeInputs(request);

	    // Extract authorization fields
	    String source = servletRequest.getParameter("source");
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

	    // Log request
	    logRequest(request);
	    
	    /*
	     * UploadBitstream logic starts here
	     */

	    boolean isDocUploaded = request.getFile() != null && !request.getFile().trim().isEmpty();

	    if (isDocUploaded) {

	        String base64EncodedString = request.getFile();
	        String docType = request.getDocumentType();
	        String fileType = request.getFileType();
	        String documentName = request.getDocumentName();

	        if (!base64EncodedString.isEmpty() && !docType.isEmpty() && !fileType.isEmpty() && !documentName.isEmpty()) {

	            String msg = "";

	            if (!(msg = validInputs(context, request)).isEmpty()) {

	                context.abort();

	                uploadResp = new UploadResponse(null, UploadStatus.DATA_MISSING);

	                return uploadResp;
	            }
	        }
	    }
	    
	    return null;
	}
	
	/**
     * Normalize incoming request fields (replicates UploadUtil.getTrimmedValue())
     */
	private void normalizeInputs(SingleUploadRequest request) {

	    request.setBundle(trim(request.getBundle()));
	    request.setFile(trim(request.getFile()));

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
    
    private void logRequest(SingleUploadRequest request) {

        String logBody = "{bundle: " + request.getBundle() + ", title: " + (request.getMetadata() != null ? request.getMetadata().get("dc.title") : "null") + "}";

        log.info("Single Upload Request: " + logBody);
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

        String[] otherParams = {
                req.getCboNo(),
                req.getCboName(),
                req.getAadhaar(),
                req.getEmail(),
                req.getContactNo(),
                req.getFileType(),
                req.getCreatedBy(),
                req.getRequirementId()
        };

        if (corporateAckNo.isEmpty() && corporateSubAckNo.isEmpty() && retailSubAckNo.isEmpty()
                && agencyAckNo.isEmpty() && legacyType.isEmpty()
                && agentAckNo.isEmpty() && choNo.isEmpty() && pran.isEmpty()
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

        else if ((!primary.isEmpty() && primaryType.isEmpty())
                || (primary.isEmpty() && !primaryType.isEmpty())) {

            msg = "Primary with Primary Type is mandatory";
        }

        else if (!isSupportedInput(corporateAckNo)
                || !isSupportedInput(corporateSubAckNo)
                || !isSupportedInput(retailSubAckNo)
                || !isSupportedInput(agencyAckNo)
                || !isSupportedInput(agentAckNo)
                || !isSupportedInput(individualAgentAckNo)
                || !isSupportedInput(choNo)
                || !isSupportedInput(pan)
                || !isSupportedInput(agencyOrAgentPAN)
                || !isSupportedInput(pran)
                || !isSupportedInput(agencyID)
                || !isSupportedInput(agentID)
                || !isSupportedInput(primary)
                || !isSupportedInput(primaryType)
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

        else if (!isSupportedInput(creationDate, SIMPLE_REGEX)
                || !isSupportedInput(corpName, SIMPLE_REGEX)) {

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
    public AuthorizationStatus authorizeRequest(Context context, String source, String token)
            throws SQLException {

        // Replicates UploadBitstream logic exactly

        if (token == null || token.trim().isEmpty()) {
            return AuthorizationStatus.MISSING_TOKEN;
        }

        if (source == null || source.trim().isEmpty()) {
            return AuthorizationStatus.MISSING_SOURCE;
        }

        // Find source in DB
        SourceToken tokenRow = sourceTokenDAO.findBySource(context, source);

        if (tokenRow == null) {
            return AuthorizationStatus.UNRECOGNIZED_SOURCE;
        }

        if (!tokenRow.isActive()) {
            return AuthorizationStatus.DEACTIVATED_TOKEN;
        }

        if (!tokenRow.getToken().equals(token)) {
            return AuthorizationStatus.INVALID_TOKEN;
        }

        return AuthorizationStatus.AUTHORIZED;
    }
    
	/**
	 * Main method executed by the controller.
	 *
	 * This method performs the entire upload workflow.
	 *
	 * Steps performed: 1. Split mapping (Parent|Child) 2. Validate mapping exists
	 * in bundle_map table 3. Create Item with metadata 4. Check or create parent
	 * bundle 5. Check or create child bundle 6. Decode Base64 file 7. Create
	 * Bitstream and attach to child bundle
	 */
	@Override
	public void handleSingleUpload(Context context, String bundle, String base64File, String title)
			throws SQLException, AuthorizeException, IOException {

		/**
		 * Split bundle string into parent and child bundle names Example: "A|B"
		 * parentName = A childName = B
		 */
		String[] parts = bundle.split("\\|");

		String parentName = parts[0];
		String childName = parts[1];

		/**
		 * Validate that this mapping exists in bundle_map table
		 *
		 * This ensures only predefined bundle hierarchies are allowed
		 */
		validateMapping(context, bundle, parentName, childName);

		// Create a new Item and add metadata (dc.title)
		Item item = createItemWithMetadata(context, title);

		// Resolve bundle hierarchy
		Bundle workingBundle = resolveWorkingBundle(context, bundle, item);

		// Decode the Base64 file and create a Bitstream
		// The Bitstream is then attached to the child bundle
		addBitstream(context, workingBundle, base64File);

		// Update Item after all changes are complete
		itemService.update(context, item);
	}

	// Validates whether the given mapping exists in the bundle_map table
	// If the mapping does not exist,the upload should be rejected

	private void validateMapping(Context context, String bundle, String parent, String child) throws SQLException {

		boolean exists = bundleMapService.isValidMapping(context, bundle, parent, child);

		if (!exists) {
			throw new IllegalArgumentException("Mapping does not exist in bundle_map table");
		}
	}

	// Creates a new Item inside a predefined collection and adds metadata to it
	// Currently only dc.title is added

	private Item createItemWithMetadata(Context context, String title) throws SQLException, AuthorizeException {

		// Find the collection where the item should be created
		Collection collection = null;

		if (collection == null) {
			throw new IllegalArgumentException("Collection not found");
		}

		// Create a workspace item first (required in new tech)
		WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);

		// Get the actual Item object from the workspace item
		Item item = workspaceItem.getItem();
		
		// Add metadata: dc.title
		// Authority and confidence are not used for this field
		itemService.addMetadata(context, item, "dc", "title", null, null, title, null, -1, -1);

		// Save item changes
		itemService.update(context, item);

		return item;
	}

	/**
	 * This method determines the exact bundle where the uploaded file should
	 * finally be stored.
	 *
	 * The hierarchy is decided using the bundle_map table.
	 */
	private Bundle resolveWorkingBundle(Context context, String bundleName, Item item)
			throws SQLException, AuthorizeException {

		//Find the mapping in bundle_map table
		List<BundleMap> mappings = bundleMapService.findByBundle(context, bundleName);
		
		if (mappings == null || mappings.isEmpty()) {
		    throw new IllegalArgumentException("No bundle mapping found for " + bundleName);
		}

		//For now use the first mapping
		BundleMap mapping = mappings.get(0);
				
		//Extract bundle names and hierarchy
		String parentBundleName = mapping.getParentBundleName();
		String childBundleName = mapping.getChildBundleName();

		//Find or create the parent bundle inside Item
		Bundle parentBundle = null;

		//Search existing bundles attached to the item
		for (Bundle bundle : item.getBundles()) {

			if (bundle.getName().equalsIgnoreCase(parentBundleName)) {
				parentBundle = bundle;
				break;
			}
		}

		//If parent bundle does not exist, create it
		if (parentBundle == null) {

			parentBundle = bundleService.create(context, item, parentBundleName);
		}

		//Find or create the child bundle
		Bundle childBundle = null;

		// Check if child bundle already exists under parent
		for (Bundle subBundle : parentBundle.getSubBundles()) {

			if (subBundle.getName().equalsIgnoreCase(childBundleName)) {
				childBundle = subBundle;
				break;
			}
		}

		//If child bundle does not exist, create it
		if (childBundle == null) {

			childBundle = bundleService.create(context, null, childBundleName);

			//Attach the child bundle under parent bundle
			parentBundle.getSubBundles().add(childBundle);

			//Save changes
			bundleService.update(context, parentBundle);
		}

		//Final bundle where the file will be uploaded
		return childBundle;
	}

	// Decodes Base64 file and creates a Bitstream.
	// The Bitstream is stored inside the child bundle.
	private Bitstream addBitstream(Context context, Bundle childBundle, String base64File)
			throws IOException, SQLException, AuthorizeException {

		// Decode Base64 string into binary data.
		byte[] decodedBytes = Base64.getDecoder().decode(base64File);

		InputStream inputStream = new ByteArrayInputStream(decodedBytes);

		// Create Bitstream inside the child bundle.
		Bitstream bitstream = bitstreamService.create(context, childBundle, inputStream);

		// Assign a temporary file name.
		bitstream.setName(context, "uploaded_file");

		// Save Bitstream changes.
		bitstreamService.update(context, bitstream);

		// Update bundle.
		bundleService.update(context, childBundle);

		return bitstream;
	}
	
}