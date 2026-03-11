/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;
import org.dspace.eperson.EPerson;

/**
 * The WorkflowItem REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(name = WorkflowItemRest.STEP, method = "getStep"),
    @LinkRest(name = WorkflowItemRest.SUBMITTER, method = "getWorkflowItemSubmitter"),
    @LinkRest(name = WorkflowItemRest.ITEM, method = "getWorkflowItemItem"),
    @LinkRest(name = WorkflowItemRest.COLLECTION, method = "getWorkflowItemCollection")
})
public class WorkflowItemRest extends AInprogressSubmissionRest {
    public static final String NAME = "workflowitem";
    public static final String PLURAL_NAME = "workflowitems";
    public static final String CATEGORY = RestAddressableModel.WORKFLOW;

    public static final String STEP = "step";

    public static final String SUBMITTER = "submitter";
    public static final String ITEM = "item";
    public static final String COLLECTION = "collection";

    private GroupRest group;

    private CollectionRest collection;

    private String batchName;
        
    private EPerson uploader;
    
    public EPerson getUploader() {
		return uploader;
	}

	public void setUploader(EPerson uploader) {
		this.uploader = uploader;
	}

	public GroupRest getGroup() {
		return group;
	}

	public void setGroup(GroupRest group) {
		this.group = group;
	}

   public CollectionRest getCollection() {
		return collection;
	}
   
	public void setCollection(CollectionRest collection) {
		this.collection = collection;
	}

	public String getBatchName() {
		return batchName;
	}

	public void setBatchName(String batchName) {
		this.batchName = batchName;
	}

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
}
