package org.dspace.uploaddetails;

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
@Table(name = "uploaddetails")
public class UploadDetails extends CacheableDSpaceObject implements DSpaceObjectLegacySupport{


	@Column(name = "batch_name")
	private String batchName;
	
	@Column(name="upload_id", insertable = false, updatable = false)
	private Integer legacyId;
	
	@Column(name="upload_status")
	private String uploadstatus;
	
	@Column(name = "time")
    private Instant time;
	
	@Column(name= "batch_dir")
	private String batchdir;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id")
    private EPerson submitter = null;
    
	
	public String getBatchName() {
		return batchName;
	}

	public void setBatchName(String batchName) {
		this.batchName = batchName;
	}


	public String getUploadstatus() {
		return uploadstatus;
	}

	public void setUploadstatus(String uploadstatus) {
		this.uploadstatus = uploadstatus;
	}

	public Instant getTime() {
		return time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}
  
	public String getBatchDir() {
		return batchdir;
	}

	public void setBatchDir(String batchdir) {
		this.batchdir = batchdir;
	}

    public EPerson getSubmitter() {
        return submitter;
    }

    public void setSubmitter(EPerson sub) {
        this.submitter = sub;
        setModified();
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
