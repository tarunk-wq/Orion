package org.dspace.batchdetails;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.batchdetails.dao.BatchdetailsDAO;
import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.batchhistory.BatchHistory;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

public class BatchdetailsServiceImpl extends DSpaceObjectServiceImpl<Batchdetails> implements BatchdetailsService{

	@Autowired(required = true)
    protected BatchdetailsDAO batchdetailsDAO;
	

	@Override
	public Batchdetails find(Context context, UUID uuid) throws SQLException {
		return batchdetailsDAO.findByID(context, Batchdetails.class, uuid);
	}

	@Override
	public void updateLastModified(Context context, Batchdetails dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, Batchdetails dso) throws SQLException, AuthorizeException, IOException {
		batchdetailsDAO.delete(context, dso);
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}
	 @Override
	    public Batchdetails create(Context context,String filename,
	    		                    List<Long> counts) throws SQLException {
	    	
	    	
	    	Batchdetails batchdetails = new Batchdetails();
	    	batchdetails.setMainPdfPageCount(Integer.parseInt(String.valueOf(counts.get((3)))));
	    	batchdetails.setTotalPdfs(Integer.parseInt(String.valueOf(counts.get((2)))));
	    	batchdetails.setFileSize(Integer.parseInt(String.valueOf(counts.get((1)))));
	    	batchdetails.setTotalPages(Integer.parseInt(String.valueOf(counts.get((4)))));
	    	batchdetails.setTotalFiles(Integer.parseInt(String.valueOf(counts.get((0)))));
	    	batchdetails.setState("processing");
	    	if(filename.lastIndexOf(".") != -1) {
	    		batchdetails.setBatchName(filename.substring(0, filename.lastIndexOf(".")));
	    	}else{
	    		batchdetails.setBatchName(filename);
	    	}
	        Batchdetails createdBatchDetail = batchdetailsDAO.create(context, batchdetails);
	        return createdBatchDetail;
	    }

	 @Override
	 public Batchdetails create(Context context, String batchName) throws SQLException{
	    	Batchdetails batchdetails = new Batchdetails();
	    	batchdetails.setBatchName(batchName);
	    	batchdetails.setState("processing");
	        Batchdetails createdBatchDetail = batchdetailsDAO.create(context, batchdetails);
	        return createdBatchDetail;
	 }
	 
	@Override
	public Batchdetails findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Batchdetails findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Batchdetails findByWorkFlowId(Context context, XmlWorkflowItem id) throws SQLException {
		return batchdetailsDAO.findByWorkFlowId(context,id);
	}
	
	@Override
	public List<Batchdetails> findAll(Context context) throws SQLException {
		return batchdetailsDAO.findAll(context);
	}
	
	@Override
	public List<Batchdetails> findAll(Context context, int limit, int offset) throws SQLException {
		return batchdetailsDAO.findAll(context, limit,offset);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return batchdetailsDAO.countRows(context);
	}
	
	@Override
	public Batchdetails findByBatchName(Context context, String batchname) throws SQLException {
		return batchdetailsDAO.findByBatchName(context,batchname);
	}
	
	@Override
	public List<Batchdetails> findAllByBatchName(Context context, String batchname) throws SQLException {
		return batchdetailsDAO.findAllByBatchName(context,batchname);
	}

	@Override
	public List<Batchdetails> findListByBatchName(Context context, List<String> batchname,int limit, int offset) throws SQLException {
		return batchdetailsDAO.findListByBatchName(context, batchname, limit, offset);
	}
	
	@Override
	public List<Batchdetails> findBatchDetailsByBatchNames(Context context, List<String> batchname,int limit, int offset) throws SQLException {
		return batchdetailsDAO.findBatchDetailsByBatchNames(context, batchname, limit, offset);
	}
	
	@Override
	public List<Batchdetails> findListByBatchName(Context context, List<String> batchname) throws SQLException {
		return batchdetailsDAO.findListByBatchName(context, batchname);
	}

	@Override
	public Batchdetails findByHistoryId(Context context, BatchHistory bh) throws SQLException{
		return batchdetailsDAO.findByHistoryId(context, bh);
	}

	@Override
	public List<Batchdetails> findByState(Context context, String state) throws SQLException{
		return batchdetailsDAO.findByState(context,state);
	}
	
	@Override
	public List<Batchdetails> findListByHistoryId(Context context,List<BatchHistory> historyids) throws SQLException{
		return batchdetailsDAO.findListByHistoryId(context, historyids);
	}
	
	@Override
	public List<Batchdetails> findListByProcess(Context context,List<Process> processes) throws SQLException{
		return batchdetailsDAO.findListByProcess(context, processes);
	}
	
}