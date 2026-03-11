package org.dspace.userbitstream.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.userbitstream.UserBitstream;

public interface UserBitstreamDao extends DSpaceObjectDAO<UserBitstream>, DSpaceObjectLegacySupportDAO<UserBitstream>{
	
	public void insert (Context context, EPerson eperson, Bitstream originalBistream, Bitstream annotatedBistream, Date creationDate, Item itemId) throws SQLException;
	
	int countRows(Context context) throws SQLException;

	public List<UserBitstream> findByEperson(Context context, EPerson eperson, Bitstream oldBitstream) throws SQLException;

	public List<UserBitstream> findByItemId(Context context, Item item) throws SQLException;

	public List<UserBitstream> findByOriginalBistream(Context context, UUID bitreamId)  throws SQLException;
}