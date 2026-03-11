package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.converter.FolderConverter;
import org.dspace.app.rest.model.DepartmentHierarchyDTO;
import org.dspace.app.rest.model.ModifyMetadataResponseDto;
import org.dspace.app.rest.model.SubDepartmentRest;
import org.dspace.app.rest.model.UserMetadataDto;
import org.dspace.app.rest.service.SubDepartmentService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.constants.enums.NodeType;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.usermetadata.UserMetadataFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/core/sub-department")
public class SubDepartmentController {
	
	@Autowired
	private SubDepartmentService subDepartmentService;
	
	@Autowired
	private FolderConverter folderConverter;
	
	@PostMapping
    public ResponseEntity<SubDepartmentRest> create(@RequestBody SubDepartmentRest rest, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        Context context = ContextUtil.obtainContext(request);
		UserMetadataDto[] userMetaDataList = rest.getUserMetadataList();
        SubDepartmentRest subDepartmentRest = subDepartmentService.create(context, rest, userMetaDataList);
        context.complete();
        return new ResponseEntity<>(subDepartmentRest, HttpStatus.OK);
    }
	
	@GetMapping("{uuid}")
    public ResponseEntity<DepartmentHierarchyDTO> findById(@PathVariable UUID uuid,
    						HttpServletRequest request) throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);
        Community subDepartment = subDepartmentService.findById(context, uuid);
        return new ResponseEntity<>(folderConverter.convert(subDepartment, NodeType.SUB_DEPARTMENT.name()), HttpStatus.OK);
    }

    @GetMapping("allUser/{uuid}")
    public ResponseEntity<?> findAllEPerson(@PathVariable String uuid, HttpServletRequest request) throws SQLException {
    	Context context = ContextUtil.obtainContext(request);
    	return ResponseEntity.ok(subDepartmentService.getEpersonsArrayNode(context, uuid));
    }
	
	@DeleteMapping("{uuid}")
    public ResponseEntity<String> delete(@PathVariable UUID uuid,
    						HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        try {
        	Context context = ContextUtil.obtainContext(request);
        	Community sub = subDepartmentService.findById(context, uuid);
        	if(sub != null) {
        		subDepartmentService.delete(context, uuid);
        	}
        	context.complete();
        	return new ResponseEntity<>("Sub departement deleted successfully", HttpStatus.OK);
			
		} catch (ResponseStatusException e) {
			return new ResponseEntity<>(e.getReason(), e.getStatusCode());		
		}
    }
	
	@PostMapping("/assignAdmin/{uuid}")
    public ResponseEntity<String> assignAdmin(@PathVariable UUID uuid, @RequestBody String userId, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        Context context = ContextUtil.obtainContext(request);
        subDepartmentService.assignAdmin(context, uuid, userId);
        context.complete();
        return new ResponseEntity<>("Admin assigned successfully", HttpStatus.OK);
    }
	@PatchMapping("/removeAdmin/{uuid}")
	public ResponseEntity<String> removeAdmin(@PathVariable UUID uuid, @RequestBody List<UUID> userIds, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
		Context context = ContextUtil.obtainContext(request);
		subDepartmentService.removeAdmin(context, uuid, userIds);
		context.complete();
		return new ResponseEntity<>("Admin assigned successfully", HttpStatus.OK);
	}
		
	@GetMapping({"/modifyMetadata/{subDepartmentId}"})
	public ResponseEntity<ModifyMetadataResponseDto> modifyMetadata(@PathVariable UUID subDepartmentId, HttpServletRequest request) throws SQLException {
		Context context = ContextUtil.obtainContext(request);		
		try {
			ModifyMetadataResponseDto response = subDepartmentService.getSubDepartmentMetadata(context, subDepartmentId);
	        
	        return ResponseEntity.ok(response);

	    } catch (Exception e) {
	        return ResponseEntity.internalServerError().build();
	    } finally {
	        context.complete();
	    }
	}
	
}
