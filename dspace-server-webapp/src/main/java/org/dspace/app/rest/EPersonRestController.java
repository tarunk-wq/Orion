package org.dspace.app.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.repository.EPersonRestRepository;
import org.dspace.app.rest.service.UserPermissionService;
import org.dspace.app.rest.service.UserService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.PasswordValidatorService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.PasswordHistoryService;
import org.dspace.exception.HttpStatusException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/eperson/")
public class EPersonRestController {
	
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(EPersonRestController.class);
	
	@Autowired(required = true)
	EPersonService epersonService;
	
	@Autowired(required = true)
	BitstreamService bitstreamService;
	
	@Autowired(required = true)
	BitstreamFormatService bitstreamFormatService;
	
	@Autowired
	PasswordHistoryService passwordHistoryService;
	
	@Autowired
	PasswordValidatorService passwordValidatorService;
	
	@Autowired(required = true)
	ItemService itemService;
	
	@Autowired(required = true)
	GroupService groupService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private EPersonRestRepository eRR;
	
	@Autowired(required = true)
	UserPermissionService userPermissionService;
	
	private static final int expiryDays = DSpaceServicesFactory.getInstance().getConfigurationService()
            .getIntProperty("eperson.password.expiry.days", 90);
	
	/**
     * Upload or replace profile image
	 * @throws SQLException 
	 * @throws IOException 
     */
	@PostMapping("/profile-picture/{epersonId}")
	public ResponseEntity<?> uploadProfilePicture( HttpServletResponse response, HttpServletRequest request,
												   @RequestParam("file") MultipartFile file, @PathVariable UUID epersonId
												) throws SQLException, IOException{
		 Context context = ContextUtil.obtainContext(request);
		 EPerson ePerson = epersonService.find(context, epersonId);
		 
		 if (ePerson == null)
			 return ResponseEntity.notFound().build();
		 
		 try {
			 InputStream inputStream = file.getInputStream();
			 
			 Bitstream bitstream = bitstreamService.create(context, inputStream);
			 bitstream.setName(context, file.getOriginalFilename());
			 BitstreamFormat format = bitstreamFormatService.findByMIMEType(context, file.getContentType());
		     bitstream.setFormat(context, format);
		     bitstreamService.update(context, bitstream);
		     
		     // Save profile image UUID in metadata or a custom field
	         epersonService.setMetadataSingleValue(context, ePerson, MetadataSchemaEnum.EPERSON.getName(), "profileimage", null, null, bitstream.getID().toString());
	         epersonService.update(context, ePerson);

	         context.complete();
	         return ResponseEntity.ok().body("Profile image uploaded successfully");
		} catch (Exception e) {
			log.error("Error in uploading profile picture",e);
			return ResponseEntity.internalServerError().body("Error in uploading profile picture");
		}
		 
	}

	@PostMapping("/bulk-import")
	public ResponseEntity<?> bulkImportEperson( HttpServletResponse response, HttpServletRequest request,
			@RequestParam("file") MultipartFile file) throws SQLException, IOException{
		Context context = ContextUtil.obtainContext(request);
		// File is the CSV File.
		try {
			List<String[]> rows = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
	            String line;
	            while ((line = br.readLine()) != null) {
	                // Split CSV line by comma
	                String[] values = line.split(",");
	                rows.add(values);
	            }
	        }
			Map<Integer,String> errorMap = eRR.importEperson(context ,rows);
			context.complete();
			StringBuilder errors = new StringBuilder();
			if(!errorMap.isEmpty()) {
				for (Map.Entry<Integer, String> entry : errorMap.entrySet()) {
					errors.append("Row "+entry.getKey()+": "+entry.getValue());
					errors.append("\n");
				}
				return ResponseEntity.ok().body(String.valueOf(errors));
			}
		} catch (Exception e) {
			log.error("Error in uploading profile picture",e);
			return ResponseEntity.internalServerError().body("E01");
		}
		return ResponseEntity.ok().body("S01"); // Success
		
	}
	
	@GetMapping("/profile-picture/{epersonId}")
	public ResponseEntity<?> getProfilePicture(HttpServletResponse response, HttpServletRequest request,@PathVariable UUID epersonId) throws SQLException, IOException, AuthorizeException{
		 Context context = ContextUtil.obtainContext(request);
		 EPerson ePerson = epersonService.find(context, epersonId);
		 
		 if (ePerson == null)
			 return ResponseEntity.notFound().build();
		 
		 context.turnOffAuthorisationSystem();
		 
		 List<MetadataValue> profilePicBitstreams = epersonService.getMetadataByMetadataString(ePerson, String.join(".", MetadataSchemaEnum.EPERSON.getName(),"profileimage"));
		 if(profilePicBitstreams.isEmpty()) {
			 return ResponseEntity.notFound().build();
		 }
		 
		 UUID bitstreamUUID = UUID.fromString(profilePicBitstreams.get(0).getValue());
		 Bitstream profilePic = bitstreamService.find(context, bitstreamUUID);
		 
		 InputStreamResource resource = new InputStreamResource(bitstreamService.retrieve(context, profilePic));
		 
		 context.restoreAuthSystemState();
		 
         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.parseMediaType(profilePic.getFormat(context).getMIMEType()));
         headers.setContentLength(profilePic.getSizeBytes());
         headers.setContentDisposition(ContentDisposition.inline().filename(profilePic.getName()).build());
         
         return new ResponseEntity<>(resource, headers, HttpStatus.OK);
	}
	
	@PutMapping("/update-profile-picture/{epersonId}")
	public ResponseEntity<?> updateProfilePicture(HttpServletResponse response, HttpServletRequest request,@RequestParam("file") MultipartFile file,
			@PathVariable UUID epersonId) throws SQLException{
		 Context context = ContextUtil.obtainContext(request);
		 EPerson ePerson = epersonService.find(context, epersonId);
		 
		 if (ePerson == null)
			 return ResponseEntity.notFound().build();
		 
		 try {
			 InputStream inputStream = file.getInputStream();
			 
			 List<MetadataValue> profilePicBitstreams = epersonService.getMetadataByMetadataString(ePerson, String.join(".", MetadataSchemaEnum.EPERSON.getName(),"profileimage"));
			 if(!profilePicBitstreams.isEmpty()) {
				 UUID bitstreamUUID = UUID.fromString(profilePicBitstreams.get(0).getValue());
				 Bitstream profilePic = bitstreamService.find(context, bitstreamUUID);
				 bitstreamService.updateDocument(context, profilePic, inputStream);
				 bitstreamService.update(context, profilePic);
			 }else {
				 Bitstream bitstream = bitstreamService.create(context, inputStream);
				 bitstream.setName(context, file.getOriginalFilename());
				 BitstreamFormat format = bitstreamFormatService.findByMIMEType(context, file.getContentType());
			     bitstream.setFormat(context, format);
			     bitstreamService.update(context, bitstream);
			     
		         epersonService.setMetadataSingleValue(context, ePerson, MetadataSchemaEnum.EPERSON.getName(), "profileimage", null, null, bitstream.getID().toString());
		         epersonService.update(context, ePerson);
			 }
			 
	         context.complete();
	         return ResponseEntity.ok().body("Profile image updated successfully");
		} catch (Exception e) {
			log.error("Error in updating profile picture",e);
			return ResponseEntity.internalServerError().body("Error in updated profile picture");
		}
	}
	
	@PutMapping("/{uuid}/resetpassword")
	public ResponseEntity<?> resetPassword(@PathVariable UUID uuid,
											@RequestBody List<JsonNode> operations,
	                                       HttpServletRequest request) throws SQLException {
	    Context context = null;

	    try {
	        context = ContextUtil.obtainContext(request);
	        
	        if(StringUtils.isBlank(request.getParameter("token"))) {
	        	log.warn("token missing");
	            return ResponseEntity.badRequest().body(Map.of("error", "token missing"));
	        }
	        
	        String password = null;

	        // 🌟 Extract password from JSON
	        for (JsonNode operation : operations) {
	            String path = operation.get("path").asText();
	            if ("/password".equalsIgnoreCase(path)) {
	                JsonNode valueNode = operation.get("value");
	                if (valueNode != null && valueNode.has("new_password")) {
	                    password = valueNode.get("new_password").asText();
	                }
	            }
	        }

	        if (password == null) {
	        	log.warn("password missing in request");
	            return ResponseEntity.badRequest().body(Map.of("error", "Missing password in request"));
	        }

	        // Now validate and update password like before...
	        EPerson eperson = epersonService.find(context, uuid);
			
			if (!passwordValidatorService.isPasswordValid(password)) {
				log.warn("Password does not match the regex"); return
				ResponseEntity.badRequest().body(Map.of("error", "Password does not meet complexity requirements")); 
			}

	        if (passwordHistoryService.isPasswordReused(context, eperson, password)) {
	        	log.warn("Old Password Reused!");
	            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
	                    .body(Map.of("error", "Old password cannot be reused"));
	        }

	        context.turnOffAuthorisationSystem();
	        epersonService.setPassword(eperson, password);

	        Calendar cal = Calendar.getInstance();
	        cal.add(Calendar.DAY_OF_YEAR, expiryDays);
	        eperson.setPasswordExpiryDate(cal.getTime());

	        passwordHistoryService.addPasswordHistory(context, eperson, password);
	        epersonService.update(context, eperson);
	        context.restoreAuthSystemState();

	        log.info("Password updated successfully!");
	        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));

	    } catch (Exception e) {
	    	log.error("Internal Server Error", e);
	        return ResponseEntity.internalServerError()
	                .body(Map.of("error", "Internal Server error: " + e.getMessage()));
	    } finally {
	        if (context != null && context.isValid()) {
	            context.complete();
	        }
	    }
	}
	
	@PutMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody ObjectNode body,
	                                       HttpServletRequest request) throws SQLException, AuthorizeException {
	    
		try {
			Context context = ContextUtil.obtainContext(request);
			context.turnOffAuthorisationSystem();
			String email = body.get("email").asText();
			String oldPassword = body.get("oldPassoword").asText();
			String newPassword = body.get("newPassoword").asText();

			epersonService.resetPassword(context, email, oldPassword, newPassword);
			context.complete();
			return new ResponseEntity<>("Password reset successfully", HttpStatus.OK);
			
		} catch (HttpStatusException e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.valueOf(e.getStatusCode()));
		}    
	}
	
	@GetMapping("/search")
	public ResponseEntity<?> getEpersonData(HttpServletRequest request) throws Exception {
		Context context = ContextUtil.obtainContext(request);			
		EPerson currentUser = context.getCurrentUser();
		
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not logged in");
		}
		
		List<EPerson> allUsers = epersonService.findAll(context, EPerson.ID);
		
		return ResponseEntity.ok(userService.getFilteredUser(context,allUsers));
	}
	
	@GetMapping("/{uuid}/permissions")
	public ResponseEntity<?> getEpersonPermissions(@PathVariable UUID uuid,
			@RequestParam String type, HttpServletRequest request) throws Exception {
		
		Context context = ContextUtil.obtainContext(request);			
		EPerson currentUser = context.getCurrentUser();
		
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not logged in");
		}
		
		return ResponseEntity.ok(userPermissionService.getEpersonPermissions(context, currentUser, type, uuid));		
	}
	
	@GetMapping("/permissions")
	public ResponseEntity<?> getPermissions(HttpServletRequest request) throws Exception {
		
		Context context = ContextUtil.obtainContext(request);			
		EPerson currentUser = context.getCurrentUser();
		
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not logged in");
		}
		
		return ResponseEntity.ok(userPermissionService.getPermissions(context, currentUser));		
	}
}
