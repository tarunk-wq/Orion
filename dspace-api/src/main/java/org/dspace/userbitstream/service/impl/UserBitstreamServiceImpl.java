package org.dspace.userbitstream.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.userbitstream.UserBitstream;
import org.dspace.userbitstream.dao.UserBitstreamDao;
import org.dspace.userbitstream.service.UserBitstreamService;
import org.springframework.beans.factory.annotation.Autowired;

public class UserBitstreamServiceImpl extends DSpaceObjectServiceImpl<UserBitstream> implements UserBitstreamService{
	
	@Autowired(required = true)
	protected UserBitstreamDao userBitstreamDao;

	@Override
	public UserBitstream find(Context context, UUID uuid) throws SQLException {
		return userBitstreamDao.findByID(context, UserBitstream.class, uuid);
	}

	@Override
	public void updateLastModified(Context context, UserBitstream dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, UserBitstream dso) throws SQLException, AuthorizeException, IOException {
		userBitstreamDao.delete(context, dso);
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public UserBitstream findByIdOrLegacyId(Context context, String id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserBitstream findByLegacyId(Context context, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public List<UserBitstream> findByOriginalBistream(Context context, UUID bitreamId) throws SQLException{
		return userBitstreamDao.findByOriginalBistream(context, bitreamId);
	}

	@Override
	public void insert(Context context, EPerson eperson, Bitstream originalBistream, Bitstream annotatedBistream,
			Date creationDate, Item itemId) throws SQLException {
		userBitstreamDao.insert(context, eperson, originalBistream, annotatedBistream, creationDate, itemId);
	}

	@Override
	public int countRows(Context context) throws SQLException {
		return userBitstreamDao.countRows(context);
	}

	@Override
	public List<UserBitstream> findAll(Context context) throws SQLException {
		return userBitstreamDao.findAll(context, UserBitstream.class);
	}

	@Override
	public List<UserBitstream> findByEperson(Context context, EPerson eperson, Bitstream oldBitstream) throws SQLException {
		return userBitstreamDao.findByEperson(context,eperson,oldBitstream);
	}
	
	@Override
	public List<UserBitstream> findByItemId(Context context, Item item) throws SQLException {
		return userBitstreamDao.findByItemId(context,item);
	}
	
}