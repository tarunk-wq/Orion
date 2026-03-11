package org.dspace.tempaccess.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.tempaccess.TempAccess;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface TempAccessService extends DSpaceObjectService<TempAccess> {
	TempAccess create(Context context, ObjectNode tempAccess) throws SQLException, AuthorizeException, Exception;

	TempAccess findByItemAndUser(Context context, UUID itemUuid, String userEmail) throws SQLException;

	List<TempAccess> findByItem(Context context, UUID itemUuid, int limit, int offset) throws SQLException;

	void deleteByItemAndUser(Context context, UUID itemUuid, String userEmail) throws SQLException;

	long countByItem(Context context, UUID itemUuid) throws SQLException;
}