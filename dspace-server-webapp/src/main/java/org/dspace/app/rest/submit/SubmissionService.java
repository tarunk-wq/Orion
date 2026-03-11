/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.AInprogressSubmissionRest;
import org.dspace.app.rest.model.AccessConditionDTO;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.PatchMetadataBundle;
import org.dspace.app.rest.model.PotentialDuplicateRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataCCLicense;
import org.dspace.app.rest.model.step.DataDuplicateDetection;
import org.dspace.app.rest.model.step.DataUpload;
import org.dspace.app.rest.model.step.UploadBitstreamRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.WorkflowItemRestRepository;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.service.UserPermissionService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DuplicateDetectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.virtual.PotentialDuplicate;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.discovery.SearchServiceException;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.service.SubmissionConfigService;
import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.json.patch.PatchException;
import org.springframework.jdbc.datasource.init.UncategorizedScriptException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service to manipulate in-progress submissions.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class SubmissionService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SubmissionService.class);

    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    protected CollectionService collectionService;
    @Autowired
    protected ItemService itemService;
    @Autowired
    protected WorkspaceItemService workspaceItemService;
    @Autowired
    protected WorkflowItemService<XmlWorkflowItem> workflowItemService;
    @Autowired
    protected WorkflowService<XmlWorkflowItem> workflowService;
    @Autowired
    protected CreativeCommonsService creativeCommonsService;
    @Autowired
    private RequestService requestService;
    @Lazy
    @Autowired
    private ConverterService converter;
    @Autowired
    private org.dspace.app.rest.utils.Utils utils;
    @Autowired
    protected CommunityService communityService;
    
    private SubmissionConfigService submissionConfigService;
    @Autowired
    private DuplicateDetectionService duplicateDetectionService;
    @Autowired
    private MetadataFieldService metadataFieldService;
    @Autowired
    private UserPermissionService userPermissionService;
    @Autowired
    private UserMetadataFieldsService userMetadataFieldsService;
    
    private final Map<String, Integer> metadataNameVsID = new ConcurrentHashMap<>();


    public SubmissionService() throws SubmissionConfigReaderException {
        submissionConfigService = SubmissionServiceFactory.getInstance().getSubmissionConfigService();
    }

    /**
     * Create a workspaceitem using the information in the request
     *
     * @param context
     *            the dspace context
     * @param request
     *            the request containing the details about the workspace to create
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     */
    public WorkspaceItem createWorkspaceItem(Context context, Request request) throws SQLException, AuthorizeException {
        WorkspaceItem wsi = null;
        Collection collection = null;
        String collectionUUID = request.getHttpServletRequest().getParameter("owningCollection");
        String communityUUID = request.getHttpServletRequest().getParameter("owningCommunity");

        if (StringUtils.isBlank(collectionUUID)) {
            collectionUUID = configurationService.getProperty("submission.default.collection");
        }

        try {
            if (StringUtils.isNotBlank(collectionUUID)) {
                collection = collectionService.find(context, UUID.fromString(collectionUUID));
            	if (collection == null) {
                    throw new RESTAuthorizationException("collectionUUID=" + collectionUUID + " not found");
                }
            	wsi = workspaceItemService.create(context, collection, true);
            } else if (StringUtils.isNotBlank(communityUUID)) {
            	Community community = communityService.find(context, UUID.fromString(communityUUID));
            	if (community == null) {
                    throw new RESTAuthorizationException("communityUUID= " + communityUUID + " not found");
                }
            	wsi = workspaceItemService.create(context, community, null, true);
            } else {
            	throw new IllegalArgumentException("Parent is null");
//                final List<Collection> findAuthorizedOptimized = collectionService.findAuthorizedOptimized(context,
//                        Constants.ADD);
//                if (findAuthorizedOptimized != null && findAuthorizedOptimized.size() > 0) {
//                    collection = findAuthorizedOptimized.get(0);
//                } else {
//                    throw new RESTAuthorizationException("No collection suitable for submission for the current user");
//                }
            }
        } catch (SQLException e) {
            // wrap in a runtime exception as we cannot change the method signature
            throw new UncategorizedScriptException(e.getMessage(), e);
        } catch (AuthorizeException ae) {
            throw new RESTAuthorizationException(ae);
        }

        return wsi;
    }

    public void saveWorkspaceItem(Context context, WorkspaceItem wsi) {
        try {
            workspaceItemService.update(context, wsi);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Build the rest representation of a bitstream as used in the upload section
     * ({@link DataUpload}. It contains all its metadata and the list of applied
     * access conditions (@link {@link AccessConditionDTO}
     *
     * @param configurationService the DSpace ConfigurationService
     * @param source               the bitstream to translate in its rest submission
     *                             representation
     * @return
     * @throws SQLException
     */
    public UploadBitstreamRest buildUploadBitstream(ConfigurationService configurationService, Bitstream source)
            throws SQLException {
        UploadBitstreamRest data = new UploadBitstreamRest();

        for (MetadataValue md : source.getMetadata()) {

            MetadataValueRest dto = new MetadataValueRest();
            dto.setAuthority(md.getAuthority());
            dto.setConfidence(md.getConfidence());
            dto.setLanguage(md.getLanguage());
            dto.setPlace(md.getPlace());
            dto.setValue(md.getValue());

            String[] metadataToCheck = Utils.tokenize(md.getMetadataField().toString());
            if (data.getMetadata()
                    .containsKey(Utils.standardize(metadataToCheck[0], metadataToCheck[1], metadataToCheck[2], "."))) {
                data.getMetadata().get(Utils.standardize(md.getMetadataField().getMetadataSchema().getName(),
                                                         md.getMetadataField().getElement(),
                                                         md.getMetadataField().getQualifier(), ".")).add(dto);
            } else {
                List<MetadataValueRest> listDto = new ArrayList<>();
                listDto.add(dto);
                data.getMetadata().put(Utils.standardize(md.getMetadataField().getMetadataSchema().getName(),
                                                         md.getMetadataField().getElement(),
                                                         md.getMetadataField().getQualifier(), "."), listDto);
            }

        }
        Projection projection = utils.obtainProjection();
        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        data.setFormat(converter.toRest(source.getFormat(ContextUtil.obtainContext(request)), projection));

        for (ResourcePolicy rp : source.getResourcePolicies()) {
            if (ResourcePolicy.TYPE_CUSTOM.equals(rp.getRpType())) {
                AccessConditionDTO uploadAccessCondition = createAccessConditionFromResourcePolicy(rp);
                data.getAccessConditions().add(uploadAccessCondition);
            }
        }

        data.setUuid(source.getID());
        CheckSumRest checksum = new CheckSumRest();
        checksum.setCheckSumAlgorithm(source.getChecksumAlgorithm());
        checksum.setValue(source.getChecksum());
        data.setCheckSum(checksum);
        data.setSizeBytes(source.getSizeBytes());
        data.setUrl(configurationService.getProperty("dspace.server.url") + "/api/" + BitstreamRest.CATEGORY + "/" +
                        BitstreamRest.PLURAL_NAME + "/" + source.getID() + "/content");
        return data;
    }

    /**
     * Create a workflowitem using the information in the request
     *
     * @param context
     *            the dspace context
     * @param requestUriListString
     *            the id of the workspaceItem
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws WorkflowException
     */
    public XmlWorkflowItem createWorkflowItem(Context context, String requestUriListString)
            throws SQLException, AuthorizeException, WorkflowException {
        XmlWorkflowItem wi = null;
        if (StringUtils.isBlank(requestUriListString)) {
            throw new UnprocessableEntityException("Malformed body..." + requestUriListString);
        }
        String regex = "\\/api\\/" + WorkspaceItemRest.CATEGORY + "\\/" + WorkspaceItemRest.PLURAL_NAME
                + "\\/";
        String[] split = requestUriListString.split(regex, 2);
        if (split.length != 2) {
            throw new UnprocessableEntityException("Malformed body..." + requestUriListString);
        }
        WorkspaceItem wsi = null;
        int id = 0;
        try {
            id = Integer.parseInt(split[1]);
            wsi = workspaceItemService.find(context, id);
        } catch (NumberFormatException e) {
            throw new UnprocessableEntityException("The provided workspaceitem URI is not valid", e);
        }
        if (wsi == null) {
            throw new UnprocessableEntityException("Workspace item is not found");
        }
        WorkspaceItemRest wsiRest = converter.toRest(wsi, utils.obtainProjection());
        if (!wsiRest.getErrors().isEmpty()) {
            throw new UnprocessableEntityException(
                    "Start workflow failed due to validation error on workspaceitem");
        }

        try {
            wi = workflowService.start(context, wsi);
        } catch (IOException e) {
            throw new RuntimeException("The workflow could not be started for workspaceItem with" +
                                               " id:  " + id, e);
        }

        return wi;
    }

    private AccessConditionDTO createAccessConditionFromResourcePolicy(ResourcePolicy rp) {
        AccessConditionDTO accessCondition = new AccessConditionDTO();

        accessCondition.setId(rp.getID());
        accessCondition.setName(rp.getRpName());
        accessCondition.setDescription(rp.getRpDescription());
        accessCondition.setStartDate(rp.getStartDate());
        accessCondition.setEndDate(rp.getEndDate());
        return accessCondition;
    }

    public void saveWorkflowItem(Context context, XmlWorkflowItem source) throws SQLException, AuthorizeException {
        workflowItemService.update(context, source);
    }

    /**
     * Builds the CC License data of an inprogress submission based on the cc license info present in the metadata
     *
     * @param obj   - the in progress submission
     * @return an object representing the CC License data
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public DataCCLicense getDataCCLicense(InProgressSubmission obj)
            throws SQLException, IOException, AuthorizeException {
        DataCCLicense result = new DataCCLicense();
        Item item = obj.getItem();

        result.setUri(creativeCommonsService.getLicenseURI(item));
        result.setRights(creativeCommonsService.getLicenseName(item));

        Bitstream licenseRdfBitstream = creativeCommonsService.getLicenseRdfBitstream(item);
        if (licenseRdfBitstream != null) {
            result.setFile(converter.toRest(licenseRdfBitstream, Projection.DEFAULT));
        }

        return result;
    }

    /**
     * Prepare section data containing a list of potential duplicates, for use in submission steps.
     * This method belongs in SubmissionService and not DuplicateDetectionService because it depends on
     * the DataDuplicateDetection class which only appears in the REST project.
     *
     * @param context DSpace context
     * @param obj     The in-progress submission object
     * @return        A DataDuplicateDetection object which implements SectionData for direct use in
     *                a submission step (see DuplicateDetectionStep)
     * @throws SearchServiceException if an error is encountered during Discovery search
     */
    public DataDuplicateDetection getDataDuplicateDetection(Context context, InProgressSubmission obj)
            throws SearchServiceException {
        // Test for a valid object or throw a not found exception
        if (obj == null) {
            throw new ResourceNotFoundException("Duplicate data step could not find valid in-progress submission obj");
        }
        // Initialise an empty section data object
        DataDuplicateDetection data = new DataDuplicateDetection();

        // Get the item for this submission object, throw a not found exception if null
        Item item = obj.getItem();
        if (item == null) {
            throw new ResourceNotFoundException("Duplicate data step could not find valid item for the" +
                    " current in-progress submission obj id=" + obj.getID());
        }
        // Initialise empty list of PotentialDuplicateRest objects for use in the section data object
        List<PotentialDuplicateRest> potentialDuplicateRestList = new LinkedList<>();

        // Get discovery search result for a duplicate detection search based on this item and populate
        // the list of REST objects
        List<PotentialDuplicate> potentialDuplicates = duplicateDetectionService.getPotentialDuplicates(context, item);
        for (PotentialDuplicate potentialDuplicate : potentialDuplicates) {
            // Convert and add the potential duplicate to the list
            potentialDuplicateRestList.add(converter.toRest(
                    potentialDuplicate, utils.obtainProjection()));
        }

        // Set the final duplicates list of the section data object
        data.setPotentialDuplicates(potentialDuplicateRestList);

        // Return section data
        return data;
    }

    /**
     * Utility method used by the {@link WorkspaceItemRestRepository} and
     * {@link WorkflowItemRestRepository} to deal with the upload in an inprogress
     * submission
     * 
     * @param context DSpace Context Object
     * @param request the http request containing the upload request
     * @param wsi     the inprogress submission current rest representation
     * @param source  the current inprogress submission
     * @param file    the multipartfile of the request
     * @return the errors present in the resulting inprogress submission
     */
    public List<ErrorRest> uploadFileToInprogressSubmission(Context context, HttpServletRequest request,
            AInprogressSubmissionRest wsi, InProgressSubmission source, MultipartFile file, List<String> bundleNames) {
        List<ErrorRest> errors = new ArrayList<ErrorRest>();
        SubmissionConfig submissionConfig =
            submissionConfigService.getSubmissionConfigByName(wsi.getSubmissionDefinition().getName());
        List<Object[]> stepInstancesAndConfigs = new ArrayList<Object[]>();
        // we need to run the preProcess of all the appropriate steps and move on to the
        // upload and postProcess step
        // We will initialize the step class just one time so that it will be the same
        // instance over all the phase and we will reduce initialization time as well
        for (int i = 0; i < submissionConfig.getNumberOfSteps(); i++) {
            SubmissionStepConfig stepConfig = submissionConfig.getStep(i);
            /*
             * First, load the step processing class (using the current
             * class loader)
             */
            ClassLoader loader = this.getClass().getClassLoader();
            Class stepClass;
            try {
                stepClass = loader.loadClass(stepConfig.getProcessingClassName());
                if (UploadableStep.class.isAssignableFrom(stepClass)) {
                    Object stepInstance = stepClass.newInstance();
                    stepInstancesAndConfigs.add(new Object[] {stepInstance, stepConfig});
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        for (Object[] stepInstanceAndCfg : stepInstancesAndConfigs) {
            UploadableStep uploadableStep = (UploadableStep) stepInstanceAndCfg[0];
            if (uploadableStep instanceof ListenerProcessingStep) {
                ((ListenerProcessingStep) uploadableStep).doPreProcessing(context, source);
            }
        }
        for (Object[] stepInstanceAndCfg : stepInstancesAndConfigs) {
            UploadableStep uploadableStep = (UploadableStep) stepInstanceAndCfg[0];
            ErrorRest err;
            try {
                err = uploadableStep.upload(context, this, (SubmissionStepConfig) stepInstanceAndCfg[1],
                        source, file, bundleNames);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (err != null) {
                errors.add(err);
            }
        }
        for (Object[] stepInstanceAndCfg : stepInstancesAndConfigs) {
            UploadableStep uploadableStep = (UploadableStep) stepInstanceAndCfg[0];
            if (uploadableStep instanceof ListenerProcessingStep) {
                ((ListenerProcessingStep) uploadableStep).doPostProcessing(context, source);
            }
        }
        return errors;
    }

    /**
     * Utility method used by the {@link WorkspaceItemRestRepository} and
     * {@link WorkflowItemRestRepository} to deal with the patch of an inprogress
     * submission
     * 
     * @param context DSpace Context Object
     * @param request the http request
     * @param source  the current inprogress submission
     * @param wsi     the inprogress submission current rest representation
     * @param section the section that is involved in the patch
     * @param op      the patch operation
     */
    public void evaluatePatchToInprogressSubmission(Context context, HttpServletRequest request,
            InProgressSubmission source, AInprogressSubmissionRest wsi, String section, Operation op) {
        boolean sectionExist = false;
        SubmissionConfig submissionConfig = submissionConfigService
                .getSubmissionConfigByName(wsi.getSubmissionDefinition().getName());
        List<Object[]> stepInstancesAndConfigs = new ArrayList<Object[]>();
        // we need to run the preProcess of all the appropriate steps and move on to the
        // doPatchProcessing and postProcess step
        // We will initialize the step classes just one time so that it will be the same
        // instance over all the phase and we will reduce initialization time as well
        for (int i = 0; i < submissionConfig.getNumberOfSteps(); i++) {
            SubmissionStepConfig stepConfig = submissionConfig.getStep(i);
            if (section.equals(stepConfig.getId())) {
                sectionExist = true;
            }
            /*
             * First, load the step processing class (using the current class loader)
             */
            ClassLoader loader = this.getClass().getClassLoader();
            Class stepClass;
            try {
                stepClass = loader.loadClass(stepConfig.getProcessingClassName());
                if (RestProcessingStep.class.isAssignableFrom(stepClass)) {
                    Object stepInstance = stepClass.newInstance();
                    stepInstancesAndConfigs.add(new Object[] { stepInstance, stepConfig });
                } else {
                    throw new DSpaceBadRequestException("The submission step class specified by '"
                            + stepConfig.getProcessingClassName()
                            + "' does not implement the interface org.dspace.app.rest.submit.RestProcessingStep!"
                            + " Therefore it cannot be used by the Configurable Submission as the <processing-class>!");
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new PatchException("Error processing the patch request", e);
            }
        }
        if (!sectionExist) {
            throw new UnprocessableEntityException(
                    "The section with name " + section + " does not exist in this submission!");
        }
        for (Object[] stepInstanceAndCfg : stepInstancesAndConfigs) {
            if (stepInstanceAndCfg[0] instanceof ListenerProcessingStep) {
                ListenerProcessingStep step = (ListenerProcessingStep) stepInstanceAndCfg[0];
                step.doPreProcessing(context, source);
            }
        }
        for (Object[] stepInstanceAndCfg : stepInstancesAndConfigs) {
            // only the step related to the involved section need to be invoked
            SubmissionStepConfig stepConfig = (SubmissionStepConfig) stepInstanceAndCfg[1];
            if (!section.equals(stepConfig.getId())) {
                continue;
            }
            DataProcessingStep step = (DataProcessingStep) stepInstanceAndCfg[0];
            try {
                step.doPatchProcessing(context, request, source, op, stepConfig);
            } catch (UnprocessableEntityException e) {
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new PatchException("Error processing the patch request", e);
            }
        }
        for (Object[] stepInstanceAndCfg : stepInstancesAndConfigs) {
            if (stepInstanceAndCfg[0] instanceof ListenerProcessingStep) {
                ListenerProcessingStep step = (ListenerProcessingStep) stepInstanceAndCfg[0];
                step.doPostProcessing(context, source);
            }
        }
    }
    
    public PatchMetadataBundle collectMetadataFromPatch(List<Operation> operations) {

        PatchMetadataBundle bundle = new PatchMetadataBundle();

        for (Operation op : operations) {

            if (!"add".equalsIgnoreCase(op.getOp()) || op.getValue() == null) {
                continue;
            }

            String[] pathParts = op.getPath().substring(1).split("/", 3);
            if (pathParts.length < 3) {
                continue;
            }

            String metadataField = pathParts[2];
            Object rawValue = op.getValue();
            JsonNode valueNode = null;

            if (rawValue instanceof JsonValueEvaluator) {
                valueNode = ((JsonValueEvaluator) rawValue).getValueNode();
            } else if (rawValue instanceof JsonNode) {
                valueNode = (JsonNode) rawValue;
            } else if (rawValue instanceof String) {
                // Handle simple string PATCH
                String str = (String) rawValue;
                if (!str.isBlank()) {
                    bundle.addValue(metadataField, str);
                }
                continue;
            } 
            
            if (valueNode != null && valueNode.isArray()) {
                for (JsonNode node : valueNode) {
                    String val = node.path("value").asText(null);
                    if (val != null && !val.isBlank()) {
                        bundle.addValue(metadataField, val);
                    }
                }
            }
        }

        return bundle;
    }
    
    public boolean checkForDuplicateItems(Context context, Integer workspaceitemId, Map<String, List<String>> metadataMap) throws SQLException {
    	WorkspaceItem workspaceItem = workspaceItemService.find(context, workspaceitemId);
    	DSpaceObject dspaceParent = getParentFromWorkspaceItem(context, workspaceItem);
        Community subDepartment = getSectionFromWorkspaceItem(context, dspaceParent, workspaceitemId);
    	List<UserMetadataFields> userMetadataFields = userMetadataFieldsService.getMetadataFieldBySubDeptHandle(context, subDepartment.getHandle());
    	Map<String,String> uniqueMetadataMap = getUniqueMetadataMap(context,userMetadataFields, metadataMap);
    	
    	if(uniqueMetadataMap.isEmpty()) {
    		return false;
    	}
    	
    	Item currentItem = workspaceItem.getItem();
    	UUID currentItemId = currentItem != null ? currentItem.getID() : null;
    	List<Item> itemList = getItemByMetadatas(context, uniqueMetadataMap, dspaceParent);
    	for (Item dso : itemList) {

    	    if (currentItemId != null && dso.getID().equals(currentItemId)) {
    	        continue;
    	    }
    		
    	    if (dso.isArchived() || workflowItemService.findByItem(context, dso) != null) {
    	        return true;
    	    }
		}
    	
    	return false;
	}
    
    private Map<String, String> getUniqueMetadataMap(Context context, List<UserMetadataFields> userMetadataFields,
			Map<String, List<String>> itemMetadataMap) {
			List<UserMetadataFields> uniqueMedataList = userMetadataFields.stream()
													.filter(e -> Boolean.TRUE.equals(e.getIsUniqueMetadata()))
													.collect(Collectors.toList());
		
		if(uniqueMedataList.isEmpty()) {
			return Map.of();
		}
		
		Map<String, String> metadataMap = new HashMap<>();
		for(UserMetadataFields userMetadataField: uniqueMedataList) {
			String systemFieldName = userMetadataField.getSystemFieldName();
			if(itemMetadataMap.containsKey(systemFieldName)) {
				String metadataValue = itemMetadataMap.get(systemFieldName).get(0);
				metadataMap.put(systemFieldName, metadataValue);
			}
		}
		
		return metadataMap;
	}

	private List<Item> getItemByMetadatas(Context c, Map<String, String> metadataMap, DSpaceObject dspaceParent) {
		List<Item> items = new ArrayList<Item>();
		try {
			if (metadataMap != null) {
				Set<String> metadataNames = metadataMap.keySet();
				if (metadataNames != null && !metadataNames.isEmpty()) {
					fillMetadataRegistryMap(c, metadataNames);
					String hql = "SELECT item FROM Item as item";
			        Map<String, Object> params = new HashMap<>();
					int count = 1;
					for (String metadataName : metadataNames) {
						
						Integer metadataFieldRegistryID = metadataNameVsID.get(metadataName);
						String mdValueJoin = " JOIN MetadataValue AS a%s ON item = a%s.dSpaceObject AND a%s.metadataField.id = "
								+ metadataFieldRegistryID +" AND a%s.value = '" + metadataMap.get(metadataName).replace("'", "''") + "'";
						hql += String.format(mdValueJoin, count, count, count, count);
						count++;
					}
					
					if(dspaceParent instanceof Collection) {
						hql = hql + " WHERE item.owningCollection.id = :parentId ";
					}else if(dspaceParent instanceof Community) {
						hql = hql + " WHERE item.owningCommunity.id = :parentId ";
					}
			        params.put("parentId", dspaceParent.getID());

					items = itemService.findByCustomNativeQuery(c, hql, params);
				}
			}
		} catch (Exception e) {
			log.error("Error in searching item with unique metadata fields", e);
		}
		return items;
	}
    
    private void fillMetadataRegistryMap(Context c, Set<String> metadataNames) throws SQLException {
		if (metadataNames != null) {
			for (String metadataName : metadataNames) {
				if (!metadataNameVsID.containsKey(metadataName)) {
					MetadataField mdfield = metadataFieldService.findByString(c, metadataName, '.');
					if (mdfield != null) {
					    metadataNameVsID.put(metadataName, mdfield.getID());
					}
				}
			}
		}
	}

	public Community getSectionFromWorkspaceItem(Context context, DSpaceObject dspaceParent, Integer workspaceitemId) {
		try {
			DSpaceObject section = userPermissionService.getSecondTopLevelCommunity(context, dspaceParent);
			
			if (section == null) {
	            throw new IllegalStateException("No second top level community found for workspace item: " + workspaceitemId);
	        }
			
			return (Community) section;
		} catch (Exception e) {
			log.error("Error in getting section from workspace item",e);
			throw new IllegalArgumentException("Error in getting section from workspace item");
		}
	}
	
	public DSpaceObject getParentFromWorkspaceItem(Context context, WorkspaceItem workspaceItem) {
		try {
			if (workspaceItem == null) {
	            throw new IllegalArgumentException("WorkspaceItem not found.");
	        }
			DSpaceObject dspaceParent = workspaceItem.getParent();
			return dspaceParent;
		} catch (Exception e) {
			log.error("Error in getting parent of workspace item",e);
			throw new IllegalArgumentException("Error in getting parent of workspace item");
		}
	}

}
