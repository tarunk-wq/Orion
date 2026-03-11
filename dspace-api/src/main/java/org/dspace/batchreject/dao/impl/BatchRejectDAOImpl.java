package org.dspace.batchreject.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.dspace.batchreject.BatchReject;
import org.dspace.batchreject.dao.BatchRejectDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;

public class BatchRejectDAOImpl extends AbstractHibernateDSODAO<BatchReject> implements BatchRejectDAO {

	@Override
	public int countRows(Context context) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<BatchReject> criteriaQuery = getCriteriaQuery(criteriaBuilder, BatchReject.class);
        Root<BatchReject> batchrejectRoot = criteriaQuery.from(BatchReject.class);
        criteriaQuery.select(batchrejectRoot);

        return count(context, criteriaQuery, criteriaBuilder, batchrejectRoot);
	}

	@Override
	public List<BatchReject> findAll(Context context, int limit, int offset) throws SQLException {
		return findAll(context, BatchReject.class,limit,offset);

	}

	@Override
	public List<BatchReject> findAll(Context context) throws SQLException {
		return findAll(context, BatchReject.class);
	}

	@Override
	public List<BatchReject> findByTime(Context context, Date startDate, Date endDate) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<BatchReject> criteriaQuery = getCriteriaQuery(criteriaBuilder, BatchReject.class);
        Root<BatchReject> batchRejectRoot = criteriaQuery.from(BatchReject.class);
        List<Predicate> predicates = new ArrayList<>();
        Predicate timebetween=criteriaBuilder.between(batchRejectRoot.get(BatchReject_.TIME), startDate, endDate);
        criteriaQuery.select(batchRejectRoot);
        predicates.add(timebetween);
        criteriaQuery.where((predicates.toArray(new Predicate[predicates.size()])));
        
		return list(context,criteriaQuery, false,BatchReject.class,-1,-1);
	}

	@Override
	public List<BatchReject> findByBatchName(Context context, String batchname) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<BatchReject> criteriaQuery = getCriteriaQuery(criteriaBuilder, BatchReject.class);
        Root<BatchReject> batchrejectRoot = criteriaQuery.from(BatchReject.class);
        criteriaQuery.select(batchrejectRoot);
        criteriaQuery.where((criteriaBuilder.equal(batchrejectRoot.get(BatchReject_.BATCH_NAME), batchname)));
        return list(context, criteriaQuery, true, BatchReject.class, -1, -1);
	}

}