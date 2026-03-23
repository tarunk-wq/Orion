package org.dspace.primarytype.dao.impl;

import java.sql.SQLException;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.primarytype.PrimaryType;
import org.dspace.primarytype.dao.PrimaryTypeDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;


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