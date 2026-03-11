package org.dspace.savedsearch.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.savedsearch.SavedSearch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;

import org.dspace.core.Context;

public interface SavedSearchService extends DSpaceObjectService<SavedSearch>, DSpaceObjectLegacySupportService<SavedSearch> {
	
	/**
	 * insert SavedSearch
	 * @param context, epersonId, name, URL
	 * @return
	 * @throws SQLException
	 */
	
	public void insert(Context context, UUID epersonId, String searchName, String url) throws SQLException;
	
	/**
	 * fetch all SavedSearch with respect to the epersonId
	 * @param context, epersonId
	 * @return List<SavedSearch>
	 * @throws SQLException
	 */

	public List<SavedSearch> getSavedSearchByEPersonId(Context context, UUID epersonId) throws SQLException;
	
	/**
	 * fetch SavedSearch by searchName submitted by the epersonId
	 * @param context, epersonId, searchName
	 * @return List<SavedSearch>
	 * @throws SQLException
	 */
	public List<SavedSearch> getSavedSearchByEPersonIdAndSearchName(Context context, UUID epersonId, String searchName) throws SQLException;
	
	/**
	 * delete the SavedSearch with respect to the epersonId on the basis of name
	 * @param context, epersonId
	 * @return 
	 * @throws SQLException, AuthorizeException, IOException
	 */
	public void deleteByID(Context context, UUID epersonId, UUID uuid) throws SQLException, AuthorizeException, IOException;

	List<SavedSearch> findAll(Context context, int limit, int offset) throws SQLException;

	List<SavedSearch> findAll(Context context) throws SQLException;

	int countTotal(Context context) throws SQLException;

}