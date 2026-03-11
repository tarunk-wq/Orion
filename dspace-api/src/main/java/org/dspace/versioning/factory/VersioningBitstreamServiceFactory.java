/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.service.VersioningBitstreamService;

public abstract class VersioningBitstreamServiceFactory 
{

	public abstract VersioningBitstreamService getVersioningBitstreamService();

	public static VersioningBitstreamServiceFactory getInstance() {
		return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("versioningBitstreamServiceFactory",
				VersioningBitstreamServiceFactory.class);
	}
}
