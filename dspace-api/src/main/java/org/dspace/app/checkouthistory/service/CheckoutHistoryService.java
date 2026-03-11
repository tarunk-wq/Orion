package org.dspace.app.checkouthistory.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import org.dspace.app.checkouthistory.CheckoutHistory;
import org.dspace.content.Item;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface CheckoutHistoryService extends DSpaceObjectService<CheckoutHistory> {

	public CheckoutHistory checkout(Context context, Item item, EPerson ePerson, Date checkoutTime) throws SQLException;

	public void checkin(Context context, UUID itemId, UUID ePersonId, Date returnTime) throws SQLException;

	void deleteByEPerson(Context context, UUID ePersonId) throws SQLException;
}