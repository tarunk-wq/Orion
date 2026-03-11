package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.AuditTrailRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(AuditTrailRest.NAME)
public class AuditTrailResource extends DSpaceResource<AuditTrailRest> {

	public AuditTrailResource(AuditTrailRest data, Utils utils) {
		super(data, utils);
	}

}
