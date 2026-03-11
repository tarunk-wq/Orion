/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.factory;

import org.dspace.versioning.service.VersioningBitstreamService;
import org.springframework.beans.factory.annotation.Autowired;

public class VersioningBitstreamServiceFactoryImpl extends VersioningBitstreamServiceFactory 
{

	@Autowired(required = true)
    protected VersioningBitstreamService versionBitstreamService;
	
	@Override
	public VersioningBitstreamService getVersioningBitstreamService() 
	{		
		return versionBitstreamService;
	}

}
