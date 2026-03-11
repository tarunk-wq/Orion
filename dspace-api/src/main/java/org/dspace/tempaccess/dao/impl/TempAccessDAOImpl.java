package org.dspace.tempaccess.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.tempaccess.TempAccess;
import org.dspace.tempaccess.dao.TempAccessDAO;

public class TempAccessDAOImpl extends AbstractHibernateDSODAO<TempAccess> implements TempAccessDAO {

	@Override
	public TempAccess findByItemAndUser(Context context, Item item, EPerson eperson) throws SQLException {
		String hql = "FROM TempAccess t " + "JOIN FETCH t.item i " + "JOIN FETCH t.eperson e "
				+ "JOIN FETCH e.metadata " + "WHERE t.item = :item AND t.eperson = :eperson";
		Query query = createQuery(context, hql);
		query.setParameter("item", item);
		query.setParameter("eperson", eperson);
		return singleResult(query);
	}

	@Override
	public List<TempAccess> findByItem(Context context, Item item, int limit, int offset) throws SQLException {
		String hql = "FROM TempAccess t " + "JOIN FETCH t.item i " + "JOIN FETCH t.eperson e "
				+ "JOIN FETCH e.metadata " + "WHERE t.item = :item AND t.deleted = false AND t.endDate >= CURRENT_DATE";
		Query query = createQuery(context, hql);
		query.setParameter("item", item);
		query.setFirstResult(offset);
		query.setMaxResults(limit);
		return list(query);
	}

	@Override
	public TempAccess create(Context context, TempAccess tempAccess) throws SQLException {
		return super.create(context, tempAccess);
	}

	@Override
	public void delete(Context context, Item item, EPerson eperson) throws SQLException {
		TempAccess tempAccess = findByItemAndUser(context, item, eperson);
		if (tempAccess != null) {
			tempAccess.setDeleted(true);
			save(context, tempAccess);
		}
	}

	@Override
	public long countByItem(Context context, Item item) throws SQLException {
		String hql = "SELECT COUNT(t) FROM TempAccess t "
				+ "WHERE t.item = :item AND t.deleted = false AND t.endDate >= CURRENT_DATE";
		Query query = createQuery(context, hql);
		query.setParameter("item", item);
		Number count = (Number) query.getSingleResult();
		return count != null ? count.longValue() : 0L;
	}

	@Override
	public TempAccess find(Context context, UUID uuid) throws SQLException {
		String hql = "FROM TempAccess t " + "WHERE t.id = :uuid";
		Query query = createQuery(context, hql);
		query.setParameter("uuid", uuid);
		return singleResult(query);
	}
}