package org.dspace.content;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.BundleMap;
import org.dspace.content.service.BundleResolverService;
import org.dspace.content.service.BundleService;
import org.dspace.content.upload.UploadResponse;
import org.dspace.content.service.BundleMapService;
import org.dspace.core.Context;
import org.dspace.authorize.AuthorizeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;

@Service
public class BundleResolverServiceImpl implements BundleResolverService {

    private static final Logger log = LoggerFactory.getLogger(BundleResolverServiceImpl.class);

    @Autowired
    private BundleService bundleService;

    @Autowired
    private BundleMapService bundleMapService;

    @Override
    public Bundle resolveWorkingBundle(Context context, String bundleName, String primary, Item dspaceItem)
            throws SQLException, AuthorizeException {

        // This will store the final bundle where file will be uploaded
        Bundle workingBundle = null;

        /*
         * STEP 1: Get bundle mapping from bundle_map table
         * (Replaces legacy SQL query)
         */
        BundleMap bundleMap = bundleMapService.findFirstByBundle(context, bundleName);

        // If no mapping found -> throw 404 error (same as legacy)
        if (bundleMap == null) {

            log.error("Doc Type = " + bundleName + " not exists!");

            UploadResponse uploadResp = new UploadResponse(null, UploadStatus.NOTFOUND);

            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity(uploadResp)
                            .type("application/json")
                            .build()
            );
        }

        // Get parent (base) and child bundle names from mapping
        String baseBundleName = bundleMap.getParentBundleName();
        String childBundleName = bundleMap.getChildBundleName();

        /*
         * STEP 2: PRIMARY LOGIC
         * If primary is provided -> create hierarchy under primary bundle
         */
        if (primary != null && !primary.isEmpty()) {

            // Try to find primary bundle inside item
            for (Bundle bndle : dspaceItem.getBundles()) {
                if (bndle.getName().equalsIgnoreCase(primary)) {
                    workingBundle = bndle;
                    break;
                }
            }

            // If primary bundle not found -> create it
            if (workingBundle == null) {
                workingBundle = bundleService.create(context, dspaceItem, primary);
            }

            /*
             * Now inside primary -> find or create base bundle
             */
            Bundle subBundle = findSubBundleByName(workingBundle, baseBundleName);

            if (subBundle == null) {
                // If not found -> create base bundle
                workingBundle = bundleService.createSubBundle(context, workingBundle, baseBundleName);
            } else {
                workingBundle = subBundle;
            }

        } else {

            /*
             * STEP 2 (NO PRIMARY CASE)
             * Directly work under item -> base bundle
             */

            // Find base bundle inside item
            for (Bundle bndle : dspaceItem.getBundles()) {
                if (bndle.getName().equalsIgnoreCase(baseBundleName)) {
                    workingBundle = bndle;
                    break;
                }
            }

            // If not found → create base bundle
            if (workingBundle == null) {
                workingBundle = bundleService.create(context, dspaceItem, baseBundleName);
            }
        }

        /*
         * STEP 3: FINAL CHILD BUNDLE
         * Inside base bundle -> find or create child bundle
         */
        Bundle requiredSubBundle = findSubBundleByName(workingBundle, childBundleName);

        if (requiredSubBundle == null) {
            // Create child bundle if not exists
            workingBundle = bundleService.createSubBundle(context, workingBundle, childBundleName);
        } else {
            workingBundle = requiredSubBundle;
        }

        // Return final bundle where file will be uploaded
        return workingBundle;
    }

    /*
     * Helper method to find a sub-bundle inside a parent bundle by name
     *
     * Replaces legacy:
     * workingBundle.findSubBundleByName(...)
     */
    private Bundle findSubBundleByName(Bundle parent, String name) {

        // If parent has no sub-bundles, return null
        if (parent.getSubBundles() == null) {
            return null;
        }

        // Loop through all sub-bundles of the parent
        for (Bundle b : parent.getSubBundles()) {

            // Compare names (ignore case for safety)
            if (b.getName().equalsIgnoreCase(name)) {
                return b;  // Found matching sub-bundle
            }
        }

        // If no matching sub-bundle found, return null
        return null;
    }
}