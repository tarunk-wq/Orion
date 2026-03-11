package org.dspace.userbitstream.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.userbitstream.service.UserBitstreamService;

public abstract class UserBitstreamFactory{
	
	public abstract UserBitstreamService getUserBitstreamService();
	
	public static UserBitstreamFactory getInstance() {
		return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("userBitstreamFactory", UserBitstreamFactory.class);
	}
}