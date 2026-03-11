package org.dspace.batchdetails.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchhistory.BatchHistory;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public interface BatchdetailsService extends DSpaceObjectService<Batchdetails>, DSpaceObjectLegacySupportService<Batchdetails>{
	
	public List<Batchdetails> findAll(Context context) throws SQLException;
	
	int countRows(Context context) throws SQLException;

	Batchdetails findByBatchName(Context context, String batchname) throws SQLException;

	Batchdetails findByWorkFlowId(Context context, XmlWorkflowItem id) throws SQLException;

	public Batchdetails findByHistoryId(Context context, BatchHistory bh) throws SQLException;

	List<Batchdetails> findListByBatchName(Context context, List<String> batchname, int limit, int offset)
			throws SQLException;


	List<Batchdetails> findListByHistoryId(Context context, List<BatchHistory> historyids) throws SQLException;

	List<Batchdetails> findByState(Context context, String state) throws SQLException;

	List<Batchdetails> findListByProcess(Context context, List<Process> processes) throws SQLException;

	List<Batchdetails> findListByBatchName(Context context, List<String> batchname) throws SQLException;

	List<Batchdetails> findAll(Context context, int limit, int offset) throws SQLException;

	Batchdetails create(Context context, String filename, List<Long> counts) throws SQLException;

	Batchdetails create(Context context, String batchName) throws SQLException;

	public List<Batchdetails> findAllByBatchName(Context context, String batchname) throws SQLException;

	List<Batchdetails> findBatchDetailsByBatchNames(Context context, List<String> batchname, int limit, int offset)
			throws SQLException;
}