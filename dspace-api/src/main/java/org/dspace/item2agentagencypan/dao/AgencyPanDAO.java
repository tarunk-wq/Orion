package org.dspace.item2agentagencypan.dao;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.item2agentagencypan.Item2AgentAgencyPan;

public interface AgencyPanDAO {

    boolean existsByPan(Context context, String pan) throws SQLException;
    void create(Context context, String pan, Item item) throws SQLException;
    Item2AgentAgencyPan findByPan(Context context, String pan) throws SQLException;

}