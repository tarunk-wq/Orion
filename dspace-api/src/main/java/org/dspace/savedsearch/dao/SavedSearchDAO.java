package org.dspace.savedsearch.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.savedsearch.SavedSearch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;

public interface SavedSearchDAO extends DSpaceObjectDAO<SavedSearch>, DSpaceObjectLegacySupportDAO<SavedSearch> {
	
	public List<SavedSearch> getSavedSearchByEPersonId(Context context, UUID epersonId) throws SQLException;
	
	public List<SavedSearch> getSavedSearchByEPersonIdAndSearchName(Context context, UUID epersonId, String searchName) throws SQLException;
	
	int countRows(Context context) throws SQLException;
	
	public void insert(Context context, UUID epersonId, String searchName, String url) throws SQLException;
	
	public void deleteByID(Context context, UUID epersonId, UUID uuid) throws SQLException, AuthorizeException, IOException;
	
}