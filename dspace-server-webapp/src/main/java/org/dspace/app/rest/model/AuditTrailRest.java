package org.dspace.app.rest.model;

import java.util.Date;

import jakarta.persistence.Column;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;


public class AuditTrailRest extends DSpaceObjectRest {
    public static final String NAME = "audittrail";
    public static final String PLURAL_NAME = "audittrails";
    public static final String SEARCH = "filter";
    public static final String CATEGORY = RestAddressableModel.AUDITTRAIL;

	@Column(name = "audittrail_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name = "sequence_id")
    private Integer sequenceId = -1;
	
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
	
	@Column(name = "url")
	private String url;
	
	public Integer getLegacyId() {
		return legacyId;
	}

	public void setLegacyId(Integer legacyId) {
		this.legacyId = legacyId;
	}

	public Integer getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Integer sequenceId) {
		this.sequenceId = sequenceId;
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
    @JsonProperty(access = Access.READ_ONLY)
	public String getType() {
		 return NAME;
	}

	@Override
	public String getCategory() {
        return CATEGORY;
	}

	@Override
	public String getTypePlural() {
		// TODO Auto-generated method stub
		return PLURAL_NAME;
	}
}
