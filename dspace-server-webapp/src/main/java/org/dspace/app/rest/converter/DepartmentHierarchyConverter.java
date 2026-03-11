package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.dspace.app.rest.model.DepartmentHierarchyDTO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.constants.enums.FolderPermission;
import org.dspace.constants.enums.NodeType;
import org.dspace.content.Community;
import org.dspace.content.Department;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DepartmentHierarchyConverter {
	
	@Autowired
	private AuthorizeService authorizeService;
	
	@Autowired
	private GroupService groupService;

    public DepartmentHierarchyDTO convert(Context context, Department department, boolean isAdmin) throws SQLException {
        Community rootCommunity = department.getCommunity();
        
        boolean isDepartmentAdmin = false;
        if (!isAdmin) {
        	isDepartmentAdmin = isDepartmentAdmin(context, rootCommunity);
        } else {
        	isDepartmentAdmin = true;
        }
        boolean canSeeDepartment = isDepartmentAdmin;

        DepartmentHierarchyDTO root = new DepartmentHierarchyDTO();
        root.setName(department.getDepartmentName());
        root.setNodeType(NodeType.DEPARTMENT.name());
        root.setChildren(new ArrayList<>());
        root.setUuid(String.valueOf(department.getID()));
        root.setCommunityId(String.valueOf(rootCommunity.getID()));
        root.setHandle(rootCommunity.getHandle());

        // Add sub-communities as branches
        if(rootCommunity != null) {
	        List<Community> subCommunities = rootCommunity.getSubcommunities();
	        for (Community sub : subCommunities) {
	        	
	        	// check for subdepartment permissions
	        	if (!isDepartmentAdmin && !hasSubDepartmentPermissions(context, sub)) {
	        		continue;
	        	}
	        	canSeeDepartment = true;
	        	
	            DepartmentHierarchyDTO branch = new DepartmentHierarchyDTO();
	            branch.setName(sub.getName());
	            branch.setNodeType(NodeType.SUB_DEPARTMENT.name());
	            branch.setChildren(new ArrayList<>());
	            branch.setUuid(String.valueOf(sub.getID()));	   
	            branch.setHandle(sub.getHandle());
	
	            root.getChildren().add(branch);
	        }
        }
        
        // if user has no permission on dep or sections then dont show department
        if (!canSeeDepartment) {
        	return null;
        }
        
        return root;
    }

	private boolean hasSubDepartmentPermissions(Context context, Community subDepartment) throws SQLException {
		EPerson currentUser = context.getCurrentUser();
		
		// check all permissions
		String groupNamePrefix = String.join("_", subDepartment.getName(), subDepartment.getID().toString());
		
		for (FolderPermission perm : EnumSet.of(FolderPermission.VIEW, FolderPermission.DOWNLOAD, FolderPermission.UPLOAD, FolderPermission.REMOVE,FolderPermission.EDIT, FolderPermission.ADMIN)) {
			String groupName = String.join("_", groupNamePrefix, perm.name());
			Group group = groupService.findByName(context, groupName);
			if (groupService.isMember(context, currentUser, group)) {
				return true;
			}
		}
		return false;
	}

	private boolean isDepartmentAdmin(Context context, Community community) throws SQLException {
		// check admin group permission
		String adminGroupName = String.join("_", community.getName(), community.getID().toString(), FolderPermission.ADMIN.name());
		Group adminGroup = groupService.findByName(context, adminGroupName);
		return groupService.isMember(context, context.getCurrentUser(), adminGroup);
	}

	public List<DepartmentHierarchyDTO> filterDepartments(Context context, List<Department> departments) throws SQLException, AuthorizeException {
		boolean isAdmin = authorizeService.isAdmin(context);
		List<DepartmentHierarchyDTO> list = new ArrayList<DepartmentHierarchyDTO>();
		
		for (Department department : departments) {
			DepartmentHierarchyDTO dto = convert(context, department, isAdmin);
			if (dto != null) {
				list.add(dto);
			}
		}
		return list;
	}

	public DepartmentHierarchyDTO convert(Context context, Department department) throws SQLException {
		return convert(context, department, authorizeService.isAdmin(context));
	}
}