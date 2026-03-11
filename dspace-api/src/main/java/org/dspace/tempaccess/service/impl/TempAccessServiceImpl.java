package org.dspace.tempaccess.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.app.util.service.DSpaceObjectPermissionService;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.tempaccess.TempAccess;
import org.dspace.tempaccess.dao.TempAccessDAO;
import org.dspace.tempaccess.service.TempAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TempAccessServiceImpl extends DSpaceObjectServiceImpl<TempAccess> implements TempAccessService {

	private static final Logger log = LogManager.getLogger(TempAccessServiceImpl.class);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final TempAccessDAO dao;
	private final EPersonService ePersonService;
	private final ItemService itemService;
	private final ItemImportService itemImportService;
	private final AuditTrailService auditTrailService;
	private final DSpaceObjectPermissionService dspaceObjectPermissionService;

	@Autowired
	public TempAccessServiceImpl(TempAccessDAO dao, EPersonService ePersonService, ItemService itemService,
			ItemImportService itemImportService, AuditTrailService auditTrailService,
			DSpaceObjectPermissionService dspaceObjectPermissionService) {
		this.dao = dao;
		this.ePersonService = ePersonService;
		this.itemService = itemService;
		this.itemImportService = itemImportService;
		this.auditTrailService = auditTrailService;
		this.dspaceObjectPermissionService = dspaceObjectPermissionService;
	}

	@Override
	public TempAccess create(Context context, ObjectNode json) throws SQLException, AuthorizeException, Exception {
		if (!json.hasNonNull("itemUuid") || !json.hasNonNull("epersonUuid")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing itemUuid or epersonUuid");
		}

		UUID itemUuid = UUID.fromString(json.get("itemUuid").asText());
		UUID epersonUuid = UUID.fromString(json.get("epersonUuid").asText());

		Item item = itemService.find(context, itemUuid);
		EPerson eperson = ePersonService.find(context, epersonUuid);

		if (item == null || eperson == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item or eperson UUID");
		}

		if (!dspaceObjectPermissionService.hasAdminPermission(context, item)) {
			throw new AuthorizeException("You do not have permission to grant temporary access for this item");
		}

		boolean isNewAccess = false;
		boolean reactivateDeleted = false;

		TempAccess tempAccess = dao.findByItemAndUser(context, item, eperson); // Check if access already exists

		if (tempAccess == null) {
			tempAccess = new TempAccess();
			tempAccess.setItem(item);
			tempAccess.setEperson(eperson);

			try {
				DSpaceObject topLevelCommunity = dspaceObjectPermissionService.getTopLevelCommunity(context, item);
				if (topLevelCommunity != null) {
					tempAccess.setDepartmentUuid(topLevelCommunity.getID());
					log.info("Department Name : " + topLevelCommunity.getName());
				}
			} catch (Exception e) {
				log.warn("Failed to resolve top-level community for item " + itemUuid, e);
			}

			isNewAccess = true;
		} else if (tempAccess.isDeleted()) {
			reactivateDeleted = true;
		}

		try {
			LocalDate today = LocalDate.now();
			LocalDate startDate = null;
			LocalDate endDate = null;

			if (json.has("startDate")) {
				startDate = LocalDate.parse(json.get("startDate").asText(), DATE_FORMATTER);
				if (startDate.isBefore(today)) { // startDate must be today or in the future
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be before today.");
				}
				tempAccess.setStartDate(startDate);
			}

			if (json.has("endDate")) {
				endDate = LocalDate.parse(json.get("endDate").asText(), DATE_FORMATTER);
				if (endDate.isBefore(today)) { // endDate must be today or in the future
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before today.");
				}
				tempAccess.setEndDate(endDate);
			}

			if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date.");
			}
		} catch (DateTimeParseException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format. Expected yyyy-MM-dd");
		}

		tempAccess.setDeleted(false);

		try {
			if (isNewAccess) {
				dao.create(context, tempAccess);
				itemImportService.manageTempAccessRolePolicies(context, item, eperson);
				logTempAudit(context, item, AuditAction.TEMP_ACCESS_CREATED);
			} else {
				dao.save(context, tempAccess);
				if (reactivateDeleted) {
					itemImportService.manageTempAccessRolePolicies(context, item, eperson);
				}
				logTempAudit(context, item, AuditAction.TEMP_ACCESS_UPDATED);
			}
		} catch (Exception e) {
			log.error("Failed to persist TempAccess for item " + itemUuid + " and user " + epersonUuid, e);
			if (context != null && context.isValid())
				context.abort();
			throw e;
		}

		TempAccess result = dao.findByItemAndUser(context, item, eperson);
        context.commit();
        return result;
	}

	@Override
	public TempAccess findByItemAndUser(Context context, UUID itemUuid, String userEmail) throws SQLException {
		Item item = itemService.find(context, itemUuid);
		EPerson eperson = ePersonService.findByEmail(context, userEmail);
		if (item != null && eperson != null) {
			return dao.findByItemAndUser(context, item, eperson);
		}
		return null;
	}

	@Override
	public List<TempAccess> findByItem(Context context, UUID itemUuid, int limit, int offset) throws SQLException {
		Item item = itemService.find(context, itemUuid);
		if (item != null) {
			logTempAudit(context, item, AuditAction.TEMP_ACCESS_RETRIEVED);
			return dao.findByItem(context, item, limit, offset);
		}
		return Collections.emptyList();
	}

	@Override
	public void deleteByItemAndUser(Context context, UUID itemUuid, String userEmail) throws SQLException {
		Item item = itemService.find(context, itemUuid);
		EPerson eperson = ePersonService.findByEmail(context, userEmail);
		if (item != null && eperson != null) {
			try {
				itemImportService.deleteTempAccessRolePolicies(context, item, eperson);
				dao.delete(context, item, eperson);
				logTempAudit(context, item, AuditAction.TEMP_ACCESS_DELETED);
				context.commit();
			} catch (Exception e) {
				log.error("Failed to delete resource policies for item: " + itemUuid + ", user: " + userEmail, e);
				if (context != null && context.isValid())
					context.abort();
			}
		}
	}

	@Override
	public long countByItem(Context context, UUID itemUuid) throws SQLException {
		Item item = itemService.find(context, itemUuid);
		return item != null ? dao.countByItem(context, item) : 0;
	}

	@Override
	public TempAccess find(Context context, UUID uuid) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateLastModified(Context context, TempAccess dso) throws SQLException, AuthorizeException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Context context, TempAccess dso) throws SQLException, AuthorizeException, IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	private void logTempAudit(Context context, Item item, AuditAction action) {
		try {
			if (item != null && action != null) {
				String handle = item.getHandle() != null ? item.getHandle() : "";
				String itemName = item.getName() != null ? item.getName().toString() : "";
				String parentName = "";
				DSpaceObject parent = itemService.getParentObject(context, item);
				if (parent instanceof Collection) {
					parentName = ((Collection) parent).getName();
				} else if (parent instanceof Community) {
					parentName = ((Community) parent).getName();
				}
				auditTrailService.logAction(context, handle, action, itemName, parentName);
				context.commit();
			}
		} catch (Exception e) {
			log.warn("Audit logging failed for action " + action + " on item " + (item != null ? item.getName() : ""),
					e);
		}
	}
}
