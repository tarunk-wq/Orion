package org.dspace.usermetadata.service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;


//import org.dspace.content.department.DepartmentInfo;
import org.dspace.core.Context;
import org.dspace.usermetadata.UserMetadataFields;

public interface UserMetadataFieldsService {
		
	public void savePeMetadataFields(Context context, String userFieldName, String systemFieldName, int fieldType,
			int fieldPosition, String subDeptHandle) throws SQLException;
	
	public void savePeMetadataFields(Context context, UserMetadataFields userMetadatafield, String subDepartmentHandle) throws SQLException;

	public void savePeMetadataValues(Context context, int metadataFieldId, String fieldValue) throws SQLException;
	
	public List<UserMetadataFields> getMetadataFieldBySubDeptHandle(Context context,String subDeptHandle) throws SQLException;
	
	public void saveDepartment(Context context,String departmentName,String departmentHandle,String adminGroupName,String departmentAbbreviation) throws SQLException;
	
//	public List<DepartmentInfo> findAllDepartmentInfo(Context context) throws SQLException;
	
//	public DepartmentInfo findByDepartmentHandle(Context context,String departmentHandle) throws SQLException;
	
	public HashMap<String,String> getMetadataFieldsBySubDeptHandle(Context context,String subDeptHandle) throws SQLException;
	
	public HashMap<String,String> getDocumentMetadataFieldsBySubDeptHandle(Context context,String subDeptHandle) throws SQLException;

//	public void delete(Context context,DepartmentInfo departmentInfo) throws SQLException;
	
	public List<UserMetadataFields> getMetadataFieldsBySubDept(Context context,String subDeptHandle) throws SQLException;
	
	public void delete(Context context,UserMetadataFields userMetadataFields) throws SQLException;
	
	public UserMetadataFields findUserMetadatafieldBySystemName(Context context,String subDeptHandle,String systemFieldName) throws SQLException;
	
//	public DepartmentInfo findByDepartmentAbbreviation(Context context,String departmentAbbr) throws SQLException;
	
	public List<String> findAllSubDepartmentHandle(Context context) throws SQLException;
	
	public UserMetadataFields findById(Context context, int metadataId) throws SQLException;
	
	void update(Context context, UserMetadataFields userMetadata) throws SQLException;

	List<UserMetadataFields> getUniqueMetadataFieldsBySubDept(Context context, String subDeptHandle)
			throws SQLException;
}