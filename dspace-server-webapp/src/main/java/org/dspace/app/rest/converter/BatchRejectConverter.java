package org.dspace.app.rest.converter;

import java.sql.SQLException;

import org.dspace.app.rest.model.BatchRejectRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.batchreject.BatchReject;
import org.dspace.batchreject.service.BatchRejectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class BatchRejectConverter 
                extends DSpaceObjectConverter<BatchReject,BatchRejectRest>{

	@Lazy
    @Autowired
    private ConverterService converter;
	
	@Autowired
	BatchRejectService batchrejectService;
	
	@Autowired
	private BatchdetailsService batchdetailsService;
	
	@Override
	public Class<BatchReject> getModelClass() {
		// TODO Auto-generated method stub
		return BatchReject.class;
	}
	 @Override
	    public BatchRejectRest convert(BatchReject obj, Projection projection) {
	        Context context=ContextUtil.obtainCurrentRequestContext();
	        
	        try {
				obj=batchrejectService.find(context, obj.getID());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        BatchRejectRest taskRest = super.convert(obj, projection);
	        Batchdetails bd=new Batchdetails();
	        try {
				 bd=batchdetailsService.findByBatchName(context, obj.getBatchName());
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        taskRest.setProjection(projection);
	        taskRest.setBatchName(obj.getBatchName());
	        taskRest.setReason(obj.getReason());
	        taskRest.setTotalFiles(bd.getTotalFiles());
	        taskRest.setTotalPages(bd.getTotalPages());
	        taskRest.setTotalPdfs(bd.getTotalPdfs());
	        return taskRest;
	 }
	@Override
	protected BatchRejectRest newInstance() {
		// TODO Auto-generated method stub
		return new BatchRejectRest();
	}

}
