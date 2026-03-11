/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.batchhistory.BatchHistory;
import org.dspace.batchhistory.service.BatchHistoryService;
import org.dspace.batchreject.BatchReject;
import org.dspace.batchreject.service.BatchRejectService;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.scripts.service.ProcessService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.actions.Action;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Represent an action that can be offered to a workflow step's user(s).
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class ProcessingAction extends Action {

    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired
    protected XmlWorkflowService xmlWorkflowService;

    @Autowired
    private BatchHistoryService batchhistoryService;
    
    @Autowired
    private BatchdetailsService batchdetailsService ;
    
    @Autowired
    private BatchRejectService batchrejectService;

    @Autowired
    private ProcessService processService;
    
    public static final String SUBMIT_EDIT_METADATA = "submit_edit_metadata";
    public static final String SUBMIT_CANCEL = "submit_cancel";
    protected static final String SUBMIT_APPROVE = "submit_approve";
    protected static final String SUBMIT_REJECT = "submit_reject";
    protected static final String RETURN_TO_POOL = "return_to_pool";
    protected static final String REJECT_REASON = "reason";

    private static final Logger log = LogManager.getLogger();

    @Override
    public boolean isAuthorized(Context context, HttpServletRequest request, XmlWorkflowItem wfi) throws SQLException {
        ClaimedTask task = null;
        if (context.getCurrentUser() != null) {
            task = claimedTaskService.findByWorkflowIdAndEPerson(context, wfi, context.getCurrentUser());
        }
        //Check if we have claimed the current task
        return task != null &&
            task.getWorkflowID().equals(getParent().getStep().getWorkflow().getID()) &&
            task.getStepID().equals(getParent().getStep().getId()) &&
            task.getActionID().equals(getParent().getId());
    }

    /**
     * Process result when option {@link this#SUBMIT_REJECT} is selected.
     * - Sets the reason and workflow step responsible on item in dc.description.provenance
     * - Send workflow back to the submission
     * If reason is not given => error
     */
    public ActionResult processRejectPage(Context c, XmlWorkflowItem wfi, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        String reason = request.getParameter(REJECT_REASON);
        if (reason == null || 0 == reason.trim().length()) {
            request.setAttribute("page", 1);
            addErrorField(request, REJECT_REASON);
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }
        for(UUID uuid:wfi.getItem_ids())
        {
        	Item item=itemService.find(c, uuid);
        	XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
            .sendEachWorkflowItemBackSubmission(c, wfi, c.getCurrentUser(),
                this.getProvenanceStartId(), reason,item);

        }
        Batchdetails bd=batchdetailsService.findByWorkFlowId(c, wfi);
        BatchReject br=batchrejectService.create(c, bd.getBatchName(), reason);
        List<org.dspace.scripts.Process> pList= processService.findByBatchName(c, bd.getBatchName()); 
        if(!pList.isEmpty()) {
        	org.dspace.scripts.Process p = pList.get(0);
        	p.setProcessStatus(ProcessStatus.REJECTED);
        	processService.update(c, p);
        }
        br.setItemIds(bd.getItemIds());
        br.setUser(c.getCurrentUser());
        batchrejectService.update(c, br);
        bd.setState("rejected");
        BatchHistory bh=batchhistoryService.create(c,bd.getBatchName(),c.getCurrentUser());
        bh.setToState("rejected");
        bh.setFromState("claimed");
        batchhistoryService.update(c, bh);
        bd.setHistoryid(bh);
        batchdetailsService.update(c, bd);
        //Closing Workflow
        XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().closeWorkflow(c,wfi);
    	log.info("REJECTED Batch::"+bd.getBatchName());
        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }

    @Override
    protected boolean isAdvanced() {
        return !getAdvancedOptions().isEmpty();
    }
}
