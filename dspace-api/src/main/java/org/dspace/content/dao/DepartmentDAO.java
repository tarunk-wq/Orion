package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Community;
import org.dspace.content.Department;
import org.dspace.core.Context;

public interface DepartmentDAO extends DSpaceObjectDAO<Department>, DSpaceObjectLegacySupportDAO<Department>{
	
	public List<Department> findAll(Context context) throws SQLException;

	public Department findByDepartmentAbbreviation(Context context, String departmentAbbr) throws SQLException; 
	
	Department findByCommunity(Context context, Community community) throws SQLException;
	
	public Department findByUuid(Context context, UUID uuid) throws SQLException;
	
	public Department findByName(Context context, String name) throws SQLException;
	
}
