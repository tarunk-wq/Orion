package org.dspace.batchhistory.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.dspace.batchhistory.BatchHistory;
import org.dspace.batchhistory.dao.BatchHistoryDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

public class BatchHistoryDAOImpl extends AbstractHibernateDAO<BatchHistory> implements BatchHistoryDAO{

	@Override
	public List<BatchHistory> findAll(Context context) throws SQLException {
		return findAll(context,BatchHistory.class);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<BatchHistory> criteriaQuery = getCriteriaQuery(criteriaBuilder, BatchHistory.class);
        Root<BatchHistory> batchhistoryRoot = criteriaQuery.from(BatchHistory.class);
        criteriaQuery.select(batchhistoryRoot);

        return count(context, criteriaQuery, criteriaBuilder, batchhistoryRoot);
	}

	@Override
	public List<BatchHistory> findListByBatchName(Context context, List<String> batchNames) throws SQLException {
		  CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
	        CriteriaQuery<BatchHistory> criteriaQuery = getCriteriaQuery(criteriaBuilder, BatchHistory.class);
	        Root<BatchHistory> batchhistoryRoot = criteriaQuery.from(BatchHistory.class);
	        criteriaQuery.select(batchhistoryRoot);
	        criteriaQuery.where((batchhistoryRoot.get(BatchHistory_.BATCH_NAME).in(batchNames)));
	        criteriaQuery.orderBy(criteriaBuilder.desc(batchhistoryRoot.get(BatchHistory_.TIME)));
	        return list(context, criteriaQuery, false, BatchHistory.class,-1,-1);
	}
	
	@Override
	public List<BatchHistory> findAllByBatchName(Context context, String batchName) throws SQLException {
		  CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
	        CriteriaQuery<BatchHistory> criteriaQuery = getCriteriaQuery(criteriaBuilder, BatchHistory.class);
	        Root<BatchHistory> batchhistoryRoot = criteriaQuery.from(BatchHistory.class);
	        criteriaQuery.select(batchhistoryRoot);
	        criteriaQuery.where((criteriaBuilder.equal(batchhistoryRoot.get(BatchHistory_.BATCH_NAME), batchName)));
	        criteriaQuery.orderBy(criteriaBuilder.desc(batchhistoryRoot.get(BatchHistory_.TIME)));
	        return list(context, criteriaQuery, false, BatchHistory.class,-1,-1);
	}
	
	@Override
	public BatchHistory findByLegacyId(Context context, int legacyId, Class<BatchHistory> clazz) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BatchHistory> findByTimeandState(Context context, Date startDate, Date endDate, String state) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<BatchHistory> criteriaQuery = getCriteriaQuery(criteriaBuilder, BatchHistory.class);
        Root<BatchHistory> batchhistoryRoot = criteriaQuery.from(BatchHistory.class);
        List<Predicate> predicates = new ArrayList<>();
        Predicate timebetween=criteriaBuilder.between(batchhistoryRoot.get(BatchHistory_.TIME), startDate, endDate);
        Predicate status=criteriaBuilder.equal(batchhistoryRoot.get(BatchHistory_.TO_STATE), state);
        criteriaQuery.select(batchhistoryRoot);
        predicates.add(timebetween);
        predicates.add(status);
        criteriaQuery.where((predicates.toArray(new Predicate[predicates.size()])));
        
		return list(context,criteriaQuery, false,BatchHistory.class,-1,-1);
	}
	
}