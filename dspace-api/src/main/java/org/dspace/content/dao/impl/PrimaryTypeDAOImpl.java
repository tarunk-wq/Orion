package org.dspace.content.dao.impl;

import java.sql.SQLException;

import org.dspace.content.PrimaryType;
import org.dspace.content.dao.PrimaryTypeDAO;
import org.springframework.stereotype.Repository;

import org.hibernate.query.Query;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;


@Repository
public class PrimaryTypeDAOImpl extends AbstractHibernateDAO<PrimaryType>
        implements PrimaryTypeDAO {

    @Override
    public boolean existsByPrimaryType(Context context, String primaryType)
            throws SQLException {

    	String hql = "FROM PrimaryType WHERE lower(primaryTypeName) = lower(:type)";

        Query<PrimaryType> query = getHibernateSession(context).createQuery(hql, PrimaryType.class);

        query.setParameter("type", primaryType);

        return query.uniqueResult() != null;
    }
}