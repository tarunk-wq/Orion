package org.dspace.thesaurus.factory;

import org.dspace.thesaurus.service.ThesaurusService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the thesaurus package, use ThesaurusServiceFactory.getInstance() to
 * retrieve an implementation
 *
 * @author virsoftech.com
 */

public class ThesaurusServiceFactoryImpl extends ThesaurusServiceFactory {

	@Autowired
	private ThesaurusService thesaurusService;
	
	@Override
	public ThesaurusService getThesaurusService() {
		return thesaurusService;
	}
}
