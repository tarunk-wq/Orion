/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.model.ClaimedTaskRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.discovery.IndexableObject;
import org.dspace.uploaddetails.UploadDetails;
import org.dspace.uploaddetails.service.UploadDetailsService;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the laimTask in the DSpace API data model
 * and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ClaimedTaskConverter
        implements IndexableObjectConverter<ClaimedTask, ClaimedTaskRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Autowired
    protected XmlWorkflowFactory xmlWorkflowFactory;

    @Autowired
    UploadDetailsService uploaddetails;
    
    @Override
    public ClaimedTaskRest convert(ClaimedTask obj, Projection projection) {
        ClaimedTaskRest taskRest = new ClaimedTaskRest();
        taskRest.setProjection(projection);
        XmlWorkflowItem witem = obj.getWorkflowItem();
        taskRest.setId(obj.getID());
        taskRest.setAction(converter.toRest(xmlWorkflowFactory.getActionByName(obj.getActionID()), projection));
        taskRest.setOwnerId(obj.getOwner().getID().toString());
        taskRest.setBatchname(obj.getWorkflowItem().getBatchName());
        taskRest.setWorkflowitemid(Integer.toString(witem.getID()));
        taskRest.setStep(obj.getStepID());
        if (witem.getCollection() !=null) {
        	taskRest.setCollectionName(witem.getCollection().getName());
        } else if (witem.getCommunity() !=null) {
        	taskRest.setCommunityName(witem.getCommunity().getName());
        }
        List<UploadDetails> ud=null;
        try {
			ud=uploaddetails.findListByBatchName(ContextUtil.obtainCurrentRequestContext(),taskRest.getBatchname());
		} catch (SQLException e) {
			e.printStackTrace();
		}
        if(ud!=null)
        {
        	taskRest.setSubmitter(ud.get(0).getSubmitter().getFullName());
        }
        return taskRest;
    }

    @Override
    public Class<ClaimedTask> getModelClass() {
        return ClaimedTask.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof ClaimedTask;
    }
}
