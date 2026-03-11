package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SavedSearchRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.savedsearch.SavedSearch;
import org.springframework.stereotype.Component;

@Component
public class SavedSearchRestConverter extends DSpaceObjectConverter<SavedSearch, SavedSearchRest> {

	@Override
    public SavedSearchRest convert(SavedSearch obj, Projection projection) {
		SavedSearchRest savedSearch = super.convert(obj, projection);
		savedSearch.setEpersonId(obj.getEpersonId());
		savedSearch.setName(obj.getSearchName());
		savedSearch.setSearchName(obj.getSearchName());
		savedSearch.setURL(obj.getURL());
        return savedSearch;
    }
	
	@Override
	public Class<SavedSearch> getModelClass() {
		// TODO Auto-generated method stub
		return SavedSearch.class;
	}

	@Override
	protected SavedSearchRest newInstance() {
		// TODO Auto-generated method stub
		return new SavedSearchRest();
	}


}