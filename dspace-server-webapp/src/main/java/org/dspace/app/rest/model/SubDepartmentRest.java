package org.dspace.app.rest.model;

import java.util.UUID;

public class SubDepartmentRest {

	private UUID id;
	
	private String name;
	
	private String parentId;
	
	private UserMetadataDto[] userMetadataList;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	
	public UserMetadataDto[] getUserMetadataList() {
	      return userMetadataList;
	}
	public void setUserMetadataList(UserMetadataDto[] userMetadataList) {
		this.userMetadataList = userMetadataList;
	}
	
}
