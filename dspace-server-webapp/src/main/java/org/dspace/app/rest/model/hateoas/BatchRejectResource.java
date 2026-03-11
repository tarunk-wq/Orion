package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BatchRejectRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(BatchRejectRest.NAME)
public class BatchRejectResource extends DSpaceResource<BatchRejectRest>{
	
	public BatchRejectResource(BatchRejectRest data, Utils utils) {
		super(data, utils);
	}
}
