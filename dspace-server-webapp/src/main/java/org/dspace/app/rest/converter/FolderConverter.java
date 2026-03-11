package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.dspace.app.rest.model.DepartmentHierarchyDTO;
import org.dspace.app.rest.model.FolderRest;
import org.dspace.constants.enums.NodeType;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.springframework.stereotype.Component;

@Component
public class FolderConverter {

	public DepartmentHierarchyDTO convert(Community rootCommunity, String nodeType) {

        DepartmentHierarchyDTO root = new DepartmentHierarchyDTO();
        root.setName(rootCommunity.getName());
        root.setNodeType(nodeType);
        root.setChildren(new ArrayList<>());
        root.setUuid(rootCommunity.getID().toString());
        root.setCommunityId(rootCommunity.getID().toString());

        // Add sub-communities as branches
        if(rootCommunity != null) {
	        List<Community> subCommunities = rootCommunity.getSubcommunities();
	        for (Community sub : subCommunities) {
	            DepartmentHierarchyDTO branch = new DepartmentHierarchyDTO();
	            branch.setName(sub.getName());
	            branch.setNodeType(NodeType.BRANCH.name());
	            branch.setChildren(new ArrayList<>());
	            branch.setUuid(String.valueOf(sub.getID()));
	            
//	            for (Community subCom : sub.getSubcommunities()) {
//	                DepartmentHierarchyDTO leaf = new DepartmentHierarchyDTO();
//	                leaf.setName(subCom.getName());
//	                leaf.setNodeType(NodeType.BRANCH.name());
//	                leaf.setChildren(Collections.emptyList());
//	                branch.setUuid(String.valueOf(subCom.getID()));
//	
//	                branch.getChildren().add(leaf);
//	            }
	
	            root.getChildren().add(branch);
	        }
	
	        for (Collection collection : rootCommunity.getCollections()) {
	            DepartmentHierarchyDTO leaf = new DepartmentHierarchyDTO();
	            leaf.setName(collection.getName());
	            leaf.setNodeType(NodeType.LEAF.name());
	            leaf.setChildren(Collections.emptyList());
	            leaf.setUuid(String.valueOf(collection.getID()));

	            root.getChildren().add(leaf);
	        }
        }
        return root;
	}

	public FolderRest convertLeaf(Collection collection) throws SQLException {
		FolderRest folderRest = new FolderRest();
		folderRest.setId(collection.getID());
		folderRest.setName(collection.getName());
		folderRest.setParentId(collection.getCommunities().get(0).getID().toString());
		folderRest.setNodeType(NodeType.LEAF.name());
		return folderRest;
	}

	public FolderRest convertBranch(Community community) {
		FolderRest folderRest = new FolderRest();
		folderRest.setId(community.getID());
		folderRest.setName(community.getName());
		folderRest.setParentId(community.getParentCommunities().get(0).getID().toString());
		folderRest.setNodeType(NodeType.LEAF.name());
		return folderRest;
	}

}
