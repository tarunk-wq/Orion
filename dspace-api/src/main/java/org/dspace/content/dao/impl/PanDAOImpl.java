package org.dspace.content.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Item2Pan;
import org.dspace.content.dao.PanDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.springframework.stereotype.Repository;

import org.hibernate.query.Query;

@Repository
public class PanDAOImpl extends AbstractHibernateDAO<Item2Pan> implements PanDAO {

    @Override
    public boolean existsByPan(Context context, String pan) throws SQLException {

        String hql = "FROM Item2Pan WHERE lower(pan) = lower(:pan)";

        Query<Item2Pan> query = getHibernateSession(context).createQuery(hql, Item2Pan.class);

        query.setParameter("pan", pan);

        return query.uniqueResult() != null;
    }
}