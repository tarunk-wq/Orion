package org.dspace.batchreject.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.batchreject.BatchReject;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

public interface BatchRejectDAO extends DSpaceObjectDAO<BatchReject>, DSpaceObjectLegacySupportDAO<BatchReject>,GenericDAO<BatchReject>  {
	
	int countRows(Context context) throws SQLException;
	
	public List<BatchReject> findAll(Context context, int limit, int offset) throws SQLException;
	
	public List<BatchReject> findAll(Context context)throws SQLException;

	List<BatchReject> findByTime(Context context, Date startDate, Date endDate) throws SQLException;

	public List<BatchReject> findByBatchName(Context context, String batchname) throws SQLException;

}
