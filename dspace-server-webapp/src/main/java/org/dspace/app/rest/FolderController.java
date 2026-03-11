package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.converter.FolderConverter;
import org.dspace.app.rest.model.FolderRest;
import org.dspace.app.rest.service.FolderService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.constants.enums.NodeType;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/core/folder")
public class FolderController {
	
	@Autowired
	private FolderConverter folderConverter;
	
	@Autowired
	private FolderService folderService;
	
	@Autowired
	private CommunityService communityService;
	
	@Autowired
	private CollectionService collectionService;
	
	@PostMapping
    public ResponseEntity<FolderRest> create(@RequestBody FolderRest rest, 
    						HttpServletRequest request) throws SQLException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);
        
        FolderRest folderRest = folderService.create(context, rest);
        context.complete();
        return new ResponseEntity<>(folderRest, HttpStatus.OK);
    }

	@GetMapping("{uuid}")
    public ResponseEntity<?> fetchSubFolders(@PathVariable UUID uuid, @RequestParam String type,
    						HttpServletRequest request) throws Exception {
        
		Context context = ContextUtil.obtainContext(request);
        if (type.equalsIgnoreCase(NodeType.BRANCH.name())) {       	
        	Community subDepartment = communityService.find(context, uuid);
        	return new ResponseEntity<>(folderConverter.convert(subDepartment, NodeType.BRANCH.name()), HttpStatus.OK);

        } else if (type.equalsIgnoreCase(NodeType.LEAF.name())) {       	
        	Collection collection = collectionService.find(context, uuid);
        	return new ResponseEntity<>(folderConverter.convertLeaf(collection), HttpStatus.OK);
        
        } else {
        	throw new Exception("Invalid node type");
        }
    }
	
	@GetMapping("/findById/{uuid}")
    public ResponseEntity<?> findById(@PathVariable UUID uuid, @RequestParam String type,
    						HttpServletRequest request) throws Exception {
        
		Context context = ContextUtil.obtainContext(request);
        if (type.equalsIgnoreCase(NodeType.BRANCH.name())) {       	
        	Community community = communityService.find(context, uuid);
        	return new ResponseEntity<>(folderConverter.convertBranch(community), HttpStatus.OK);

        } else if (type.equalsIgnoreCase(NodeType.LEAF.name())) {       	
        	Collection collection = collectionService.find(context, uuid);
        	return new ResponseEntity<>(folderConverter.convertLeaf(collection), HttpStatus.OK);
        
        } else {
        	throw new Exception("Invalid node type");
        }
    }
	
	@PutMapping("/rename/{uuid}")
    public ResponseEntity<String> renameFolder(@PathVariable UUID uuid, @RequestParam String name, 
    		@RequestParam String type, HttpServletRequest request) throws Exception {
        
		Context context = ContextUtil.obtainContext(request);       
        folderService.rename(context, uuid, name, type);
        context.complete();
        return new ResponseEntity<>("Folder name changed successfully", HttpStatus.OK);
    }
	
	@DeleteMapping("/{uuid}")
    public ResponseEntity<String> delete(@PathVariable UUID uuid, @RequestParam String type,
    						HttpServletRequest request) throws Exception {
        
		try {
        	Context context = ContextUtil.obtainContext(request);       
        	DSpaceObject folder = folderService.findById(context, uuid);
        	if(folder != null) {
        		folderService.delete(context, uuid, type);
        	}
        	context.complete();
        	return new ResponseEntity<>("Folder deleted successfully", HttpStatus.OK);
        	
        } catch (DataIntegrityViolationException e) {
			if (e.getMessage().equalsIgnoreCase("CANNOT-DELETE")) {
				return new ResponseEntity<>("Cannot delete: folder is not empty.", HttpStatus.BAD_REQUEST);
			}
			throw e;
		}
    }
	
	@GetMapping("/findByName/{parentId}")
    public ResponseEntity<ArrayNode> findByName(@PathVariable UUID parentId,
    		@RequestParam String name, HttpServletRequest request) throws SQLException, AuthorizeException {
        
		Context context = ContextUtil.obtainContext(request);
        
		ArrayNode result = folderService.findByName(context, parentId, name);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
	
	@PostMapping("/givePermission")
    public ResponseEntity<String> givePermission(@RequestBody ObjectNode body, HttpServletRequest request) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        
        folderService.givePermission(context, body);
        context.complete();
        return new ResponseEntity<>("Permission set successfully", HttpStatus.OK);
    }
	
	@PostMapping("/revokePermission")
    public ResponseEntity<String> revokePermission(@RequestBody ObjectNode body, HttpServletRequest request) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        
        folderService.revokePermission(context, body);
        context.complete();
        return new ResponseEntity<>("Permission revoked successfully", HttpStatus.OK);
    }
	
	@GetMapping("/fetch-permissions/{folderId}")
    public ResponseEntity<ArrayNode> fetchPermissions(@PathVariable UUID folderId, @RequestParam String type, HttpServletRequest request) throws Exception {
        Context context = ContextUtil.obtainContext(request);     
        ArrayNode response = folderService.fetchPermissions(context, folderId, type);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

	@GetMapping("/fetch-group-permissions/{groupId}")
	public ResponseEntity<ArrayNode> fetchGRoupPermissions(@PathVariable UUID groupId, HttpServletRequest request) throws Exception {
		Context context = ContextUtil.obtainContext(request);     
		ArrayNode response = folderService.fetchGroupPermissions(context, groupId);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@GetMapping("/search/{folderId}")
    public ResponseEntity<ArrayNode> searchFolder(@PathVariable UUID folderId, @RequestParam String name, HttpServletRequest request) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        ArrayNode response = folderService.searchFolder(context, folderId, name);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
	
	@GetMapping("/get-folder-hierarchy/{folderId}")
    public ResponseEntity<String> getFolderHierarchy(@PathVariable UUID folderId, HttpServletRequest request) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        String path = folderService.getFolderHierarchy(context, folderId);
        return new ResponseEntity<>(path, HttpStatus.OK);
    }
}
