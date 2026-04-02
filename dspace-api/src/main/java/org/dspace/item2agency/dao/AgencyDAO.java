package org.dspace.item2agency.dao;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.item2agency.Item2Agency;

/**
 * DAO for item2agency table
 */
public interface AgencyDAO {

    // Check if agency already exists
    boolean existsByAgencyId(Context context, String agencyId) throws SQLException;

    // Insert agency into DB
    void create(Context context, String agencyId, Item item) throws SQLException;
    
    Item2Agency findByAgencyId(Context context, String agencyId) throws SQLException;
}