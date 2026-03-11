/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.audittrail;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.dspace.content.CacheableDSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;

/**
 * Class representing an audittrail of e-person.
 *
 * @author virsoftech.com
 */
@Entity
@Table(name = "audittrail")
public class AuditTrail extends CacheableDSpaceObject implements DSpaceObjectLegacySupport {

	@Column(name = "audittrail_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name = "time", nullable = false)
	private Date time;
    
	@Column(name = "ip_address")
	private String ipAddress;
	
	@Column(name = "username")
	private String userName;
	
	@Column(name = "action")
	private String action;
	
	@Column(name = "handle")
	private String handle;
	
	@Column(name = "description", columnDefinition = "text")
	private String description;

	public AuditTrail() {
		// TODO
	}
	public AuditTrail(Date date, String ipAddress, String userName, String action, String handle, String description) {
		this.time = date;
		this.ipAddress = ipAddress;
		this.userName = userName;
		this.action = action;
		this.handle = handle;
		this.description = description;
	}

	@Override
	public Integer getLegacyId() {
		// TODO Auto-generated method stub
		return null;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public String getDescription() {
	    return description;
	}

	public void setDescription(String description) {
	    this.description = description;
	}

	public void setLegacyId(Integer legacyId) {
		this.legacyId = legacyId;
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
