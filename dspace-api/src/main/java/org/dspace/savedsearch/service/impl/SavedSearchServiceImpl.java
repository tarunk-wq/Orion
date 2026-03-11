package org.dspace.savedsearch.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.savedsearch.SavedSearch;
import org.dspace.savedsearch.dao.SavedSearchDAO;
import org.dspace.savedsearch.service.SavedSearchService;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class SavedSearchServiceImpl extends DSpaceObjectServiceImpl<SavedSearch> implements SavedSearchService{

	@Autowired(required = true)
	protected SavedSearchDAO savedSearchDAO;

	
	@Override
	public List<SavedSearch> findAll(Context context) throws SQLException {
		return savedSearchDAO.findAll(context, SavedSearch.class);
	}

	@Override
	public List<SavedSearch> findAll(Context context, int limit, int offset) throws SQLException {
		return savedSearchDAO.findAll(context, SavedSearch.class, limit, offset);
	}
	    
	@Override
	public void deleteByID(Context context, UUID epersonId, UUID uuid) throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub
		savedSearchDAO.deleteByID(context, epersonId, uuid);
	}

	@Override
	public void insert(Context context, UUID epersonId, String searchName, String url) throws SQLException {
		savedSearchDAO.insert(context, epersonId, searchName, url);
	}

	@Override
	public List<SavedSearch> getSavedSearchByEPersonId(Context context, UUID epersonId) throws SQLException {
		return savedSearchDAO.getSavedSearchByEPersonId(context, epersonId);
	}
	
	@Override
	public List<SavedSearch> getSavedSearchByEPersonIdAndSearchName(Context context, UUID epersonId, String searchName) throws SQLException {
		return savedSearchDAO.getSavedSearchByEPersonIdAndSearchName(context, epersonId, searchName);
	}
	
	@Override
	public int countTotal(Context context) throws SQLException {
        return savedSearchDAO.countRows(context);
    }

	@Override
	public SavedSearch find(Context context, UUID epersonId) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLastModified(Context context, SavedSearch dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, SavedSearch dso) throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SavedSearch findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SavedSearch findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
}