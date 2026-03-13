package org.dspace.content.dao.impl;

import org.dspace.content.BundleMap;
import org.dspace.content.dao.BundleMapDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Hibernate implementation of BundleMapDAO
 *
 * DAO Layer Responsibility:
 * - Directly communicate with database
 * - Execute HQL queries
 * - No business logic here (only in service)
 *
 * 
 * We extend AbstractHibernateDAO (not AbstractHibernateDSODAO)
 * because BundleMap is not a DSpaceObject
 */
public class BundleMapDAOImpl extends AbstractHibernateDAO<BundleMap> implements BundleMapDAO {

    /**
     * Protected constructor (Spring creates the bean)
     */
    protected BundleMapDAOImpl() {
        super();
    }

    /**
     * Find a BundleMap entry using its UUID (primary key)
     */
    @Override
    public BundleMap find(Context context, UUID id) throws SQLException {

        // findByID() is provided by AbstractHibernateDAO
        return findByID(context, BundleMap.class, id);
    }

    /**
     * Get all mappings under a specific root bundle
     *
     * Example:
     * If bundle = "A"
     * will return all rows where bundle column is "A"
     */
    @Override
    public List<BundleMap> findByBundle(Context context,
                                        String bundle) throws SQLException {

        // HQL (Hibernate Query Language)
        // We use entity name (BundleMap)
        // not table name (bundle_map)
        return createQuery(context,
                "FROM BundleMap WHERE bundle = :bundle")
                .setParameter("bundle", bundle)
                .getResultList();
    }

    /**
     * Find specific mapping using 3-level match:
     * bundle + parent + child
     *
     * useful for validation.
     */
    @Override
    public BundleMap findByBundleParentChild(Context context, String bundle, String parent, String child) throws SQLException {

        List<BundleMap> result = createQuery(context,
                "FROM BundleMap WHERE bundle = :bundle " + "AND parentBundleName = :parent " + "AND childBundleName = :child")
                .setParameter("bundle", bundle)
                .setParameter("parent", parent)
                .setParameter("child", child)
                .getResultList();

        // If no result found, return null
        return result.isEmpty() ? null : result.get(0);
    }


}