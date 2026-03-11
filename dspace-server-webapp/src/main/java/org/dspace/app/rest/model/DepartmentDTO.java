package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentDTO extends DSpaceObjectRest {
	
	public static final String CATEGORY = RestAddressableModel.DEPARTMENT;
    public static final String NAME = "department";
    public static final String PLURAL_NAME = "departments";
    

    private String departmentName;
    private String abbreviation;
    private String adminGroupName;
    private String communityId;
    
	public String getDepartmentName() {
		return departmentName;
	}

	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	public String getAdminGroupName() {
		return adminGroupName;
	}

	public void setAdminGroupName(String adminGroupName) {
		this.adminGroupName = adminGroupName;
	}

	public String getCommunityId() {
		return communityId;
	}

	public void setCommunityId(String parentCommunityId) {
		this.communityId = parentCommunityId;
	}

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	public String getType() {
		return NAME;
	}

    public String getCategory() {
        return CATEGORY;
    }

	public String getTypePlural() {
		// TODO Auto-generated method stub
		return PLURAL_NAME;
	}
}
