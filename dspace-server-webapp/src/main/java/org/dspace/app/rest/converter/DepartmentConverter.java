package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.DepartmentDTO;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentConverter extends DSpaceObjectConverter<Department, DepartmentDTO>{

	@Override
    public DepartmentDTO convert(Department dept, Projection projection) {
		DepartmentDTO rest = super.convert(dept, projection);
	        rest.setUuid(String.valueOf(dept.getID()));
	        rest.setDepartmentName(dept.getDepartmentName());
	        rest.setAbbreviation(dept.getAbbreviation());
	        rest.setAdminGroupName(dept.getAdminGroupName());
	        if (dept.getCommunity() != null) {
	            rest.setCommunityId(String.valueOf(dept.getCommunity().getID()));
	        }
	        rest.setProjection(projection);
	        return rest;
    }
	
	@Override
	public Class<Department> getModelClass() {
		return Department.class;
	}

	@Override
	protected DepartmentDTO newInstance() {
		return new DepartmentDTO();
	}
	

}
