package org.dspace.content.dao.impl;

import java.sql.SQLException;

import org.dspace.content.SourceToken;
import org.dspace.content.dao.SourceTokenDAO;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

/**
 * DAO Implementation for SourceToken
 *
 * Uses DSpace AbstractHibernateDAO to access Hibernate session.
 */
@Repository
public class SourceTokenDAOImpl extends AbstractHibernateDAO<SourceToken> implements SourceTokenDAO {

    @Override
    public SourceToken findBySource(Context context, String source) throws SQLException {

        String hql = "FROM SourceToken WHERE lower(source) = lower(:source)";

        Query<SourceToken> query = getHibernateSession(context).createQuery(hql, SourceToken.class);

        query.setParameter("source", source);

        return query.uniqueResult();
    }
}