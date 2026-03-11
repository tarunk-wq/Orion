package org.dspace.thesaurus.dao.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.thesaurus.Thesaurus;
import org.dspace.thesaurus.dao.ThesaurusDAO;

import jakarta.persistence.Query;

public class ThesaurusDAOImpl extends AbstractHibernateDSODAO<Thesaurus> implements ThesaurusDAO {

	@Override
	public Thesaurus searchByWord(Context context, String word) throws SQLException {
		String hql = "FROM Thesaurus WHERE word= :word ORDER BY word";
		Query query = createQuery(context, hql);
		query.setParameter("word", word);
		
		return list(query).size() > 0 ? list(query).get(0) : null;
	}

	public List<String> searchByValueAndWord(Context context, String valueOrWord) throws SQLException {
		String hql = "SELECT t FROM Thesaurus t WHERE t.word= :word OR t.value LIKE :keyword ORDER BY word";
		Query query = createQuery(context, hql);
		query.setParameter("word", valueOrWord);
		query.setParameter("keyword", "%" + valueOrWord + "%");
		
		List<Thesaurus> list = list(query);
		List<String> finaStringList = new ArrayList<String>();
		if (list != null && !list.isEmpty()) {
			for (Thesaurus thesaurus : list) {
				String word = thesaurus.getWord();
				if (!finaStringList.contains(word.toLowerCase())) {
					finaStringList.add(word.toLowerCase());
				}
				
				String values = thesaurus.getValue();
				if (!values.isEmpty()) {
					String[] valueList = values.split(",");
					for (String value : valueList) {
						if (!finaStringList.contains(value.toLowerCase())) {
							finaStringList.add(value.toLowerCase());
						}
					}
				}
			}
		}
		
		return finaStringList;
	}
	
	@Override
	public List<Thesaurus> findAll(Context context) throws SQLException {
		String hql = "FROM Thesaurus ORDER BY word";
		Query query = createQuery(context, hql);
		
		return list(query);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return count(createQuery(context, "SELECT count(*) FROM Thesaurus"));
	}
}
