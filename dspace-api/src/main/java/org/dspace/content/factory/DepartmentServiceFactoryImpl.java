package org.dspace.content.factory;

import org.dspace.content.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;

public class DepartmentServiceFactoryImpl implements DepartmentServiceFactory {
	@Autowired(required = true)
    protected DepartmentService departmentService;
	
	@Override
	public DepartmentService getDepartmentService() {
		return departmentService;
	}
}
