package org.dspace.app.checkouthistory.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.checkouthistory.CheckoutHistory;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.core.Context;

public interface CheckoutHistoryDAO extends DSpaceObjectDAO<CheckoutHistory>{

	public List<CheckoutHistory> findByItemAndUser(Context context, UUID itemId, UUID ePersonId) throws SQLException;

	void deleteByEPerson(Context context, UUID ePersonId) throws SQLException;

}
