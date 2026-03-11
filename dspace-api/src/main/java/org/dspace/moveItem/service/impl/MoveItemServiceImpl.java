package org.dspace.moveItem.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.moveItem.service.MoveItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MoveItemServiceImpl implements MoveItemService {

	private static final Logger log = LogManager.getLogger(MoveItemServiceImpl.class);

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private AuditTrailService auditTrailService;

	@Override
	public List<Map<String, Object>> getAllContainersInSubDepartment(Context context, UUID subdepartmentUUID)
			throws SQLException {
		List<Map<String, Object>> results = new ArrayList<>();
		Community subDepartment = communityService.find(context, subdepartmentUUID);
		if (subDepartment != null) {
			collectAllContainers(context, subDepartment, results);
			logMoveItemAudit(context, subDepartment.getHandle(), AuditAction.MOVE_ITEM_GET_COLLECTIONS_SUBDEPARTMENT,
					subDepartment.getName());
		}
		return results;
	}

	private void collectAllContainers(Context context, Community community, List<Map<String, Object>> collector)
			throws SQLException {
		collector.add(convertToMap(community));

		List<Collection> collections = community.getCollections();
		if (collections != null && !collections.isEmpty()) {
			for (Collection c : collections) {
				collector.add(convertToMap(c));
			}
		}

		List<Community> subs = community.getSubcommunities();
		if (subs != null && !subs.isEmpty()) {
			for (Community sub : subs) {
				collectAllContainers(context, sub, collector);
			}
		}
	}

	private Map<String, Object> convertToMap(DSpaceObject dso) {
		Map<String, Object> m = new HashMap<>();
		m.put("uuid", dso.getID());
		m.put("name", dso.getName());
		m.put("type", (dso instanceof Collection) ? "collection" : "community");
		return m;
	}

	@Override
	public List<Map<String, Object>> getDspaceObjectFromDepartment(Context context, DSpaceObject dso)
			throws SQLException {
		DSpaceObject subdepartmentDSO = getSecondTopLevelCommunity(context, dso);
		if (!(subdepartmentDSO instanceof Community)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Subdepartment not found for the given object: " + dso.getID());
		}
		Community subdepartment = (Community) subdepartmentDSO;
		List<Map<String, Object>> allTargets = getAllContainersInSubDepartment(context, subdepartment.getID());
		final UUID excludeId = dso.getID();
		final UUID subdepartmentId = subdepartment.getID();

		return allTargets.stream().filter(m -> {
			UUID id = (UUID) m.get("uuid");
			return !excludeId.equals(id) && !subdepartmentId.equals(id);
		}).collect(Collectors.toList());
	}

	private DSpaceObject getSecondTopLevelCommunity(Context context, DSpaceObject dso) throws SQLException {
		if (dso == null)
			return null;
		if (dso instanceof Collection) {
			return getSecondTopLevelCommunity(context, collectionService.getParentObject(context, (Collection) dso));
		} else if (dso instanceof Community) {
			DSpaceObject parent = communityService.getParentObject(context, (Community) dso);
			if (parent == null)
				return null;
			DSpaceObject grandParent = communityService.getParentObject(context, (Community) parent);
			return (grandParent == null) ? dso : getSecondTopLevelCommunity(context, parent);
		}
		return null;
	}

	private void logMoveItemAudit(Context context, String handle, AuditAction action, String communityName) {
		try {
			if (action != null) {
				handle = handle != null ? handle : "";
				communityName = communityName != null ? communityName : "";

				auditTrailService.logAction(context, handle, action, communityName);
				context.commit();
			}
		} catch (Exception e) {
			log.warn("Audit logging failed for action " + action + " on subdepartment " + communityName, e);
		}
	}
}