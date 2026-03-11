package org.dspace.xmlworkflow.storedcomponents.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.storedcomponents.CommunityRole;
import org.dspace.xmlworkflow.storedcomponents.dao.CommunityRoleDAO;

public class CommunityRoleDAOImpl extends AbstractHibernateDAO<CommunityRole> implements CommunityRoleDAO {

	protected CommunityRoleDAOImpl() {
        super();
    }
	
	@Override
	public List<CommunityRole> findByCommunity(Context context, UUID communityId) throws SQLException {
		String hql = "FROM CommunityRole WHERE community.id = :communityId";
	    Query query = createQuery(context, hql);
	    query.setParameter("communityId", communityId);
	    return list(query);
	}

	@Override
	public List<CommunityRole> findByGroup(Context context, UUID groupId) throws SQLException {
		String hql = "FROM CommunityRole WHERE group.id = :groupId";
	    Query query = createQuery(context, hql);
	    query.setParameter("groupId", groupId);
	    return list(query);
	}

	@Override
	public CommunityRole findByCommunityAndRole(Context context, UUID communityId, String role) throws SQLException {
		String hql = "FROM CommunityRole WHERE community.id = :communityId AND roleId = :roleId";	    
		Query query = createQuery(context, hql);
	    query.setParameter("communityId", communityId);
	    query.setParameter("roleId", role);
	    return singleResult(query);
	}

	@Override
	public void deleteByCommunity(Context context, UUID communityId) throws SQLException {
		String hql = "DELETE FROM CommunityRole WHERE community.id = :communityId";
        Query query = createQuery(context, hql);
        query.setParameter("communityId", communityId);
        query.executeUpdate();
	}
	
	@Override
    public void deleteByCommunity(Context context, Community community) throws SQLException {
        String hql = "delete from CommunityRole WHERE community=:community";
        Query query = createQuery(context, hql);
        query.setParameter("community", community);
        query.executeUpdate();
    }

}
