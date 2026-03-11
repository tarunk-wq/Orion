/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.versioning.VersionBitstream;

public interface VersionBitstreamDAO extends DSpaceObjectDAO<VersionBitstream>, DSpaceObjectLegacySupportDAO<VersionBitstream>
{
	public VersionBitstream create(Context context, Bundle bundle, Bitstream bitstream, double versionNumber,
			EPerson ePerson, VersionBitstream parentVersion, boolean isActive, Boolean deleted) throws SQLException;
	
	public VersionBitstream findVersion(Context context, UUID bundle, UUID bitstream) throws SQLException;

	public VersionBitstream findParentVersion(Context context, UUID bitstream) throws SQLException;

	public List<VersionBitstream> findAllVersions(Context context, UUID bitstream) throws SQLException;

	public List<VersionBitstream> findAllVersions(Context context, UUID bundle, String bitstreamName)
			throws SQLException;

	public List<VersionBitstream> findAllVersionInBundle(Context context, UUID bundle) throws SQLException;

	public List<VersionBitstream> findAllActiveVersions(Context context, UUID bundle) throws SQLException;

	public VersionBitstream findAllVersionsByBitstream(Context context, UUID bitstream) throws SQLException;

	VersionBitstream create(Context context, Bundle bundle, Bitstream bitstream, double versionNumber, EPerson ePerson,
			VersionBitstream parentVersion, boolean isActive, boolean deleted, String comment, String name) throws SQLException;

	List<VersionBitstream> findByBundle(Context context, UUID bundle) throws SQLException;
}
