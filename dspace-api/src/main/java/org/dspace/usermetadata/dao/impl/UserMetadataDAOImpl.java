/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usermetadata.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.dao.UserMetadataFieldsDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for
 * the EPerson object. This class is responsible for all database calls for the
 * EPerson object and is autowired by Spring. This class should never be
 * accessed directly.
 *
 * @author virsoftech.com
 */
public class UserMetadataDAOImpl extends AbstractHibernateDAO<UserMetadataFields> implements UserMetadataFieldsDAO {

	@Override
	public List<UserMetadataFields> findAll(Context context, Class<UserMetadataFields> clazz) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserMetadataFields> findAll(Context context, Class<UserMetadataFields> clazz, Integer limit,
			Integer offset) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserMetadataFields findUnique(Context context, String query) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public UserMetadataFields findByID(Context context, Class clazz, int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public UserMetadataFields findByID(Context context, Class clazz, UUID id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserMetadataFields> findMany(Context context, String query) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void create(Context context, String userFieldName, String systemFieldName, Integer metadataFieldType,
			Integer metadataFieldPosition, String subCommHandle) throws SQLException {
		UserMetadataFields userMetadataFields = new UserMetadataFields();

		userMetadataFields.setUserFieldName(userFieldName);
		userMetadataFields.setSystemFieldName(systemFieldName);
		userMetadataFields.setFieldType(metadataFieldType);
		userMetadataFields.setFieldPosition(metadataFieldPosition);
		userMetadataFields.setSubDeptCommunityHandle(subCommHandle);
	    userMetadataFields.setIsUniqueMetadata(false); 

		create(context, userMetadataFields);
	}
	
	@Override
	public void create(Context context, UserMetadataFields userMetadataField, String subCommHandle) throws SQLException {
		userMetadataField.setSubDeptCommunityHandle(subCommHandle);
		create(context, userMetadataField);
	}

	@Override
	public List<UserMetadataFields> findAll(Context context) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserMetadataFields findById(Context context, int metadataId) throws SQLException {
		// TODO Auto-generated method stub
		String queryStr = "SELECT umf FROM UserMetadataFields umf WHERE umf.id = :metadataId";

		Query query = createQuery(context, queryStr);
		query.setParameter("metadataId", metadataId);

		return (UserMetadataFields) query.getSingleResult();
	}

	@Override
	public UserMetadataFields findByUserFieldName(Context context, String userFieldName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserMetadataFields findBySystemFieldName(Context context, String systemFieldName) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserMetadataFields> findBySubDeptHandle(Context context, String subDeptHandle) throws SQLException {
		// TODO Auto-generated method stub
		String queryStr = "From UserMetadataFields where subDeptCommunityHandle =:sub_dept_community_handle";
		Query query = createQuery(context, queryStr);
		query.setParameter("sub_dept_community_handle", subDeptHandle);
		
		return list(query);
	}

	@Override
	public List<UserMetadataFields> findDocumentMetadataBySubDeptHandle(Context context, String subDeptHandle)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserMetadataFields findBySystemFieldName(Context context, String subDeptHandle, String systemFieldName)
			throws SQLException {
		// TODO Auto-generated method stub
		String queryStr = "From UserMetadataFields where subDeptCommunityHandle =:sub_dept_community_handle AND systemFieldName =:system_field_name";
		Query query = createQuery(context, queryStr);
		query.setParameter("sub_dept_community_handle", subDeptHandle);
		query.setParameter("system_field_name", systemFieldName);
		
		return list(query).get(0);	
		}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> findAllSubDepartmentHandle(Context context) throws SQLException {
		String queryStr = "SELECT DISTINCT umf.subDeptCommunityHandle FROM UserMetadataFields umf";
		Query query = createQuery(context, queryStr);
		return query.getResultList();
	}
	
	@Override
	public List<UserMetadataFields> getUniqueMetadataFieldsBySubDept(Context context, String subDeptHandle) throws SQLException{
		String queryStr = "SELECT umf FROM UserMetadataFields umf WHERE subDeptCommunityHandle =:sub_dept_community_handle AND isUniqueMetadata = true";
		Query query = createQuery(context, queryStr);
		query.setParameter("sub_dept_community_handle", subDeptHandle);
		return query.getResultList();
	}

}
