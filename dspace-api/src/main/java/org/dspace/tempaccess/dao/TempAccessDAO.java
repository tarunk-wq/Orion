package org.dspace.tempaccess.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.tempaccess.TempAccess;

public interface TempAccessDAO extends DSpaceObjectDAO<TempAccess> {
	TempAccess findByItemAndUser(Context context, Item item, EPerson eperson) throws SQLException;

	List<TempAccess> findByItem(Context context, Item item, int limit, int offset) throws SQLException;

	TempAccess create(Context context, TempAccess access) throws SQLException;

	public void delete(Context context, Item item, EPerson eperson) throws SQLException;

	long countByItem(Context context, Item item) throws SQLException;

	TempAccess find(Context context, UUID uuid) throws SQLException;
}