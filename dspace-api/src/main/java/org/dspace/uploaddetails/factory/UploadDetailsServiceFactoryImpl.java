package org.dspace.uploaddetails.factory;

import org.dspace.uploaddetails.service.UploadDetailsService;
import org.springframework.beans.factory.annotation.Autowired;

public class UploadDetailsServiceFactoryImpl extends UploadDetailsServiceFactory{

    @Autowired(required = true)
	private UploadDetailsService uploadDetailsService;
	
	@Override
	public UploadDetailsService getUploadDetailsService() {
		return uploadDetailsService;
	}
}
