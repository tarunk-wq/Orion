package org.dspace.app.rest.model;

import java.util.UUID;

public class SavedSearchRest extends DSpaceObjectRest {
	public static final String NAME = "savedsearch";
	public static final String PLURAL_NAME = "savedsearches";
	public static final String CATEGORY = RestAddressableModel.SAVEDSEARCH;

	private UUID epersonId;
	
	private String searchName;
	
	private String url;
	
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
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTypePlural() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCategory() {
		// TODO Auto-generated method stub
		return null;
	}
}