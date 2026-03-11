package org.dspace.batchhistory.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.batchhistory.BatchHistory;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

public interface  BatchHistoryDAO extends DSpaceObjectDAO<BatchHistory>, DSpaceObjectLegacySupportDAO<BatchHistory>, GenericDAO<BatchHistory>{
	
	public List<BatchHistory> findAll(Context context)throws SQLException;
	
	int countRows(Context context) throws SQLException;

	List<BatchHistory> findByTimeandState(Context context, Date startDate, Date endDate, String state) throws SQLException;

	List<BatchHistory> findListByBatchName(Context context, List<String> batchNames) throws SQLException;

	List<BatchHistory> findAllByBatchName(Context context, String batchName) throws SQLException;
}