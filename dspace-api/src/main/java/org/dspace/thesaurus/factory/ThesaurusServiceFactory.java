package org.dspace.thesaurus.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.thesaurus.service.ThesaurusService;

public abstract class ThesaurusServiceFactory {

	public abstract ThesaurusService getThesaurusService();
    
    public static ThesaurusServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("thesaurusServiceFactory", ThesaurusServiceFactory.class);
    }
}
