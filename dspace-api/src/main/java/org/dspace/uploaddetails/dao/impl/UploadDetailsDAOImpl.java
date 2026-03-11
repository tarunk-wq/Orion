package org.dspace.uploaddetails.dao.impl;

import java.sql.SQLException;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.dspace.uploaddetails.UploadDetails;
import org.dspace.uploaddetails.dao.UploadDetailsDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class UploadDetailsDAOImpl extends AbstractHibernateDSODAO<UploadDetails> implements UploadDetailsDAO{

	@Override
	public List<UploadDetails> findAll(Context context) throws SQLException {
		return findAll(context, UploadDetails.class);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UploadDetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, UploadDetails.class);
        Root<UploadDetails> uploaddetailsRoot = criteriaQuery.from(UploadDetails.class);
        criteriaQuery.select(uploaddetailsRoot);

        return count(context, criteriaQuery, criteriaBuilder, uploaddetailsRoot);
	}

	@Override
	public UploadDetails findByLegacyId(Context context, int legacyId, Class<UploadDetails> clazz) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<UploadDetails> findBySubmitter(Context context, EPerson submitter) throws SQLException{
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UploadDetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, UploadDetails.class);
        Root<UploadDetails> uploaddetailsRoot = criteriaQuery.from(UploadDetails.class);
        criteriaQuery.select(uploaddetailsRoot);
        criteriaQuery.where((criteriaBuilder.equal(uploaddetailsRoot.get(UploadDetails_.SUBMITTER), submitter)));
        criteriaQuery.orderBy(criteriaBuilder.desc(uploaddetailsRoot.get(UploadDetails_.TIME)));
        return list(context,criteriaQuery, false, UploadDetails.class,-1,-1);
	}

	@Override
	public List<UploadDetails> findListByBatchName(Context context, String batchName) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UploadDetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, UploadDetails.class);
        Root<UploadDetails> uploaddetailsRoot = criteriaQuery.from(UploadDetails.class);
        criteriaQuery.select(uploaddetailsRoot);
        criteriaQuery.where((criteriaBuilder.equal(uploaddetailsRoot.get(UploadDetails_.BATCH_NAME), batchName)));
        criteriaQuery.orderBy(criteriaBuilder.desc(uploaddetailsRoot.get(UploadDetails_.TIME)));
        return list(context,criteriaQuery, false, UploadDetails.class,-1,-1);
	}

	@Override
	public List<UploadDetails> findByBatchNameAndStatus(Context context, String batchName, String status) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<UploadDetails> criteriaQuery = getCriteriaQuery(criteriaBuilder, UploadDetails.class);
        Root<UploadDetails> uploaddetailsRoot = criteriaQuery.from(UploadDetails.class);
        criteriaQuery.select(uploaddetailsRoot);
        criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(uploaddetailsRoot.get(UploadDetails_.BATCH_NAME), batchName),
        		             criteriaBuilder.equal(uploaddetailsRoot.get(UploadDetails_.UPLOAD_STATUS), status)));
        criteriaQuery.orderBy(criteriaBuilder.desc(uploaddetailsRoot.get(UploadDetails_.TIME)));
        return list(context, criteriaQuery, true, UploadDetails.class,-1,-1);
	}
	
}