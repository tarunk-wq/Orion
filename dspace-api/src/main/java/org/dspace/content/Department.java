package org.dspace.content;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.dspace.eperson.EPerson;

@Entity
@Table(name = "department")
public class Department extends DSpaceObject {

	@Column(name = "department_name")
	private String departmentName;
	
	@Column(name = "abbreviation")
	private String abbreviation;
	
	@ManyToOne
	@JoinColumn(name = "created_by")
	private EPerson createdBy;
	
	@Column(name = "creation_time")
	private Date creationTime;
	
	@Column(name = "admin_group_name")
	private String adminGroupName;
	
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;
	
	public String getDepartmentName() {
		return departmentName;
	}

	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	public EPerson getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(EPerson createdBy) {
		this.createdBy = createdBy;
	}

	public Community getCommunity() {
		return community;
	}

	public void setCommunity(Community community) {
		this.community = community;
	}

	public String getAdminGroupName() {
		return adminGroupName;
	}

	public void setAdminGroupName(String adminGroupName) {
		this.adminGroupName = adminGroupName;
	}
	
	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@Transient
	public List<MetadataValue> getMetadata() {
	    return null;
	}
	
	@Override
	public void setMetadata(List<MetadataValue> metadata) {
	    // No-op if you're not using it
	}
}
