package org.dspace.batchhistory.factory;

import org.dspace.batchhistory.service.BatchHistoryService;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchHistoryServiceFactoryImpl extends BatchHistoryServiceFactory {

	@Autowired(required = true)
	private BatchHistoryService batchHistoryService;
	
	@Override
	public BatchHistoryService getBatchHistoryService() {
		return batchHistoryService;
	}
}
