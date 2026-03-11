package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public class ThesaurusRest extends DSpaceObjectRest {
	public static final String NAME = "thesauruse";
    public static final String SEARCH = "filter";
	public static final String CATEGORY = RestAddressableModel.THESAURUS;
	
	private Integer legacyId;
	
	private String word;
	
	private String value;
	
	public Integer getLegacyId() {
		return legacyId;
	}

	public void setLegacyId(Integer legacyId) {
		this.legacyId = legacyId;
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
	@JsonProperty(access = Access.READ_ONLY)
	public String getType() {
		return NAME;
	}

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

	@Override
	public String getTypePlural() {
		// TODO Auto-generated method stub
		return null;
	}

}
