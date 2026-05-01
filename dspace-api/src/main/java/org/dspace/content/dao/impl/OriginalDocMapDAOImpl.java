package org.dspace.content.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.dspace.core.Context;
import org.dspace.content.OriginalDocMap;
import org.dspace.content.dao.OriginalDocMapDAO;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

/*
 * DAO Implementation
 * Equivalent to:
 * DatabaseManager.insertNoSeq(...)
 */
@Repository
public class OriginalDocMapDAOImpl implements OriginalDocMapDAO {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public void create(Context context, OriginalDocMap map) throws SQLException {
		// Simply persist the mapping into DB
		entityManager.persist(map);
	}
}