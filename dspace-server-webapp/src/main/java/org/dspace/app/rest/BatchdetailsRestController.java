package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.uploaddetails.UploadDetails;
import org.dspace.uploaddetails.service.UploadDetailsService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batchdetailr/batchdetails")
public class BatchdetailsRestController {
    private static final Logger log = LogManager.getLogger();

    @Autowired
	XmlWorkflowItemService xmlWorkflowItemService;

	@Autowired
	BatchdetailsService batchdetailsService;
	
	@Autowired
	UploadDetailsService uploaddetailsService;
	
	@Autowired
	EPersonService epersonService;
	
	@Autowired
	ItemService itemService;
	
	@Autowired
	protected ConfigurationService configurationService;
	
	@PostMapping("/viewRejectedSubmissions")
	 public Batchdetails[] findBatchRejected(
			 HttpServletResponse response,
			   HttpServletRequest request,
			   @RequestParam(name = "Email") String email
	    		) throws SQLException,SearchServiceException, IOException, AuthorizeException{
	    	List<String> uploaddetailsbatch=new ArrayList<>();
	    	List<String> batchdetailsbatch=new ArrayList<>();
	    	List<String> finalbatch =new ArrayList<>();
	        Context context = ContextUtil.obtainContext(request);
	    	if(email==null)
	    	{
	            throw new DSpaceBadRequestException("USER is not registered");
	    	}
	    	EPerson eperson= epersonService.findByEmail(context, email);
	    	List<UploadDetails> udl = uploaddetailsService.findBySubmitter(context, eperson);
	    	for(UploadDetails ud:udl)
	    	{
	    		uploaddetailsbatch.add(ud.getBatchName());
	    	}
	    	List<Batchdetails> bdl = batchdetailsService.findByState(context, "rejected");
	    	for(Batchdetails bd:bdl)
	    	{
	    		batchdetailsbatch.add(bd.getBatchName());
	    	}
	    	for(String b: batchdetailsbatch)
	    	{
	    		if(uploaddetailsbatch.contains(b))
	    		{
	    			finalbatch.add(b);
	    		}
	    	}
	    	List<Batchdetails> finaldetails=batchdetailsService.findListByBatchName(context, finalbatch);
	    	return finaldetails.toArray(new Batchdetails[finaldetails.size()]);
	    }
    
	@PostMapping("/viewItems")
	public List<String[]> viewItems(
			 HttpServletResponse response,
			   HttpServletRequest request,
			   @RequestParam(name = "batchName") String batchName,
			   @RequestParam(name = "collection") String collection
	    		) throws SQLException,SearchServiceException, IOException, AuthorizeException{
        Context context = ContextUtil.obtainContext(request);
        Batchdetails bd = batchdetailsService.findByBatchName(context, batchName);
        log.info("BatchName:: ", batchName);
        List<String[]> finalList = new ArrayList<String[]>();
    	List<String> view = new ArrayList<String>();
    	if(collection.equalsIgnoreCase(configurationService.getProperty("collection.admin.uuid"))) {
    		view =Arrays.asList(configurationService.getArrayProperty("process.details.admin.metadata"));
    	} else if (collection.equalsIgnoreCase(configurationService.getProperty("collection.gazettefile.uuid"))) {
    		view =Arrays.asList(configurationService.getArrayProperty("process.details.gazettefile.metadata"));
    	} else {
    		view =Arrays.asList(configurationService.getArrayProperty("process.details.case.metadata"));
    	}
    	for(UUID uuid : bd.getItemIds()) {
        	Item item = itemService.find(context, uuid);
        	List<String> values = new ArrayList<String>();
        	for(String v:view) {
        		String meta = "";
        		if(v.contains(".")) {
        			String[] com = v.split("\\.");
            		meta = itemService.getMetadataFirstValue(item, MetadataSchemaEnum.DC.getName(), com[0], com[1], Item.ANY);
        		} else {
            		meta = itemService.getMetadataFirstValue(item, MetadataSchemaEnum.DC.getName(), v, null, Item.ANY);
        		}
        		values.add(meta);
        	}
        	finalList.add(values.toArray(new String[0]));
        }
        return finalList;
	}
    
}
