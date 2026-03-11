package org.dspace.auditactionconfig.factory;

import org.dspace.auditactionconfig.service.AuditActionConfigService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Factory to obtain AuditActionConfigService from the DSpace service manager.
 */
public abstract class AuditActionConfigServiceFactory {

    public abstract AuditActionConfigService getAuditActionConfigService();

    public static AuditActionConfigServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance()
                .getServiceManager()
                .getServiceByName("auditActionConfigServiceFactory", AuditActionConfigServiceFactory.class);
    }
}
