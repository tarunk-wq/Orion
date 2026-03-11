package org.dspace.thesaurus;

import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;

/**
 * Class representing an audittrail of e-person.
 *
 * @author virsoftech.com
 */
@Entity
@Table(name = "thesaurus")
public class Thesaurus extends DSpaceObject implements DSpaceObjectLegacySupport {

	@Column(name = "thesaurus_id", insertable = false, updatable = false)
    private Integer legacyId;
	
	@Column(name = "word", unique = true)
	private String word;
	
	@Column(name = "value", columnDefinition = "text")
	private String value;
	
	public Thesaurus() {
		// TODO nothing to do
	}
	
	public Thesaurus(String word, String value) {
		this.word = word;
		this.value = value;
	}
	
	@Override
	public Integer getLegacyId() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
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
