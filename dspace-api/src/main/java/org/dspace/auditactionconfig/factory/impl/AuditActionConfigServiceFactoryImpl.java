package org.dspace.auditactionconfig.factory.impl;

import org.dspace.auditactionconfig.factory.AuditActionConfigServiceFactory;
import org.dspace.auditactionconfig.service.AuditActionConfigService;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditActionConfigServiceFactoryImpl extends AuditActionConfigServiceFactory {

    @Autowired(required = true)
    private AuditActionConfigService auditActionConfigService;

    @Override
    public AuditActionConfigService getAuditActionConfigService() {
        return auditActionConfigService;
    }
}
