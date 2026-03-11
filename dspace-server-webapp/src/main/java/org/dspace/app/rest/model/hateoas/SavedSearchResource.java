package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SavedSearchRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(SavedSearchRest.NAME)
public class SavedSearchResource extends DSpaceResource<SavedSearchRest>{

	public SavedSearchResource(SavedSearchRest data, Utils utils) {
		super(data, utils);
	}

}
