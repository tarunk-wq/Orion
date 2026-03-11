package org.dspace.audittrail.dao.impl;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Query;
import org.dspace.audittrail.DocumentSearch;
import org.dspace.audittrail.dao.DocumentSearchDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class DocumentSearchDAOImpl extends AbstractHibernateDSODAO<DocumentSearch> implements DocumentSearchDAO {

	@Override
	public void insert(Context context, String itemIds) throws SQLException {
		// TODO Auto-generated method stub
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
        
        create(context, new DocumentSearch(new Date(), ipAddress, userName, itemIds));
	}

	@Override
	public List<DocumentSearch> getDocumentSearchByDateRange(Context context, Date startDate, Date endDate)
			throws SQLException {
		String hql = "FROM DocumentSearch WHERE time>= :startDate AND time<= :endDate ORDER by time DESC";
		Query query = createQuery(context, hql);
		query.setParameter("startDate", startDate);
		query.setParameter("endDate", endDate);
		
		//return list of DocumentSearch
		return list(query);
	}

	@Override
	public List<DocumentSearch> getDocumentSearchByUser(Context context, String userName) throws SQLException {
		String hql = "FROM DocumentSearch WHERE username= :userName ORDER by time DESC";
		Query query = createQuery(context, hql);
		query.setParameter("userName", userName);
		
		//return list of DocumentSearch
		return list(query);
	}

	@Override
	public List<DocumentSearch> getDocumentSearch(Context context, String userName, Date startDate, Date endDate)
			throws SQLException {
		String hql = "FROM DocumentSearch WHERE time>= :startDate AND time<= :endDate AND username= :userName ORDER by time DESC";
		Query query = createQuery(context, hql);
		query.setParameter("startDate", startDate);
		query.setParameter("endDate", endDate);
		query.setParameter("userName", userName);
		
		//return list of DocumentSearch
		return list(query);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		// count all entry in document-search
		return count(createQuery(context, "SELECT count(*) FROM DocumentSearch"));
	}
	
	@Override
	public void insert(Context context, String itemIds, String query) throws SQLException {
		// TODO Auto-generated method stub
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
        
        if(query.equalsIgnoreCase("*"))
        	return;
        
        create(context, new DocumentSearch(new Date(), ipAddress, userName, itemIds, query));
	}

}
