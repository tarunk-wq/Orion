package org.dspace.app.rest.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TempAccessDTO extends DSpaceObjectRest {

	private static final long serialVersionUID = 1L;
	public static final String CATEGORY = RestAddressableModel.TEMP_ACCCESS;
	public static final String NAME = "tempaccess";
	public static final String PLURAL_NAME = "tempaccesses";

	private String id;
	private UUID itemUuid;
	private UUID epersonUuid;
	private String epersonName;
	private UUID departmentUuid;
	private String startDate;
	private String endDate;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public UUID getItemUuid() {
		return itemUuid;
	}

	public void setItemUuid(UUID itemUuid) {
		this.itemUuid = itemUuid;
	}

	public UUID getEpersonUuid() {
		return epersonUuid;
	}

	public void setEpersonUuid(UUID epersonUuid) {
		this.epersonUuid = epersonUuid;
	}

	public UUID getDepartmentUuid() {
		return departmentUuid;
	}

	public void setDepartmentUuid(UUID departmentUuid) {
		this.departmentUuid = departmentUuid;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getName() {
		return NAME;
	}

	public static String getPluralName() {
		return PLURAL_NAME;
	}

	@Override
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	public String getType() {
		// TODO Auto-generated method stub
		return "tempaccess";
	}

	@Override
	public String getCategory() {
		// TODO Auto-generated method stub
		return CATEGORY;
	}

	public String getEpersonName() {
		return epersonName;
	}

	public void setEpersonName(String epersonName) {
		this.epersonName = epersonName;
	}

	@Override
	public String getTypePlural() {
		// TODO Auto-generated method stub
		return PLURAL_NAME;
	}
}
