/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usermetadata.factory;

import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the Audittrail package, use AudittrailServiceFactory.getInstance() to
 * retrieve an implementation
 *
 * @author virsoftech.com
 */
public class UserMetadataFieldsServiceFactoryImpl extends UserMetadataFieldsServiceFactory {


    @Autowired(required = true)
    private UserMetadataFieldsService userMetadataFieldsService;
	
	@Override
	public UserMetadataFieldsService getUserMetadataFieldsService() {
		return userMetadataFieldsService;
	}
	
}
