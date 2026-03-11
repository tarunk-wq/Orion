/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;

/**
 * The PoolTask REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(name = PoolTaskRest.STEP, method = "getStep")
})
public class PoolTaskRest extends BaseObjectRest<Integer> {
    public static final String NAME = "pooltask";
    public static final String PLURAL_NAME = "pooltasks";
    public static final String CATEGORY = RestAddressableModel.WORKFLOW;

    public static final String STEP = "step";

    private String action;

    private String epersonId;
    
    private String groupId;

    private String collectionName;
    
    private String communityName;
    
    private String owner;
    
    private String batchname;
    
    private String workflowitemid; 
    
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
     * @see PoolTask#getActionID()
     * @return
     */
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @see PoolTask#getEperson()
     * @return
     */
    public String getEpersonId() {
        return epersonId;
    }

    public void setEpersonId(String eperson) {
        this.epersonId = eperson;
    }

    /**
     * @see PoolTask#getGroup()
     * @return
     */
	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

    /**
     * 
     * @return the WorkflowItemRest that belong to this pool task
     */
    
    public String getWorkflowitemid() {
		return workflowitemid;
	}

	public void setWorkflowitemid(String workflowitemid) {
		this.workflowitemid = workflowitemid;
	}

	public String getBatchname() {
		return batchname;
	}

	public void setBatchname(String batchname) {
		this.batchname = batchname;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getReviewLevel() {
		return reviewLevel;
	}

	public void setReviewLevel(String reviewLevel) {
		this.reviewLevel = reviewLevel;
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

	public String getStep() {
		return step;
	}

	public void setStep(String step) {
		this.step = step;
	}
	
}
