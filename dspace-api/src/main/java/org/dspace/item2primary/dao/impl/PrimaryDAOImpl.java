package org.dspace.item2primary.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.item2primary.Item2Primary;
import org.dspace.item2primary.dao.PrimaryDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Implementation of PrimaryDAO using Hibernate
 */
@Repository
public class PrimaryDAOImpl extends AbstractHibernateDAO<Item2Primary> implements PrimaryDAO {

    /**
     * Insert primary_id + primary_type into item2primary table
     */
    @Override
    public void create(Context context, String primaryId, String primaryType, Item item)
            throws SQLException {

        // Create entity object
        Item2Primary entity = new Item2Primary();

        // Set primary ID
        entity.setPrimaryId(primaryId);

        // Set primary type
        entity.setPrimaryType(primaryType);

        // Link to Item
        entity.setItem(item);

        // Save using Hibernate (replaces SQL insert)
        save(context, entity);
    }

    /**
     * Check if mapping already exists (used later for validation)
     */
    @Override
    public boolean exists(Context context, String primaryId, String primaryType)
            throws SQLException {

        String hql = "FROM Item2Primary WHERE lower(primaryId) = lower(:primaryId) "
                   + "AND lower(primaryType) = lower(:primaryType)";

        Query<Item2Primary> query =
                getHibernateSession(context).createQuery(hql, Item2Primary.class);

        query.setParameter("primaryId", primaryId);
        query.setParameter("primaryType", primaryType);

        return query.uniqueResult() != null;
    }
    
    @Override
    public Item2Primary find(Context context, String primaryId, String primaryType) throws SQLException {

        String hql = "FROM Item2Primary WHERE lower(primaryId) = lower(:primaryId) "
                   + "AND lower(primaryType) = lower(:primaryType)";

        Query<Item2Primary> query = getHibernateSession(context)
                .createQuery(hql, Item2Primary.class);

        query.setParameter("primaryId", primaryId);
        query.setParameter("primaryType", primaryType);

        return query.uniqueResult();
    }
}