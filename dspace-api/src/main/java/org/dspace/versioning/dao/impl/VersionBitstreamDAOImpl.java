/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.versioning.VersionBitstream;
import org.dspace.versioning.dao.VersionBitstreamDAO;

import jakarta.persistence.Query;


public class VersionBitstreamDAOImpl extends AbstractHibernateDAO<VersionBitstream> implements VersionBitstreamDAO
{
	protected VersionBitstreamDAOImpl()
	{
		super();
	}

	@Override
	public VersionBitstream findVersion(Context context, UUID bundle, UUID bitstream) throws SQLException
	{
		String hql = "FROM VersionBitstream  WHERE bundle.id = :bundle AND bitstream.id = :bitstream";
		Query query = createQuery(context, hql);
		query.setParameter("bundle", bundle);
		query.setParameter("bitstream", bitstream);
		
		List<VersionBitstream> versionList = list(query);
		
		if (versionList.size() < 1)
		{
			return null;
		}
		else
		{
			return versionList.get(0);
		}
	}

	@Override
	public VersionBitstream findParentVersion(Context context, UUID bitstream) throws SQLException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<VersionBitstream> findAllVersions(Context context, UUID bitstream) throws SQLException
	{
		
		return null;
	}
	
	@Override
	public List<VersionBitstream> findAllVersionInBundle(Context context, UUID bundle) throws SQLException{
		String hql = "FROM VersionBitstream  WHERE bundle.id = :bundle ORDER BY versionNumber DESC";
		
		Query query = createQuery(context, hql);
		query.setParameter("bundle", bundle);
		
		return list(query);
	}

	@Override
	public List<VersionBitstream> findAllActiveVersions(Context context, UUID bundle)
			throws SQLException
	{
		String hql = "FROM VersionBitstream  WHERE bundle.id = :bundle  AND activeVersion = true ORDER BY versionNumber DESC";
		
		Query query = createQuery(context, hql);
		query.setParameter("bundle", bundle);
		
		return list(query);
	}
	
	@Override
	public List<VersionBitstream> findAllVersions(Context context, UUID bundle, String bitstreamName)
			throws SQLException
	{
		String hql = "FROM VersionBitstream  WHERE bundle.id = :bundle AND bitstreamName = :bitstreamName AND deleted = false ORDER BY versionNumber DESC";
		
		Query query = createQuery(context, hql);
		query.setParameter("bundle", bundle);
		query.setParameter("bitstreamName", bitstreamName);
		
		return list(query);
	}

	@Override
	public VersionBitstream create(Context context, Bundle bundle, Bitstream bitstream, double versionNumber,
			EPerson ePerson, VersionBitstream parentVersion, boolean isActive, Boolean deleted) throws SQLException
	{
		return create(context, new VersionBitstream(bundle, bitstream, versionNumber, ePerson, parentVersion, isActive, deleted));
	}

	@Override
	public VersionBitstream findByLegacyId(Context context, int legacyId, Class<VersionBitstream> clazz)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public VersionBitstream findAllVersionsByBitstream(Context context, UUID bitstream) throws SQLException {

		String hql = "FROM VersionBitstream  WHERE bitstream.id = :bitstream";
		Query query = createQuery(context, hql);
		query.setParameter("bitstream", bitstream);
		return singleResult(query);
	}
	
	@Override
	public VersionBitstream create(Context context, Bundle bundle, Bitstream bitstream, double versionNumber,
			EPerson ePerson, VersionBitstream parentVersion, boolean isActive, boolean deleted, String comment, String name) throws SQLException
	{
		VersionBitstream versionBitstream = new VersionBitstream(bundle, bitstream, versionNumber, ePerson, parentVersion, isActive, deleted);
		versionBitstream.setComment(comment);
		versionBitstream.setBitstreamName(name);
		return create(context, versionBitstream);
	}
	
	@Override
	public List<VersionBitstream> findByBundle(Context context, UUID bundle) throws SQLException {
		String hql = "FROM VersionBitstream  WHERE bundle.id = :bundle";
		Query query = createQuery(context, hql);
		query.setParameter("bundle", bundle);
		return list(query);
	}
}
