package org.dspace.item2agency.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.item2agency.Item2Agency;
import org.dspace.item2agency.dao.AgencyDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Implementation of AgencyDAO using Hibernate
 */
@Repository
public class AgencyDAOImpl extends AbstractHibernateDAO<Item2Agency> implements AgencyDAO {

    /**
     * Check if agency ID already exists
     */
    @Override
    public boolean existsByAgencyId(Context context, String agencyId) throws SQLException {

        String hql = "FROM Item2Agency WHERE lower(agencyId) = lower(:agencyId)";

        Query<Item2Agency> query = getHibernateSession(context).createQuery(hql, Item2Agency.class);

        query.setParameter("agencyId", agencyId);

        return query.uniqueResult() != null;
    }

    /**
     * Insert agency into item2agency table
     */
    @Override
    public void create(Context context, String agencyId, Item item) throws SQLException {

        // Create entity object
        Item2Agency entity = new Item2Agency();

        // Set agency ID
        entity.setAgencyId(agencyId);

        // Link to Item
        entity.setItem(item);

        // Save (replaces SQL insert)
        save(context, entity);
    }
    
    @Override
    public Item2Agency findByAgencyId(Context context, String agencyId) throws SQLException {

        String hql = "FROM Item2Agency WHERE lower(agencyId) = lower(:agencyId)";

        Query<Item2Agency> query = getHibernateSession(context)
                .createQuery(hql, Item2Agency.class);

        query.setParameter("agencyId", agencyId);

        return query.uniqueResult();
    }
}