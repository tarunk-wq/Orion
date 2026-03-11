/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.audittrail.dao.impl;


import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.AuditTrail;
import org.dspace.audittrail.dao.AuditTrailDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Hibernate implementation of the Database Access Object interface class for the EPerson object.
 * This class is responsible for all database calls for the EPerson object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author virsoftech.com
 */
public class AuditTrailDAOImpl extends AbstractHibernateDSODAO<AuditTrail> implements AuditTrailDAO {
    protected AuditTrailDAOImpl() {
        super();
    }

    @PersistenceContext
    private EntityManager entityManager;

	@Override
	public void insert(Context context, String handle, String action, String url) throws SQLException {
		String ipAddress = "";
        if(context.getExtraLogInfo().indexOf("ip_addr") != -1){  
        	ipAddress = context.getExtraLogInfo().substring(context.getExtraLogInfo().indexOf("ip_addr") + "ip_addr".length() + 1);
        	if(ipAddress.equals("0:0:0:0:0:0:0:1")) {
        		ipAddress = "127.0.0.1";
        	}
        }

		String userName = "anonymous";
		EPerson person = context.getCurrentUser();
        if(person != null){
        	userName = person.getNetid() != null ? person.getNetid().toString() : person.getEmail(); //This returns username that's used to login.
        }
		create(context, new AuditTrail(new Date(), ipAddress, userName, action, handle, url));
	}

	@Override
	public List<AuditTrail> getAuditTrailByDateRange(Context context, Date startDate, Date endDate)
			throws SQLException {
		// sql query
		String hql = "FROM AuditTrail WHERE time>= :startDate AND time<= :endDate AND action NOT IN ('view-document', 'download-document', 'view item') ORDER BY time DESC";
		Query query = createQuery(context, hql);
		query.setParameter("startDate", startDate);
		query.setParameter("endDate", endDate);
		
		//return list of AuditTrail
		return list(query);
	}

	@Override
	public List<AuditTrail> getAuditTrailByDateRange(Context context, String action, Date startDate, Date endDate)
			throws SQLException {
		// sql query
		String hql = "FROM AuditTrail WHERE time>= :startDate AND time<= :endDate AND action= :action ORDER BY time DESC";
		Query query = createQuery(context, hql);
		query.setParameter("startDate", startDate);
		query.setParameter("endDate", endDate);
		query.setParameter("action", action);
		
		//return list of AuditTrail
		return list(query);
	}

	@Override
	public List<AuditTrail> getAuditTrailByUserName(Context context, String userName) throws SQLException {
		// sql query
		String hql = "FROM AuditTrail WHERE username= :username AND action not in ('view-document', 'download-document', 'view item') ORDER BY time DESC";
		Query query = createQuery(context, hql);
		query.setParameter("username", userName);
		
		//return list of AuditTrail
		return list(query);
	}

	@Override
	public List<AuditTrail> getAuditTrailByUserName(Context context, Date startDate, Date endDate, String userName)
			throws SQLException {
		// sql query
		String hql = "FROM AuditTrail WHERE time>= :startDate AND time<= :endDate AND username = :username and action NOT IN ('view-document', 'download-document', 'view item') ORDER BY time DESC";
		
		Query query = createQuery(context, hql);
		query.setParameter("startDate", startDate);
		query.setParameter("endDate", endDate);
		query.setParameter("username", userName);
		
		//return list of AuditTrail
		return list(query);
	}

	@Override
	public List<AuditTrail> getAuditTrailByUserName(Context context, String userName, String action)
			throws SQLException {
		// sql query
		String hql = "FROM AuditTrail WHERE username= :username AND action= :action ORDER BY time DESC";
		Query query = createQuery(context, hql);
		query.setParameter("username", userName);
		query.setParameter("action", action);
		
		//return list of AuditTrail
		return list(query);
	}

	@Override
	public List<AuditTrail> getAuditTrail(Context context, String action, String userName, Date startDate, Date endDate)
			throws SQLException {
		// sql query
		String hql = "FROM AuditTrail WHERE time>= :startDate AND time<= :endDate AND action= :action AND username= :username ORDER BY time DESC";
		
		Query query = createQuery(context, hql);
		query.setParameter("startDate", startDate);
		query.setParameter("endDate", endDate);
		query.setParameter("action", action);
		query.setParameter("username", userName);
		
		//return list of AuditTrail
		return list(query);
	}
	
	@Override
	public  List<AuditTrail> getAuditTarilBySearchFacets(Context context, String action, Timestamp startDate, Timestamp endDate)
			throws SQLException{
		// pattern Matching for searched facet and its value.
		String facetSearch = "%"+action +"%";
		String hql = "From AuditTrail Where time>= :startDate AND time<= :endDate AND action like :facetSearch";
		
		Query query = createQuery(context,hql);
		query.setParameter("startDate", startDate);
		query.setParameter("endDate", endDate);
		query.setParameter("facetSearch", facetSearch);
		
		//return list of AuditTrail
		return list(query);
		
	}
	
	@Override
	public int countRows(Context context) throws SQLException {
		// count all entry in audittrail
		return count(createQuery(context, "SELECT count(*) FROM AuditTrail"));
	}    
	
	@Override
	public void logAction(Context context, String handle, AuditAction action,  Object... args) throws SQLException {
		// logs an audit action with dynamic description formatting
		String ipAddress = "";
	    if (context.getExtraLogInfo() != null && context.getExtraLogInfo().contains("ip_addr")) {
	        ipAddress = context.getExtraLogInfo().substring(
	            context.getExtraLogInfo().indexOf("ip_addr") + "ip_addr".length() + 1
	        );
	        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
	            ipAddress = "127.0.0.1";
	        }
	    }

	    String userName = "anonymous";
	    EPerson person = context.getCurrentUser();
	    if (person != null) {
	        userName = person.getNetid() != null ? person.getNetid() : person.getEmail();
	    }

	    // Use the enum's formatter with arguments
	    String formattedDescription = action.formatDescription(args);

	    create(context, new AuditTrail(
	        new Date(),
	        ipAddress,
	        userName,
	        action.getCode(),
	        handle != null ? handle : "",
	        formattedDescription
	    ));
	}
	
	@Override
	public List<AuditTrail> getEnabledAuditTrailByUserName(Context context, String username, Date startDate, Date endDate, List<String> allowedActions) throws SQLException {
	    String hql = "FROM AuditTrail WHERE time >= :startDate AND time <= :endDate AND userName = :username AND action IN :allowedActions ORDER BY time DESC";
	    Query query = createQuery(context, hql);
	    query.setParameter("startDate", startDate);
	    query.setParameter("endDate", endDate);
	    query.setParameter("username", username);
	    query.setParameter("allowedActions", allowedActions);
	    return list(query);
	}
	
	@Override
	public List<AuditTrail> getEnabledAuditTrailByDateRange(Context context, Date startDate, Date endDate, List<String> allowedActions) throws SQLException {
	    String hql = "FROM AuditTrail WHERE time >= :startDate AND time <= :endDate AND action IN :allowedActions ORDER BY time DESC";
	    Query query = createQuery(context, hql);
	    query.setParameter("startDate", startDate);
	    query.setParameter("endDate", endDate);
	    query.setParameter("allowedActions", allowedActions);
	    return list(query);
	}
	
	@Override
	public List<AuditTrail> filterAudittrail(Context context, String action, Date startDate, Date endDate) throws SQLException {
		if (action.equalsIgnoreCase("all")) {
			if (startDate != null) {
				return entityManager.createQuery("FROM AuditTrail WHERE time>= :startDate AND time<= :endDate", AuditTrail.class)
						.setParameter("startDate", startDate).setParameter("endDate", endDate).getResultList();
			} else {
				return entityManager.createQuery("FROM AuditTrail WHERE time<= :endDate", AuditTrail.class)
						.setParameter("endDate", endDate).getResultList();
			}
		} else {
			if (startDate != null) {
				return entityManager
						.createQuery(
								"FROM AuditTrail WHERE time>= :startDate AND time<= :endDate AND action = :action", AuditTrail.class)
						.setParameter("startDate", startDate).setParameter("endDate", endDate)
						.setParameter("action", action).getResultList();
			} else {
				return entityManager.createQuery("FROM AuditTrail WHERE time<= :endDate AND action = :action", AuditTrail.class)
						.setParameter("endDate", endDate).setParameter("action", action).getResultList();
			}
		}
	}
}
