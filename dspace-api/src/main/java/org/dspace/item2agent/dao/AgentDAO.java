package org.dspace.item2agent.dao;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.item2agent.Item2Agent;

/**
 * DAO for item2agent table
 */
public interface AgentDAO {

    // Check if agent already exists
    boolean existsByAgentId(Context context, String agentId) throws SQLException;

    // Insert agent into DB
    void create(Context context, String agentId, Item item) throws SQLException;
    
    Item2Agent findByAgentId(Context context, String agentId) throws SQLException;
}