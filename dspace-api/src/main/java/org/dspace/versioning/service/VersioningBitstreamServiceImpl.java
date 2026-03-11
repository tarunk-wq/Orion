/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.userbitstream.UserBitstream;
import org.dspace.userbitstream.service.UserBitstreamService;
import org.dspace.versioning.VersionBitstream;
import org.dspace.versioning.dao.VersionBitstreamDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *  	   After creating a version of a bitstream we need to call update
 *         on the respective item. while updating the item, sequence id is also
 *         provided to all the bitstreams present in all the bundles of the
 *         item.
 */
public class VersioningBitstreamServiceImpl extends DSpaceObjectServiceImpl<VersionBitstream> implements VersioningBitstreamService
{
	@Autowired(required = true)
	private VersionBitstreamDAO versionBitstreamDAO;
	
	@Autowired
	private AuditTrailService auditTrailService;
	
	@Autowired
	private  UserBitstreamService userBitstreamservice;

	@Override
	public VersionBitstream createVersion(Context context, EPerson ePerson, Bundle bundle, Bitstream olderBitstream,
			Bitstream newBitstream) throws SQLException, AuthorizeException
	{
		// creating version entries if not already present.
		VersionBitstream olderVersion = findVersion(context, bundle, olderBitstream);
		
		if (olderVersion == null)
		{
			// if version of older version is not already present then this is the first entry.
			olderVersion = versionBitstreamDAO.create(context, bundle, olderBitstream, 1.0, ePerson, null, false, false);
		}
		else
		{
			olderVersion.setActiveVersion(false);
		}
		double bitstreamVersion = olderVersion.getVersionNumber() + 1;
		VersionBitstream newVersion =
				versionBitstreamDAO.create(context, bundle, newBitstream, bitstreamVersion, ePerson, olderVersion, true, false);
		
		// remove older bitstream link to bundle.
		bundle.removeBitstream(olderBitstream);
		olderBitstream.getBundles().remove(bundle);
		
		// Update user bitstream table with new version id if entry is present  
		updateAnnotationBitstream(context,olderVersion.getBitstream(),newVersion);
		
		// setting old bitstream sequence id as new bitstreams sequence id.
		newBitstream.setSequenceID(olderBitstream.getSequenceID());
				
		return newVersion;
	}
	
	private void updateAnnotationBitstream(Context context,Bitstream oldBitstreamVersion, VersionBitstream newVersionBitstream) throws SQLException {
		List<UserBitstream> allUsersAnnotations = userBitstreamservice.findByOriginalBistream(context,oldBitstreamVersion.getID()) ;
		for(UserBitstream entry  : allUsersAnnotations) {
			entry.setOriginalBistream(newVersionBitstream.getBitstream());
		}
	}

	@Override
	public VersionBitstream createFirstVersion(Context context, EPerson ePerson, Bundle bundle, Bitstream newBitstream, String comment, String name)
			throws SQLException, AuthorizeException {

		VersionBitstream newVersion =
				versionBitstreamDAO.create(context, bundle, newBitstream, 1.0, ePerson, null, true, false, comment, name);
		
		return newVersion;
	}

	@Override
	public VersionBitstream findVersion(Context context, Bundle bundle, Bitstream bitstream) throws SQLException
	{
		return versionBitstreamDAO.findVersion(context, bundle.getID(), bitstream.getID());
	}

	@Override
	public VersionBitstream findParentVersion(Context context, Bitstream bitstream) throws SQLException 
	{
		return versionBitstreamDAO.findParentVersion(context, bitstream.getID());
	}

	@Override
	public List<VersionBitstream> findAllVersions(Context context, UUID bitstreamID) throws SQLException {
		VersionBitstream versionBitstream = versionBitstreamDAO.findAllVersionsByBitstream(context, bitstreamID);
		List<VersionBitstream> resultList = new ArrayList<>();

		if (versionBitstream != null) {
			resultList.add(versionBitstream);

			while (versionBitstream.getParentVersion() != null) {
				VersionBitstream parentVersion = versionBitstream.getParentVersion();
				versionBitstream = parentVersion;
				resultList.add(versionBitstream);
			}
		} else {
			return resultList;
		}
		
		Comparator<VersionBitstream> titleComparator 
	     = (c1, c2) -> c1.getVersionDate().compareTo(c2.getVersionDate()); 
		
	    resultList.sort(titleComparator);
	    
	    // audittrail entry
        auditTrailService.insert(context, versionBitstream.getBitstream().getHandle(), "RETRIEVAL", "Getting all versions of document");
	
		return resultList;
	}

	@Override
	public List<VersionBitstream> findAllVersions(Context context, Bundle bundle, String bitstreamName)
			throws SQLException 
	{
		return versionBitstreamDAO.findAllVersions(context, bundle.getID(), bitstreamName);
	}

	@Override
	public void deteleVersionBitstream(Context context, Bundle bundle, Bitstream bitstream) throws SQLException
	{
		List<VersionBitstream> bitVersions = findAllVersions(context, bitstream.getID());
		//List<VersionBitstream> bitVersions = findAllVersions(context, bundle, bitstream.getID());
		if (bitVersions.size() > 0)
		{
			for (VersionBitstream versionBitstream : bitVersions)
			{
				versionBitstreamDAO.delete(context, versionBitstream);
			}
		}
		else
		{ // assuming this delete call is for the item created for the versioning of bitstream.
			
			VersionBitstream versionBitstream = findVersion(context, bundle, bitstream);
			
			if (versionBitstream != null)
			{
				versionBitstreamDAO.delete(context, versionBitstream);
			}
		}
	}
	
	@Override
	public void deteleVersionBitstream(Context context, Bundle bundle) throws SQLException {
		
		List<VersionBitstream> bitVersions = versionBitstreamDAO.findAllVersionInBundle(context, bundle.getID());
		
		if (bitVersions.size() > 0)
		{
			for (VersionBitstream versionBitstream : bitVersions)
			{
				versionBitstreamDAO.delete(context, versionBitstream);
			}
		}
	}

	@Override
	public VersionBitstream findVersion(Context context, int versionId) throws SQLException
	{
		return versionBitstreamDAO.findByID(context, VersionBitstream.class, versionId);
	}

	@Override
	public List<VersionBitstream> findAllVersionInBundle(Context context, Bundle bundle) throws SQLException {
	
		List<VersionBitstream> bitVersions = versionBitstreamDAO.findAllVersionInBundle(context, bundle.getID());
		return bitVersions;
	}

	@Override
	public VersionBitstream find(Context context, UUID uuid) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLastModified(Context context, VersionBitstream dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, VersionBitstream dso) throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public VersionBitstream findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VersionBitstream findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<VersionBitstream> findAllActiveVersions(Context context, Bundle bundle) throws SQLException {
	
		return versionBitstreamDAO.findAllActiveVersions(context, bundle.getID());
	}

	@Override
	public VersionBitstream findByBitstream(Context context, Bitstream bitstream) throws SQLException {
		
		return versionBitstreamDAO.findAllVersionsByBitstream(context, bitstream.getID());
	}
	
	@Override
	public VersionBitstream createVersion(Context context, EPerson ePerson, Bundle bundle, Bitstream olderBitstream,
			Bitstream newBitstream, String comment, String name) throws SQLException, AuthorizeException
	{
		// creating version entries if not already present.
		VersionBitstream olderVersion = findVersion(context, bundle, olderBitstream);
		
		if (olderVersion == null)
		{
			// if version of older version is not already present then this is the first entry.
			olderVersion = versionBitstreamDAO.create(context, bundle, olderBitstream, 1.0, ePerson, null, false, false, comment, name);
		}
		else
		{
			olderVersion.setActiveVersion(false);
		}
		double bitstreamVersion = olderVersion.getVersionNumber() + 1;
		VersionBitstream newVersion =
				versionBitstreamDAO.create(context, bundle, newBitstream, bitstreamVersion, ePerson, olderVersion, true, false, comment, name);
		
		// remove older bitstream link to bundle.
		bundle.removeBitstream(olderBitstream);
		olderBitstream.getBundles().remove(bundle);
		
		// Update user bitstream table with new version id if entry is present  
		updateAnnotationBitstream(context,olderVersion.getBitstream(),newVersion);
		
		// setting old bitstream sequence id as new bitstreams sequence id.
		newBitstream.setSequenceID(olderBitstream.getSequenceID());
				
		return newVersion;
	}
	
	@Override
	public List<VersionBitstream> findByBundle(Context context, Bundle bundle) throws SQLException {
		return versionBitstreamDAO.findByBundle(context, bundle.getID());
	}
}
