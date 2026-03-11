package org.dspace.uploaddetails.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.uploaddetails.UploadDetails;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface UploadDetailsService extends DSpaceObjectService<UploadDetails>, DSpaceObjectLegacySupportService<UploadDetails>{
	
	public List<UploadDetails> findAll(Context context) throws SQLException;
	
	int countRows(Context context) throws SQLException;
	
	UploadDetails create(Context context,String filename, EPerson ePerson,
            List<Long> counts) throws SQLException;

	UploadDetails create(Context context, String batchname, EPerson ePerson) throws SQLException;

	List<UploadDetails> findBySubmitter(Context context, EPerson sumbitter) throws SQLException;

	List<UploadDetails> findListByBatchName(Context context, String batchName) throws SQLException;

	List<UploadDetails> findByBatchNameAndStatus(Context context, String batchName, String status) throws SQLException;
}