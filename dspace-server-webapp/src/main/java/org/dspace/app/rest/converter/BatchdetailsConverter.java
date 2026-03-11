package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.BatchDetailsRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.batchreject.BatchReject;
import org.dspace.batchreject.service.BatchRejectService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class BatchdetailsConverter 
		extends DSpaceObjectConverter<Batchdetails, BatchDetailsRest>  {
	
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(BatchdetailsConverter.class);

	@Lazy
    @Autowired
    private ConverterService converter;
	
	@Autowired
	private ItemService itemService;
	
	@Autowired
	private BatchdetailsService batchdetailsService;
	
	@Autowired(required = true)
	private BatchRejectService batchRejectService;
	
    @Override
    public BatchDetailsRest convert(Batchdetails obj, Projection projection) {
        Context context=ContextUtil.obtainCurrentRequestContext();
    	try {
			obj=batchdetailsService.find(context, obj.getID());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	BatchDetailsRest taskRest = super.convert(obj, projection);
    	List<ItemRest> lr=new ArrayList<>();
    	ItemRest item= new ItemRest();
         taskRest.setProjection(projection);
         XmlWorkflowItem witem = obj.getWorkflowId();
         if(witem !=null)
         {
             taskRest.setWorkflowitemid(witem.getID());
             if (witem.getCollection() !=null) {
             	taskRest.setCollectionname(witem.getCollection().getName());
             	taskRest.setCollectionId(witem.getCollection().getID());
             } else if (witem.getCommunity() != null) { 
              	taskRest.setCommunityname(witem.getCommunity().getName());
              	taskRest.setCommunityId(witem.getCommunity().getID());
             }
         }
         taskRest.setId(obj.getID().toString());
         taskRest.setBatchName(obj.getBatchName());
         taskRest.setFileSize(obj.getFileSize());
         taskRest.setTotalFiles(obj.getTotalFiles());
         taskRest.setTotalPages(obj.getTotalPages());
         taskRest.setTotalPdfs(obj.getTotalPdfs());
         taskRest.setMainPdfPageCount(obj.getMainPdfPageCount());
         taskRest.setState(obj.getState());
         
         if(obj.getBatchName() != null) {
        	 try {
				List<BatchReject> batchRejectList = batchRejectService.findByBatchName(context, obj.getBatchName());
				if(!batchRejectList.isEmpty()) {
					taskRest.setRejectReason(batchRejectList.get(0).getReason());
				}
			} catch (Exception e) {
				log.error("Error in returning reason for batchrejection: ",e);
			}
         }
         
         if(obj.getItemIds()!=null)
         {
           taskRest.setItemIdslength(obj.getItemIds().length);
        	 for (UUID itemId : obj.getItemIds()) {
        		 try { 
	        		 Item itemOr = itemService.find(context, itemId);
	            	  if(itemOr!=null)
	            	  {
	            		  if(itemOr.getOwningCollection() != null) {
	                       	taskRest.setCollectionname(itemOr.getOwningCollection().getName());
	                     	taskRest.setCollectionId(itemOr.getOwningCollection().getID());
	            		  }
	            		  item = converter.toRest(itemOr, projection);
	            		  lr.add(item);
	            	  }
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
             // Add ItemRest to the list
        	 }
        	 taskRest.setListOfItemsRest(lr);
         }
         return taskRest;
     }

	@Override
	public Class<Batchdetails> getModelClass() {
		return Batchdetails.class;
	}

	@Override
	protected BatchDetailsRest newInstance() {
		return new BatchDetailsRest();
	}

}
