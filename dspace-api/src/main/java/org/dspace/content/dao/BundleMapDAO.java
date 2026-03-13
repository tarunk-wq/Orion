package org.dspace.content.dao;

import org.dspace.content.BundleMap;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * DAO interface for BundleMap entity.
 *
 * Responsible only for database operations.
 */
public interface BundleMapDAO {

    // Find by primary key
    BundleMap find(Context context, UUID id) throws SQLException;

    // Find all mappings for a specific root bundle
    List<BundleMap> findByBundle(Context context, String bundle) throws SQLException;

    // Find mapping using full 3-level match
    BundleMap findByBundleParentChild(Context context, String bundle, String parent, String child) throws SQLException;

    // Save new mapping
    BundleMap create(Context context, BundleMap bundleMap) throws SQLException;

    // Delete mapping
    void delete(Context context, BundleMap bundleMap) throws SQLException;
}