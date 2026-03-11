package org.dspace.batchreject;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.batchreject.dao.BatchRejectDAO;
import org.dspace.batchreject.service.BatchRejectService;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchRejectServiceImpl extends DSpaceObjectServiceImpl<BatchReject> implements BatchRejectService {

	@Autowired(required = true)
	BatchRejectDAO batchrejectDAO;
	
	@Override
	public BatchReject find(Context context, UUID uuid) throws SQLException {
		return batchrejectDAO.findByID(context, BatchReject.class, uuid);
	}

	@Override
	public BatchReject create(Context context, String batchname, String reason) throws SQLException{
		BatchReject batchReject=new BatchReject();
		batchReject.setReason(reason);
		batchReject.setBatchName(batchname);
		batchReject.setTime(Instant.now());

		BatchReject createdbatchReject = batchrejectDAO.create(context, batchReject);
        return createdbatchReject;
	}
	@Override
	public void updateLastModified(Context context, BatchReject dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, BatchReject dso) throws SQLException, AuthorizeException, IOException {
		batchrejectDAO.delete(context, dso);		
	}
	
	@Override
	public List<BatchReject> findAll(Context context) throws SQLException {
		return batchrejectDAO.findAll(context);
	}
	
	@Override
	public List<BatchReject> findAll(Context context, int limit, int offset) throws SQLException {
		return batchrejectDAO.findAll(context, limit,offset);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return batchrejectDAO.countRows(context);
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BatchReject findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BatchReject findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<BatchReject> findByTimeRange(Context context,Date startDate, Date endDate) throws SQLException{
		return batchrejectDAO.findByTime(context,startDate,endDate);
	}
	
	public List<BatchReject> findByBatchName(Context context, String batchname) throws SQLException{
		return batchrejectDAO.findByBatchName(context,batchname);
	}

}
