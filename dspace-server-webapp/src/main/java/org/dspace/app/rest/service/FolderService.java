package org.dspace.app.rest.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.dspace.app.rest.model.FolderRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.constants.enums.FolderPermission;
import org.dspace.constants.enums.NodeType;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FolderService {
	
	@Autowired
	private CommunityService communityService;
	
	@Autowired
	private CollectionService collectionService;
	
	@Autowired
	private AuthorizeService authorizeService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private EPersonService ePersonService;
	
	@Autowired
	private AuditTrailService auditTrailService;
	
	@Autowired
	private ItemService itemService;

	public DSpaceObject findById(Context context, UUID uuid) throws SQLException {
		DSpaceObject dspaceObj = communityService.find(context, uuid);
		
		if (dspaceObj == null) {
			dspaceObj = collectionService.find(context, uuid);;
		}
		
		return dspaceObj;
	}

	public FolderRest create(Context context, FolderRest rest) throws SQLException, AuthorizeException {
		Community parent = communityService.find(context, UUID.fromString(rest.getParentId()));
		String folderName = rest.getName();
		
		Community subDepartment = extractSubDepartment(parent);
		
		if (rest.getNodeType().equalsIgnoreCase(NodeType.BRANCH.name())) {
			if (checkDuplicateBranch(parent, folderName)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch with the same name already exists.");
			}
			
			Community community = communityService.create(parent, context);
			community.setName(folderName);
			
			// create groups and copy permissions from subdepartment
			addResourcePolicies(context, community, subDepartment);
			
			communityService.save(context, community);
			communityService.update(context, community);
			rest.setId(community.getID());
			auditTrailService.logAction(context, community.getID().toString(), AuditAction.FOLDER_CREATED, folderName);
		
		} else if (rest.getNodeType().equalsIgnoreCase(NodeType.LEAF.name())) {		
			if (checkDuplicateLeaf(parent, folderName)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Leaf with the same name already exists.");
			}
			
			Collection collection = collectionService.create(context, parent);
			collection.setName(folderName);
			
			// create groups and copy permissions from subdepartment
			addResourcePolicies(context, collection, subDepartment);
			
			collectionService.save(context, collection);
			collectionService.update(context, collection);
			rest.setId(collection.getID());
			auditTrailService.logAction(context, collection.getID().toString(), AuditAction.FOLDER_CREATED, folderName);
		}
		
		return rest;
	}
	
	private Community extractSubDepartment(Community parent) throws SQLException {
		Community current = parent;
		Community subDepartment = current;
		
		while (current != null) {
			subDepartment = current;
			current = current.getParentCommunities().get(0);
			if (current.getParentCommunities().size() == 0) {
				return subDepartment;
			}
		}
		
		throw new SQLException("Cannot find subdepartment for folder: " + parent.getID().toString());
	}

	private void addResourcePolicies(Context context, DSpaceObject dspaceObject, Community subDepartment) throws SQLException, AuthorizeException {
		
		String groupNamePrefix = String.join("_", subDepartment.getName(), subDepartment.getID().toString());
		
		// add view group
		String viewGroupName = String.join("_", groupNamePrefix, FolderPermission.VIEW.name());
		Group viewGroup = groupService.findByName(context, viewGroupName);
		authorizeService.createResourcePolicy(context, dspaceObject, viewGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, viewGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, viewGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);
		
		// add download group
		String downloadGroupName = String.join("_", groupNamePrefix, FolderPermission.DOWNLOAD.name());
		Group downloadGroup = groupService.findByName(context, downloadGroupName);
		authorizeService.createResourcePolicy(context, dspaceObject, downloadGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, downloadGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, downloadGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);
		
		// add remove group
		String removeGroupName = String.join("_", groupNamePrefix, FolderPermission.REMOVE.name());
		Group removeGroup = groupService.findByName(context, removeGroupName);
		authorizeService.createResourcePolicy(context, dspaceObject, removeGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, removeGroup, null, Constants.REMOVE, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, removeGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, removeGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);							
		
		// add upload group
		String uploadGroupName = String.join("_", groupNamePrefix, FolderPermission.UPLOAD.name());
		Group uploadGroup = groupService.findByName(context, uploadGroupName);
		authorizeService.createResourcePolicy(context, dspaceObject, uploadGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);		
		authorizeService.createResourcePolicy(context, dspaceObject, uploadGroup, null, Constants.WRITE, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, uploadGroup, null, Constants.ADD, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, uploadGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
		authorizeService.createResourcePolicy(context, dspaceObject, uploadGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);

		// add edit group
		String editGroupName = String.join("_", groupNamePrefix, FolderPermission.EDIT.name());
		Group editGroup = groupService.findByName(context, editGroupName);
		if(editGroup != null) {			
			authorizeService.createResourcePolicy(context, dspaceObject, editGroup, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);		
			authorizeService.createResourcePolicy(context, dspaceObject, editGroup, null, Constants.WRITE, ResourcePolicy.TYPE_CUSTOM);
			authorizeService.createResourcePolicy(context, dspaceObject, editGroup, null, Constants.ADD, ResourcePolicy.TYPE_CUSTOM);
			authorizeService.createResourcePolicy(context, dspaceObject, editGroup, null, Constants.REMOVE, ResourcePolicy.TYPE_CUSTOM);
			authorizeService.createResourcePolicy(context, dspaceObject, editGroup, null, Constants.DEFAULT_ITEM_READ, ResourcePolicy.TYPE_CUSTOM);
			authorizeService.createResourcePolicy(context, dspaceObject, editGroup, null, Constants.DEFAULT_BITSTREAM_READ, ResourcePolicy.TYPE_CUSTOM);
		}
		
		// add admin group
		String adminGroupName = String.join("_", groupNamePrefix, FolderPermission.ADMIN.name());
		Group adminGroup = groupService.findByName(context, adminGroupName);
		authorizeService.createResourcePolicy(context, dspaceObject, adminGroup, null, Constants.ADMIN, ResourcePolicy.TYPE_CUSTOM);

	}

	private boolean checkDuplicateLeaf(Community parent, String folderName) {
		for (Collection collection : parent.getCollections()) {
			if (collection.getName().equalsIgnoreCase(folderName)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkDuplicateBranch(Community parent, String folderName) {
		for (Community community : parent.getSubcommunities()) {
			if (community.getName().equalsIgnoreCase(folderName)) {
				return true;
			}
		}
		return false;
	}

	public void rename(Context context, UUID uuid, String name, String type) throws Exception {
		if (type.equalsIgnoreCase(NodeType.BRANCH.name())) {       	
        	Community community = communityService.find(context, uuid);
        	String oldName = community.getName();       	
        	community.setName(name);  
        	communityService.save(context, community);
        	auditTrailService.logAction(context, community.getID().toString(), AuditAction.FOLDER_RENAMED, oldName, name);			
        	
        } else if (type.equalsIgnoreCase(NodeType.LEAF.name())) {       	
        	Collection collection = collectionService.find(context, uuid);
        	String oldName = collection.getName();
        	collection.setName(name);
        	collectionService.save(context, collection);
        	auditTrailService.logAction(context, collection.getID().toString(), AuditAction.FOLDER_RENAMED, oldName, name);
        	
        } else {
        	throw new Exception("Invalid node type");
        }
	}

	public void delete(Context context, UUID uuid, String type) throws Exception {
		if (type.equalsIgnoreCase(NodeType.BRANCH.name())) {       	
        	Community community = communityService.find(context, uuid);
        	String name = community.getName();
        	String handle = community.getID().toString();
        	if (community.getSubcommunities().size() > 0 || itemService.findAllByCommunity(context, community).hasNext()) {
        		throw new DataIntegrityViolationException("CANNOT-DELETE");
        	}
        	
        	community = context.reloadEntity(community);
        	communityService.delete(context, community);
        	auditTrailService.logAction(context, handle, AuditAction.FOLDER_DELETED, name);
        	
        } else if (type.equalsIgnoreCase(NodeType.LEAF.name())) {       	
        	Collection collection = collectionService.find(context, uuid);
        	String name = collection.getName();
        	String handle = collection.getID().toString();
        	if (itemService.findAllByCollection(context, collection).hasNext()) {
        		throw new DataIntegrityViolationException("CANNOT-DELETE");
        	}
        	
        	collection = context.reloadEntity(collection);
        	collectionService.delete(context, collection);
        	auditTrailService.logAction(context, handle, AuditAction.FOLDER_DELETED, name);
        	
        } else {
        	throw new Exception("Invalid node type");
        }
	}

	public ArrayNode findByName(Context context, UUID parentId, String name) throws SQLException {
		Community community = communityService.find(context, parentId);
		
		String folderPath = Utils.extractPath(community);
		folderPath += "/";
		name = name.toLowerCase();
		ArrayNode nodes = mapper.createArrayNode();
		
		// add branch
		for (Community subCommunity : community.getSubcommunities()) {
			String folderName = subCommunity.getName();
			if (folderName.toLowerCase().contains(name)) {
				ObjectNode node = mapper.createObjectNode();
				
				node.put("id", subCommunity.getID().toString());
				node.put("name", folderName);
				node.put("path", folderPath + folderName);
				node.put("nodeType", NodeType.BRANCH.name());
				
				nodes.add(node);
			}
		}
		
		// add leafs
		for (Collection collection : community.getCollections()) {
			String folderName = collection.getName();
			if (folderName.toLowerCase().contains(name)) {
				ObjectNode node = mapper.createObjectNode();
				
				node.put("id", collection.getID().toString());
				node.put("name", folderName);
				node.put("path", folderPath + folderName);
				node.put("nodeType", NodeType.LEAF.name());
				
				nodes.add(node);
			}
		}
		
		return nodes;		
	}

	public void givePermission(Context context, ObjectNode body) throws Exception {
		
		String folderId = body.get("folderId").asText();
		String type = body.get("type").asText();
		ArrayNode users = (ArrayNode) body.get("users");
		ArrayNode groupIdList = (ArrayNode) body.get("groups");
		ArrayNode permissions = (ArrayNode) body.get("permissions");
		Community community = communityService.find(context, UUID.fromString(folderId));
		if(users != null && users.size() > 0) {			
			Collection collection = community == null ? collectionService.find(context, UUID.fromString(folderId)) : null;
			String handle = community == null ? collection.getHandle() : community.getHandle();
			String folderName = community == null ? collection.getName() : community.getName();
		
			String subDepartmentId = null;
			
			if (type.equalsIgnoreCase(NodeType.SUB_DEPARTMENT.name())) {       	     	
	        	subDepartmentId = folderId;
	        	
	        } else if (type.equalsIgnoreCase(NodeType.BRANCH.name())) {       	     	
	        	subDepartmentId = extractDepAndSub(community)[1].getID().toString();
	        	
	        } else if (type.equalsIgnoreCase(NodeType.LEAF.name())) {       	
	        	subDepartmentId = extractDepAndSub(collection.getCommunities().get(0))[1].getID().toString();
	        	
	        } else {
	        	throw new Exception("Invalid node type");
	        }
			List<Group> groups = groupService.findByFolder(context, subDepartmentId);
			
			for (Group group : groups) {
				String groupName = group.getName();
				for (JsonNode permissionNode : permissions) {
					String permission = permissionNode.asText();
					if (groupName.contains(permission)) {
						addUsers(context, group, users,folderName, permission,handle);
					}
				}
			}
		}
		
		if(groupIdList != null && groupIdList.size() > 0) {			
			List<Group> allGroups = new ArrayList<Group>();
			for (JsonNode groupId : groupIdList) {
				Group singleGroup = groupService.find(context, UUID.fromString(groupId.asText()));
				allGroups.add(singleGroup);
			}
			for (JsonNode permis : permissions) {
				String parentGroupName = String.join("_", community.getName(),community.getID().toString(),permis.asText());
				Group parentGroup = groupService.findByName(context, parentGroupName);
				for(Group childGroup : allGroups) {
					if(childGroup == null) {
						continue;
					}
					groupService.addMember(context, parentGroup,childGroup);
				}
			}
		}
	}

	private void addUsers(Context context, Group group, ArrayNode users, String folderName, String permission, String handle) throws SQLException {
		for (JsonNode userNode : users) {
			UUID userId = UUID.fromString(userNode.asText());
			EPerson ePerson = ePersonService.find(context, userId);
			groupService.addMember(context, group, ePerson);
			auditTrailService.logAction(context,handle, AuditAction.GRANT_FOLDER_PERMISSION, permission, ePerson.getEmail(), folderName);
		}
	}

	public ArrayNode fetchPermissions(Context context, UUID folderId, String type) throws SQLException {
		// Extract sub-department from the folder (assumed to be size 2 in the result)    
		UUID subDepartmentId = null;
		
		if (type.equalsIgnoreCase(NodeType.SUB_DEPARTMENT.name())) {      	
        	subDepartmentId = folderId;
        	
        } else if (type.equalsIgnoreCase(NodeType.BRANCH.name())) {       	
        	Community community = communityService.find(context, folderId);      	
        	subDepartmentId = extractDepAndSub(community)[1].getID();
        	
        } else if (type.equalsIgnoreCase(NodeType.LEAF.name())) {       	
        	Collection collection = collectionService.find(context, folderId);
        	subDepartmentId = extractDepAndSub(collection.getCommunities().get(0))[1].getID();
        	
        } else {
        	throw new IllegalArgumentException("Invalid node type");
        }
		
		// Get all groups assigned to the sub-department
		List<Group> groups = groupService.findGroupsAssignedToFolder(context, subDepartmentId);		
		
		// Map to collect permissions assigned to each user
		Map<UUID, Set<String>> userPermissionMap = new HashMap<UUID, Set<String>>();
		Map<UUID, EPerson> userMap = new HashMap<UUID, EPerson>();
		
		// Iterate over each group to extract user and permission info
		for (Group group : groups) {
			// Extract permission name from group name (e.g., "COMMUNITY_ID_READ" -> "READ")		       
			String groupName = group.getName();		
			
			String permission = groupName.substring(groupName.lastIndexOf("_") + 1);
			// skip department and sub-department users
			if (permission.equals(FolderPermission.ADMIN.name())) {
				continue;
			}
			
			for (EPerson ePerson : group.getMembers()) {
				UUID ePersonId = ePerson.getID();
				if (!userPermissionMap.containsKey(ePersonId)) {
					userPermissionMap.put(ePersonId, new HashSet<String>());
					userMap.put(ePersonId, ePerson);
				}
				userPermissionMap.get(ePersonId).add(permission);				
			}
		}
		
		ArrayNode userGroupPermissions = mapper.createArrayNode();
		
		// Loop through the user map to build JSON entries
		for (Map.Entry<UUID, EPerson> entry : userMap.entrySet()) {
		    UUID userId = entry.getKey();
		    EPerson user = entry.getValue();

		    ObjectNode node = mapper.createObjectNode();
			
			node.put("id", userId.toString());
			node.put("email", user.getEmail());
			node.set("permissions", mapper.valueToTree(userPermissionMap.get(userId)));
			
			userGroupPermissions.add(node);
		}

		return userGroupPermissions;
	}

	public ArrayNode fetchGroupPermissions(Context context, UUID folderId) throws SQLException {
				
		Group group = groupService.find(context, folderId);
		ArrayNode userGroupPermissions = mapper.createArrayNode();
		if(group == null) {
			return userGroupPermissions;
		}
		List<Group> parentGroups = group.getParentGroups();
		// Iterate over each group to extract user and permission info
		for (Group parentGroup : parentGroups) {
			// Extract permission name from group name (e.g., "COMMUNITY_ID_READ" -> "READ")		       
			String groupName = parentGroup.getName();		
			
			String permission = groupName.substring(groupName.lastIndexOf("_") + 1);
			// skip department and sub-department users
			if (permission.equals(FolderPermission.ADMIN.name())) {
				continue;
			}
			userGroupPermissions.add(permission);
		}
		
		return userGroupPermissions;
	}

	public Community[] extractDepAndSub(Community folder) throws SQLException {
		Community current  = folder;
		Community subDepartment = folder;
		
		while (current != null) {
			subDepartment = current;
			current = current.getParentCommunities().get(0);
			if (current.getParentCommunities().size() == 0) {
				return new Community[] {current, subDepartment};
			}
		}
		throw new SQLException("Cannot find department and subdepartment for folder: " + folder.getID().toString());
	}

	public void revokePermission(Context context, ObjectNode body) throws SQLException {
		String folderId = body.get("folderId").asText();
		String userId = body.get("userId").asText();
		String type = body.get("type").asText();
		EPerson user = ePersonService.find(context, UUID.fromString(userId));

		ArrayNode permissions = (ArrayNode) body.get("permissions");
		Community community = communityService.find(context, UUID.fromString(folderId));
		Collection collection = community == null ? collectionService.find(context, UUID.fromString(folderId)) : null;
		String handle = community == null ? collection.getID().toString() : community.getID().toString();
		
		String subDepartmentId = null;
		
		if (type.equalsIgnoreCase(NodeType.SUB_DEPARTMENT.name())) {       	
        	subDepartmentId = extractDepAndSub(community)[1].getID().toString();
        	
        } else if (type.equalsIgnoreCase(NodeType.BRANCH.name())) {       	
        	subDepartmentId = extractDepAndSub(community)[1].getID().toString();
        	
        } else if (type.equalsIgnoreCase(NodeType.LEAF.name())) {       	
        	subDepartmentId = extractDepAndSub(collection.getCommunities().get(0))[1].getID().toString();
        	
        } else {
        	throw new IllegalArgumentException("Invalid node type");
        }
		
		List<Group> groups = groupService.findByFolder(context, subDepartmentId);
		
		for (Group group : groups) {
			if (groupService.isMember(context, user, group)) {
				groupService.removeMember(context, group, user);
				String[] groupName = group.getName().split("_");
				if(groupName.length > 2) {
					auditTrailService.logAction(context,handle,AuditAction.REVOKED_FOLDER_PERMISSION, groupName[2], user.getEmail(), groupName[0]);					
				}
			}
		}
		
		JsonNode groupIdNode = body.get("group");
		
		if(groupIdNode != null && !groupIdNode.isNull()) {
			String groupId = groupIdNode.asText();
			
			Group singleGroup = groupService.find(context, UUID.fromString(groupId));
			if(singleGroup == null) {					
				System.out.println("single group NULL");
				return;
			}
			for (JsonNode permis : permissions) {
				String parentGroupName = String.join("_", community.getName(),community.getID().toString(),permis.asText());
				System.out.println("Parent Group Name: "+parentGroupName);
				Group parentGroup = groupService.findByName(context, parentGroupName);
				if(parentGroup == null) {
					System.out.println("parent group NULL");
					return;
				}
				groupService.removeMember(context, parentGroup, singleGroup);
			}
		}
	}

	public ArrayNode searchFolder(Context context, UUID folderId, String name) throws SQLException {
		Community community = communityService.find(context, folderId);
		String path = Utils.extractPath(community);
		ArrayNode nodes = mapper.createArrayNode();
		
		traverseFolder(community, nodes, path, name.toLowerCase());		
		return nodes;
	}

	private void traverseFolder(Community community, ArrayNode nodes, String path, String name) {
		// traverse branches
		for (Community child : community.getSubcommunities()) {
			String childPath = path + "/" + child.getName();
			
			// search with name
			if (child.getName().toLowerCase().contains(name)) {				
				ObjectNode node = mapper.createObjectNode();
				
				node.put("id", child.getID().toString());
				node.put("name", child.getName());
				node.put("path", childPath);
				
				nodes.add(node);
			}
			
			// call its child recursively
			traverseFolder(child, nodes, childPath, name);
		}
		
		// traverse leaves
		for (Collection child : community.getCollections()) {
			String childPath = path + "/" + child.getName();
			
			// search with name
			if (child.getName().toLowerCase().contains(name)) {				
				ObjectNode node = mapper.createObjectNode();
				
				node.put("id", child.getID().toString());
				node.put("name", child.getName());
				node.put("path", childPath);
				
				nodes.add(node);
			}
		}
	}

	public String getFolderHierarchy(Context context, UUID folderId) throws SQLException {
		Collection collection = collectionService.find(context, folderId);
		
		if (collection != null) {
			String path = Utils.extractPath(collection.getCommunities().get(0));
			path += "/" + collection.getName();
			return path;
		} else {
			Community community = communityService.find(context, folderId);
			return Utils.extractPath(community);
		}
	}
	
	public ArrayNode fetchPermissions(Context context, UUID folderId) throws SQLException {
		List<Group> groups = groupService.findGroupsAssignedToFolder(context, folderId);		
		
		Map<UUID, Set<String>> userPermissionMap = new HashMap<UUID, Set<String>>();
		Map<UUID, EPerson> userMap = new HashMap<UUID, EPerson>();
		
		for (Group group : groups) {
			String groupName = group.getName();
			String permission = groupName.substring(groupName.lastIndexOf("_") + 1);
			
			for (EPerson ePerson : group.getMembers()) {
				UUID ePersonId = ePerson.getID();
				if (!userPermissionMap.containsKey(ePersonId)) {
					userPermissionMap.put(ePersonId, new HashSet<String>());
					userMap.put(ePersonId, ePerson);
				}
				userPermissionMap.get(ePersonId).add(permission);				
			}
		}
		
		ArrayNode userGroupPermissions = mapper.createArrayNode();
		
		for (Map.Entry<UUID, EPerson> entry : userMap.entrySet()) {
		    UUID userId = entry.getKey();
		    EPerson user = entry.getValue();

		    ObjectNode node = mapper.createObjectNode();
			
			node.put("id", userId.toString());
			node.put("email", user.getEmail());
			node.set("permissions", mapper.valueToTree(userPermissionMap.get(userId)));
			
			userGroupPermissions.add(node);
		}

		return userGroupPermissions;
	}

}
