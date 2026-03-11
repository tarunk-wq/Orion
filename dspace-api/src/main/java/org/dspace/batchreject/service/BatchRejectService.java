package org.dspace.batchreject.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchreject.BatchReject;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;

public interface BatchRejectService extends DSpaceObjectService<BatchReject>, DSpaceObjectLegacySupportService<BatchReject>{

	BatchReject create(Context context, String batchname, String reason) throws SQLException;

	List<BatchReject> findAll(Context context) throws SQLException;

	List<BatchReject> findAll(Context context, int limit, int offset) throws SQLException;

	int countRows(Context context) throws SQLException;
	
	public List<BatchReject> findByTimeRange(Context context, Date startDate, Date endDate)
			throws SQLException;
	
	List<BatchReject> findByBatchName(Context context, String batchname) throws SQLException;

}
