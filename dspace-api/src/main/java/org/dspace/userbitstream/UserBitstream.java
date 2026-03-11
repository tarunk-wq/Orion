package org.dspace.userbitstream;

import java.util.Date;

import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;

/*
 * This entity is created to store BITSTREAM versions and user mapping.
 * */
@Entity
@Table(name = "user_bitstream")
public class UserBitstream extends DSpaceObject implements DSpaceObjectLegacySupport{

	private static final long serialVersionUID = 1L;
	
	@ManyToOne
	@JoinColumn(name="user_id")
	private EPerson eperson;
	
	@ManyToOne
	@JoinColumn(name="original_bistream_id")
	private Bitstream originalBistream;
	
	@ManyToOne
	@JoinColumn(name="annotated_bistream_id")
	private Bitstream annotatedBistream;
	
	@Column(name="creation_date")
	@Temporal(TemporalType.TIMESTAMP)
	private Date creationDate;
	
	@ManyToOne
	@JoinColumn(name="item_id")
	private Item itemId;
	
	public UserBitstream() {
		super();
	}

	public UserBitstream(EPerson eperson, Bitstream originalBistream, Bitstream annotatedBistream, Date creationDate,
			Item itemId) {
		super();
		this.eperson = eperson;
		this.originalBistream = originalBistream;
		this.annotatedBistream = annotatedBistream;
		this.creationDate = creationDate;
		this.itemId = itemId;
	}

	public EPerson getEperson() {
		return eperson;
	}

	public void setEperson(EPerson eperson) {
		this.eperson = eperson;
	}

	public Bitstream getOriginalBistream() {
		return originalBistream;
	}

	public void setOriginalBistream(Bitstream originalBistream) {
		this.originalBistream = originalBistream;
	}

	public Bitstream getVersionedBistream() {
		return annotatedBistream;
	}

	public void setVersionedBistream(Bitstream annotatedBistream) {
		this.annotatedBistream = annotatedBistream;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Item getItemId() {
		return itemId;
	}

	public void setItemId(Item itemId) {
		this.itemId = itemId;
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
	public Integer getLegacyId() {
		// TODO Auto-generated method stub
		return null;
	}
	
}