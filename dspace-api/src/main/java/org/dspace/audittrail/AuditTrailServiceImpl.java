/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.audittrail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.dspace.auditactionconfig.dao.AuditActionConfigDAO;
import org.dspace.audittrail.dao.AuditTrailDAO;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Audittrail object. This class is responsible
 * for all business logic calls for the Audittrail object and is autowired by
 * spring. This class should never be accessed directly.
 *
 * @author virsofetch.com
 */
public class AuditTrailServiceImpl extends DSpaceObjectServiceImpl<AuditTrail> implements AuditTrailService {

    @Autowired(required=true)
    protected ConfigurationService configurationService;
    
	@Autowired(required = true)
	protected AuditTrailDAO audittrailDAO;
	
	@Autowired
	protected AuditActionConfigDAO auditActionConfigDAO;

	@Override
	public AuditTrail find(Context context, UUID uuid) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLastModified(Context context, AuditTrail dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, AuditTrail dso) throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public AuditTrail findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AuditTrail findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void insert(Context context, String handle, String action, String url) throws SQLException {
		audittrailDAO.insert(context, handle, action, url);
	}

	@Override
	public List<AuditTrail> getAuditTrailByDateRange(Context context, Date startDate, Date endDate)
			throws SQLException {
		return audittrailDAO.getAuditTrailByDateRange(context, startDate, endDate);
	}

	@Override
	public List<AuditTrail> getAuditTrailUserName(Context context, String userName) throws SQLException {
		return audittrailDAO.getAuditTrailByUserName(context, userName);
	}

	@Override
	public List<AuditTrail> getAuditTrailUserName(Context context, String userName, String action) throws SQLException {
		return audittrailDAO.getAuditTrailByUserName(context, userName, action);
	}

	@Override
	public List<AuditTrail> getAuditTrailUserName(Context context, String userName, Date startDate, Date endDate)
			throws SQLException {
		return audittrailDAO.getAuditTrailByUserName(context, startDate, endDate, userName);
	}

	@Override
	public List<AuditTrail> getAuditTrail(Context context, String action, String userName, Date startDate,
			Date endDate) throws SQLException {
		return audittrailDAO.getAuditTrail(context, action, userName, startDate, endDate);
	}

	@Override
	public void generateUrlAndInsert(Context context, String handle, String action, HttpServletRequest request)
			throws SQLException {
		
		String query = "";
		
		@SuppressWarnings("rawtypes")
		Enumeration en = request.getParameterNames();
		if (en.hasMoreElements()) {
			Object firstElement = en.nextElement();
			query = firstElement +"="+ request.getParameter((String) firstElement);
			while(en.hasMoreElements()) {
				Object element = en.nextElement();
				if(element.toString().contains("submit_")) {
					continue;
				}
				query = query + "&" + element;
				query = query + "=" + request.getParameter((String) element);
			}
		}
		
		String domainLink = configurationService.getProperty("dms.domain");
		String url = domainLink + request.getRequestURI();;
		insert(context, handle, action, url + "?" + query);
	}
	
	@Override
	public void generateWithoutUrlAndInsert(Context context, String handle, String action)	throws SQLException {
		insert(context, handle, action,"");
	}

	@Override
	public int countRows(Context context) throws SQLException {
		// count all entry in audittrail
		return audittrailDAO.countRows(context);
	}
	
	@Override
	public void logAction(Context context, String handle, AuditAction action, Object... args) throws SQLException {
	    audittrailDAO.logAction(context, handle, action, args);
	}
	
	@Override
	public List<AuditTrail> getEnabledAuditTrailByUserName(Context context, String username, Date startDate, Date endDate, List<String> allowedActions) throws SQLException {
	    return audittrailDAO.getEnabledAuditTrailByUserName(context, username, startDate, endDate, allowedActions);
	}

	@Override
	public List<AuditTrail> getEnabledAuditTrailByDateRange(Context context, Date startDate, Date endDate, List<String> allowedActions) throws SQLException {
	    return audittrailDAO.getEnabledAuditTrailByDateRange(context, startDate, endDate, allowedActions);
	}
	
	@Override
	public long purgeAuditTrail(Context context, String action, Date startDate, Date endDate)
			throws SQLException {
		
	    List<AuditTrail> list = audittrailDAO.filterAudittrail(context, action, startDate, endDate);
	    for (AuditTrail trail : list) {
	    	audittrailDAO.delete(context, trail);
	    }
	    return list.size();
	}
}