package org.dspace.userbitstream.factory.impl;

import org.dspace.userbitstream.factory.UserBitstreamFactory;
import org.dspace.userbitstream.service.UserBitstreamService;
import org.springframework.beans.factory.annotation.Autowired;

public class UserBitstreamFactoryImpl extends UserBitstreamFactory{

	@Autowired(required = true)
	private UserBitstreamService userBitstreamService;
	
	@Override
	public UserBitstreamService getUserBitstreamService() {
		return userBitstreamService;
	}
	
}