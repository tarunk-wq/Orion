package org.dspace.app.checkouthistory.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.app.checkouthistory.CheckoutHistory;
import org.dspace.app.checkouthistory.dao.CheckoutHistoryDAO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

public class CheckoutHistoryServiceImpl extends DSpaceObjectServiceImpl<CheckoutHistory> implements CheckoutHistoryService {

	@Autowired(required = true)
	protected CheckoutHistoryDAO checkoutHistoryDAO;
	
	@Override
	public CheckoutHistory find(Context context, UUID uuid) throws SQLException {
		return checkoutHistoryDAO.findByID(context, CheckoutHistory.class, uuid);
	}

	@Override
	public void updateLastModified(Context context, CheckoutHistory dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Context context, CheckoutHistory dso) throws SQLException, AuthorizeException, IOException {
		checkoutHistoryDAO.delete(context, dso);
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public CheckoutHistory checkout(Context context, Item item, EPerson ePerson, Date checkoutTime) throws SQLException {
		CheckoutHistory checkoutHistory = new CheckoutHistory();
		
		checkoutHistory.setItem(item);
		checkoutHistory.setePerson(ePerson);
		checkoutHistory.setCheckoutTime(checkoutTime);
		
		return checkoutHistoryDAO.create(context, checkoutHistory);
	}

	private List<CheckoutHistory> findByItemAndUser(Context context, UUID itemId, UUID ePersonId) throws SQLException {
		return checkoutHistoryDAO.findByItemAndUser(context, itemId, ePersonId);
	}

	@Override
	public void checkin(Context context, UUID itemId, UUID ePersonId, Date returnTime) throws SQLException {
		List<CheckoutHistory> checkoutHistoryList = findByItemAndUser(context, itemId, ePersonId);
		if (checkoutHistoryList.size() == 0) {
			throw new IllegalArgumentException("Checkout item first");
		}
		CheckoutHistory checkoutHistory = checkoutHistoryList.get(0);	
		checkoutHistory.setReturnTime(returnTime);
		
		checkoutHistoryDAO.save(context, checkoutHistory);
	}
	
	@Override
	public void deleteByEPerson(Context context, UUID ePersonId) throws SQLException {
		checkoutHistoryDAO.deleteByEPerson(context, ePersonId);
	}

}
