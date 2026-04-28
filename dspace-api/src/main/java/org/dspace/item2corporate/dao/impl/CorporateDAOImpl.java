package org.dspace.item2corporate.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.item2corporate.Item2Corporate;
import org.dspace.item2corporate.dao.CorporateDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Implementation of CorporateDAO using Hibernate
 */
@Repository
public class CorporateDAOImpl extends AbstractHibernateDAO<Item2Corporate> implements CorporateDAO {

    /**
     * Check if CHO already exists
     */
    @Override
    public boolean existsByChoNo(Context context, String choNo) throws SQLException {

        String hql = "FROM Item2Corporate WHERE lower(choNo) = lower(:choNo)";

        Query<Item2Corporate> query = getHibernateSession(context).createQuery(hql, Item2Corporate.class);

        query.setParameter("choNo", choNo);

        return query.uniqueResult() != null;
    }

    /**
     * Insert CHO into item2corporate table
     */
    @Override
    public void create(Context context, String choNo, Item item) throws SQLException {

        // Create entity object
        Item2Corporate entity = new Item2Corporate();

        // Set CHO number
        entity.setChoNo(choNo);

        // Link to Item
        entity.setItem(item);

        // Save (replaces SQL insert)
        save(context, entity);
    }
    
    @Override
    public Item2Corporate findByChoNo(Context context, String choNo) throws SQLException {

        String hql = "FROM Item2Corporate WHERE lower(choNo) = lower(:choNo)";

        Query<Item2Corporate> query = getHibernateSession(context)
                .createQuery(hql, Item2Corporate.class);

        query.setParameter("choNo", choNo);

        return query.uniqueResult();
    }
}