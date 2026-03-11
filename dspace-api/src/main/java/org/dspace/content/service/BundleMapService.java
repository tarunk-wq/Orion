package org.dspace.content.service;

import org.dspace.content.BundleMap;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for BundleMap
 *
 * This is the business logic layer
 * Controllers (or other services) will use this
 */
public interface BundleMapService {

    // Find by primary key
    BundleMap find(Context context, UUID id) throws SQLException;

    // Get all mappings under a root bundle
    List<BundleMap> findByBundle(Context context, String bundle) throws SQLException;

    // Validate if specific mapping exists
    boolean isValidMapping(Context context, String bundle, String parent, String child) throws SQLException;

    // Create new mapping
    BundleMap create(Context context, String bundle, String parent, String child) throws SQLException;

    // Delete mapping
    void delete(Context context, UUID id) throws SQLException;
}