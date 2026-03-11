package org.dspace.app.rest;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.repository.BitstreamRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.userbitstream.UserBitstream;
import org.dspace.userbitstream.service.UserBitstreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/" + ItemRest.CATEGORY + "/" + ItemRest.NAME)
public class ItemRestController {

	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemRestController.class);
	
	public final static String ITEM_IMPORT_BASE_PATH = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.item.import.base.path");
	
	public final static String CSV_EXT = ".csv";
	
	public final static String ZIP_EXT = ".zip";
	
	@Autowired(required = true)
	ItemService itemService;
	
	@Autowired
    BitstreamRestRepository bitstreamRestRepository;
	
	@Autowired(required = true)
	private BitstreamService bitstreamService;
	
	@Autowired(required = true)
	protected UserBitstreamService userBitstreamService;
	
	@PreAuthorize("hasAuthority('AUTHENTICATED')")
	@PostMapping("/update-file/{bitstreamID}/{itemId}")
	public ResponseEntity<BitstreamRest> processAnnotatedBitstream(
	        HttpServletResponse response,
	        HttpServletRequest request,
	        @RequestBody MultipartFile file,
	        @PathVariable UUID bitstreamID,
	        @PathVariable UUID itemId) throws SQLException, IOException, AuthorizeException {

	    if (file == null || file.isEmpty()) {
	        return ResponseEntity.badRequest().body(null);
	    }

	    Context context = ContextUtil.obtainContext(request);
	    Bitstream oldBitstream = bitstreamService.find(context, bitstreamID);

	    if (oldBitstream == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
	    }

	    EPerson eperson = context.getCurrentUser();
	    boolean checkIfBitstreamVersionExist = Utils.checkIfAnnotatedBitstreamExist(context, eperson, oldBitstream);

	    context.turnOffAuthorisationSystem();
	    
	    try (InputStream inputStream = file.getInputStream()) {
	        if (checkIfBitstreamVersionExist) {
	            try {
	                bitstreamID = Utils.updateExistingAnnotatedBitstream(context, inputStream, eperson, oldBitstream);
	            } catch (Exception e) {
	                log.error("Error updating existing bitstream version", e);
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	            }
	        } else {
	            try {
	                bitstreamID = Utils.createAnnotatedBitstream(context, itemId, inputStream, eperson, oldBitstream);
	            } catch (Exception e) {
	                log.error("Error creating new bitstream version", e);
	                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	            }
	        }

	        BitstreamRest updatedBitstream = bitstreamRestRepository.findOne(context, bitstreamID);
	        context.commit();
	        return ResponseEntity.status(HttpStatus.CREATED).body(updatedBitstream);

	    } finally {
	        context.restoreAuthSystemState();
	    }
	}
	
	@PreAuthorize("hasAuthority('AUTHENTICATED')")
	@GetMapping("/delete-annotation/{bitstreamID}")
	public ResponseEntity<Boolean> deleteBitstreamVersion(HttpServletResponse response, HttpServletRequest request,
	        @PathVariable UUID bitstreamID) throws SQLException {

	    Context context = ContextUtil.obtainContext(request);
	    Bitstream oldBitstream = bitstreamService.find(context, bitstreamID);
	    if (oldBitstream == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
	    }
	    EPerson eperson = context.getCurrentUser();
	    boolean checkIfBitstreamVersionExist = Utils.checkIfAnnotatedBitstreamExist(context, eperson, oldBitstream);
	    
	    if(checkIfBitstreamVersionExist) {
        	UserBitstream userBitstream = userBitstreamService.findByEperson(context, eperson, oldBitstream).get(0);
			try {
				UUID versionBitstream = userBitstream.getVersionedBistream().getID();
	        	bitstreamService.delete(context, userBitstream.getVersionedBistream());
				userBitstreamService.delete(context, userBitstream);
				log.info("Version Deleted Successfully " + versionBitstream + " of " + bitstreamID);	
		        return ResponseEntity.status(HttpStatus.OK).body(true);       
			} catch (SQLException | AuthorizeException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		        return ResponseEntity.status(HttpStatus.OK).body(false);       
			} finally {
				context.commit();
			}
		} else {
			log.info("No Bitstream Version for Bitstream " + bitstreamID);
	        return ResponseEntity.status(HttpStatus.OK).body(false);       
		}    	
	}
	
	@PreAuthorize("hasAuthority('AUTHENTICATED')")
	@GetMapping("/check-bitstream-version/{bitstreamID}")
	public ResponseEntity<Boolean> checkIfBitstreamVersionExist(
	        HttpServletResponse response,
	        HttpServletRequest request,
	        @PathVariable UUID bitstreamID) throws SQLException, IOException {

	    Context context = ContextUtil.obtainContext(request);
	    Bitstream oldBitstream = bitstreamService.find(context, bitstreamID);

	    if (oldBitstream == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
	    }

	    EPerson eperson = context.getCurrentUser();
	    boolean checkIfBitstreamVersionExist = Utils.checkIfAnnotatedBitstreamExist(context, eperson, oldBitstream);

	    return ResponseEntity.ok(checkIfBitstreamVersionExist);
	}
	
	@PreAuthorize("hasAuthority('AUTHENTICATED')")
	@PutMapping("/checkout/{itemId}")
	public ResponseEntity<String> checkout(
			HttpServletResponse response, HttpServletRequest request,
			@PathVariable UUID itemId, @RequestParam String action) throws Exception {		

		try {
			Context context = ContextUtil.obtainContext(request);
			itemService.checkout(context, itemId, context.getCurrentUser(), action);
			return new ResponseEntity<>("Item checkout successfully", HttpStatus.OK);		
		
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}	
	}
}
