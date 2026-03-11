package org.dspace.content.factory;

import org.dspace.content.service.DepartmentService;
import org.dspace.services.factory.DSpaceServicesFactory;

public interface DepartmentServiceFactory {

	public abstract DepartmentService getDepartmentService();

	public static DepartmentServiceFactory getInstance() {
		return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("DepartmentServiceFactory",
				DepartmentServiceFactory.class);
	}
}
