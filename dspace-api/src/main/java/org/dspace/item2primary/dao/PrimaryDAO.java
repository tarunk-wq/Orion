package org.dspace.item2primary.dao;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.item2primary.Item2Primary;

/**
 * DAO for item2primary table
 */
public interface PrimaryDAO {

    /**
     * Insert primary mapping into DB
     * (maps primary_id + primary_type to item)
     */
    void create(Context context, String primaryId, String primaryType, Item item) throws SQLException;

    /**
     * Optional (for later use): check if primary already exists
     */
    boolean exists(Context context, String primaryId, String primaryType) throws SQLException;
    
    Item2Primary find(Context context, String primaryId, String primaryType) throws SQLException;
}