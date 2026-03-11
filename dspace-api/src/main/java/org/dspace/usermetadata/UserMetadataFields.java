/**
 * 
 */
package org.dspace.usermetadata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.dspace.core.ReloadableEntity;

/**
 * @author sumanta
 *
 */
@Entity
@Table(name="user_metadata_fields")
public class UserMetadataFields implements ReloadableEntity<Integer>{

	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE,generator="user_metadata_fields_id_seq")
	@SequenceGenerator(name="user_metadata_fields_id_seq",sequenceName="user_metadata_fields_id_seq",allocationSize = 1, initialValue = 1)
	
	@Column(name = "id")
	private Integer id;

	@Column(name = "user_field_name")
	private String userFieldName;

	@Column(name = "system_field_name")
	private String systemFieldName;

	@Column(name = "field_type") //0->text, 1->integer, 2 -> date
	private Integer fieldType;

	@Column(name = "field_position")
	private Integer fieldPosition;

	@Column(name = "sub_dept_community_handle")
	private String subDeptCommunityHandle;
	
	@Column(name = "user_field_value")
	private String userFieldValue;
	
	@Column(name = "is_unique_metadata")
	private Boolean isUniqueMetadata = false;
	
	public Boolean getIsUniqueMetadata() {
	    return isUniqueMetadata;
	}

	public void setIsUniqueMetadata(Boolean isUniqueMetadata) {
	    this.isUniqueMetadata = isUniqueMetadata;
	}

	public String getUserFieldValue() {
		return userFieldValue;
	}

	public void setUserFieldValue(String userFieldValue) {
		this.userFieldValue = userFieldValue;
	}

	public String getUserFieldName() {
		return userFieldName;
	}

	public void setUserFieldName(String userFieldName) {
		this.userFieldName = userFieldName;
	}

	public String getSystemFieldName() {
		return systemFieldName;
	}

	public void setSystemFieldName(String systemFieldName) {
		this.systemFieldName = systemFieldName;
	}

	public Integer getFieldType() {
		return fieldType;
	}

	public void setFieldType(Integer fieldType) {
		this.fieldType = fieldType;
	}

	public Integer getFieldPosition() {
		return fieldPosition;
	}

	public void setFieldPosition(Integer fieldPosition) {
		this.fieldPosition = fieldPosition;
	}

	public String getSubDeptCommunityHandle() {
		return subDeptCommunityHandle;
	}

	public void setSubDeptCommunityHandle(String subDeptCommunityHandle) {
		this.subDeptCommunityHandle = subDeptCommunityHandle;
	}

	@Override
	public Integer getID() {
		return id;
	}

}
