package org.dspace.item2agent.dao.impl;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.item2agent.Item2Agent;
import org.dspace.item2agent.dao.AgentDAO;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

/**
 * Implementation of AgentDAO using Hibernate
 */
@Repository
public class AgentDAOImpl extends AbstractHibernateDAO<Item2Agent> implements AgentDAO {

    /**
     * Check if agent ID already exists
     */
    @Override
    public boolean existsByAgentId(Context context, String agentId) throws SQLException {

        String hql = "FROM Item2Agent WHERE lower(agentId) = lower(:agentId)";

        Query<Item2Agent> query = getHibernateSession(context).createQuery(hql, Item2Agent.class);

        query.setParameter("agentId", agentId);

        return query.uniqueResult() != null;
    }

    /**
     * Insert agent into item2agent table
     */
    @Override
    public void create(Context context, String agentId, Item item) throws SQLException {

        // Create entity object
        Item2Agent entity = new Item2Agent();

        // Set agent ID
        entity.setAgentId(agentId);

        // Link to Item
        entity.setItem(item);

        // Save (replaces SQL insert)
        save(context, entity);
    }
    
    @Override
    public Item2Agent findByAgentId(Context context, String agentId) throws SQLException {

        String hql = "FROM Item2Agent WHERE lower(agentId) = lower(:agentId)";

        Query<Item2Agent> query = getHibernateSession(context)
                .createQuery(hql, Item2Agent.class);

        query.setParameter("agentId", agentId);

        return query.uniqueResult();
    }
}