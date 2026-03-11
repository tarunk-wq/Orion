package org.dspace.usermetadata;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.usermetadata.dao.UserMetadataFieldsDAO;
import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.springframework.beans.factory.annotation.Autowired;

public class UserMetadataFieldsServiceImpl implements UserMetadataFieldsService {

	@Autowired(required = true)
	private UserMetadataFieldsDAO userMetadataFieldsDAO;

	@Override
	public void savePeMetadataFields(Context context, String userFieldName, String systemFieldName, int fieldType,
			int fieldPosition, String subDeptHandle) throws SQLException {
		// TODO Auto-generated method stub
		userMetadataFieldsDAO.create(context, userFieldName, systemFieldName, fieldType, fieldPosition, subDeptHandle);

	}
	
	@Override
	public void savePeMetadataFields(Context context, UserMetadataFields userMetadaField, String subDepartmentHandle) throws SQLException {
		// TODO Auto-generated method stub
		userMetadataFieldsDAO.create(context, userMetadaField, subDepartmentHandle);

	}

	@Override
	public void savePeMetadataValues(Context context, int metadataFieldId, String fieldValue) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<UserMetadataFields> getMetadataFieldBySubDeptHandle(Context context, String subDeptHandle)
			throws SQLException {
		// TODO Auto-generated method stub
		return userMetadataFieldsDAO.findBySubDeptHandle(context, subDeptHandle);
	}

	@Override
	public void saveDepartment(Context context, String departmentName, String departmentHandle, String adminGroupName,
			String departmentAbbreviation) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public HashMap<String, String> getMetadataFieldsBySubDeptHandle(Context context, String subDeptHandle)
			throws SQLException {
		// TODO Auto-generated method stub
		List<UserMetadataFields> userMetadataFields = userMetadataFieldsDAO.findBySubDeptHandle(context, subDeptHandle);

		HashMap<String, String> userMetaFieldMap = new HashMap<>();

		for (UserMetadataFields umf : userMetadataFields) {
			userMetaFieldMap.put(umf.getSystemFieldName(), umf.getUserFieldName());
		}

		return userMetaFieldMap;
	}

	@Override
	public HashMap<String, String> getDocumentMetadataFieldsBySubDeptHandle(Context context, String subDeptHandle)
			throws SQLException {
		// TODO Auto-generated method stub
		List<UserMetadataFields> userMetadataFields = userMetadataFieldsDAO.findDocumentMetadataBySubDeptHandle(context,
				subDeptHandle);

		HashMap<String, String> userMetaFieldMap = new HashMap<>();

		for (UserMetadataFields umf : userMetadataFields) {
			userMetaFieldMap.put(umf.getSystemFieldName(), umf.getUserFieldName());
		}

		return userMetaFieldMap;
	}

	@Override
	public List<UserMetadataFields> getMetadataFieldsBySubDept(Context context, String subDeptHandle)
			throws SQLException {
		// TODO Auto-generated method stub
		return userMetadataFieldsDAO.findBySubDeptHandle(context, subDeptHandle);
	}

	@Override
	public void delete(Context context, UserMetadataFields userMetadataFields) throws SQLException {
		// TODO Auto-generated method stub
		userMetadataFieldsDAO.delete(context, userMetadataFields);
	}

	@Override
	public UserMetadataFields findUserMetadatafieldBySystemName(Context context, String subDeptHandle,
			String systemFieldName) throws SQLException {
		// TODO Auto-generated method stub
		return userMetadataFieldsDAO.findBySystemFieldName(context, subDeptHandle,systemFieldName);
	}

	@Override
	public List<String> findAllSubDepartmentHandle(Context context) throws SQLException {
		return userMetadataFieldsDAO.findAllSubDepartmentHandle(context);
	}
	
	@Override
	public UserMetadataFields findById(Context context, int metadataId) throws SQLException {
		return userMetadataFieldsDAO.findById(context, metadataId);
	}

	@Override
	public void update(Context context, UserMetadataFields userMetadata)
	        throws SQLException {
	    userMetadataFieldsDAO.save(context, userMetadata);
	}

	@Override
	public List<UserMetadataFields> getUniqueMetadataFieldsBySubDept(Context context, String subDeptHandle)
			throws SQLException {
		return userMetadataFieldsDAO.getUniqueMetadataFieldsBySubDept(context, subDeptHandle);
	}
}
