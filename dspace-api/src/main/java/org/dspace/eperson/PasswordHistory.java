package org.dspace.eperson;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;

@Entity
@Table(name = "passwordhistory")
public class PasswordHistory extends DSpaceObject implements DSpaceObjectLegacySupport{
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="eperson")
	private EPerson eperson;
	
	@Column(name = "password")
	private String password;
		
	@Column(name = "creation_date")
	private Date creationDate;

	public EPerson getEperson() {
		return eperson;
	}

	public void setEperson(EPerson eperson) {
		this.eperson = eperson;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@Override
	public Integer getLegacyId() {
		// TODO Auto-generated method stub
		return null;
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

}
