package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;

import org.dspace.content.Community;
import org.dspace.content.Department;
import org.dspace.content.dao.DepartmentDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;

public class DepartmentDAOImpl extends AbstractHibernateDSODAO<Department> implements DepartmentDAO {

	@Override
	public Department findByCommunity(Context context, Community community) throws SQLException {
		String hql = "FROM Department d WHERE d.community = :community";
		Query query = createQuery(context, hql);
		query.setParameter("community", community);
		return singleResult(query);
	}

	@Override
	public Department findByName(Context context, String name) throws SQLException {
		String queryStr = "From Department WHERE LOWER(departmentName) = :departmentName";
		Query query = createQuery(context, queryStr);
		query.setParameter("departmentName", name);
		
		return singleResult(query);
	}

	@Override
	public List<Department> findAll(Context context) throws SQLException {
		String queryStr = "From Department";
		Query query = createQuery(context, queryStr);
		
		return list(query);
	}

	@Override
	public Department findByDepartmentAbbreviation(Context context, String departmentAbbr) throws SQLException {
		String queryStr = "From Department where abbreviation =:departmentAbbr";
		Query query = createQuery(context, queryStr);
		query.setParameter("departmentAbbr", departmentAbbr);
		
		return uniqueResult(query);
	}

	@Override
	public Department findByUuid(Context context, UUID uuid) throws SQLException {
		String queryStr = "From Department d where d.id =:uuid";
		Query query = createQuery(context, queryStr);
		query.setParameter("uuid", uuid);
		
		return uniqueResult(query); 
	}
}
