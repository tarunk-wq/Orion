package org.dspace.batchreject.factory;

import org.dspace.batchreject.service.BatchRejectService;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchRejectServiceFactoryImpl extends BatchRejectServiceFactory {
	@Autowired(required = true)
	private BatchRejectService batchRejectService;
	
	@Override
	public BatchRejectService getBatchRejectService() {
		return batchRejectService;
	}
}
