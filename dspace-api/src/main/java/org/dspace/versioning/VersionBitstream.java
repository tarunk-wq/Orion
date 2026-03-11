/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import java.util.Date;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.eperson.EPerson;

import jakarta.persistence.*;

@Entity
@Table(name = "versionbitstream")
public class VersionBitstream extends DSpaceObject implements DSpaceObjectLegacySupport
{
	
	@ManyToOne
	@JoinColumn(name = "uuid_bundle")
	private Bundle bundle;
	
	@OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bitstream_uuid")
    private Bitstream bitstream;

    @Column(name = "version_number")
    private double versionNumber;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private EPerson ePerson;

    @Column(name = "version_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date versionDate;

    @ManyToOne
    @JoinColumn(name = "parent_version")
    private VersionBitstream parentVersion;

    @OneToMany(mappedBy="parentVersion")
    private List<VersionBitstream> childVersions;
    
    @Column(name = "bitstream_name")
    private String bitstreamName;

    @Column(name = "active_version")
    private Boolean activeVersion;
    
    @Column(name= "deleted")
    private Boolean deleted;
    
    @Column(name= "comment")
    private String comment;
    
    protected VersionBitstream() {
    	
    }
    
    public VersionBitstream(Bundle bundle, Bitstream bitstream, double versionNumber, EPerson ePerson,
			VersionBitstream parentVersion, Boolean isActive, Boolean deleted)
	{
    	this.bundle = bundle;
    	this.bitstream = bitstream;
    	this.versionNumber = versionNumber;
    	this.ePerson = ePerson;
    	this.versionDate = new Date();
    	this.parentVersion = parentVersion;
    	this.bitstreamName = bitstream.getName();
    	this.activeVersion = isActive;
    	this.deleted = deleted;
    }

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public Bitstream getBitstream() {
		return bitstream;
	}

	public void setBitstream(Bitstream bitstream) {
		this.bitstream = bitstream;
	}

	public double getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(double versionNumber) {
		this.versionNumber = versionNumber;
	}

	public EPerson getePerson() {
		return ePerson;
	}

	public void setePerson(EPerson ePerson) {
		this.ePerson = ePerson;
	}

	public Date getVersionDate() {
		return versionDate;
	}

	public void setVersionDate(Date versionDate) {
		this.versionDate = versionDate;
	}

	public VersionBitstream getParentVersion() {
		return parentVersion;
	}

	public void setParentVersion(VersionBitstream parentVersion) {
		this.parentVersion = parentVersion;
	}

	public List<VersionBitstream> getChildVersions() {
		return childVersions;
	}

	public void setChildVersions(List<VersionBitstream> childVersions) {
		this.childVersions = childVersions;
	}

	public String getBitstreamName() {
		return bitstreamName;
	}

	public void setBitstreamName(String bitstreamName) {
		this.bitstreamName = bitstreamName;
	}

	public Boolean getActiveVersion() {
		return activeVersion;
	}

	public void setActiveVersion(Boolean activeVersion) {
		this.activeVersion = activeVersion;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}
	
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
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
		return bitstreamName;
	}
}
