package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BatchDetailsRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(BatchDetailsRest.NAME)
public class BatchDetailsResource extends DSpaceResource<BatchDetailsRest>{

	public BatchDetailsResource(BatchDetailsRest data, Utils utils) {
		super(data, utils);
	}

}
