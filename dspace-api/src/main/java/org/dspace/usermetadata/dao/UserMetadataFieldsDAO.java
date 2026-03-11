/**
 * 
 */
package org.dspace.usermetadata.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.usermetadata.UserMetadataFields;

/**
 * @author sumanta
 *
 */
public interface UserMetadataFieldsDAO extends GenericDAO<UserMetadataFields>{

	public void create(Context context, String userFieldName, String systemFieldName, 
			Integer metadataFieldType, Integer metadataFieldPosition, String subCommHandle) throws SQLException;
	
	public List<UserMetadataFields> findAll(Context context) throws SQLException;

	public UserMetadataFields findById(Context context, int id) throws SQLException;

	public UserMetadataFields findByUserFieldName(Context context, String userFieldName) throws SQLException;

	public UserMetadataFields findBySystemFieldName(Context context, String systemFieldName) throws SQLException;
	
	public List<UserMetadataFields> findBySubDeptHandle(Context context,String subDeptHandle) throws SQLException;
	
	public List<UserMetadataFields> findDocumentMetadataBySubDeptHandle(Context context,String subDeptHandle) throws SQLException;
	
	public UserMetadataFields findBySystemFieldName(Context context,String subDeptHandle,String systemFieldName) throws SQLException;
	
	public void create(Context context, UserMetadataFields userMetadafield, String subDepartmentHandle ) throws SQLException;

	public List<String> findAllSubDepartmentHandle(Context context) throws SQLException;

	List<UserMetadataFields> getUniqueMetadataFieldsBySubDept(Context context, String subDeptHandle)
			throws SQLException;

}
