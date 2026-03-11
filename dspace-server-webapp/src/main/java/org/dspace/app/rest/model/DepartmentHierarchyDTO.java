package org.dspace.app.rest.model;

import java.util.List;

public class DepartmentHierarchyDTO extends DSpaceObjectRest{
	
	private String communityId;
	private String name;              // Display name
    private String nodeType;              // "department", "branch", or "leaf"
    private List<DepartmentHierarchyDTO> children; // Recursive children
    

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<DepartmentHierarchyDTO> getChildren() {
		return children;
	}

	public void setChildren(List<DepartmentHierarchyDTO> children) {
		this.children = children;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
	
	public String getNodeType() {
		return this.nodeType;
	}

	public String getCommunityId() {
		return communityId;
	}

	public void setCommunityId(String communityId) {
		this.communityId = communityId;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCategory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTypePlural() {
		// TODO Auto-generated method stub
		return null;
	}

}
