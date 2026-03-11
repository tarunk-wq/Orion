package org.dspace.batchhistory;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.dspace.content.CacheableDSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.eperson.EPerson;

@Entity
@Table(name="batchhistory")
public class BatchHistory  extends CacheableDSpaceObject implements DSpaceObjectLegacySupport{

    @Column(name = "history_id", insertable = false, updatable = false)
    private Integer legacyId;
	
	@Column(name="batch_name")
	private String batchName;
	
	@Column(name="from_state")
	private String fromState;
	
	@Column(name="to_state")
	private String toState;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="owner")
	private EPerson owner;
	
	@Column(name = "time")
    private Instant time;

	public String getBatchName() {
		return batchName;
	}

	public void setBatchName(String batchName) {
		this.batchName = batchName;
	}
	
	public String getFromState() {
		return fromState;
	}

	public void setFromState(String fromState) {
		this.fromState = fromState;
	}

	public String getToState() {
		return toState;
	}

	public void setToState(String toState) {
		this.toState = toState;
	}

	public EPerson getOwner() {
	        return owner;
	  }
	 
    public void setOwner(EPerson sub) {
        this.owner = sub;
        setModified();
    }

	public Instant getTime() {
		return time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}

	
	 protected BatchHistory() {

	    }
	
	@Override
	public Integer getLegacyId() {
		// TODO Auto-generated method stub
		return legacyId;
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