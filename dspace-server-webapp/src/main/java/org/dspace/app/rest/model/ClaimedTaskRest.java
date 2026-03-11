/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The ClaimedTask REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(name = ClaimedTaskRest.STEP, method = "getStep")
})
public class ClaimedTaskRest extends BaseObjectRest<Integer> {
    public static final String NAME = "claimedtask";
    public static final String PLURAL_NAME = "claimedtasks";
    public static final String CATEGORY = RestAddressableModel.WORKFLOW;

    public static final String STEP = "step";

    private WorkflowActionRest action;

    private String owner;

    private String workflowitemid; 
    
    private String batchname;
    
    private String collectionName;
    
    private String communityName;

    private String submitter;

    private String reviewLevel;
    
    private String step;
    
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    /**
     * @see ClaimedTaskRest#getAction()
     * @return the action
     */
    public WorkflowActionRest getAction() {
        return action;
    }

    public void setAction(WorkflowActionRest action) {
        this.action = action;
    }

    /**
     * @see ClaimedTaskRest#getOwner()
     * @return the owner of the task
     */
    public String setOwnerId() {
        return owner;
    }

    public void setOwnerId(String owner) {
        this.owner = owner;
    }

    /**
     *
     * @return the WorkflowItemRest that belong to this claimed task
     */
	public String getWorkflowitemid() {
		return workflowitemid;
	}

	public void setWorkflowitemid(String workflowitemid) {
		this.workflowitemid = workflowitemid;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public String getCommunityName() {
		return communityName;
	}

	public void setCommunityName(String communityName) {
		this.communityName = communityName;
	}

	public String getSubmitter() {
		return submitter;
	}

	public void setSubmitter(String submitter) {
		this.submitter = submitter;
	}

    public String getBatchname() {
		return batchname;
	}

	public void setBatchname(String batchname) {
		this.batchname = batchname;
	}

	public String getReviewLevel() {
		return reviewLevel;
	}

	public void setReviewLevel(String reviewLevel) {
		this.reviewLevel = reviewLevel;
	}

	public String getStep() {
		return step;
	}

	public void setStep(String step) {
		this.step = step;
	}
	
}
