package org.dspace.userbitstream.dao.impl;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.userbitstream.UserBitstream;
import org.dspace.userbitstream.dao.UserBitstreamDao;

import jakarta.persistence.Query;

public class UserBitstreamDaoImpl extends AbstractHibernateDSODAO<UserBitstream> implements UserBitstreamDao{

	@Override
	public void insert(Context context, EPerson eperson, Bitstream originalBistream, Bitstream annotatedBistream,
			Date creationDate, Item itemId) throws SQLException {
		create(context,new UserBitstream(eperson, originalBistream, annotatedBistream, creationDate,itemId));
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return count(createQuery(context, "SELECT count(*) FROM UserBitstream"));
	}

	@Override
	public List<UserBitstream> findByEperson(Context context, EPerson eperson, Bitstream originalBistream) throws SQLException {
		String hql = "FROM UserBitstream WHERE eperson = :eperson and originalBistream = :originalBistream";
		Query query = createQuery(context, hql).setParameter("eperson", eperson).setParameter("originalBistream", originalBistream);
		return list(query);
	}

	@Override
	public List<UserBitstream> findByItemId(Context context, Item itemId) throws SQLException {
		String hql = "FROM UserBitstream WHERE itemId = :itemId";
		Query query = createQuery(context, hql).setParameter("itemId", itemId);
		return list(query);
	}
	
	public UserBitstream findByLegacyId(Context context, int legacyId, Class<UserBitstream> clazz) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserBitstream> findByOriginalBistream(Context context, UUID bitreamId) throws SQLException {
		String hql = "FROM UserBitstream WHERE originalBistream.id = :bitreamId";
		Query query = createQuery(context, hql);
		query.setParameter("bitreamId", bitreamId);
		return list(query);
	}
	
	
	
}