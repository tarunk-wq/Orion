package org.dspace.batchdetails.factory;

import org.dspace.batchdetails.service.BatchdetailsService;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchdetailsServiceFactoryImpl extends BatchdetailsServiceFactory{

    @Autowired(required = true)
	private BatchdetailsService batchdetailsService;
	
	@Override
	public BatchdetailsService getBatchdetailsService() {
		return batchdetailsService;
	}
	
}