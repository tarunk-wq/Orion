package org.dspace.content.service;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.authorize.AuthorizeException;

import java.sql.SQLException;

/*
 * Service to resolve correct working bundle inside an Item
 * Replaces UploadUtil.findBundleInItem()
 */
public interface BundleResolverService {

    /*
     * Determines the correct bundle (including hierarchy)
     * where the file should be uploaded
     */
    Bundle resolveWorkingBundle(Context context,String bundleName,String primary,Item item) throws SQLException, AuthorizeException;
}