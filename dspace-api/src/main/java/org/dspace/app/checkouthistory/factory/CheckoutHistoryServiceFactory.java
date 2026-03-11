package org.dspace.app.checkouthistory.factory;

import org.dspace.app.checkouthistory.service.CheckoutHistoryService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class CheckoutHistoryServiceFactory {

	public abstract CheckoutHistoryService getCheckoutHistoryService();
    
    public static CheckoutHistoryServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("checkoutHistoryServiceFactory", CheckoutHistoryServiceFactory.class);
    }
}
