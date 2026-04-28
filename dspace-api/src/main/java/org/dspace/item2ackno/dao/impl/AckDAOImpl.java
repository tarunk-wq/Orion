package org.dspace.item2ackno.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.item2ackno.Item2AckNo;
import org.dspace.item2ackno.dao.AckDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class AckDAOImpl extends AbstractHibernateDAO<Item2AckNo> implements AckDAO {

    @Override
    public boolean existsByAckNo(Context context, String ackNo) throws SQLException {

        String hql = "FROM Item2AckNo WHERE lower(ackNo) = lower(:ackNo)";
        Query<Item2AckNo> query = getHibernateSession(context).createQuery(hql, Item2AckNo.class);

        query.setParameter("ackNo", ackNo);

        return query.uniqueResult() != null;
    }

    @Override
    public void create(Context context, String ackNo, Item item) throws SQLException {

        Item2AckNo entity = new Item2AckNo();

        entity.setAckNo(ackNo);
        entity.setItem(item);

        save(context, entity);
    }
    
    @Override
    public Item2AckNo findByAckNo(Context context, String ackNo) throws SQLException {

        String hql = "FROM Item2AckNo WHERE lower(ackNo) = lower(:ackNo)";

        Query<Item2AckNo> query = getHibernateSession(context)
                .createQuery(hql, Item2AckNo.class);

        query.setParameter("ackNo", ackNo);

        return query.uniqueResult();
    }
}