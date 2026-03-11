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
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.batchhistory.BatchHistory;
import org.dspace.batchhistory.service.BatchHistoryService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Processing class of an accept/reject action
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ReviewAction extends ProcessingAction {

    public static final int MAIN_PAGE = 0;
    public static final int REJECT_PAGE = 1;

    private static final String SUBMITTER_IS_DELETED_PAGE = "submitter_deleted";
    private static final Logger log = LogManager.getLogger();

    @Autowired
    private XmlWorkflowItemService xmlWorkflowItemService;
    
    @Autowired
    private BatchHistoryService batchhistoryService;
    
    @Autowired
    private BatchdetailsService batchdetailsService ;
    
    @Override
    public void activate(Context c, XmlWorkflowItem wfItem) {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return processAccept(c, wfi);
                case SUBMIT_REJECT:
                    return super.processRejectPage(c, wfi, request);
                case SUBMITTER_IS_DELETED_PAGE:
                    return processSubmitterIsDeletedPage(c, wfi, request);
                default:
                    return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_REJECT);
        options.add(RETURN_TO_POOL);
        return options;
    }

    public ActionResult processAccept(Context c, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        super.addApprovedProvenance(c, wfi);
        Batchdetails bd=batchdetailsService.findByWorkFlowId(c, wfi);
        bd.setState("approved");
        BatchHistory bh=batchhistoryService.create(c,bd.getBatchName(),c.getCurrentUser());
        bh.setFromState("claimed");
        bh.setToState("approved");
        batchhistoryService.update(c, bh);
        bd.setHistoryid(bh);
        batchdetailsService.update(c, bd);
    	if(wfi instanceof XmlWorkflowItem) {
    		wfi = xmlWorkflowItemService.find(c, wfi.getID());
    		addApprovedProvenance(c, wfi);
    	}
    	log.info("APPROVED Batch::"+bd.getBatchName());
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    public ActionResult processSubmitterIsDeletedPage(Context c, XmlWorkflowItem wfi, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException {
        if (request.getParameter("submit_delete") != null) {
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
                                     .deleteWorkflowByWorkflowItem(c, wfi, c.getCurrentUser());
            // Delete and send user back to myDspace page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        } else if (request.getParameter("submit_keep_it") != null) {
            // Do nothing, just send it back to myDspace page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        } else {
            //Cancel, go back to the main task page
            request.setAttribute("page", MAIN_PAGE);
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        }
    }
}
