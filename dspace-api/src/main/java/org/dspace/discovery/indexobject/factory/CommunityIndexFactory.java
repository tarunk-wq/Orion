/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableCommunity;

/**
 * Factory interface for indexing/retrieving communities in the search core
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface CommunityIndexFactory extends DSpaceObjectIndexFactory<IndexableCommunity, Community> {
	
	public List<String> getCommunityLocations(Context context, Community community) throws SQLException;

}