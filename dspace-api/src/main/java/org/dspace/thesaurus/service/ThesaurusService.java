package org.dspace.thesaurus.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.thesaurus.Thesaurus;

public interface ThesaurusService extends DSpaceObjectService<Thesaurus>, DSpaceObjectLegacySupportService<Thesaurus>{
	
	public Thesaurus searchByWord(Context context, String word) throws SQLException;
	
	public List<String> searchByValueAndWord(Context context, String valueOrWord) throws SQLException;
	
	public List<Thesaurus> findAll(Context context) throws SQLException;
	
	int countRows(Context context) throws SQLException;
	
	public Thesaurus insert(Context context, String word, String value) throws SQLException;
	
	public void deleteByWord(Context context, String word) throws SQLException;

}
