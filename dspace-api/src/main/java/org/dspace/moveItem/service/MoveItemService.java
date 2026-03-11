package org.dspace.moveItem.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public interface MoveItemService {
	List<Map<String, Object>> getAllContainersInSubDepartment(Context context, UUID subdepartmentUUID)
			throws SQLException;

	List<Map<String, Object>> getDspaceObjectFromDepartment(Context context, DSpaceObject dso) throws SQLException;
}
