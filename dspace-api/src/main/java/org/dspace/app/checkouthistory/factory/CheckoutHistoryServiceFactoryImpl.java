package org.dspace.app.checkouthistory.factory;

import org.dspace.app.checkouthistory.service.CheckoutHistoryService;
import org.springframework.beans.factory.annotation.Autowired;

public class CheckoutHistoryServiceFactoryImpl extends CheckoutHistoryServiceFactory {

	@Autowired
	private CheckoutHistoryService checkoutHistoryService;
	
	@Override
	public CheckoutHistoryService getCheckoutHistoryService() {
		return checkoutHistoryService;
	}
}
