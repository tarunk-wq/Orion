package org.dspace.content;

import org.dspace.content.dao.BundleMapDAO;
import org.dspace.content.service.BundleMapService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Service Implementation for BundleMap.
 *
 * Service Layer Responsibility:
 * - Contains business logic
 * - Calls DAO layer
 * - Does not directly access database
 *
 * This class is managed by Spring.
 */
public class BundleMapServiceImpl implements BundleMapService {

    /**
     * DAO is injected by Spring.
     * Spring reads XML config and wires this automatically.
     */
    @Autowired
    protected BundleMapDAO bundleMapDAO;

    protected BundleMapServiceImpl() {
    }

    /**
     * Find mapping using UUID.
     */
    @Override
    public BundleMap find(Context context, UUID id) throws SQLException {

        return bundleMapDAO.find(context, id);
    }

    /**
     * Get all mappings for a specific root bundle.
     */
    @Override
    public List<BundleMap> findByBundle(Context context, String bundle) throws SQLException {

        return bundleMapDAO.findByBundle(context, bundle);
    }

    /**
     * Validate whether a specific mapping exists
     *
     * Returns true if row exists,
     * else false
     */
    @Override
    public boolean isValidMapping(Context context, String bundle, String parent, String child) throws SQLException {

        return bundleMapDAO.findByBundleParentChild(context, bundle, parent, child) != null;
    }

    /**
     * Create new logical mapping entry.
     *
     * This method prepares entity object,
     * then sends it to DAO 
     */
    @Override
    public BundleMap create(Context context, String bundle, String parent, String child) throws SQLException {

        BundleMap map = new BundleMap();

        map.setBundle(bundle);
        map.setParentBundleName(parent);
        map.setChildBundleName(child);

        return bundleMapDAO.create(context, map);
    }

    /**
     * Delete mapping using UUID
     */
    @Override
    public void delete(Context context, UUID id) throws SQLException {

        BundleMap map = bundleMapDAO.find(context, id);

        if (map != null) {
            bundleMapDAO.delete(context, map);
        }
    }
}