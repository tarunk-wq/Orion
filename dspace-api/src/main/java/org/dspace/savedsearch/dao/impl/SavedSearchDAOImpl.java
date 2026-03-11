package org.dspace.savedsearch.dao.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.savedsearch.SavedSearch;
import org.dspace.savedsearch.dao.SavedSearchDAO;

import jakarta.persistence.Query;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;

public class SavedSearchDAOImpl extends AbstractHibernateDSODAO<SavedSearch> implements SavedSearchDAO {
	protected SavedSearchDAOImpl() {
		super();
	}

	@Override
	public List<SavedSearch> getSavedSearchByEPersonId(Context context, UUID epersonId) throws SQLException {
		String hql = "FROM SavedSearch WHERE epersonId = :epersonId ";
		Query query = createQuery(context, hql);
		query.setParameter("epersonId", epersonId);
		return list(query);
	}
	
	@Override
	public List<SavedSearch> getSavedSearchByEPersonIdAndSearchName(Context context, UUID epersonId, String searchName)
			throws SQLException{
		String hql = "FROM SavedSearch WHERE epersonId = :epersonId AND searchName = :searchName";
		Query query = createQuery(context, hql);
		query.setParameter("epersonId", epersonId);
		query.setParameter("searchName", searchName);
		return list(query);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return count(createQuery(context, "SELECT count(*) FROM SavedSearch"));
	}
	
	@Override
	public void insert(Context context, UUID epersonId, String searchName, String url) throws SQLException {
		create(context, new SavedSearch(epersonId, searchName, url));
	}
	
	@Override
	public void deleteByID(Context context, UUID epersonId, UUID uuid) throws SQLException, AuthorizeException, IOException{
		String hql = "DELETE FROM SavedSearch WHERE eperson_id = :epersonId AND uuid = :uuid";
		 Query query = getHibernateSession(context).createNativeQuery(hql);
		query.setParameter("epersonId", epersonId);
		query.setParameter("uuid", uuid);
		query.executeUpdate();
	}
	
}