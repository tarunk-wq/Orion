/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataLicense;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Bitstream;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * License step for DSpace Spring Rest. Expose the license information about the in progress submission.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class LicenseStep extends AbstractProcessingStep {

    private static final String DCTERMS_RIGHTSDATE = "dcterms.accessRights";
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(LicenseStep.class);

    @Autowired
  	WorkflowItemService<XmlWorkflowItem> wfService;
    
    @Override
    public DataLicense getData(SubmissionService submissionService, InProgressSubmission obj,
            SubmissionStepConfig config)
        throws Exception {
    	Item item = obj.getItem();
    	try {
    		if (wfService == null) {
				wfService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
    		}
        	if (itemService == null) {
				itemService = ContentServiceFactory.getInstance().getItemService();
    		}
        	Context context= ContextUtil.obtainCurrentRequestContext();
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
        DataLicense result = new DataLicense();
        Bitstream bitstream = bitstreamService
            .getBitstreamByName(item, Constants.LICENSE_BUNDLE_NAME, Constants.LICENSE_BITSTREAM_NAME);
        if (bitstream != null) {
            String acceptanceDate = bitstreamService.getMetadata(bitstream, DCTERMS_RIGHTSDATE);
            result.setAcceptanceDate(acceptanceDate);
            result.setUrl(
                configurationService.getProperty("dspace.server.url") + "/api/" + BitstreamRest.CATEGORY + "/" + English
                    .plural(BitstreamRest.NAME) + "/" + bitstream.getID() + "/content");
            result.setGranted(true);
        }
        return result;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {

        if (op.getPath().endsWith(LICENSE_STEP_OPERATION_ENTRY)) {

            PatchOperation<String> patchOperation = new PatchOperationFactory()
                .instanceOf(LICENSE_STEP_OPERATION_ENTRY, op.getOp());
            patchOperation.perform(context, currentRequest, source, op);

        } else {
            throw new UnprocessableEntityException("The path " + op.getPath() + " cannot be patched");
        }
    }
}
