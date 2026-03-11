package org.dspace.uploaddetails;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.dspace.uploaddetails.dao.UploadDetailsDAO;
import org.dspace.uploaddetails.service.UploadDetailsService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

public class UploadDetailsServiceImpl extends DSpaceObjectServiceImpl<UploadDetails> implements UploadDetailsService {

	@Autowired(required = true)
    protected UploadDetailsDAO uploaddetailDAO;

	@Override
	public UploadDetails create(Context context,String filename, EPerson ePerson,
            List<Long> counts) throws SQLException {

    	  	
    	UploadDetails uploaddetails = new UploadDetails();
    	uploaddetails.setSubmitter(ePerson);
    	if(filename.lastIndexOf(".") != -1) {
    		uploaddetails.setBatchName(filename.substring(0, filename.lastIndexOf(".")));
    	}else{
    		uploaddetails.setBatchName(filename);
    	}
    	uploaddetails.setTime(Instant.now());
    	uploaddetails.setUploadstatus("uploading");

    	UploadDetails createdUploadDetaiks = uploaddetailDAO.create(context, uploaddetails);
        return createdUploadDetaiks;
    }
	
	@Override
	public UploadDetails create(Context context,String batchname, EPerson ePerson) throws SQLException {

    	  	
    	UploadDetails uploaddetails = new UploadDetails();
    	uploaddetails.setSubmitter(ePerson);
    	uploaddetails.setBatchName(batchname);
    	uploaddetails.setTime(Instant.now());
    	uploaddetails.setUploadstatus("uploading");

    	UploadDetails createdUploadDetaiks = uploaddetailDAO.create(context, uploaddetails);
        return createdUploadDetaiks;
    }
	
	@Override
	public List<UploadDetails> findAll(Context context) throws SQLException {
		return uploaddetailDAO.findAll(context);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return uploaddetailDAO.countRows(context);
	}

	@Override
	public UploadDetails find(Context context, UUID uuid) throws SQLException {
		return uploaddetailDAO.findByID(context, UploadDetails.class, uuid);
	}

	@Override
	public void updateLastModified(Context context, UploadDetails dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, UploadDetails dso) throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub
		uploaddetailDAO.delete(context, dso);
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public UploadDetails findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UploadDetails findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<UploadDetails> findListByBatchName(Context context, String batchName) throws SQLException {
		return uploaddetailDAO.findListByBatchName(context, batchName);
	}
	
	@Override
	public List<UploadDetails> findByBatchNameAndStatus(Context context, String batchName, String status) throws SQLException {
		return uploaddetailDAO.findByBatchNameAndStatus(context, batchName,status);
	}
	
	@Override
	public List<UploadDetails> findBySubmitter(Context context, EPerson sumbitter) throws SQLException{
		return uploaddetailDAO.findBySubmitter(context, sumbitter);
	}
}
