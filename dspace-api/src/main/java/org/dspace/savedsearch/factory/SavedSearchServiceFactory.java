package org.dspace.savedsearch.factory;

import org.dspace.savedsearch.service.SavedSearchService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class SavedSearchServiceFactory {
	public abstract SavedSearchService getSavedSearchService();
	
	public static SavedSearchServiceFactory getInstance() {
		return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("savedSearchServiceFactory", SavedSearchServiceFactory.class);
	}
}