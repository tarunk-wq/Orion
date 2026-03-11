package org.dspace.app.rest.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.constants.enums.FolderPermission;
import org.dspace.constants.enums.NodeType;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Department;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DepartmentService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class UserPermissionService {
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private ItemService itemService;
	
	@Autowired
	private AuthorizeService authorizeService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private CollectionService collectionService;
	
	@Autowired
	private CommunityService communityService;
	
	@Autowired
	private DepartmentService departmentService;

	@Autowired
	private FolderService folderService;

	public ObjectNode getEpersonPermissions(Context context, EPerson user, String type, UUID uuid) throws Exception {
		
		Community subDepartment = null;
		
		if (type.equalsIgnoreCase(NodeType.SUB_DEPARTMENT.name())) {
			subDepartment = communityService.find(context, uuid);
		
		} else if (type.equalsIgnoreCase(NodeType.LEAF.name())) {
			Collection collection = collectionService.find(context, uuid);
			subDepartment = Utils.extractDepAndSub(collection.getCommunities().get(0))[1];		
		
		} else if (type.equalsIgnoreCase("ITEM")) {
			Item item = itemService.find(context, uuid);			
			if(item == null) {
				throw new IllegalArgumentException("ItemId is invalid");
			}
			if (item.getOwningCollection() != null) {				
				subDepartment = Utils.extractDepAndSub(item.getOwningCollection().getCommunities().get(0))[1];
				
			} else  if (item.getOwningCommunity() != null) {
				subDepartment = Utils.extractDepAndSub(item.getOwningCommunity())[1];	
			
			} else {
				throw new Exception("Item should have parent");
			}
		
		} else {
			throw new Exception("Invalid node type");
		}		
		
		return fetchCommunityPermissions(context, user, subDepartment);
			
	}

	private ObjectNode fetchCommunityPermissions(Context context, EPerson user, Community community) throws SQLException {
		
		// Admin override
		boolean isAdmin = authorizeService.isAdmin(context);
		
		boolean hasDownloadPermission = isAdmin ? true : hasCommunityPermission(context, community, user, FolderPermission.DOWNLOAD.name());
		boolean hasUploadPermission = isAdmin ? true : hasCommunityPermission(context, community, user, FolderPermission.UPLOAD.name());
		boolean hasEditPermission = isAdmin ? true : hasCommunityPermission(context, community, user, FolderPermission.EDIT.name());
		boolean isDepartmentAdmin = isAdmin ? true : hasCommunityPermission(context, community.getParentCommunities().get(0), user, FolderPermission.ADMIN.name());
		boolean isSectionAdmin = isAdmin ? true : hasCommunityPermission(context, community, user, FolderPermission.ADMIN.name());
		
		ObjectNode node = mapper.createObjectNode();
		node.put("downloadPermission", hasDownloadPermission);
		node.put("uploadPermission", hasUploadPermission);
		node.put("isDepartmentAdmin", isDepartmentAdmin);
		node.put("isSectionAdmin", isSectionAdmin);
		node.put("editPermission", hasEditPermission);
		
		return node;
	}

	public boolean checkUserPermissionInFolderHierarchy(Context context, EPerson user, Collection collection, String permission) throws SQLException {
	    
	    // Admin override
	    if (authorizeService.isAdmin(context)) {
	        return true;
	    }

	    // Collection-level groups
	    String collectionPermissionGroupName = String.join("_", collection.getName(), collection.getID().toString(), permission);
	    String collectionAdminGroupName = String.join("_", collection.getName(), collection.getID().toString(), FolderPermission.ADMIN.name());

	    Group collectionPermissionGroup = groupService.findByName(context, collectionPermissionGroupName);
	    Group collectionAdminGroup = groupService.findByName(context, collectionAdminGroupName);

	    boolean hasPermission =
	        (collectionPermissionGroup != null && groupService.isMember(context, user, collectionPermissionGroup)) ||
	        (collectionAdminGroup != null && groupService.isMember(context, user, collectionAdminGroup));

	    if (hasPermission) {
	        return true;
	    }

	    // Check communities (parent-level)
	    for (Community comm : collection.getCommunities()) {
	        String commPermissionGroupName = String.join("_", comm.getName(), comm.getID().toString(), permission);
	        String commAdminGroupName = String.join("_", comm.getName(), comm.getID().toString(), FolderPermission.ADMIN.name());

	        Group commPermissionGroup = groupService.findByName(context, commPermissionGroupName);
	        Group commAdminGroup = groupService.findByName(context, commAdminGroupName);

	        boolean hasCommPermission =
	            (commPermissionGroup != null && groupService.isMember(context, user, commPermissionGroup)) ||
	            (commAdminGroup != null && groupService.isMember(context, user, commAdminGroup));

	        if (hasCommPermission) {
	            return true;
	        }
	    }

	    return false;
	}

	public ObjectNode getPermissions(Context context, EPerson currentUser) throws SQLException {		
		
		boolean isDepartmentAdmin = false;
		boolean isSectionAdmin = false;
		boolean isUploadUser = false;
		
		// Retrieve all departments in the system
		List<Department> departments = departmentService.findAllDepartment(context);
		for (Department department : departments) {
			// Check if the user is an admin of the department's community
			if (hasCommunityPermission(context, department.getCommunity(), currentUser, FolderPermission.ADMIN.name())) {
				isDepartmentAdmin = true;
				isUploadUser = true;
				break;
			}
			
			// Check user's permissions within each subdepartment of the department
			for (Community community : department.getCommunity().getSubcommunities()) {
				if (hasCommunityPermission(context, community, currentUser, FolderPermission.ADMIN.name())) {
					isSectionAdmin = true;
					isUploadUser = true;
					break;
				}
				if (hasCommunityPermission(context, community, currentUser, FolderPermission.UPLOAD.name())) {
					isUploadUser = true;
				}
			}
		}
		
		ObjectNode node = mapper.createObjectNode();
		node.put("isDepartmentAdmin", isDepartmentAdmin);
		node.put("isSectionAdmin", isSectionAdmin);
		node.put("isUploadUser", isUploadUser);
		return node;
	}

	private boolean hasCommunityPermission(Context context, Community community, EPerson user, String permission) throws SQLException {
		// find group which corresponding to given permision
		String groupName = String.join("_", community.getName(), community.getID().toString(), permission);
		Group group = groupService.findByName(context, groupName);
		return groupService.isMember(context, user, group);
	}
	
	/* Check if user is either Admin, DepartmentAdmin, SubDepartmentAdmin) */
    public boolean hasAdminPermission(Context context, DSpaceObject dso) throws Exception {
        if (authorizeService.isAdmin(context)) return true;
        Community topLevel = null;
        Community subLevel = null;
        Community[] departmentAndSubDep = null;
        Community parentFolder = null;
        if (dso instanceof Item) {
			Item dsoItem = (Item) dso;
			if(dsoItem.getOwningCommunity() != null) {
				parentFolder = dsoItem.getOwningCommunity();
			}else if(dsoItem.getOwningCollection() != null) {
				parentFolder = dsoItem.getOwningCollection().getCommunities().get(0);
			}
		}else if(dso instanceof Collection) {
			Collection dsoCollection = (Collection) dso;
			parentFolder = dsoCollection.getCommunities().get(0);
		}else if(dso instanceof Community) {
			parentFolder = (Community) dso;
		}
        
        departmentAndSubDep = folderService.extractDepAndSub(parentFolder);			
        if(departmentAndSubDep != null) {        	
        	if(departmentAndSubDep.length > 1) {        	
        		topLevel = departmentAndSubDep[0];			
        		subLevel = departmentAndSubDep[1];
        	}else if(departmentAndSubDep.length > 0) {
        		topLevel = departmentAndSubDep[0];			        	
        	}
        	if (topLevel != null && isDepartmentAdmin(context, topLevel)) return true;
        	if (subLevel != null && isSubDepartmentAdmin(context, subLevel)) return true;				
        }
        
        return false;
    }
    
    public boolean isDepartmentAdmin(Context context, Community community) throws Exception {
        if (community == null) return false;
        String adminGroupName = String.join("_", community.getName(), community.getID().toString(), FolderPermission.ADMIN.name());
        Group adminGroup = groupService.findByName(context, adminGroupName);
        return adminGroup != null && groupService.isMember(context, context.getCurrentUser(), adminGroup);
    }

    public boolean isSubDepartmentAdmin(Context context, Community community) throws Exception {
        if (community == null) return false;
        String adminGroupName = String.join("_", community.getName(), community.getID().toString(), FolderPermission.ADMIN.name());
        Group adminGroup = groupService.findByName(context, adminGroupName);
        return adminGroup != null && groupService.isMember(context, context.getCurrentUser(), adminGroup);
    }
    
    public DSpaceObject getTopLevelCommunity(Context context, DSpaceObject dso) throws Exception {
        if (dso == null) return null;
        
        if (dso instanceof Item) {
        	Item item = (Item) dso;
        	if (item.getOwningCollection() != null) return getTopLevelCommunity(context, item.getOwningCollection());
        	else if (item.getOwningCommunity() != null) return getTopLevelCommunity(context, item.getOwningCommunity());
        	return null;
        } else if (dso instanceof Collection) {
            return getTopLevelCommunity(context, collectionService.getParentObject(context, (Collection) dso));
        } else if (dso instanceof Community) {
        	Community currentCommunity = (Community) dso;
            DSpaceObject parent = communityService.getParentObject(context, currentCommunity);
            return (parent == null) ? currentCommunity : getTopLevelCommunity(context, parent);
        }
        return null;
    }
    
    public DSpaceObject getSecondTopLevelCommunity(Context context, DSpaceObject dso) throws Exception {
        if (dso == null) return null;
        
        if (dso instanceof Item) {
        	Item item = (Item) dso;
        	if (item.getOwningCollection() != null) return getSecondTopLevelCommunity(context, item.getOwningCollection());
        	else if (item.getOwningCommunity() != null) return getSecondTopLevelCommunity(context, item.getOwningCommunity());
        	return null;
        } else if (dso instanceof Collection) {
            return getSecondTopLevelCommunity(context, collectionService.getParentObject(context, (Collection) dso));
        } else if (dso instanceof Community) {
        	Community currentCommunity = (Community) dso;
            DSpaceObject parentDso = communityService.getParentObject(context, currentCommunity);
            if (!(parentDso instanceof Community)) return null;
            Community parentComm = (Community) parentDso;
            DSpaceObject grandParentDso = communityService.getParentObject(context, parentComm);
            return (grandParentDso == null) ? currentCommunity : getSecondTopLevelCommunity(context, parentComm);
        }
        return null;
    }
    
    public boolean isDepOrSectionAdmin(Context context, Community department) throws SQLException {
		// Check if the user is an admin of the department's community
		if (hasCommunityPermission(context, department, context.getCurrentUser(), FolderPermission.ADMIN.name())) {
			return true;
		}
		
		// Check user's permissions within each subdepartment of the department
		for (Community community : department.getSubcommunities()) {
			if (hasCommunityPermission(context, community, context.getCurrentUser(), FolderPermission.ADMIN.name())) {
				return true;
			}
		}
		return false;
	}

	public boolean hasPermission(Context context, DSpaceObject folder, String permission) throws SQLException {
		// check admin
		if (authorizeService.isAdmin(context)) {
			return true;
		}
		
		Community community = null;
		if (folder instanceof Collection) {
			community = ((Collection) folder).getCommunities().get(0);
		} else {
			community = (Community) folder;
		}
		
		Community depAndSub[] = Utils.extractDepAndSub(community);
		EPerson ePerson = context.getCurrentUser();
		
		// check dep admin, sub dep admin or has upload rights
		if (hasCommunityPermission(context, depAndSub[0], ePerson, FolderPermission.ADMIN.name()) || 
			hasCommunityPermission(context, depAndSub[1], ePerson, FolderPermission.ADMIN.name()) || 
			hasCommunityPermission(context, depAndSub[1], ePerson, permission)) {
			return true;
		}
		return false;
	}
	
	public boolean hasUploadPermission(Context context, DSpaceObject folder) throws SQLException {
		return hasPermission(context, folder, FolderPermission.UPLOAD.name());
	}

	public boolean hasPermission(Context context, UUID folderId, String type, String permission) throws SQLException {
		if (type.equals(NodeType.LEAF.name())) {
			return hasPermission(context, collectionService.find(context, folderId), permission);
		
		} else if (type.equals(NodeType.BRANCH.name())) {
			return hasPermission(context, communityService.find(context, folderId), permission);

		} else {
			throw new RuntimeException("Invalid node type: " + type);
		}
	}
}
