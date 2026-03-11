package org.dspace.batchreject;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.dspace.content.CacheableDSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.eperson.EPerson;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rejectiondetails")
public class BatchReject extends CacheableDSpaceObject implements DSpaceObjectLegacySupport{

	@Column(name = "batch_name")
	private String batchName;
	
	@Column(name="item_ids", columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
	private UUID[] itemIds;
	
	@Column(name="remarks")
	private String reason;
	
	@Column(name = "time")
    private Instant time;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private EPerson user = null;

	public String getBatchName() {
		return batchName;
	}

	public void setBatchName(String batchName) {
		this.batchName = batchName;
	}

	public UUID[] getItemIds() {
		return itemIds;
	}

	public void setItemIds(UUID[] itemIds) {
		this.itemIds = itemIds;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Instant getTime() {
		return time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}

	public EPerson getUser() {
		return user;
	}

	public void setUser(EPerson user) {
		this.user = user;
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
