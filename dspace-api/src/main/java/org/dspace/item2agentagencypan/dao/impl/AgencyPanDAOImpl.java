package org.dspace.item2agentagencypan.dao.impl;

import java.sql.SQLException;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.item2agentagencypan.Item2AgentAgencyPan;
import org.dspace.item2agentagencypan.dao.AgencyPanDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class AgencyPanDAOImpl extends AbstractHibernateDAO<Item2AgentAgencyPan>
        implements AgencyPanDAO {

    @Override
    public boolean existsByPan(Context context, String pan) throws SQLException {

        String hql =
                "FROM Item2AgentAgencyPan WHERE lower(pan) = lower(:pan)";

        Query<Item2AgentAgencyPan> query = getHibernateSession(context).createQuery(hql, Item2AgentAgencyPan.class);

        query.setParameter("pan", pan);

        return query.uniqueResult() != null;
    }
}