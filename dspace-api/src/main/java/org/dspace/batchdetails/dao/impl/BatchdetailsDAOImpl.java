package org.dspace.batchdetails.dao.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchdetails.dao.BatchdetailsDAO;
import org.dspace.batchhistory.BatchHistory;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem_;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public class BatchdetailsDAOImpl extends AbstractHibernateDSODAO<Batchdetails> implements BatchdetailsDAO{
	
	@PersistenceContext
    private EntityManager entityManager;

	@Override
	public List<Batchdetails> findAll(Context context) throws SQLException {
		return findAll(context, Batchdetails.class);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, Batchdetails.class);
        Root<Batchdetails> batchdetailsRoot = criteriaQuery.from(Batchdetails.class);
        criteriaQuery.select(batchdetailsRoot);

        return count(context, criteriaQuery, criteriaBuilder, batchdetailsRoot);
	}
	
	@Override
    public Batchdetails findByBatchName(Context context, String batchName) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, Batchdetails.class);
        Root<Batchdetails> batchdetailsRoot = criteriaQuery.from(Batchdetails.class);
        criteriaQuery.select(batchdetailsRoot);
        criteriaQuery.where((criteriaBuilder.equal(batchdetailsRoot.get(Batchdetails_.BATCH_NAME), batchName)));
        return uniqueResult(context, criteriaQuery, true, Batchdetails.class);
    }
	
	@Override
    public List<Batchdetails> findAllByBatchName(Context context, String batchName) throws SQLException {
		
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, Batchdetails.class);
        Root<Batchdetails> batchdetailsRoot = criteriaQuery.from(Batchdetails.class);
        criteriaQuery.select(batchdetailsRoot);
        criteriaQuery.where((criteriaBuilder.equal(batchdetailsRoot.get(Batchdetails_.BATCH_NAME), batchName)));
//	        criteriaQuery.orderBy(criteriaBuilder.desc(batchdetailsRoot.get(Batchdetails_.WORKFLOW_ID)));
        return list(context, criteriaQuery, true, Batchdetails.class, -1, -1);
	}

	@Override
	public Batchdetails findByLegacyId(Context context, int legacyId, Class<Batchdetails> clazz) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Batchdetails> findListByBatchName(Context context, List<String> batchNames,int limit, int offset) throws SQLException {
        if (batchNames == null || batchNames.isEmpty()) {
            return Collections.emptyList();
        }

        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> cq = getCriteriaQuery(cb, Batchdetails.class);
        Root<Batchdetails> root = cq.from(Batchdetails.class);

        // WHERE batch_name IN (:batchNames)
        Predicate batchNamesPredicate =
                root.get(Batchdetails_.BATCH_NAME).in(batchNames);

        // ---- ORDER BY using subquery (Hibernate 6 safe) ----
        Subquery<Integer> orderSubquery = cq.subquery(Integer.class);
        Root<Batchdetails> subRoot = orderSubquery.from(Batchdetails.class);
	    Join<Batchdetails, XmlWorkflowItem> wiJoin =
	            subRoot.join(Batchdetails_.WORKFLOW_ID);

	    orderSubquery
        .select(wiJoin.get(XmlWorkflowItem_.id))
        .where(cb.equal(subRoot.get(Batchdetails_.WORKFLOW_ID),
                        root.get(Batchdetails_.WORKFLOW_ID)));
     
        cq.select(root)
          .where(batchNamesPredicate)
          .orderBy(cb.desc(orderSubquery));

        return list(context, cq, false, Batchdetails.class, limit, offset);
    }
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Batchdetails> findBatchDetailsByBatchNames(Context context, List<String> batchNames, int limit, int offset) throws SQLException {
	    String query = "SELECT b.* " +
	                   "FROM batchdetails b " +
	                   "JOIN (SELECT batch_name, MAX(time) AS max_time FROM batchhistory GROUP BY batch_name) bh " +
	                   "ON b.batch_name = bh.batch_name " +
	                   "WHERE b.batch_name IN :batchNames " +
	                   "ORDER BY bh.max_time DESC " +
	                   "LIMIT :limit OFFSET :offset";

	    Query nativeQuery = entityManager.createNativeQuery(query, Batchdetails.class);
	    nativeQuery.setParameter("batchNames", batchNames);
	    nativeQuery.setParameter("limit", limit);
	    nativeQuery.setParameter("offset", offset);

	    return nativeQuery.getResultList();
	}

	@Override
	public List<Batchdetails> findListByBatchName(Context context, List<String> batchNames) throws SQLException {
	    if (batchNames == null || batchNames.isEmpty()) {
	        return Collections.emptyList();
	    }

	    CriteriaBuilder cb = getCriteriaBuilder(context);
	    CriteriaQuery<Batchdetails> cq = cb.createQuery(Batchdetails.class);
	    Root<Batchdetails> root = cq.from(Batchdetails.class);

	    // WHERE batch_name IN (:batchNames)
	    Predicate batchNamePredicate =
	            root.get(Batchdetails_.batchName).in(batchNames);

	    // ----- SUBQUERY FOR ORDER BY -----
	    Subquery<Integer> orderSubquery = cq.subquery(Integer.class);
	    Root<Batchdetails> subRoot = orderSubquery.from(Batchdetails.class);
	    Join<Batchdetails, XmlWorkflowItem> wiJoin =
	            subRoot.join(Batchdetails_.WORKFLOW_ID);

	    orderSubquery
	        .select(wiJoin.get(XmlWorkflowItem_.id))
	        .where(cb.equal(subRoot.get(Batchdetails_.WORKFLOW_ID),
	                        root.get(Batchdetails_.WORKFLOW_ID)));

	    // MAIN QUERY
	    cq.select(root)
	      .where(batchNamePredicate)
	      .orderBy(cb.desc(orderSubquery));

	    return list(context, cq, false, Batchdetails.class, -1, -1);
	}

	@Override
	public Batchdetails findByWorkFlowId(Context context, XmlWorkflowItem workflowId) throws SQLException{
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, Batchdetails.class);
        Root<Batchdetails> batchdetailsRoot = criteriaQuery.from(Batchdetails.class);
        criteriaQuery.select(batchdetailsRoot);
        criteriaQuery.where((criteriaBuilder.equal(batchdetailsRoot.get(Batchdetails_.WORKFLOW_ID), workflowId)));
        Batchdetails batchdetails = uniqueResult(context, criteriaQuery, true, Batchdetails.class);
        return batchdetails;
	}
	
	@Override
	public Batchdetails findByHistoryId(Context context, BatchHistory historyid) throws SQLException{
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, Batchdetails.class);
        Root<Batchdetails> batchdetailsRoot = criteriaQuery.from(Batchdetails.class);
        criteriaQuery.select(batchdetailsRoot);
        criteriaQuery.where((criteriaBuilder.equal(batchdetailsRoot.get(Batchdetails_.HISTORY_ID), historyid)));
        Batchdetails batchdetails = uniqueResult(context, criteriaQuery, true, Batchdetails.class);
        return batchdetails;
	}
	
	@Override
	public List<Batchdetails> findListByHistoryId(Context context,List<BatchHistory> historyids) throws SQLException{
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, Batchdetails.class);
        Root<Batchdetails> batchdetailsRoot = criteriaQuery.from(Batchdetails.class);
        criteriaQuery.select(batchdetailsRoot);
        Predicate historyIdPredicate = batchdetailsRoot.get(Batchdetails_.BATCH_NAME).in(historyids);
        criteriaQuery.where(historyIdPredicate);
        criteriaQuery.orderBy(criteriaBuilder.desc(batchdetailsRoot.get(Batchdetails_.HISTORY_ID)));
        return list(context, criteriaQuery, false, Batchdetails.class, -1, -1);
	}
	
	@Override
	public List<Batchdetails> findByState(Context context, String state) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, Batchdetails.class);
        Root<Batchdetails> batchdetailsRoot = criteriaQuery.from(Batchdetails.class);
        criteriaQuery.select(batchdetailsRoot);
        Predicate status=criteriaBuilder.equal(batchdetailsRoot.get(Batchdetails_.STATE), state);
        criteriaQuery.where(status);

        return list(context, criteriaQuery, false, Batchdetails.class,  -1, -1);
	}

	@Override
	public List<Batchdetails> findListByProcess(Context context, List<Process> processes) throws SQLException{
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Batchdetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, Batchdetails.class);
        Root<Batchdetails> batchdetailsRoot = criteriaQuery.from(Batchdetails.class);
        criteriaQuery.select(batchdetailsRoot);
        Predicate historyIdPredicate = batchdetailsRoot.get(Batchdetails_.BATCH_NAME).in(processes);
        criteriaQuery.where(historyIdPredicate);
        criteriaQuery.orderBy(criteriaBuilder.desc(batchdetailsRoot.get(Batchdetails_.BATCH_NAME)));
        return list(context, criteriaQuery, false, Batchdetails.class, -1, -1);
	}

	@Override
	public List<Batchdetails> findAll(Context context, int limit, int offset) throws SQLException {
		return findAll(context, Batchdetails.class,limit,offset);
	}
	  
}