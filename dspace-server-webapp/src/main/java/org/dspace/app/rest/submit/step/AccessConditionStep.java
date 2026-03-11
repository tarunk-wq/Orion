/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.AccessConditionDTO;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataAccessCondition;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AccessCondition step for DSpace Spring Rest. Expose information about
 * the resource policies for the in progress submission.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class AccessConditionStep extends AbstractProcessingStep {

	private static final Logger log = LogManager.getLogger();

	@Autowired
	WorkflowItemService<XmlWorkflowItem> wfService;
	
    @Override
    @SuppressWarnings("unchecked")
    public DataAccessCondition getData(SubmissionService submissionService, InProgressSubmission obj,
            SubmissionStepConfig config) throws Exception {
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
        DataAccessCondition accessCondition = new DataAccessCondition();
        accessCondition.setDiscoverable(item.isDiscoverable());
        accessCondition.setAccessConditions(getAccessConditionList(item));
        return accessCondition;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {
        String instance = StringUtils.EMPTY;
        if (op.getPath().contains(ACCESS_CONDITION_STEP_OPERATION_ENTRY)) {
            instance = ACCESS_CONDITION_STEP_OPERATION_ENTRY;
        } else if (op.getPath().contains(ACCESS_CONDITION_POLICY_STEP_OPERATION_ENTRY)) {
            instance = ACCESS_CONDITION_POLICY_STEP_OPERATION_ENTRY;
        }

        if (StringUtils.isBlank(instance)) {
            throw new UnprocessableEntityException("The path " + op.getPath() + " is not supported by the operation "
                                                                              + op.getOp());
        }

        currentRequest.setAttribute("accessConditionSectionId", stepConf.getId());
        PatchOperation<String> patchOperation = new PatchOperationFactory().instanceOf(instance, op.getOp());
        patchOperation.perform(context, currentRequest, source, op);
    }

    private List<AccessConditionDTO> getAccessConditionList(Item item) {
        List<AccessConditionDTO> accessConditions = new ArrayList<AccessConditionDTO>();
        for (ResourcePolicy rp : item.getResourcePolicies()) {
            if (ResourcePolicy.TYPE_CUSTOM.equals(rp.getRpType())) {
                AccessConditionDTO accessConditionDTO = createAccessConditionFromResourcePolicy(rp);
                accessConditions.add(accessConditionDTO);
            }
        }
        return accessConditions;
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

}