package org.dspace.thesaurus.dao;

import java.sql.SQLException;
import java.util.List;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.thesaurus.Thesaurus;

public interface ThesaurusDAO  extends DSpaceObjectDAO<Thesaurus>, DSpaceObjectLegacySupportDAO<Thesaurus> {
	
	public Thesaurus searchByWord(Context context, String word) throws SQLException;
	
	public List<String> searchByValueAndWord(Context context, String valueOrWord) throws SQLException;
	
	public List<Thesaurus> findAll(Context context) throws SQLException;
	
	int countRows(Context context) throws SQLException;
}
