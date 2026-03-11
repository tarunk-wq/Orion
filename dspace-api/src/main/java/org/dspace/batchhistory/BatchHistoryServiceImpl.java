package org.dspace.batchhistory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Date;

import org.dspace.authorize.AuthorizeException;
import org.dspace.batchhistory.dao.BatchHistoryDAO;
import org.dspace.batchhistory.service.BatchHistoryService;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchHistoryServiceImpl extends DSpaceObjectServiceImpl<BatchHistory> implements BatchHistoryService{
	
	@Autowired(required = true)
    protected BatchHistoryDAO batchhistoryDAO;
	
	@Override
    public BatchHistory create(Context context,String filename, EPerson ePerson,
            List<Long> counts) throws SQLException {
    	
    	BatchHistory batchhistory = new BatchHistory();
    	if(filename.lastIndexOf(".") != -1) {
    		batchhistory.setBatchName(filename.substring(0, filename.lastIndexOf(".")));
    	}else{
    		batchhistory.setBatchName(filename);
    	}
    	batchhistory.setOwner(ePerson);
    	batchhistory.setTime(Instant.now());
    	batchhistory.setFromState("new");
     
        BatchHistory createdBatchHistory = batchhistoryDAO.create(context, batchhistory);
        return createdBatchHistory;
    }
	
	@Override
    public BatchHistory create(Context context,String batchname, EPerson ePerson) throws SQLException {
    	
    	BatchHistory batchhistory = new BatchHistory();
    	batchhistory.setBatchName(batchname);
    	batchhistory.setOwner(ePerson);
    	batchhistory.setTime(Instant.now());
        BatchHistory createdBatchHistory = batchhistoryDAO.create(context, batchhistory);
        return createdBatchHistory;
    }
	
	@Override
	public List<BatchHistory> findAll(Context context) throws SQLException {
		return batchhistoryDAO.findAll(context);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return batchhistoryDAO.countRows(context);
	}

	@Override
	public BatchHistory findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BatchHistory findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BatchHistory find(Context context, UUID uuid) throws SQLException {
		// TODO Auto-generated method stub
		return batchhistoryDAO.findByID(context, BatchHistory.class, uuid);
	}

	@Override
	public void updateLastModified(Context context, BatchHistory dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public List<BatchHistory> getByTimeandState(Context context,Date startDate, Date endDate, String state) throws SQLException{
		return batchhistoryDAO.findByTimeandState(context,startDate,endDate,state);
	}

	@Override
	public List<BatchHistory> findListByBatchName(Context context, List<String> batchNames) throws SQLException {
		return batchhistoryDAO.findListByBatchName(context, batchNames);
	}
	
	@Override
	public List<BatchHistory> findAllByBatchName(Context context, String batchName) throws SQLException {
		return batchhistoryDAO.findAllByBatchName(context, batchName);
	}
	
	@Override
	public void delete(Context context, BatchHistory dso) throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub
		batchhistoryDAO.delete(context, dso);
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}
}