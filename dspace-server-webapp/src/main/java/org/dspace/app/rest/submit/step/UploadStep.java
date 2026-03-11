/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataUpload;
import org.dspace.app.rest.model.step.UploadBitstreamRest;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload step for DSpace Spring Rest. Expose information about the bitstream
 * uploaded for the in progress submission.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class UploadStep extends AbstractProcessingStep
        implements UploadableStep {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(UploadStep.class);

    private static final Pattern UPDATE_METADATA_PATTERN =
        Pattern.compile("^/sections/[^/]+/files/[^/]+/metadata/[^/]+(/[^/]+)?$");
    private static final Pattern PRIMARY_FLAG_PATTERN =
        Pattern.compile("^/sections/[^/]+/primary$");
    private static final Pattern ACCESS_CONDITION_PATTERN =
        Pattern.compile("^/sections/[^/]+/files/[^/]+/accessConditions(/[^/]+)?$");

    @Autowired
  	WorkflowItemService<XmlWorkflowItem> wfService;
    
    @Override
    public DataUpload getData(SubmissionService submissionService, InProgressSubmission obj,
                              SubmissionStepConfig config) throws Exception {
    	Context context = ContextUtil.obtainCurrentRequestContext();
    	Item item = obj.getItem();
    	try {
    		if (wfService == null) {
				wfService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
    		}
        	if (itemService == null) {
				itemService = ContentServiceFactory.getInstance().getItemService();
    		}
    		if (obj instanceof XmlWorkflowItem) {
    			obj = wfService.find(context, obj.getID());
				UUID[] itemIDs = ((XmlWorkflowItem) obj).getItem_ids();
				if (itemIDs != null && itemIDs.length > 0) {
					item = itemService.find(context, itemIDs[0]);
				}
			}
		} catch (Exception e) {
			log.error("Error in fetching XmlWorkflowItem instance ", e);
		}
        DataUpload result = new DataUpload();
        List<Bundle> bundles = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);
        for (Bundle bundle : bundles) {
            Bitstream primaryBitstream = bundle.getPrimaryBitstream();
            if (Objects.nonNull(primaryBitstream)) {
                result.setPrimary(primaryBitstream.getID());
            }
            for (Bitstream source : bundle.getBitstreams()) {
                UploadBitstreamRest b = submissionService.buildUploadBitstream(configurationService, source);
                result.getFiles().add(b);
            }
        }
        return result;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {

        String instance = null;
        if ("remove".equals(op.getOp())) {
            if (UPDATE_METADATA_PATTERN.matcher(op.getPath()).matches()) {
                instance = UPLOAD_STEP_METADATA_OPERATION_ENTRY;
            } else if (ACCESS_CONDITION_PATTERN.matcher(op.getPath()).matches()) {
                instance = stepConf.getType() + "." + UPLOAD_STEP_ACCESSCONDITIONS_OPERATION_ENTRY;
            } else if (PRIMARY_FLAG_PATTERN.matcher(op.getPath()).matches()) {
                instance = PRIMARY_FLAG_ENTRY;
            } else {
                instance = UPLOAD_STEP_REMOVE_OPERATION_ENTRY;
            }
        } else if ("move".equals(op.getOp())) {
            if (UPDATE_METADATA_PATTERN.matcher(op.getPath()).matches()) {
                instance = UPLOAD_STEP_METADATA_OPERATION_ENTRY;
            } else {
                instance = UPLOAD_STEP_MOVE_OPERATION_ENTRY;
            }
        } else {
            if (ACCESS_CONDITION_PATTERN.matcher(op.getPath()).matches()) {
                instance = stepConf.getType() + "." + UPLOAD_STEP_ACCESSCONDITIONS_OPERATION_ENTRY;
            } else if (UPDATE_METADATA_PATTERN.matcher(op.getPath()).matches()) {
                instance = UPLOAD_STEP_METADATA_OPERATION_ENTRY;
            } else if (PRIMARY_FLAG_PATTERN.matcher(op.getPath()).matches()) {
                instance = PRIMARY_FLAG_ENTRY;
            }
        }
        if (StringUtils.isBlank(instance)) {
            throw new UnprocessableEntityException("The path " + op.getPath() + " is not supported by the operation "
                                                                              + op.getOp());
        }
        PatchOperation<?> patchOperation = new PatchOperationFactory().instanceOf(instance, op.getOp());
        patchOperation.perform(context, currentRequest, source, op);
    }

    @Override
    public ErrorRest upload(Context context, SubmissionService submissionService, SubmissionStepConfig stepConfig,
                            InProgressSubmission wsi, MultipartFile file, List<String> bundleNames) {

        Bitstream source = null;
        BitstreamFormat bf = null;

        Item item = wsi.getItem(); // Get the Item from the current submission
        List<Bundle> bundles = null;
        try {      
            // Read uploaded file as input stream


            InputStream inputStream = new BufferedInputStream(file.getInputStream());

            
            // STEP 1: Validate bundle list which is sent from UI/frontend           

            // If UI did not send bundle hierarchy, return error
            if (bundleNames == null || bundleNames.isEmpty()) {

                ErrorRest error = new ErrorRest();
                error.setMessage("Bundle hierarchy is required for upload.");
                return error;
            }

            // This will help us move level-by-level in hierarchy
            Bundle parent = null;

            
            // STEP 2: Build Bundle Hierarchy
            
            // Go through bundle names in the exact order received from UI
            for (String bundleName : bundleNames) {

                // This will store existing bundle if found
                Bundle existing = null;

                if (parent == null) {
                    // First level → check directly under Item/if not sent in order then duplicate bundle will be created under Item

                    // Get bundles under Item with this name/check if bundles with the name already exists under Item
                    List<Bundle> itemBundles = itemService.getBundles(item, bundleName);

                    // If bundle already exists, reuse it
                    if (!itemBundles.isEmpty()) {
                        existing = itemBundles.get(0);
                    }

                } else {
                    // Not first level → check under current parent bundle

                    for (Bundle child : parent.getSubBundles()) {

                        // If child with same name exists, reuse it
                        if (child.getName().equals(bundleName)) {
                            existing = child;
                            break;
                        }
                    }
                }

                // If bundle does not exist, create it
                if (existing == null) {

                    // Create new bundle under Item
                    existing = bundleService.create(context, item, bundleName);

                    // If this is not first level, link child to parent
                    if (parent != null) {
                        bundleService.addSubBundle(context, parent, existing);
                        bundleService.update(context, parent);
                        bundleService.update(context, existing);
                    }
                    
                }

                // Move one level deeper
                parent = existing;
            }

            
            // STEP 3: Attach Bitstream  

            // After loop ends, 'parent' is the last bundle in hierarchy, the last created bundle
            Bundle targetBundle = parent;

            // Create bitstream inside the deepest bundle
            source = bitstreamService.create(context, targetBundle, inputStream);

            // Set file name properly
            source.setName(context, Utils.getFileName(file));

            // Set original file name
            source.setSource(context, file.getOriginalFilename());

            // Detect and set file format
            bf = bitstreamFormatService.guessFormat(context, source);
            source.setFormat(context, bf);

            // Update bitstream in database
            bitstreamService.update(context, source);
            bundleService.update(context, targetBundle);

            // Update item
            itemService.update(context, item);
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorRest result = new ErrorRest();
            result.setMessage(e.getMessage());
            if (bundles != null && bundles.size() > 0) {
                result.getPaths().add(
                    "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + stepConfig.getId() + "/files/" +
                    bundles.get(0).getBitstreams().size());
            } else {
                result.getPaths()
                    .add("/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + stepConfig.getId());
            }
            return result;
        }

        return null;
    }
}
