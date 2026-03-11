package org.dspace.eperson.factory;

import org.dspace.eperson.service.PasswordHistoryService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class PasswordHistoryServiceFactory {
    public abstract PasswordHistoryService getPasswordHistoryService();

    public static PasswordHistoryServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("passwordHistoryServiceFactory", PasswordHistoryServiceFactory.class);
    }
}