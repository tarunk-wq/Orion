/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.model.PoolTaskRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.uploaddetails.UploadDetails;
import org.dspace.uploaddetails.service.UploadDetailsService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the PoolTask in the DSpace API data model
 * and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class PoolTaskConverter
    implements IndexableObjectConverter<PoolTask, org.dspace.app.rest.model.PoolTaskRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;
    
    @Autowired
    UploadDetailsService uploaddetails;

    @Autowired
    PoolTaskService poolTaskService;
    
    @Override
    public PoolTaskRest convert(PoolTask obj, Projection projection) {
    	Context context = ContextUtil.obtainCurrentRequestContext();

        PoolTaskRest taskRest = new PoolTaskRest();
        taskRest.setProjection(projection);

        XmlWorkflowItem witem = obj.getWorkflowItem();
        taskRest.setId(obj.getID());
        PoolTask poolTask = null;
        try {
			poolTask = poolTaskService.findByWorkflowId(context, witem);
		} catch (SQLException | AuthorizeException | IOException e) {
		}
        
        if (obj.getEperson() != null) {
            taskRest.setEpersonId(obj.getEperson().getID().toString());
        }
        
        if(obj.getGroup() != null) {
        	taskRest.setGroupId(poolTask.getGroup().getID().toString());
        }
        if (witem.getCollection() != null) {
        	taskRest.setCollectionName(witem.getCollection().getName());        
        } else if (witem.getCommunity() != null) {
        	taskRest.setCommunityName(witem.getCommunity().getName());
        }
        taskRest.setBatchname(obj.getWorkflowItem().getBatchName());
        taskRest.setReviewLevel(Utils.getTaskLevelByCollectionUser(context,poolTask.getGroup()));
        taskRest.setStep(obj.getStepID());
        List<UploadDetails> ud=null;
        try {
			ud=uploaddetails.findListByBatchName(ContextUtil.obtainCurrentRequestContext(),taskRest.getBatchname());
		} catch (SQLException e) {
			e.printStackTrace();
		}
        if(ud!=null)
        {
        	taskRest.setOwner(ud.get(0).getSubmitter().getFullName());
        }
        taskRest.setWorkflowitemid(Integer.toString(witem.getID()));
        taskRest.setAction(obj.getActionID());
        return taskRest;
    }

    @Override
    public Class<PoolTask> getModelClass() {
        return PoolTask.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof PoolTask;
    }
}
