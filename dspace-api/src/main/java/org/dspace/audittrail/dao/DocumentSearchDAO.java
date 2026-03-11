package org.dspace.audittrail.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.audittrail.DocumentSearch;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;

public interface DocumentSearchDAO extends DSpaceObjectDAO<DocumentSearch>, DSpaceObjectLegacySupportDAO<DocumentSearch> {

	public void insert(Context context, String itemIds) throws SQLException ;
	
	public List<DocumentSearch> getDocumentSearchByDateRange(Context context, Date startDate, Date endDate) throws SQLException;
	
	public List<DocumentSearch> getDocumentSearchByUser(Context context, String userName) throws SQLException;
	
	public List<DocumentSearch> getDocumentSearch(Context context, String userName, Date startDate, Date endDate) throws SQLException;
	
	int countRows(Context context) throws SQLException;

	void insert(Context context, String itemIds, String query) throws SQLException;
}
