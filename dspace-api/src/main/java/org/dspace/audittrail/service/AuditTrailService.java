/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.audittrail.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.AuditTrail;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

/**
 * Service interface class for the Audittrail object.
 * The implementation of this class is responsible for all business logic calls
 * for the Audittrail object and is autowired by Spring.
 *
 * @author virsofetch.com
 */
public interface AuditTrailService extends DSpaceObjectService<AuditTrail>, DSpaceObjectLegacySupportService<AuditTrail> {
	
	/**
	 * create an entry in AuditTrail table
	 * @param context
	 * @param handle
	 * @param action
	 * @param url
	 * @throws SQLException
	 */
	public void insert(Context context, String handle, String action, String url) throws SQLException ;
	
	/**
	 * fetch audittrail by date range
	 * @param context
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws SQLException
	 */
	public List<AuditTrail> getAuditTrailByDateRange(Context context, Date startDate, Date endDate) throws SQLException;
	
	/**
	 * fetch audittrail by username
	 * @param context
	 * @param userName
	 * @return
	 * @throws SQLException
	 */
	public List<AuditTrail> getAuditTrailUserName(Context context, String userName) throws SQLException;
	
	/**
	 * fetch audittrail by username for a specific action
	 * @param context
	 * @param userName
	 * @param action
	 * @return
	 * @throws SQLException
	 */
	public List<AuditTrail> getAuditTrailUserName(Context context, String userName, String action) throws SQLException;
	
	/**
	 * fetch audittrail by username for a specific date range
	 * @param context
	 * @param userName
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws SQLException
	 */
	public List<AuditTrail> getAuditTrailUserName(Context context, String userName, Date startDate, Date endDate) throws SQLException;
	
	/**
	 * fetch audittrail by username for a specific date range and action
	 * @param context
	 * @param action
	 * @param userName
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws SQLException
	 */
	public List<AuditTrail> getAuditTrail(Context context, String action, String userName, Date startDate, Date endDate) throws SQLException;
	
	/**
	 * generate URL from HTTP-Request and insert audittrail
	 * @param context
	 * @param handle
	 * @param action
	 * @param request
	 * @throws SQLException
	 */
	public void generateUrlAndInsert(Context context, String handle, String action, HttpServletRequest request) throws SQLException;
	
	
	public void generateWithoutUrlAndInsert(Context context, String handle, String action) throws SQLException;
	
	/**
	 * count of all audittrail entries
	 * @param context
	 * @return
	 * @throws SQLException
	 */
	int countRows(Context context) throws SQLException;
	
    /**
     * Logs an audit action with dynamic description formatting.
     * @param action the AuditAction enum
     * @param args   dynamic values to be injected into the action's template
     */
	void logAction(Context context, String handle, AuditAction action, Object... args) throws SQLException;
	
	/**
	 * Fetches audit trails for a user, filtered by a list of allowed actions and date range.
	 *
	 * @param context         the DSpace context
	 * @param username        the username to filter by
	 * @param startDate       start date for filtering
	 * @param endDate         end date for filtering
	 * @param allowedActions  list of enabled action codes
	 * @return list of AuditTrail records matching the filters
	 * @throws SQLException if a database error occurs
	 */
	List<AuditTrail> getEnabledAuditTrailByUserName(Context context, String username, Date startDate, Date endDate, List<String> allowedActions) throws SQLException;

	/**
	 * Fetches audit trails for a date range, filtered by allowed actions.
	 *
	 * @param context         the DSpace context
	 * @param startDate       start date for filtering
	 * @param endDate         end date for filtering
	 * @param allowedActions  list of enabled action codes
	 * @return list of AuditTrail records matching the filters
	 * @throws SQLException if a database error occurs
	 */
	List<AuditTrail> getEnabledAuditTrailByDateRange(Context context, Date startDate, Date endDate, List<String> allowedActions) throws SQLException;

	/**
	 * purge audit trail entries from DB
	 * @param context the DSpace context
	 * @param action  action to be fetched
	 * @param startDate date-range start point for purging
	 * @param endDate   date-range end point for purging
	 * @return
	 * @throws SQLException
	 */
	long purgeAuditTrail(Context context, String action, Date startDate, Date endDate)
			throws SQLException;
}
