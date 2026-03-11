package org.dspace.batchdetails.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchhistory.BatchHistory;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.scripts.Process;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public interface BatchdetailsDAO extends DSpaceObjectDAO<Batchdetails>, DSpaceObjectLegacySupportDAO<Batchdetails>, GenericDAO<Batchdetails>{
	
	public List<Batchdetails> findAll(Context context)throws SQLException;
	
	int countRows(Context context) throws SQLException;

	Batchdetails findByBatchName(Context context, String batchName) throws SQLException;

	public Batchdetails findByWorkFlowId(Context context, XmlWorkflowItem id) throws SQLException;

	Batchdetails findByHistoryId(Context context, BatchHistory historyid) throws SQLException;

	List<Batchdetails> findListByProcess(Context context, List<Process> processes)
			throws SQLException;

	List<Batchdetails> findByState(Context context, String state) throws SQLException;

	List<Batchdetails> findListByHistoryId(Context context, List<BatchHistory> historyids)
			throws SQLException;

	List<Batchdetails> findListByBatchName(Context context, List<String> batchNames, int limit, int offset)
			throws SQLException;
	
	List<Batchdetails> findBatchDetailsByBatchNames(Context context, List<String> batchNames, int limit, int offset)
			throws SQLException;

	List<Batchdetails> findListByBatchName(Context context, List<String> batchNames) throws SQLException;

	public List<Batchdetails> findAll(Context context, int limit, int offset) throws SQLException;

	List<Batchdetails> findAllByBatchName(Context context, String batchName) throws SQLException;
}