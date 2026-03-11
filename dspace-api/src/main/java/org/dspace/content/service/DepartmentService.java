package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.Department;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

import com.fasterxml.jackson.databind.node.ArrayNode;

public interface DepartmentService extends DSpaceObjectService<Department> {
	
	public List<Department> findAllDepartment(Context context) throws SQLException;
	
	public Department findByDepartmentAbbreviation(Context context,String departmentAbbr) throws SQLException;
	
	public Department create(Context context, String departmentName, String adminGroupName, String abbreviation,
			String communityId) throws SQLException, AuthorizeException;
	
    public Department findByCommunity(Context context, Community community) throws SQLException;
    
    public Department findbyUuid(Context context, String uuid) throws SQLException;
    
    public Department findByName(Context context, String name) throws SQLException;

	public ArrayNode findAllLeaf(Context context, UUID depCommId) throws SQLException;

	public void assignAdmin(Context context, UUID depId, String userId) throws SQLException;

	public void removeAdmin(Context context, UUID depId, List<UUID> userIds) throws SQLException;
	
	public ArrayNode getEpersonsArrayNode(Context context, String depId) throws SQLException;

	public ArrayNode findAllFolders(Context context, UUID communityUuid, String query) throws SQLException, AuthorizeException, SearchServiceException;
}
