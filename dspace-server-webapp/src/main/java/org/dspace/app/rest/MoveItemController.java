package org.dspace.app.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.service.UserPermissionService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.moveItem.service.MoveItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/core/moveItems")
public class MoveItemController {

	private static final Logger log = LogManager.getLogger(MoveItemController.class);

	@Autowired
	private MoveItemService moveItemService;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private UserPermissionService userpermissionService;

	@GetMapping("/uuid/{uuid}")
	public ResponseEntity<Map<String, List<Map<String, Object>>>> getDspaceObjectFromSubdepartment(
			@PathVariable UUID uuid, HttpServletRequest request) {
		Context context = ContextUtil.obtainContext(request);
		try {
			DSpaceObject dso = collectionService.find(context, uuid);
			if (dso == null)
				dso = communityService.find(context, uuid);

			if (dso == null)
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			if (!userpermissionService.hasAdminPermission(context, dso))
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

			List<Map<String, Object>> dspaceObjects = moveItemService.getDspaceObjectFromDepartment(context, dso);
			Map<String, List<Map<String, Object>>> response = new HashMap<>();
			response.put("dspaceObjects", dspaceObjects);
			context.complete();
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			if (context != null && context.isValid())
				context.abort();
			log.error("Error retrieving move dspaceObjects for uuid: " + uuid, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}