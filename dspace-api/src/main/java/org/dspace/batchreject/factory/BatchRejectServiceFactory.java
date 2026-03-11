package org.dspace.batchreject.factory;

import org.dspace.batchreject.service.BatchRejectService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class BatchRejectServiceFactory {
public abstract BatchRejectService getBatchRejectService();
	
	public static BatchRejectServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("batchRejectServiceFactory", BatchRejectServiceFactory.class);
	}
}
