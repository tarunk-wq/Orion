package org.dspace.batchhistory.factory;

import org.dspace.batchhistory.factory.BatchHistoryServiceFactory;
import org.dspace.batchhistory.service.BatchHistoryService;
import org.dspace.services.factory.DSpaceServicesFactory;
public abstract class BatchHistoryServiceFactory {

	public abstract BatchHistoryService getBatchHistoryService();
	
	public static BatchHistoryServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("batchHistoryServiceFactory", BatchHistoryServiceFactory.class);
	}
}