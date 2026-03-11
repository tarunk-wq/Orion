package org.dspace.xmlworkflow.storedcomponents;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.dao.CommunityRoleDAO;
import org.dspace.xmlworkflow.storedcomponents.service.CommunityRoleService;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityRoleServiceImpl implements CommunityRoleService {

	@Autowired(required = true)
    protected CommunityRoleDAO communityRoleDAO;
	
	@Override
	public CommunityRole find(Context context, int id) throws SQLException {
		return communityRoleDAO.findByID(context, CommunityRole.class, id);
	}

	@Override
	public CommunityRole find(Context context, UUID communityId, String role) throws SQLException {
		return communityRoleDAO.findByCommunityAndRole(context, communityId, role);
	}

	@Override
	public List<CommunityRole> findByGroup(Context context, UUID groupId) throws SQLException {
		return communityRoleDAO.findByGroup(context, groupId);
	}

	@Override
	public List<CommunityRole> findByCommunity(Context context, UUID communityId) throws SQLException {
		return communityRoleDAO.findByCommunity(context, communityId);
	}

	@Override
	public CommunityRole create(Context context, Community community, String roleId, Group group) throws SQLException {
		CommunityRole communityRole = new CommunityRole();
		
		communityRole.setCommunity(community);
		communityRole.setRoleId(roleId);
		communityRole.setGroup(group);
		
		return communityRoleDAO.create(context, communityRole);
	}

	@Override
	public void update(Context context, CommunityRole communityRole) throws SQLException {
		communityRoleDAO.save(context, communityRole);
	}

	@Override
	public void delete(Context context, CommunityRole communityRole) throws SQLException {
		communityRoleDAO.delete(context, communityRole);
	}

	@Override
	public void deleteByCommunity(Context context, UUID communityId) throws SQLException {
		communityRoleDAO.deleteByCommunity(context, communityId);
	}

	@Override
	public void deleteByCommunity(Context context, Community community) throws SQLException {
		communityRoleDAO.deleteByCommunity(context, community);
	}

}
