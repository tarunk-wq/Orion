package org.dspace.app.checkouthistory.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.checkouthistory.CheckoutHistory;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;

import jakarta.persistence.Query;

public class CheckoutHistoryDAOImpl extends AbstractHibernateDSODAO<CheckoutHistory> implements CheckoutHistoryDAO {

	@Override
	public List<CheckoutHistory> findByItemAndUser(Context context, UUID itemId, UUID ePersonId) throws SQLException {
		String hql = "FROM CheckoutHistory WHERE item.id = :itemId AND ePerson.id = :ePersonId ORDER BY checkoutTime DESC";
		Query query = createQuery(context, hql)
				.setParameter("itemId", itemId)
				.setParameter("ePersonId", ePersonId);
		return list(query);
	}
	
	@Override
	public void deleteByEPerson(Context context, UUID ePersonId) throws SQLException {
	    String hql = "DELETE FROM CheckoutHistory ch WHERE ch.ePerson.id = :ePersonId";
	    Query query = createQuery(context, hql)
	            .setParameter("ePersonId", ePersonId);
	    query.executeUpdate();
	}

}
