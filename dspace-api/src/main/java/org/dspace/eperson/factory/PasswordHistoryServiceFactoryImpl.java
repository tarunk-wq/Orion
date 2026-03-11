package org.dspace.eperson.factory;

import org.dspace.eperson.PasswordHistoryServiceImpl;
import org.dspace.eperson.service.PasswordHistoryService;
import org.springframework.stereotype.Component;

@Component("passwordHistoryServiceFactory")
public class PasswordHistoryServiceFactoryImpl extends PasswordHistoryServiceFactory {

    private final PasswordHistoryService passwordHistoryService = new PasswordHistoryServiceImpl();

    @Override
    public PasswordHistoryService getPasswordHistoryService() {
        return passwordHistoryService;
    }
}
