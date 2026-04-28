package org.dspace.item2pan.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.item2pan.Item2Pan;
import org.dspace.item2pan.dao.PanDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class PanDAOImpl extends AbstractHibernateDAO<Item2Pan> implements PanDAO {

    @Override
    public boolean existsByPan(Context context, String pan) throws SQLException {

        String hql = "FROM Item2Pan WHERE lower(pan) = lower(:pan)";

        Query<Item2Pan> query = getHibernateSession(context).createQuery(hql, Item2Pan.class);

        query.setParameter("pan", pan);

        return query.uniqueResult() != null;
    }
    
    @Override
    public void create(Context context, String pan, Item item) throws SQLException {

        Item2Pan entity = new Item2Pan();

        // Set PAN value
        entity.setPan(pan);

        // Set relationship with item
        entity.setItem(item);

        // Save using Hibernate
        save(context, entity);
    }
    
    @Override
    public Item2Pan findByPan(Context context, String pan) throws SQLException {

        // HQL query to fetch entity by PAN
        String hql = "FROM Item2Pan WHERE lower(pan) = lower(:pan)";

        Query<Item2Pan> query = getHibernateSession(context)
                .createQuery(hql, Item2Pan.class);

        query.setParameter("pan", pan);

        // Returns entity (or null if not found)
        return query.uniqueResult();
    }
}