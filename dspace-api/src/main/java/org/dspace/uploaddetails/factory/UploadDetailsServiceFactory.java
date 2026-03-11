package org.dspace.uploaddetails.factory;

import org.dspace.uploaddetails.service.UploadDetailsService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class UploadDetailsServiceFactory {

	public abstract UploadDetailsService getUploadDetailsService();
	
	public static UploadDetailsServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("uploaddetailsServiceFactory", UploadDetailsServiceFactory.class);
    }
}
