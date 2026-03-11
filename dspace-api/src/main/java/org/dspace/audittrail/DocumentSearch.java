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
@Table(name = "document_search")
public class DocumentSearch extends CacheableDSpaceObject implements DSpaceObjectLegacySupport  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5098518690808917387L;

	@Column(name = "document_search_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name = "time", nullable = false)
	private Date time;
    
	@Column(name = "ip_address")
	private String ipAddress;
	
	@Column(name = "username")
	private String username;

	@Column(name = "item_ids", columnDefinition = "text")
	private String itemIds;

	@Column(name = "query")
	private String query;
	
	public DocumentSearch() {
		// TODO
	}
	public DocumentSearch(Date date, String ipAddress, String username, String itemIds) {
		this.time = date;
		this.ipAddress = ipAddress;
		this.username = username;
		this.itemIds = itemIds;
	}
	
	public DocumentSearch(Date date, String ipAddress, String username, String itemIds, String query) {
		this.time = date;
		this.ipAddress = ipAddress;
		this.username = username;
		this.itemIds = itemIds;
		this.query = query;
	}
	
	@Override
	public Integer getLegacyId() {
		return legacyId;
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
		return username;
	}

	public void setUserName(String userName) {
		this.username = userName;
	}

	public String getItemIds() {
		return itemIds;
	}

	public void setItemIds(String itemIds) {
		this.itemIds = itemIds;
	}

	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
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
