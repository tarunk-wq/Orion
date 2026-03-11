package org.dspace.savedsearch.factory;

import org.dspace.savedsearch.service.SavedSearchService;
import org.springframework.beans.factory.annotation.Autowired;

public class SavedSearchServiceFactoryImpl extends SavedSearchServiceFactory{
	
	@Autowired(required = true)
	private SavedSearchService savedSearchService;

	@Override
	public SavedSearchService getSavedSearchService() {
		return savedSearchService;
	}
}

