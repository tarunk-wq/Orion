package org.dspace.item2corporate.dao;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.item2corporate.Item2Corporate;

/**
 * DAO for item2corporate table
 */
public interface CorporateDAO {

    // Check if CHO already exists
    boolean existsByChoNo(Context context, String choNo) throws SQLException;

    // Insert CHO into DB
    void create(Context context, String choNo, Item item) throws SQLException;
    
    Item2Corporate findByChoNo(Context context, String choNo) throws SQLException;
}