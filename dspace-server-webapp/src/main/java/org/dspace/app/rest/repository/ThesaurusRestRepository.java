package org.dspace.app.rest.repository;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.ThesaurusRest;
import org.dspace.app.rest.model.hateoas.ThesaurusResource;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.thesaurus.Thesaurus;
import org.dspace.thesaurus.service.ThesaurusService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(ThesaurusRest.CATEGORY + "." + ThesaurusRest.NAME)
public class ThesaurusRestRepository extends DSpaceObjectRestRepository<Thesaurus, ThesaurusRest> implements InitializingBean{

	private final ThesaurusService thesaurusService;
    
	ThesaurusRestRepository(ThesaurusService dsoService) {
		super(dsoService);
		this.thesaurusService = dsoService;
	}

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;
	
    @SearchRestMethod(name = "fetchbyword")
	public Page<ThesaurusRest> getTheasaurusWords(@Parameter(value = "word", required = true) String word,
			Pageable pageable) throws SQLException {
    	Context context = obtainContext();
		Thesaurus thesaurus = thesaurusService.searchByWord(context, word.toLowerCase());
		if (thesaurus == null) {
			return null;
		}
		List<Thesaurus> list = new ArrayList<Thesaurus>();
		list.add(thesaurus);
		return converter.toRestPage(list, pageable, utils.obtainProjection());
	}
    
    @SearchRestMethod(name = "fetchthesaurus")
	public ThesaurusResource getTheasaurusWords(@Parameter(value = "word", required = true) String word) throws SQLException {
    	Context context = obtainContext();
    	ThesaurusRest thesaurus = searchByWord(context, word);
		return converter.toResource(thesaurus);
	}
    
	public ThesaurusRest searchByWord(Context context, String word) throws SQLException {
		Thesaurus thesaurus = thesaurusService.searchByWord(context, word);
		if (thesaurus == null) {
			return null;
		}
		return converter.toRest(thesaurus, utils.obtainProjection());
	}
	
	public ThesaurusRest create(Context context, String word, String value) throws SQLException {
		Thesaurus thesaurus = thesaurusService.insert(context, word.toLowerCase(), value.toLowerCase());
		ThesaurusRest thesaurusRest = converter.toRest(thesaurus, utils.obtainProjection());
		context.commit();
		return thesaurusRest;
	}
	
	public void deleteByWord(Context context, String word) throws SQLException, AuthorizeException, IOException {
		Thesaurus thesaurus = thesaurusService.searchByWord(context, word);
		if(thesaurus != null) {
			thesaurusService.delete(context, thesaurus);
			context.commit();
		}
	}
	
	public void bulkInsert(File file) {
		Context context = obtainContext();
		try {
			List<String[]> parseData = Utils.parseCSVFileInList(file, true);
			if (parseData != null && !parseData.isEmpty()) {
				for (String[] rowData : parseData) {
					if (rowData.length > 1) {
						thesaurusService.insert(context, rowData[0].toLowerCase(), rowData[1].toLowerCase());
					}
				}
			}
			context.commit();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	@Override
	public ThesaurusRest findOne(Context context, UUID id) {
		Thesaurus thesaurus;
		try {
			thesaurus = thesaurusService.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return converter.toRest(thesaurus, utils.obtainProjection());
	}

	@Override
	public Page<ThesaurusRest> findAll(Context context, Pageable pageable) {
		List<Thesaurus> thesaurusList;
		try {
			thesaurusList = thesaurusService.findAll(context);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return converter.toRestPage(thesaurusList, pageable, thesaurusList.size(),  utils.obtainProjection());
	}

	@Override
	public Class<ThesaurusRest> getDomainClass() {
		return ThesaurusRest.class;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		// nothing TODO
	}
}
