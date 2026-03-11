/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.versioning.VersionBitstream;

public interface VersioningBitstreamService extends DSpaceObjectService<VersionBitstream>, DSpaceObjectLegacySupportService<VersionBitstream>
{
	
	public VersionBitstream findVersion(Context context, int versionId) throws SQLException;
	
	public VersionBitstream findVersion(Context context, Bundle bundle, Bitstream bitstream) throws SQLException;
	
	public VersionBitstream findParentVersion(Context context, Bitstream bitstream) throws SQLException;
    
    public List<VersionBitstream> findAllVersions(Context context, UUID bitstreamID) throws SQLException;
    
    public List<VersionBitstream> findAllVersions(Context context, Bundle bundle, String bitstreamName) throws SQLException;
    
    /**
     * This method deletes all the versions of this bitstream associated with the provided bundle.
     * 
     * @param context
     * @param bundle
     * @param bitstream
     */
    public void deteleVersionBitstream(Context context, Bundle bundle, Bitstream bitstream) throws SQLException;

    public void deteleVersionBitstream(Context context, Bundle bundle) throws SQLException;

	public VersionBitstream createVersion(Context context, EPerson ePerson, Bundle bundle, Bitstream olderBitstream,
			Bitstream newBitstream) throws SQLException, AuthorizeException;
	
	public List<VersionBitstream> findAllVersionInBundle(Context context, Bundle bundle) throws SQLException;
	
	public List<VersionBitstream> findAllActiveVersions(Context context, Bundle bundle) throws SQLException;

	public VersionBitstream findByBitstream(Context context, Bitstream bitstream) throws SQLException;

	VersionBitstream createFirstVersion(Context context, EPerson ePerson, Bundle bundle, Bitstream newBitstream,
			String comment, String name) throws SQLException, AuthorizeException;

	VersionBitstream createVersion(Context context, EPerson ePerson, Bundle bundle, Bitstream olderBitstream,
			Bitstream newBitstream, String comment, String name) throws SQLException, AuthorizeException;

	List<VersionBitstream> findByBundle(Context context, Bundle bundle) throws SQLException;

}
