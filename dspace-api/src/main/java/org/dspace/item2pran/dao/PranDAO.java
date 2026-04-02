package org.dspace.item2pran.dao;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.item2pran.Item2Pran;

/**
 * DAO for item2pran table
 */
public interface PranDAO {

    // Check if PRAN already exists (used later for validation)
    boolean existsByPran(Context context, String pran) throws SQLException;

    // Insert PRAN entry into DB
    void create(Context context, String pran, Item item) throws SQLException;
    
    Item2Pran findByPran(Context context, String pran) throws SQLException;
}