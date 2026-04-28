package org.dspace.item2pran.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.item2pran.Item2Pran;
import org.dspace.item2pran.dao.PranDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Implementation of PranDAO using Hibernate
 */
@Repository
public class PranDAOImpl extends AbstractHibernateDAO<Item2Pran> implements PranDAO {

    /**
     * Check if PRAN already exists in DB
     */
    @Override
    public boolean existsByPran(Context context, String pran) throws SQLException {

        // HQL query to find PRAN
        String hql = "FROM Item2Pran WHERE lower(pran) = lower(:pran)";

        Query<Item2Pran> query = getHibernateSession(context).createQuery(hql, Item2Pran.class);

        query.setParameter("pran", pran);

        // If result exists → PRAN already present
        return query.uniqueResult() != null;
    }

    /**
     * Insert PRAN into item2pran table
     */
    @Override
    public void create(Context context, String pran, Item item) throws SQLException {

        // Create new entity object
        Item2Pran entity = new Item2Pran();

        // Set PRAN value
        entity.setPran(pran);

        // Link with Item
        entity.setItem(item);

        // Save using Hibernate (replaces SQL INSERT)
        save(context, entity);
    }
    
    @Override
    public Item2Pran findByPran(Context context, String pran) throws SQLException {

        String hql = "FROM Item2Pran WHERE lower(pran) = lower(:pran)";

        Query<Item2Pran> query = getHibernateSession(context)
                .createQuery(hql, Item2Pran.class);

        query.setParameter("pran", pran);

        return query.uniqueResult();
    }
}