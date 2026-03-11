package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ThesaurusRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(ThesaurusRest.NAME)
public class ThesaurusResource extends DSpaceResource<ThesaurusRest> {

	public ThesaurusResource(ThesaurusRest data, Utils utils) {
		super(data, utils);
	}

}
