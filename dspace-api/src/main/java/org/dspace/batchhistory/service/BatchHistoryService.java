package org.dspace.batchhistory.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.batchhistory.BatchHistory;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface BatchHistoryService extends DSpaceObjectService<BatchHistory>, DSpaceObjectLegacySupportService<BatchHistory> {
	
	public List<BatchHistory> findAll(Context context) throws SQLException;
	
	int countRows(Context context) throws SQLException;

	BatchHistory create(Context context,String filename, EPerson ePerson,
            List<Long> counts) throws SQLException;

	BatchHistory findByLegacyId(Context context, int id) throws SQLException;

	BatchHistory findByIdOrLegacyId(Context context, String id) throws SQLException;

	BatchHistory create(Context context, String batchname, EPerson ePerson) throws SQLException;

	public List<BatchHistory> getByTimeandState(Context context, Date startDate, Date endDate, String state)
			throws SQLException;

	public List<BatchHistory> findListByBatchName(Context context, List<String> batchNames) throws SQLException;
	
	public List<BatchHistory> findAllByBatchName(Context context, String batchName) throws SQLException;
}