package org.dspace.app.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.model.SavedSearchRest;
import org.dspace.savedsearch.SavedSearch;
import org.dspace.savedsearch.service.SavedSearchService;
import org.dspace.core.Context;
import org.dspace.app.rest.utils.ContextUtil;

@RestController
@RequestMapping("/api/" + SavedSearchRest.CATEGORY + "/" + SavedSearchRest.NAME)
public class SavedSearchRestController {
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SavedSearchRestController.class);

	@Autowired
	SavedSearchService savedSearchService;

	@PostMapping("/save-search")
	@PreAuthorize("hasAuthority('AUTHENTICATED')")
	public String saveSearch(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(name = "searchname") String searchName, @RequestParam(name = "url") String url)
			throws SQLException {
		Context context = ContextUtil.obtainContext(request);
		log.info("Parameters are name::" + searchName + " and url :: " + url);
		UUID epersonID = context.getCurrentUser().getID();
		log.info("Active Eperson ID :: " + epersonID);
		try {
			List<SavedSearch> activeUserSavedSearch = savedSearchService.getSavedSearchByEPersonIdAndSearchName(context,
					epersonID, searchName);
			if (activeUserSavedSearch.size() > 0) {
				return "Name Already Exsists!";
			} else {
				savedSearchService.insert(context, epersonID, searchName, url);
			}
		} catch (Exception e) {
			log.error("Error in saving search :: " + e);
		}
		context.commit();
		return "Search Saved!";
	}

	@GetMapping("/saved-search")
	public List<SavedSearch> getSavedSearch(HttpServletResponse response, HttpServletRequest request)
			throws SQLException {
		Context context = ContextUtil.obtainContext(request);
		UUID epersonID = context.getCurrentUser().getID();
		log.info("Active Eperson ID :: " + epersonID);
		try {
			List<SavedSearch> activeUserSavedSearch = savedSearchService.getSavedSearchByEPersonId(context, epersonID);
			return activeUserSavedSearch;
		} catch (Exception e) {
			log.error("Error in fetching saved search :: " + e);
		}
		context.commit();
		return new ArrayList<>();
	}

	@PostMapping("/delete-saved-search")
	public String deleteSavedSearch(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(name = "uuid") UUID uuid) throws SQLException {
		Context context = ContextUtil.obtainContext(request);
		UUID epersonID = context.getCurrentUser().getID();
		log.info("Active Eperson ID :: " + epersonID);
		log.info("Item to be deleted UUID :: " + uuid);
		try {
			savedSearchService.deleteByID(context, epersonID, uuid);
		} catch (Exception e) {
			log.error("Error in deleting saved search :: " + e.getMessage());
		}
		context.complete();
		return "Deleted Search Saved!";
	}
}