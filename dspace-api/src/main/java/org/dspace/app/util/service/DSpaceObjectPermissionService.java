/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util.service;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public interface DSpaceObjectPermissionService {
	DSpaceObject findDSpaceObject(Context context, UUID uuid) throws SQLException;
    boolean hasAdminPermission(Context context, DSpaceObject dso) throws Exception;
    DSpaceObject getTopLevelCommunity(Context context, DSpaceObject dso) throws Exception;
    DSpaceObject getSecondTopLevelCommunity(Context context, DSpaceObject dso) throws Exception;
}
