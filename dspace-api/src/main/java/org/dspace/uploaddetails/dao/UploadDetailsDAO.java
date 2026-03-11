package org.dspace.uploaddetails.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.uploaddetails.UploadDetails;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;

public interface UploadDetailsDAO extends DSpaceObjectDAO<UploadDetails>, DSpaceObjectLegacySupportDAO<UploadDetails>,GenericDAO<UploadDetails> {

public List<UploadDetails> findAll(Context context)throws SQLException;
	
	int countRows(Context context) throws SQLException;

	public List<UploadDetails> findBySubmitter(Context context, EPerson submitter) throws SQLException;

	public List<UploadDetails> findListByBatchName(Context context, String batchName) throws SQLException;

	public List<UploadDetails> findByBatchNameAndStatus(Context context, String batchName, String status) throws SQLException;

}
