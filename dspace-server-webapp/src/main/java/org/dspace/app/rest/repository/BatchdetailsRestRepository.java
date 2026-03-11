package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.BatchDetailsRest;
import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.uploaddetails.UploadDetails;
import org.dspace.uploaddetails.service.UploadDetailsService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(BatchDetailsRest.CATEGORY + "." + BatchDetailsRest.PLURAL_NAME)
public class BatchdetailsRestRepository extends DSpaceObjectRestRepository<Batchdetails, BatchDetailsRest>{
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(BatchdetailsRestRepository.class);

	@Autowired
	XmlWorkflowItemService xmlWorkflowItemService;

	@Autowired
	BatchdetailsService batchdetailsService;
	
	@Autowired
	UploadDetailsService uploaddetailsService;
	
	@Autowired
	EPersonService epersonService;
	
	@Autowired
    private ConverterService converterService;
	
	BatchdetailsRestRepository(DSpaceObjectService<Batchdetails> dsoService) {
		super(dsoService);
	}

	@Override
	@PreAuthorize("hasAuthority('AUTHENTICATED')")
	public BatchDetailsRest findOne(Context context, UUID id) {
		Batchdetails batchdetails;
		try {
			batchdetails=batchdetailsService.find(context, id);
		} catch(SQLException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		return converter.toRest(batchdetails, utils.obtainProjection());
	}

	@Override
	public Page<BatchDetailsRest> findAll(Context context, Pageable pageable) {            
		try {
			int total=batchdetailsService.countRows(context);
			List<Batchdetails> batchdetails=batchdetailsService.findAll(context,pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
			return converter.toRestPage(batchdetails, pageable, utils.obtainProjection());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }

	
	@Override
	public Class<BatchDetailsRest> getDomainClass() {
		return BatchDetailsRest.class;
	}
	
    @SearchRestMethod(name = "byProperty")
    public Page<BatchDetailsRest> findBatchDetailsByProperty(
            @Parameter(value = "WorkflowItemID") Integer workflowitemid,
            Pageable pageable)
            		throws SQLException, ParseException {
    	
    	if (workflowitemid==null) {
            throw new DSpaceBadRequestException("WorkflowId should be provided");
        }
        Context context = obtainContext();
        XmlWorkflowItem wi=null;
        if(workflowitemid !=null)
        {
        	  wi = xmlWorkflowItemService.find(context,workflowitemid);
        }
        List<Batchdetails> lbd = new ArrayList<>();
        Batchdetails bd=null;
        if(wi !=null)
        {
        	bd=batchdetailsService.findByWorkFlowId(context, wi);
            lbd.add(bd);
	    }
    	return converter.toRestPage(lbd, pageable,utils.obtainProjection());
    }

    @SearchRestMethod(name = "byReject")
    public Page<BatchDetailsRest> findBatchRejected(
    		@Parameter(value ="Email") String email,Pageable pageable
    		) throws SQLException{
    	List<String> uploaddetailsbatch=new ArrayList<>();
    	List<String> batchdetailsbatch=new ArrayList<>();
    	List<String> finalbatch =new ArrayList<>();
    	if(email==null)
    	{
            throw new DSpaceBadRequestException("USER is not registered");
    	}
    	Context context= obtainContext();
    	EPerson eperson= epersonService.findByEmail(context, email);
    	List<UploadDetails> udl = uploaddetailsService.findBySubmitter(context, eperson);
    	
    	uploaddetailsbatch = udl.stream().map(ud -> ud.getBatchName()).collect(Collectors.toList());
    	
    	List<Batchdetails> bdl = batchdetailsService.findByState(context, "rejected");
    	
    	batchdetailsbatch = bdl.stream().map(bd -> bd.getBatchName()).collect(Collectors.toList());
    	
    	finalbatch = batchdetailsbatch.stream().filter(uploaddetailsbatch::contains).collect(Collectors.toList());
    	
    	List<Batchdetails> tempbatch= batchdetailsService.findListByBatchName(context, finalbatch);
    	List<Batchdetails> finaldetails=batchdetailsService.findListByBatchName(context, finalbatch,pageable.getPageSize(),
                Math.toIntExact(pageable.getOffset()));
    	int total = tempbatch.size();
    	
    	return converterService.toRestPage(finaldetails, pageable,total, utils.obtainProjection());
    }
    
    @SearchRestMethod(name = "bySubmitter")
    public Page<BatchDetailsRest> findBatchStatus(
    		@Parameter(value ="Email") String email,Pageable pageable
    		) throws SQLException{
    	List<String> uploaddetailsbatch=new ArrayList<>();
    	if(email == null)
    	{
            throw new DSpaceBadRequestException("USER is not registered");
    	}
    	Context context= obtainContext();
    	EPerson eperson= epersonService.findByEmail(context, email);
    	List<UploadDetails> udl = uploaddetailsService.findBySubmitter(context, eperson);
    	uploaddetailsbatch = udl.stream().map(ud -> ud.getBatchName()).collect(Collectors.toList());
    	
    	List<Batchdetails> tempbatch= batchdetailsService.findListByBatchName(context, uploaddetailsbatch);
    	List<Batchdetails> finaldetails=batchdetailsService.findBatchDetailsByBatchNames(context, uploaddetailsbatch,pageable.getPageSize(),
                Math.toIntExact(pageable.getOffset()));
    	
    	return converterService.toRestPage(finaldetails, pageable,tempbatch.size(), utils.obtainProjection());
    }
}
