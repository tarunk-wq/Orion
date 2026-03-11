package org.dspace.audittrail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.audittrail.dao.DocumentSearchDAO;
import org.dspace.audittrail.service.DocumentSearchService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class DocumentSearchServiceImpl extends DSpaceObjectServiceImpl<DocumentSearch> implements DocumentSearchService {

    @Autowired(required=true)
    protected ConfigurationService configurationService;
    
	@Autowired(required = true)
	protected DocumentSearchDAO documentSearchDAO;
	
	@Override
	public DocumentSearch find(Context context, UUID uuid) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLastModified(Context context, DocumentSearch dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, DocumentSearch dso) throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DocumentSearch findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentSearch findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void insert(Context context, String itemIds) throws SQLException {
		documentSearchDAO.insert(context, itemIds);
	}
	
	@Override
	public void insert(Context context, String itemIds, String query) throws SQLException {
		documentSearchDAO.insert(context, itemIds, query);
	}

	@Override
	public List<DocumentSearch> getDocumentSearchByDateRange(Context context, Date startDate, Date endDate)
			throws SQLException {
		return documentSearchDAO.getDocumentSearchByDateRange(context, startDate, endDate);
	}

	@Override
	public List<DocumentSearch> getDocumentSearchByUser(Context context, String userName) throws SQLException {
		return documentSearchDAO.getDocumentSearchByUser(context, userName);
	}

	@Override
	public List<DocumentSearch> getDocumentSearch(Context context, String userName, Date startDate, Date endDate)
			throws SQLException {
		return documentSearchDAO.getDocumentSearch(context, userName, startDate, endDate);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return documentSearchDAO.countRows(context);
	}

}
