package org.dspace.userbitstream.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.userbitstream.UserBitstream;

public interface UserBitstreamService extends DSpaceObjectService<UserBitstream>,DSpaceObjectLegacySupportService<UserBitstream>{
	
	public void insert (Context context, EPerson eperson, Bitstream originalBistream, Bitstream annotatedBistream, Date creationDate, Item itemId) throws SQLException;
	
	int countRows(Context context) throws SQLException;
	
	public List<UserBitstream> findAll(Context context) throws SQLException;
	
	public List<UserBitstream> findByEperson(Context context, EPerson epeson, Bitstream oldBitstream) throws SQLException;

	List<UserBitstream> findByItemId(Context context, Item item) throws SQLException;

	List<UserBitstream> findByOriginalBistream(Context context, UUID bistreamId) throws SQLException;
}