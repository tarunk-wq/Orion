package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.DuplicateEntityException;
import org.dspace.app.rest.model.UserMetadataDto;
import org.dspace.app.rest.service.DynamicMetadataService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/metadata"})
public class DynamicMetadataController {
  private static final Logger log = LoggerFactory.getLogger(DynamicMetadataController.class);
	
   @Autowired
   DynamicMetadataService dynamicMetadataService;
   
   @Autowired
   UserMetadataFieldsService userMetadataFieldsService;
   
   @Autowired
   CommunityService communityService;

   @PostMapping({"/add/{uuid}"})
   public ResponseEntity<String> addMetadata(@PathVariable UUID uuid, @RequestBody UserMetadataDto[] userMetaDataList, HttpServletRequest request) throws SQLException {
      Context context = ContextUtil.obtainContext(request);

      try {
         this.dynamicMetadataService.addMetadata(context, uuid, userMetaDataList);
      } catch (Exception e) {
         return ResponseEntity.ok("Unable to Add metadata");
      }

      context.complete();
      return ResponseEntity.ok("Successfully added");
   }

   @DeleteMapping("/delete/{subDepartmentId}/{metadataId}")
   public ResponseEntity<String> deleteMetadata(@PathVariable ("subDepartmentId") UUID subDepartmentId, @PathVariable ("metadataId") int metadataId, HttpServletRequest request) throws SQLException {
      Context context = ContextUtil.obtainContext(request);
      try {
    	 UserMetadataFields userMetadata = userMetadataFieldsService.findById(context, metadataId);
         this.dynamicMetadataService.deleteMetadata(context, subDepartmentId, userMetadata);
      }	catch (DuplicateEntityException e) {
    	  throw new DuplicateEntityException(String.format("Cannot delete metadata: This field is still assigned to existing items."
      	  		+ "Please remove these references before deleting."));
      } catch (Exception e) {
         return ResponseEntity.ok("Unable to delete metadata");
      }

      context.complete();
      return ResponseEntity.ok("Successfully deleted");
   }
   
   @PatchMapping("/uniqueflagmetadata/{subDepartmentId}/{metadataId}")
   public ResponseEntity<String> updateUniqueFlagMetadata (@PathVariable ("subDepartmentId") UUID subDepartmentId, @PathVariable ("metadataId") int metadataId,
		   @RequestParam("enabled") Boolean enabled, HttpServletRequest request) throws SQLException {
	   Context context = ContextUtil.obtainContext(request);
	    try {
	        UserMetadataFields userMetadata = userMetadataFieldsService.findById(context, metadataId);
	        if (userMetadata == null) {
	            context.abort();
	            return ResponseEntity.badRequest().body("Metadata not found");
	        }
	        
	        userMetadata.setIsUniqueMetadata(enabled);
	        userMetadataFieldsService.update(context, userMetadata);
	        context.complete();
	        return ResponseEntity.ok("Successfully updated metadata");
	    } catch (Exception e) {
	        context.abort();
	        return ResponseEntity.internalServerError()
	                .body("Unable to update metadata");
	    }
   }
   
   @GetMapping("/filters/{dspaceObjectId}")
   public ResponseEntity<List<UserMetadataDto>> getFiltersBySection(HttpServletRequest request, @PathVariable UUID dspaceObjectId) throws SQLException {
	   Context context = ContextUtil.obtainContext(request);
	   List<UserMetadataDto> filters = dynamicMetadataService.getFiltersForDspaceObject(context, dspaceObjectId);
	   return ResponseEntity.ok(filters);
   }
   
   
   @PutMapping("/duplicacycheck/{subDepartmentId}")
   public ResponseEntity<String> updateDuplicacyCheckSubdepartment(
           @PathVariable("subDepartmentId") UUID subDepartmentId,  @RequestParam("enabled") Boolean enabled, HttpServletRequest request) throws Exception, AuthorizeException {
	   	Context context = ContextUtil.obtainContext(request);
	   	try {
	        communityService.updateDuplicacyCheck(context, subDepartmentId, enabled);
	        context.complete();
	        return ResponseEntity.ok("Updated unique metadata successfully");
	    } catch (AuthorizeException e) {
	        context.abort();
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body("Not authorized");

	    } catch (IllegalArgumentException e) {
	        context.abort();
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(e.getMessage());
	    }      
   }
}
