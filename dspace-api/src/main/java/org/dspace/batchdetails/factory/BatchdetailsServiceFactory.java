package org.dspace.batchdetails.factory;

import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class BatchdetailsServiceFactory{
	
	public abstract BatchdetailsService getBatchdetailsService();
	
	public static BatchdetailsServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("batchdetailsServiceFactory", BatchdetailsServiceFactory.class);
    }
}