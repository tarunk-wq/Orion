/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.audittrail.dao;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.AuditTrail;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;

/**
 * Database Access Object interface class for the EPerson object.
 * The implementation of this class is responsible for all database calls for the EPerson object and is autowired by
 * spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author virsoftech.com
 */
public interface AuditTrailDAO extends DSpaceObjectDAO<AuditTrail>, DSpaceObjectLegacySupportDAO<AuditTrail> {
	public void insert(Context context, String handle, String action, String url) throws SQLException ;
	
	public List<AuditTrail> getAuditTrailByDateRange(Context context, Date startDate, Date endDate) throws SQLException;
	
	public List<AuditTrail> getAuditTrailByDateRange(Context context, String action, Date startDate, Date endDate) throws SQLException;
		
	public List<AuditTrail> getAuditTrailByUserName(Context context, String userName) throws SQLException;
	
	public List<AuditTrail> getAuditTrailByUserName(Context context, Date startDate, Date endDate, String userName) throws SQLException;
	
	public List<AuditTrail> getAuditTrailByUserName(Context context, String userName, String action) throws SQLException;

	public List<AuditTrail> getAuditTrail(Context context, String action, String userName, Date startDate, Date endDate) throws SQLException;

    int countRows(Context context) throws SQLException;

	List<AuditTrail> getAuditTarilBySearchFacets(Context context, String action, Timestamp startDate, Timestamp endDate)
			throws SQLException;
	
	void logAction(Context context, String handle, AuditAction action,  Object... args) throws SQLException;
	
	List<AuditTrail> getEnabledAuditTrailByUserName(Context context, String username, Date startDate, Date endDate, List<String> allowedActions) throws SQLException;

	List<AuditTrail> getEnabledAuditTrailByDateRange(Context context, Date startDate, Date endDate, List<String> allowedActions) throws SQLException;

	 List<AuditTrail> filterAudittrail(Context context, String action, Date startDate, Date endDate) throws SQLException;
}
