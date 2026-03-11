/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usermetadata.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usermetadata.service.UserMetadataFieldsService;

/**
 * Abstract factory to get services for the Audittrail package, use AudittrailServiceFactory.getInstance() to retrieve an
 * implementation
 *
 * @author virsoftech.com
 */
public abstract class UserMetadataFieldsServiceFactory {

    public abstract UserMetadataFieldsService getUserMetadataFieldsService();
    
    public static UserMetadataFieldsServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("userMetadataFieldsServiceFactory", UserMetadataFieldsServiceFactory.class);
    }
}
