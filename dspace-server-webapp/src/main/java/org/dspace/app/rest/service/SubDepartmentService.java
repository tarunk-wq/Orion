package org.dspace.app.rest.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


import org.dspace.app.rest.converter.UserMetadataConverter;
import org.dspace.app.rest.exception.DuplicateEntityException;
import org.dspace.app.rest.model.ModifyMetadataResponseDto;
import org.dspace.app.rest.model.SubDepartmentRest;
import org.dspace.app.rest.model.UserMetadataDto;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.constants.enums.FolderPermission;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class SubDepartmentService {
	
	@Autowired
	private CommunityService communityService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private AuthorizeService authorizeService;
	
	@Autowired
	private EPersonService ePersonService;
	
	@Autowired 
	private AuditTrailService auditTrailService;

	@Autowired
	private UserMetadataFieldsService userMetadataFieldService;
   
	@Autowired
	private DynamicMetadataService dynamicMetadataService;
	
	@Autowired
	private UserMetadataConverter userMetadataConverter;
   
	private static final Logger log = LoggerFactory.getLogger(SubDepartmentService.class);
	private ObjectMapper mapper = new ObjectMapper();

	public SubDepartmentRest create(Context context, SubDepartmentRest rest, UserMetadataDto[] userMetadataList) throws SQLException, AuthorizeException, IOException {
		Community parent = (Community) communityService.find(context, UUID.fromString(rest.getParentId()));
		if(checkIfSectionNameExists(parent, rest.getName())) {
			throw new DuplicateEntityException(String.format("Section with this name %s already exists", rest.getName()));
		}
		String subDepartmentName = rest.getName();
		Community community = communityService.create(parent, context);
		community.setName(subDepartmentName);
		communityService.save(context, community);
		
		// create groups and add permissions
		authorizeService.addPolicies(context, parent.getResourcePolicies(), community);
		addResourcePolicies(context, community, subDepartmentName);		
		communityService.update(context, community);
		rest.setId(community.getID());
		auditTrailService.logAction(context, community.getID().toString(), AuditAction.SUB_DEPARTMENT_CREATED, new Object[]{subDepartmentName});
		if (userMetadataList != null && userMetadataList.length > 0) {
			dynamicMetadataService.addMetadata(context, parent, community, userMetadataList);
		}
		return rest;
	}
	
	private boolean checkIfSectionNameExists(Community parent, String name) {
	    if (parent == null) {
	        throw new IllegalArgumentException("Parent community cannot be null");
	    }

	    return parent.getSubcommunities()
	            .stream()
	            .anyMatch(e -> e.getName() != null && e.getName().equalsIgnoreCase(name));
	}


	private void addResourcePolicies(Context context, Community community, String subDepartmentName) throws SQLException, AuthorizeException {
		String groupNamePrefix = String.join("_", subDepartmentName, community.getID().toString());
		
		// add view group
		String viewGroupName = String.join("_", groupNamePrefix, FolderPermission.VIEW.name());
		Group viewGroup = groupService.createGroupByName(context, viewGroupName);
		authorizeService.createResourcePolicy(context, community, viewGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, viewGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, viewGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);
		
		// add download group
		String downloadGroupName = String.join("_", groupNamePrefix, FolderPermission.DOWNLOAD.name());
		Group downloadGroup = groupService.createGroupByName(context, downloadGroupName);
		authorizeService.createResourcePolicy(context, community, downloadGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, downloadGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, downloadGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);
		
		// add remove group
		String removeGroupName = String.join("_", groupNamePrefix, FolderPermission.REMOVE.name());
		Group removeGroup = groupService.createGroupByName(context, removeGroupName);
		authorizeService.createResourcePolicy(context, community, removeGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, removeGroup, null, Constants.REMOVE, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, removeGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, removeGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);				
		
		// add upload group
		String uploadGroupName = String.join("_", groupNamePrefix, FolderPermission.UPLOAD.name());
		Group uploadGroup = groupService.createGroupByName(context, uploadGroupName);
		authorizeService.createResourcePolicy(context, community, uploadGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);		
		authorizeService.createResourcePolicy(context, community, uploadGroup, null, Constants.WRITE, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, uploadGroup, null, Constants.ADD, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, uploadGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, uploadGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);

		// add edit group
		String editGroupName = String.join("_", groupNamePrefix, FolderPermission.EDIT.name());
		Group editGroup = groupService.createGroupByName(context, editGroupName);
		authorizeService.createResourcePolicy(context, community, editGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);		
		authorizeService.createResourcePolicy(context, community, editGroup, null, Constants.WRITE, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, editGroup, null, Constants.ADD, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, editGroup, null, Constants.REMOVE, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, editGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, community, editGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);
		
		// add admin group
		String adminGroupName = String.join("_", groupNamePrefix, FolderPermission.ADMIN.name());
		Group adminGroup = groupService.createGroupByName(context, adminGroupName);
		authorizeService.createResourcePolicy(context, community, adminGroup, null, Constants.ADMIN, ResourcePolicy.TYPE_CUSTOM);
		
		communityService.save(context, community);
	}

	public Community findById(Context context, UUID uuid) throws SQLException {
		return communityService.find(context, uuid);
	}

	public void delete(Context context, UUID uuid) throws SQLException, AuthorizeException, IOException {
		Community community = communityService.find(context, uuid);
		
		if (community.getSubcommunities().size() > 0 || community.getCollections().size() > 0) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete ! section is not empty.");
		}
		String name = community.getName();
		String handle = community.getID().toString();
		
		// delete groups related to it
		deleteRelatedGroup(context, community);
		
		communityService.delete(context, community);
		auditTrailService.logAction(context, handle, AuditAction.SUB_DEPARTMENT_DELETED, name);

	}

	private void deleteRelatedGroup(Context context, Community community) throws SQLException, AuthorizeException, IOException {
		String groupNamePrefix = String.join("_", community.getName(), community.getID().toString());
		
		// delete view group
		String viewGroupName = String.join("_", groupNamePrefix, FolderPermission.VIEW.name());
		Group viewGroup = groupService.findByName(context, viewGroupName);
		groupService.delete(context, viewGroup);
		
		// delete download group
		String downloadGroupName = String.join("_", groupNamePrefix, FolderPermission.DOWNLOAD.name());
		Group downloadGroup = groupService.findByName(context, downloadGroupName);
		groupService.delete(context, downloadGroup);
		
		// delete remove group
		String removeGroupName = String.join("_", groupNamePrefix, FolderPermission.REMOVE.name());
		Group removeGroup = groupService.findByName(context, removeGroupName);
		groupService.delete(context, removeGroup);		
		
		// delete upload group
		String uploadGroupName = String.join("_", groupNamePrefix, FolderPermission.UPLOAD.name());
		Group uploadGroup = groupService.findByName(context, uploadGroupName);
		groupService.delete(context, uploadGroup);
		
		// delete admin group
		String adminGroupName = String.join("_", groupNamePrefix, FolderPermission.ADMIN.name());
		Group adminGroup = groupService.findByName(context, adminGroupName);
		groupService.delete(context, adminGroup);
		
		// delete edit group
		String editGroupName = String.join("_", groupNamePrefix, FolderPermission.EDIT.name());
		Group editGroup = groupService.findByName(context, editGroupName);
		groupService.delete(context, editGroup);
		
		// remove all policies
		authorizeService.removeAllPolicies(context, community);
		
	}

	public void assignAdmin(Context context, UUID uuid, String userId) throws SQLException {
		Community community = communityService.find(context, uuid);

		// find admin group
		String adminGroupName = String.join("_", community.getName(), community.getID().toString(), FolderPermission.ADMIN.name());
		Group adminGroup = groupService.findByName(context, adminGroupName);
		EPerson ePerson = ePersonService.find(context, UUID.fromString(userId));
		groupService.addMember(context, adminGroup, ePersonService.find(context, UUID.fromString(userId)));
		auditTrailService.logAction(context,community.getID().toString(), AuditAction.ADMIN_ASSIGN_TO_SUB_DEPARTMENT, ePerson.getEmail(), community.getName());

	}

	public void removeAdmin(Context context, UUID uuid, List<UUID> userIds) throws SQLException {
		Community community = communityService.find(context, uuid);
		
		// find admin group
		String adminGroupName = String.join("_", community.getName(), community.getID().toString(), FolderPermission.ADMIN.name());
		Group adminGroup = groupService.findByName(context, adminGroupName);
		for(UUID epersonId : userIds) {			
			EPerson ePerson = ePersonService.find(context, epersonId);
			groupService.removeMember(context, adminGroup, ePerson);
			auditTrailService.logAction(context,community.getID().toString(), AuditAction.ADMIN_REMOVED_FROM_SUB_DEPARTMENT, ePerson.getEmail(), community.getName());
		}
		
	}
	
	public ArrayNode getEpersonsArrayNode(Context context, String subDepartmentUUID) {
		ArrayNode nodes = mapper.createArrayNode();
		try {
			Community community = communityService.find(context, UUID.fromString(subDepartmentUUID));
			// find admin group
			String adminGroupName = String.join("_", community.getName(), community.getID().toString(), FolderPermission.ADMIN.name());
			Group adminGroup = groupService.findByName(context, adminGroupName);
			if(adminGroup == null) {				
				return nodes;
			}
			List<EPerson> allEperson = groupService.allMembers(context, adminGroup);
			for(EPerson eperson : allEperson) {
				ObjectNode node = mapper.createObjectNode();
        		node.put("id", eperson.getID().toString());
        		node.put("name", eperson.getFullName());
        		nodes.add(node);
			}
		} catch (Exception e) {
			// TODO: handle exception
			throw new RuntimeException("Error getting sub-department admin list",e);
		}
		
		return nodes;
	}
	

	public ModifyMetadataResponseDto getSubDepartmentMetadata(Context context, UUID uuid) throws SQLException {
		Community subDepartment = (Community)communityService.find(context, uuid);
		
		if (subDepartment == null) {
	        return new ModifyMetadataResponseDto(Collections.emptyList(), false);
	    }
		
		List<UserMetadataFields> metadataFields = userMetadataFieldService.getMetadataFieldsBySubDept(context, subDepartment.getHandle());
		
		List<UserMetadataDto> userMetadataList = userMetadataConverter.convert(metadataFields, null);
		
		boolean duplicacyCheckFlag = Boolean.TRUE.equals(subDepartment.getIsDuplicateCheckEnabled());
		
		return new ModifyMetadataResponseDto(userMetadataList, duplicacyCheckFlag);
	}
}
