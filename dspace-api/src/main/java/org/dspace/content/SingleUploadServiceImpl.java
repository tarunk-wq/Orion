package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.dao.SourceTokenDAO;
import org.dspace.content.dto.SingleUploadRequest;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleMapService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SingleUploadService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.upload.UploadResponse;
import org.dspace.core.Context;
import org.dspace.item2agentagencypan.dao.AgencyPanDAO;
import org.dspace.item2pan.dao.PanDAO;
import org.dspace.primarytype.dao.PrimaryTypeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

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

	        if (!base64EncodedString.isEmpty() && !docType.isEmpty() && !fileType.isEmpty() && !documentName.isEmpty()) {

	        	String msg;

	        	if (!(msg = validInputs(context, request)).isEmpty()) {

	        	    context.abort();

	        	    return new UploadResponse(HttpStatus.BAD_REQUEST.value(), msg);
	        	}
	        	
	        	// UploadBitstream logic continued, all helpers should throw exception and not return response
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