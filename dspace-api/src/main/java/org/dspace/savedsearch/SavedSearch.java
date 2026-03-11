package org.dspace.savedsearch;

import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.*;

@Entity
@Table(name = "SavedSearch", schema = "public")
public class SavedSearch extends DSpaceObject implements DSpaceObjectLegacySupport{
	
	@Column(name = "eperson_id")
	private UUID epersonId;
	
	@Column(name = "search_name")
	private String searchName;
	
	@Column(name = "url")
	private String url;
    
    public SavedSearch() {
		
	}
    
    public SavedSearch(UUID epersonId, String searchName, String url) {
		super();
		this.epersonId = epersonId;
		this.searchName = searchName;
		this.url = url;
	}
	
	 /**
     * To get the EPersonId of the EPerson saving the search.
     * @return the epersonId.
     */
	
	public UUID getEpersonId() {
		return epersonId;
	}
	
	public void setEpersonId(UUID epersonId) {
		this.epersonId = epersonId;
	}
	
	 /**
     * To get the name of the saved search.
     * @return saved search name .
     */
	
	public String getSearchName() {
		return searchName;
	}
	
	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}
	
	 /**
     * To get the URL of the saved search.
     * @return saved search URL.
     */
	
	public String getURL() {
		return url;
	}
	
	public void setURL(String url) {
		this.url = url;
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