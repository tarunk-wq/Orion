package org.dspace.eperson.dao.impl;

import java.sql.SQLException;
import java.util.List;

import jakarta.persistence.Query;

import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.PasswordHistory;
import org.dspace.eperson.dao.PasswordHistoryDAO;

public class PasswordHistoryDAOImpl extends AbstractHibernateDSODAO<PasswordHistory> implements PasswordHistoryDAO {

    @Override
    public List<PasswordHistory> findByEPerson(Context context, EPerson eperson) throws SQLException {
    	Query query = createQuery(context, "FROM PasswordHistory WHERE eperson = :eperson ORDER BY creationDate DESC");
    	query.setParameter("eperson", eperson);
    	return list(query);
    }
}