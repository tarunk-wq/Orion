package org.dspace.xmlworkflow.storedcomponents.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.xmlworkflow.storedcomponents.CommunityRole;

public interface CommunityRoleDAO extends GenericDAO<CommunityRole> {

	public List<CommunityRole> findByCommunity(Context context, UUID communityId) throws SQLException;

    /**
     * 
     * @param context
     *            DSpace context
     * @param group
     *            EPerson Group
     * @return the list of CommunityRole assigned to the specified group
     * @throws SQLException
     */
    public List<CommunityRole> findByGroup(Context context, UUID groupId) throws SQLException;

    public CommunityRole findByCommunityAndRole(Context context, UUID communityId, String role)
        throws SQLException;

    public void deleteByCommunity(Context context, UUID communityId) throws SQLException;

	void deleteByCommunity(Context context, Community community) throws SQLException;


}
