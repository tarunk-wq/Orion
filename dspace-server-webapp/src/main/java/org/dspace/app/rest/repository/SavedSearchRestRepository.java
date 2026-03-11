package org.dspace.app.rest.repository;

import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.SavedSearchRest;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.savedsearch.SavedSearch;
import org.dspace.savedsearch.service.SavedSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(SavedSearchRest.CATEGORY + "." + SavedSearchRest.NAME)
public class SavedSearchRestRepository extends DSpaceObjectRestRepository<SavedSearch, SavedSearchRest>{
	
	@Autowired
	private SavedSearchService savedSearchService;
	
	@Autowired
    private ConverterService converterService;
	
	SavedSearchRestRepository(DSpaceObjectService<SavedSearch> dsoService) {
		super(dsoService);
	}

	@Override
	public SavedSearchRest findOne(Context context, UUID id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<SavedSearchRest> findAll(Context context, Pageable pageable) {
		try {	
	        int total = savedSearchService.countTotal(context);
			List<SavedSearch> savedSearchlist=savedSearchService.findAll(context,pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
			return converterService.toRestPage(savedSearchlist,pageable,total, utils.obtainProjection());
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

    @SearchRestMethod(name = "byUser")
    public Page<SavedSearchRest> searchByUser(Pageable pageable) {
        Context context = obtainContext();
        UUID epersonUUID = context.getCurrentUser().getID();
		try {	
			List<SavedSearch> savedSearchlist=savedSearchService.getSavedSearchByEPersonId(context, epersonUUID);
			int total = savedSearchlist.size();
			return converterService.toRestPage(savedSearchlist,pageable,total, utils.obtainProjection());
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	@Override
	public Class<SavedSearchRest> getDomainClass() {
		// TODO Auto-generated method stub
		return SavedSearchRest.class;
	}
	
}