package org.dspace.thesaurus;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.dspace.thesaurus.dao.ThesaurusDAO;
import org.dspace.thesaurus.service.ThesaurusService;
import org.springframework.beans.factory.annotation.Autowired;

public class ThesaurusServiceImpl extends DSpaceObjectServiceImpl<Thesaurus> implements ThesaurusService{

    @Autowired(required = true)
    protected ThesaurusDAO thesaurusDAO;



    @Override
	public Thesaurus insert(Context context, String word, String value) throws SQLException {
		Thesaurus thesaurus = searchByWord(context, word);
		if (thesaurus == null && !(value == null || value.isEmpty())) {
			thesaurus = new Thesaurus(word, value);
			thesaurusDAO.create(context, thesaurus);
		} else {
			if (value == null || value.isEmpty()) {
				thesaurusDAO.delete(context, thesaurus);
			} else {
				thesaurus.setValue(value);
				thesaurusDAO.save(context, thesaurus);
			}
		}
		return thesaurus;
	}
    
	@Override
	public Thesaurus searchByWord(Context context, String word) throws SQLException {
		return thesaurusDAO.searchByWord(context, word);
	}
	
	public List<String> searchByValueAndWord(Context context, String valueOrWord) throws SQLException {
		return thesaurusDAO.searchByValueAndWord(context, valueOrWord);
	}

	@Override
	public List<Thesaurus> findAll(Context context) throws SQLException {
		return thesaurusDAO.findAll(context);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return thesaurusDAO.countRows(context);
	}
	
    @Override
    public void update(Context context, Thesaurus thesaurus) throws SQLException, AuthorizeException {
        super.update(context, thesaurus);
        thesaurusDAO.save(context, thesaurus);
    }
    
	@Override
	public Thesaurus find(Context context, UUID uuid) throws SQLException {
		return thesaurusDAO.findByID(context, Thesaurus.class, uuid);
	}

	@Override
	public void updateLastModified(Context context, Thesaurus dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, Thesaurus dso) throws SQLException, AuthorizeException, IOException {
		thesaurusDAO.delete(context, dso);
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Thesaurus findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Thesaurus findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void deleteByWord(Context context, String word) throws SQLException {
		Thesaurus thesaurus = searchByWord(context, word);
		if (thesaurus != null) {
			thesaurusDAO.delete(context, thesaurus);
		}
	}
}
