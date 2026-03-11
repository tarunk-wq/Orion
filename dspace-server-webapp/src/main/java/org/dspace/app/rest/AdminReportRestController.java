package org.dspace.app.rest;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.repository.DiscoveryRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.audittrail.AuditTrail;
import org.dspace.audittrail.DocumentSearch;
import org.dspace.audittrail.dao.AuditTrailDAO;
import org.dspace.audittrail.service.DocumentSearchService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Department;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DepartmentService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dspace-admin")
public class AdminReportRestController {

	private static final Logger log = LogManager.getLogger();

	private static final String SOLR_PARSE_ERROR_CLASS = "org.apache.solr.search.SyntaxError";

	@Autowired
	protected Utils utils;

	@Autowired
	private DiscoveryRestRepository discoveryRestRepository;
	
	@Autowired
	private DocumentSearchService documentSearchSearvice;
	
	@Autowired
	private ItemService itemService;

	@Autowired
    private RequestService requestService;
	
	@Autowired(required = true)
	protected AuditTrailDAO audittrailDAO;
	
	@Autowired
    ConfigurationService configurationService;
    
	@Autowired(required = true)
    protected MetadataFieldService metadataFieldService;
	
    @Autowired(required = true)
    private UserMetadataFieldsService userMetadataFieldsService;
    
    @Autowired(required = true)
    private DepartmentService departmentService;
    
    @Autowired(required = true)
    protected CommunityService communityService;
    
    private static final DateTimeFormatter INPUT_FMT =  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter OUTPUT_FMT =  DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
	
	@RequestMapping(method = RequestMethod.GET, value = "/report/{reportType}/generatecsv")
	public void generateCSVReport(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable(name = "reportType") String reportType,
			@RequestParam(name = "query", required = false) String query,
			@RequestParam(name = "dsoType", required = false) List<String> dsoTypes,
			@RequestParam(name = "scope", required = false) String dsoScope,
			@RequestParam(name = "configuration", required = false) String configuration,
			List<SearchFilter> searchFilters,
			Pageable page) throws Exception {
		
		Context context = ContextUtil.obtainContext(request);
		dsoTypes = emptyIfNull(dsoTypes);
		String searchValue = "";
		String reportName = reportType;
		Timestamp endDate= null;
		Timestamp startDate = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (int count = 0; count < searchFilters.size(); count++) {
			String resourceType = searchFilters.get(count).getName();
			String resourceValue = searchFilters.get(count).getValue();
			if(resourceType.equalsIgnoreCase("accessioned"))
			{
				String modifiedResourceValue = resourceValue.substring(1, resourceValue.length()-1);
				String[] date = modifiedResourceValue.split(" TO ");
				reportName += modifiedResourceValue ;
				startDate =  new Timestamp(dateFormat.parse(date[0]+" 00:00:00").getTime());
				endDate =  new Timestamp(dateFormat.parse(date[1]+" 23:59:59").getTime());
			}
			else
			{
				searchValue += resourceType + ":" + resourceValue;
				searchValue+="|";

			}
		}
		if(searchValue.length()!=0)
		{
			searchValue = searchValue.substring(0, searchValue.length()-1);
		}
		List<AuditTrail> searchResultList = null;
		try 
		{
			searchResultList = audittrailDAO.getAuditTarilBySearchFacets(context,searchValue , startDate, endDate);
		}
		catch (SQLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<String[]> metadataList = null;
		if(reportType.equalsIgnoreCase("advancesearch")) {
			 metadataList = handleMetadataForAdvanceSearchReport(reportType, searchResultList,"csv");
		} else {
	        page = null;
			List<SearchResultEntryRest> searchResultListOriginal = getSearchObjects(query, dsoTypes, dsoScope, configuration, searchFilters, page);
			metadataList = handleMetadata(reportType, searchResultListOriginal);
		}
		Utils.handleCSV(context, request, response, metadataList, reportName);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/report/{reportType}/generatepdf")
	public void generatePDFReport(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable(name = "reportType") String reportType,
			@RequestParam(name = "query", required = false) String query,
			@RequestParam(name = "dsoType", required = false) List<String> dsoTypes,
			@RequestParam(name = "scope", required = false) String dsoScope,
			@RequestParam(name = "configuration", required = false) String configuration,
			List<SearchFilter> searchFilters,
			Pageable page) throws Exception {
		
		Context context = ContextUtil.obtainContext(request);
		dsoTypes = emptyIfNull(dsoTypes);
		String searchValue = "";
		String reportName = reportType;
		Timestamp endDate= null;
		Timestamp startDate = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (int count = 0; count < searchFilters.size(); count++) {
			String resourceType = searchFilters.get(count).getName();
			String resourceValue = searchFilters.get(count).getValue();
			if(resourceType.equalsIgnoreCase("accessioned"))
			{
				String modifiedResourceValue = resourceValue.substring(1, resourceValue.length()-1);
				String[] date = modifiedResourceValue.split(" TO ");
				reportName += modifiedResourceValue ;
				startDate =  new Timestamp(dateFormat.parse(date[0]+" 00:00:00").getTime());
				endDate =  new Timestamp(dateFormat.parse(date[1]+" 23:59:59").getTime());
			}
			else
			{
				searchValue += resourceType + ":" + resourceValue;
				searchValue+="|";

			}
		}
		if(searchValue.length()!=0)
		{
			searchValue = searchValue.substring(0, searchValue.length()-1);
		}
		List<AuditTrail> searchResultList = null;
		try 
		{
			searchResultList = audittrailDAO.getAuditTarilBySearchFacets(context,searchValue , startDate, endDate);
		}
		catch (SQLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<String[]> metadataList = null;
		if(reportType.equalsIgnoreCase("advancesearch")) {
			metadataList = handleMetadataForAdvanceSearchReport(reportType, searchResultList,"pdf");
		} else {
	        page = null;
			List<SearchResultEntryRest> searchResultListOriginal = getSearchObjects(query, dsoTypes, dsoScope, configuration, searchFilters, page);
			metadataList = handleMetadata(reportType, searchResultListOriginal);
		}
		Utils.handlePdf(context, request, response, metadataList, reportName);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/report/{reportType}/generate")
	public List<String[]> generateReport(
			HttpServletRequest request,
			HttpServletResponse response,
			@PathVariable(name = "reportType") String reportType,
			@RequestParam(name = "query", required = false) String query,
			@RequestParam(name = "dsoType", required = false) List<String> dsoTypes,
			@RequestParam(name = "scope", required = false) String dsoScope,
			@RequestParam(name = "configuration", required = false) String configuration,
			List<SearchFilter> searchFilters,
			Pageable page) throws Exception {
		
		Context context = ContextUtil.obtainContext(request);
		dsoTypes = emptyIfNull(dsoTypes);
		String searchValue = "";
		Timestamp endDate= null;
		Timestamp startDate = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		for (int count = 0; count < searchFilters.size(); count++) {
			String resourceType = searchFilters.get(count).getName();
			String resourceValue = searchFilters.get(count).getValue();
			if(resourceType.equalsIgnoreCase("accessioned"))
			{
				String modifiedResourceValue = resourceValue.substring(1, resourceValue.length()-1);
				String[] date = modifiedResourceValue.split(" TO ");
				startDate =  new Timestamp(dateFormat.parse(date[0]+" 00:00:00").getTime());
				endDate =  new Timestamp(dateFormat.parse(date[1]+" 23:59:59").getTime());
			}
			else
			{
				searchValue += resourceType + ":" + resourceValue;
				searchValue+="|";

			}
		}
		if(searchValue.length()!=0)
		{
			searchValue = searchValue.substring(0, searchValue.length()-1);
		}
		List<AuditTrail> searchResultList = null;
		try 
		{
			searchResultList = audittrailDAO.getAuditTarilBySearchFacets(context,searchValue , startDate, endDate);
		}
		catch (SQLException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<String[]> metadataList = null;
		if(reportType.equalsIgnoreCase("advancesearch")) {
			metadataList = handleSort20MetadataForAdvanceSearchReport(reportType, searchResultList,"pdf");
		} else {
	        page = null;
			List<SearchResultEntryRest> searchResultListOriginal = getSearchObjects(query, dsoTypes, dsoScope, configuration, searchFilters, page);
			metadataList = handleSort20Metadata(reportType, searchResultListOriginal);
		}
		return metadataList;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports/verify/generatecsv")
	public void generateCSVReport(HttpServletResponse response,
			HttpServletRequest request,
			@RequestParam(name = "subDepartment") String subDepartment,
			@RequestParam(name = "startdate") String startDate,
			@RequestParam(name = "enddate") String endDate,
			@RequestParam(value = "scriptName") String scriptName) throws Exception {

		try {
			Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
			String reportFileName = "Report-ItemUpload-";				
			UUID sectionId = UUID.fromString(subDepartment);
			Community community =  communityService.find(context, sectionId);
			if(community != null) {
				reportFileName += community.getName();
			}
			reportFileName = reportFileName+" "+ startDate + " to " + endDate;
			Iterator<Object[]> itr = getItemUploadReport(context, community);
			List<String[]> metadata = handleSortItemUploadMetadata(context, itr, community, startDate, endDate);
			
			Utils.handleCSV(context, request, response, metadata, reportFileName);
		} catch (Exception e) {
			log.error("Error in generating csv for item-upload report", e);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports/verify/generatepdf")
	public void generatePDFReport(
	    HttpServletResponse response,
	    HttpServletRequest request,
	    @RequestParam(name = "subDepartment") String subDepartment,
	    @RequestParam(name = "startdate") String startDate,
	    @RequestParam(name = "enddate") String endDate,
	    @RequestParam(name = "scriptName") String scriptName,
	    @RequestParam(name = "authentication-token", required = false) String authToken
	) throws SQLException {
		
		try {
			Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
			String reportFileName = "Report-ItemUpload-";			
		
			UUID sectionId = UUID.fromString(subDepartment);
			Community community =  communityService.find(context, sectionId);
			if(community != null) {
				reportFileName += community.getName();
			}
			reportFileName = reportFileName+" "+ startDate + " to " + endDate;
			Iterator<Object[]> itr = getItemUploadReport(context, community);

			List<String[]> metadata = handleSortItemUploadMetadata(context, itr, community, startDate, endDate);
			
			Utils.handlePdf(context, request, response, metadata, reportFileName);
		} catch (Exception e) {
			log.error("Error in generating pdf for item-upload report", e);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/reports/verify/generate")
	public List<String[]> generateReport(HttpServletResponse response,
			HttpServletRequest request,
			@RequestParam(name = "subDepartment") String subDepartment,
			@RequestParam(name = "startdate") String startDate,
			@RequestParam(name = "enddate") String endDate,
			@RequestParam(value = "scriptName") String scriptName) throws Exception {
		
		try {
			Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
			UUID sectionId = UUID.fromString(subDepartment);
			Community community =  communityService.find(context, sectionId);
			Iterator<Object[]> itr = getItemUploadReport(context, community);
			List<String[]> metadata = handleSort20ItemUploadMetadata(context, itr, community, startDate, endDate);
			return metadata;
		} catch (Exception e) {
			log.error("Error in generating item-upload report", e);
		}
		return new ArrayList<String[]>();
	}
	
	private Iterator<Object[]> getItemUploadReport(Context context, Community selectedCommunity) throws SQLException {
		Set<MetadataField> topUserFieldsSet = new LinkedHashSet<MetadataField>();
		List<Community> subCommunities = new ArrayList<Community>();
		List<Collection> subCollections = new ArrayList<Collection>();
		MetadataField uploadmf = metadataFieldService.findByString(context, "dc.date.accessioned", '.');
		if (uploadmf != null) {
			topUserFieldsSet.add(uploadmf);
		}
		if(selectedCommunity != null) {
			topUserFieldsSet.addAll(getMetadataFieldList(context, selectedCommunity));
			subCollections.addAll(selectedCommunity.getCollections());
			subCommunities.addAll(selectedCommunity.getSubcommunities());
		} else {
			List<Community> communities = communityService.findAllTop(context);
			Set<Community> subCommunitiesSet = new HashSet<Community>();
			Set<Collection> subCollectionsSet = new HashSet<Collection>();
			for(Community community : communities) {
				Department department = departmentService.findByCommunity(context, community);
				if(department != null) {
					for (Community com : community.getSubcommunities()) {
						subCommunitiesSet.addAll(com.getSubcommunities());
						subCollectionsSet.addAll(com.getCollections());
						topUserFieldsSet.addAll(getMetadataFieldList(context, com));
					}
				} else {
					subCommunitiesSet.addAll(community.getSubcommunities());
				}
				topUserFieldsSet.addAll(getMetadataFieldList(context, community));
				subCollectionsSet.addAll(community.getCollections());
			}
			subCollections.addAll(subCollectionsSet);
			subCommunities.addAll(subCommunitiesSet);
		}
		List<MetadataField> topUserFields = new ArrayList<MetadataField>(topUserFieldsSet);
		if (topUserFields.size() <= 1) {
		    return List.<Object[]>of().iterator();
		}
		return itemService.getItemUploadReport(context, subCommunities, subCollections, topUserFields.toArray(new MetadataField[0]));
	}
	
	private List<MetadataField> getMetadataFieldList(Context context, Community community) throws SQLException{
		List<MetadataField> topUserFields = new ArrayList<MetadataField>();
		String subDeptHandle = community.getHandle();
		List<UserMetadataFields> userMetadatas = userMetadataFieldsService.getUniqueMetadataFieldsBySubDept(context, subDeptHandle);
		if(userMetadatas == null || userMetadatas.isEmpty()) {
			userMetadatas = userMetadataFieldsService.getMetadataFieldsBySubDept(context, subDeptHandle);
		}
		List<UserMetadataFields> topFiveFields = userMetadatas == null ? List.of() :  userMetadatas.stream()
								  		                .sorted(Comparator.comparing(
								  		                        UserMetadataFields::getUserFieldName,
								  		                        Comparator.nullsLast(String::compareToIgnoreCase)))
								  		                .limit(5)
								  		                .toList();
		for(UserMetadataFields umf : topFiveFields) {
			MetadataField mf = metadataFieldService.findByString(context, umf.getSystemFieldName(), '.');
			if (mf != null) {
				topUserFields.add(mf);
			}
		}
		return topUserFields;
	}
	
	private String[] getItemUploadHeading(Context context, Community selectedCommunity) throws SQLException {
	    Set<String> headings = new LinkedHashSet<String>();
	    headings.add("UPLOAD DATE");
		if(selectedCommunity != null) {
	        addUserFieldHeadings(context, selectedCommunity, headings);
	   } else {
			List<Community> communities = communityService.findAllTop(context);
			for(Community community : communities) {
				Department department = departmentService.findByCommunity(context, community);
				if(department != null) {
					for (Community com : community.getSubcommunities()) {
	                    addUserFieldHeadings(context, com, headings);
					}
				} else {
					addUserFieldHeadings(context, community, headings);
				}
			}
		}
	    return headings.toArray(new String[0]);
	}
	
	private void addUserFieldHeadings(Context context, Community community, Set<String> headings) throws SQLException {

		String handle = community.getHandle();

		List<UserMetadataFields> userMetadatas = userMetadataFieldsService.getUniqueMetadataFieldsBySubDept(context,
				handle);

		if (userMetadatas == null || userMetadatas.isEmpty()) {
			userMetadatas = userMetadataFieldsService.getMetadataFieldsBySubDept(context, handle);
		}

		if (userMetadatas == null) return;

	    userMetadatas.stream()
	        .sorted(Comparator.comparing(
	            UserMetadataFields::getUserFieldName,
	            Comparator.nullsLast(String::compareToIgnoreCase)))
	        .limit(5)
	        .map(UserMetadataFields::getUserFieldName) 
	        .filter(Objects::nonNull)
	        .map(String::toUpperCase)
	        .forEach(headings::add);
	}
	
	private List<String[]> handleSortItemUploadMetadata(Context context, Iterator<Object[]> itr, Community selectedCommunity, String startDate, String endDate) throws SQLException {
		LocalDateTime startDateOb = LocalDateTime.parse(startDate + " 00:00:00",INPUT_FMT);
		String startdate= startDateOb.format(OUTPUT_FMT);
		LocalDateTime startDateObj=LocalDateTime.parse(startdate, OUTPUT_FMT);
		LocalDateTime endDateOb = LocalDateTime.parse(endDate + " 23:59:59",INPUT_FMT);
		String enddate= endDateOb.format(OUTPUT_FMT);
		LocalDateTime endDateObj=LocalDateTime.parse(enddate, OUTPUT_FMT);
		
		ZoneId istZone = ZoneId.of("Asia/Kolkata");

		List<String[]> metadata =  new ArrayList<String[]>();
		Set<String> uniquemetadata = new HashSet<>();
		String[] header = getItemUploadHeading(context, selectedCommunity);
		metadata.add(header);
		while (itr.hasNext()) {
		    Object[] row = itr.next();
		    String[] metadataArray = new String[row.length];

		    // Convert date format for the first element if it exists
		    if (row.length > 0 && row[0] != null) {
		    	String uniqueCombo =
		    		    (row.length > 2 ? row[2] : "") + "_" +
		    		    (row.length > 3 ? row[3] : "") + "_" +
		    		    (row.length > 4 ? row[4] : "");
		    	if (!uniquemetadata.add(uniqueCombo)) {
		    	    continue;
		    	}
		        String dateTimeString = row[0].toString().replaceAll("\\s", "");
		        ZonedDateTime zonedDateTime;

		        if (!dateTimeString.contains("T")) {
		            if (dateTimeString.matches("\\d{4}-\\d{2}-\\d{2}")) {
		                // Format YYYY-MM-DD
		                String temp = dateTimeString + "T00:00:00Z";
		                zonedDateTime = ZonedDateTime.parse(temp).withZoneSameInstant(istZone);
		            } else {
		                // Format DD-MM-YYYY
		                DateTimeFormatter originalFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		                LocalDate originalDate = LocalDate.parse(dateTimeString, originalFormatter);
		                String convertedDateString = originalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		                String trimDateString = convertedDateString.trim() + "T00:00:00Z";
		                zonedDateTime = ZonedDateTime.parse(trimDateString).withZoneSameInstant(istZone);
		            }
		        } else {
		            // Already in a parseable format
		            zonedDateTime = ZonedDateTime.parse(dateTimeString).withZoneSameInstant(istZone);
		        }

		        String formattedDate = zonedDateTime.format(OUTPUT_FMT);
		        LocalDateTime date = LocalDateTime.parse(formattedDate, OUTPUT_FMT);

		        // Check if the date is within the specified range
		        if ((date.isAfter(startDateObj) || date.isEqual(startDateObj)) &&
		            (date.isBefore(endDateObj) || date.isEqual(endDateObj))) {
		            metadataArray[0] = formattedDate; // Date column
		        } else {
		            continue; // Skip this row if out of date range
		        }
		    } else {
		        metadataArray[0] = ""; // Empty date if null or not available
		    }

		    // Populate remaining columns dynamically
		    for (int i = 1; i < row.length; i++) {
		        metadataArray[i] = row[i] != null ? row[i].toString() : null;
		    }
		    // Add the current row to the metadata list
		    metadata.add(metadataArray);
		}
		
		return metadata;
	}
	
	private List<String[]> handleSort20ItemUploadMetadata(Context context, Iterator<Object[]> itr, Community selectedCommunity, String startDate, String endDate) throws SQLException {
		LocalDateTime startDateOb = LocalDateTime.parse(startDate + " 00:00:00",INPUT_FMT);
		String startdate= startDateOb.format(OUTPUT_FMT);
		LocalDateTime startDateObj=LocalDateTime.parse(startdate, OUTPUT_FMT);
		LocalDateTime endDateOb = LocalDateTime.parse(endDate + " 23:59:59",INPUT_FMT);
		String enddate= endDateOb.format(OUTPUT_FMT);
		LocalDateTime endDateObj=LocalDateTime.parse(enddate, OUTPUT_FMT);
		
		ZoneId istZone = ZoneId.of("Asia/Kolkata");

		List<String[]> metadata =  new ArrayList<String[]>();
		Set<String> uniquemetadata = new HashSet<>();
		String[] header = getItemUploadHeading(context, selectedCommunity);
		metadata.add(header);
		while (itr.hasNext()) {
		    Object[] row = itr.next();
		    String[] metadataArray = new String[row.length];

		    // Convert date format for the first element if it exists
		    if (row.length > 0 && row[0] != null) {
		    	String uniqueCombo =
		    		    (row.length > 2 ? row[2] : "") + "_" +
		    		    (row.length > 3 ? row[3] : "") + "_" +
		    		    (row.length > 4 ? row[4] : "");
		    	if (!uniquemetadata.add(uniqueCombo)) {
		    	    continue;
		    	}
		        String dateTimeString = row[0].toString().replaceAll("\\s", "");
		        ZonedDateTime zonedDateTime;

		        if (!dateTimeString.contains("T")) {
		            if (dateTimeString.matches("\\d{4}-\\d{2}-\\d{2}")) {
		                // Format YYYY-MM-DD
		                String temp = dateTimeString + "T00:00:00Z";
		                zonedDateTime = ZonedDateTime.parse(temp).withZoneSameInstant(istZone);
		            } else {
		                // Format DD-MM-YYYY
		                DateTimeFormatter originalFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		                LocalDate originalDate = LocalDate.parse(dateTimeString, originalFormatter);
		                String convertedDateString = originalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		                String trimDateString = convertedDateString.trim() + "T00:00:00Z";
		                zonedDateTime = ZonedDateTime.parse(trimDateString).withZoneSameInstant(istZone);
		            }
		        } else {
		            // Already in a parseable format
		            zonedDateTime = ZonedDateTime.parse(dateTimeString).withZoneSameInstant(istZone);
		        }

		        String formattedDate = zonedDateTime.format(OUTPUT_FMT);
		        LocalDateTime date = LocalDateTime.parse(formattedDate, OUTPUT_FMT);

		        // Check if the date is within the specified range
		        if ((date.isAfter(startDateObj) || date.isEqual(startDateObj)) &&
		            (date.isBefore(endDateObj) || date.isEqual(endDateObj))) {
		            metadataArray[0] = formattedDate; // Date column
		        } else {
		            continue; // Skip this row if out of date range
		        }
		    } else {
		        metadataArray[0] = ""; // Empty date if null or not available
		    }

		    // Populate remaining columns dynamically
		    for (int i = 1; i < row.length; i++) {
		        metadataArray[i] = row[i] != null ? row[i].toString() : null;
		    }
		    // Add the current row to the metadata list
		    metadata.add(metadataArray);
		    if(metadata.size() == 21) {
            	return metadata;
            }
		}	
		return metadata;
	}

	
	@RequestMapping(method = RequestMethod.GET, value = "/audit/generatecsv")
	public void generateDocumentSearchCSVReport(HttpServletResponse response,
												HttpServletRequest request,
												@RequestParam(name = "username", required = false) String userName,
												@RequestParam(name = "startdate") String startDate,
												@RequestParam(name = "enddate") String endDate) throws Exception {

		String reportFileName = "DocumentSearchedWiseReport %s";
		Date startDateObj = null;
		Date endDateObj = null;
		Context context = ContextUtil.obtainContext(request);
		List<DocumentSearch> documentSearchList = new ArrayList<DocumentSearch>();
		if (userName != null && startDate != null && endDate != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			startDateObj = formatter.parse(startDate + " 00:00:00");
			endDateObj = formatter.parse(endDate+ " 23:59:59");
			reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
			
			documentSearchList = documentSearchSearvice.getDocumentSearch(context, userName, startDateObj, endDateObj);
		} else if (startDate != null && endDate != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			startDateObj = formatter.parse(startDate + " 00:00:00");
			endDateObj = formatter.parse(endDate+ " 23:59:59");
			reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
			
			documentSearchList = documentSearchSearvice.getDocumentSearchByDateRange(context, startDateObj, endDateObj);
		} else {
			reportFileName = String.format(reportFileName,  "").trim();
			documentSearchList = documentSearchSearvice.getDocumentSearchByUser(context, userName);
		}
		List<String[]> metadataList = handleDocumentSearchMetadata(context, documentSearchList);
		
		Utils.handleCSV(context, request, response, metadataList, reportFileName);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/audit/generatepdf")
	public void generateDocumentSearchPDFReport(HttpServletResponse response,
												HttpServletRequest request,
												@RequestParam(name = "username", required = false) String userName,
												@RequestParam(name = "startdate") String startDate,
												@RequestParam(name = "enddate") String endDate) throws Exception {

		String reportFileName = "DocumentSearchedWiseReport %s";
		Date startDateObj = null;
		Date endDateObj = null;
		Context context = ContextUtil.obtainContext(request);
		List<DocumentSearch> documentSearchList = new ArrayList<DocumentSearch>();
		if (userName != null && startDate != null && endDate != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			startDateObj = formatter.parse(startDate + " 00:00:00");
			endDateObj = formatter.parse(endDate+ " 23:59:59");
			reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
			
			documentSearchList = documentSearchSearvice.getDocumentSearch(context, userName, startDateObj, endDateObj);
		} else if (startDate != null && endDate != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			startDateObj = formatter.parse(startDate + " 00:00:00");
			endDateObj = formatter.parse(endDate+ " 23:59:59");
			reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
			
			documentSearchList = documentSearchSearvice.getDocumentSearchByDateRange(context, startDateObj, endDateObj);
		} else {
			reportFileName = String.format(reportFileName,  "").trim();
			documentSearchList = documentSearchSearvice.getDocumentSearchByUser(context, userName);
		}
		List<String[]> metadataList = handleDocumentSearchMetadata(context, documentSearchList);
		
		Utils.handlePdf(context, request, response, metadataList, reportFileName);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/audit/generate")
	public List<String[]> generateDocumentSearchReport(HttpServletResponse response,
												HttpServletRequest request,
												@RequestParam(name = "username", required = false) String userName,
												@RequestParam(name = "startdate") String startDate,
												@RequestParam(name = "enddate") String endDate) throws Exception {

		Date startDateObj = null;
		Date endDateObj = null;
		Context context = ContextUtil.obtainContext(request);
		List<DocumentSearch> documentSearchList = new ArrayList<DocumentSearch>();
		if (userName != null && startDate != null && endDate != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			startDateObj = formatter.parse(startDate + " 00:00:00");
			endDateObj = formatter.parse(endDate+ " 23:59:59");
			
			documentSearchList = documentSearchSearvice.getDocumentSearch(context, userName, startDateObj, endDateObj);
		} else if (startDate != null && endDate != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			startDateObj = formatter.parse(startDate + " 00:00:00");
			endDateObj = formatter.parse(endDate+ " 23:59:59");
			
			documentSearchList = documentSearchSearvice.getDocumentSearchByDateRange(context, startDateObj, endDateObj);
		} else {
			documentSearchList = documentSearchSearvice.getDocumentSearchByUser(context, userName);
		}
		List<String[]> metadataList = handleSort20DocumentSearchMetadata(context, documentSearchList);
		return metadataList;
	}
	
	private List<String[]> handleDocumentSearchMetadata(Context context, List<DocumentSearch> documentSearchList) {
		List<String[]> resultList = new ArrayList<String[]>();
		List<String> metadatas = new ArrayList<String>(Arrays.asList("time", "ipaddress", "username", "query", "dc.casetype", "dc.title", "dc.caseyear", "dc.pname", "dc.rname"));
		List<String> headers = new ArrayList<String>(Arrays.asList("TIME", "IP ADDRESS", "USERNAME", "QUERY", "CASE TYPE", "CASE NUMBER", "CASE YEAR", "PETITIONER NAME", "RESPONDENT NAME"));
		
		String header[] = headers.toArray((new String[0]));
		resultList.add(header);
		
		if (documentSearchList != null) {
			for (DocumentSearch documentSearch : documentSearchList) {
				String ipAddress = documentSearch.getIpAddress();
				String itemIds = documentSearch.getItemIds();
				String userName = documentSearch.getUserName();
				String query = documentSearch.getQuery();
				
				SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				String entryDate = dateFormatter.format(documentSearch.getTime());
				
				if (itemIds != null && !itemIds.trim().isEmpty()) {
					String[] allItems = itemIds.split(",");
					if (allItems != null && allItems.length > 0) {
						for (String itemID : allItems) {
							List<String> metadataArray = new ArrayList<String>(Arrays.asList(entryDate, ipAddress, userName, query, "", "", "", "", "", "", ""));
							UUID itemUUID = UUID.fromString(itemID);
							try {
								Item item = itemService.find(context, itemUUID);
								if (item != null) {
									List<MetadataValue> metadataValues = item.getMetadata();
									if (metadataValues != null) {
										for (MetadataValue metadataValue : metadataValues) {
											String schema = metadataValue.getMetadataField().getMetadataSchema().getName();
											String element = metadataValue.getMetadataField().getElement();
											String qualifier = metadataValue.getMetadataField().getQualifier();
											
											String metadataName = schema + "." + element + ( (qualifier != null && !qualifier.isEmpty()) ? "." + qualifier : "");
											if (metadatas.contains(metadataName)) {
												metadataArray.set(metadatas.indexOf(metadataName), metadataValue.getValue());
											}
										}
									}
								}
							} catch (SQLException e) {
								log.error("Error in fetching Item Details of Document Search", e);
							}
							
							resultList.add(metadataArray.toArray(new String[0]));
						}
					}
				}
			}
		}
		return resultList;
	}
	
	private List<String[]> handleSort20DocumentSearchMetadata(Context context, List<DocumentSearch> documentSearchList) {
	    List<String[]> resultList = new ArrayList<>();
	    List<String> metadatas = new ArrayList<>(Arrays.asList(
	        "time", "ipaddress", "username","query", "dc.casetype", "dc.title", "dc.caseyear", "dc.pname", "dc.rname"
	    ));
	    List<String> headers = new ArrayList<>(Arrays.asList(
	        "TIME", "IP ADDRESS", "USERNAME",  "QUERY", "CASE TYPE", "CASE NUMBER", "CASE YEAR", "PETITIONER NAME", "RESPONDENT NAME"
	    ));
	    
	    String[] header = headers.toArray(new String[0]);
	    resultList.add(header);
	    
	    if (documentSearchList != null && !documentSearchList.isEmpty()) {
	        // Sort the list by entry date in descending order and limit to top 20
	        List<DocumentSearch> sortedDocumentSearchList = documentSearchList.stream()
	            .sorted(Comparator.comparing(DocumentSearch::getTime).reversed())
	            .limit(20)
	            .collect(Collectors.toList());
	        
	        for (DocumentSearch documentSearch : sortedDocumentSearchList) {
	            String ipAddress = documentSearch.getIpAddress();
	            String itemIds = documentSearch.getItemIds();
	            String userName = documentSearch.getUserName();
				String query = documentSearch.getQuery();

	            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	            String entryDate = dateFormatter.format(documentSearch.getTime());
	            
	            if (itemIds != null && !itemIds.trim().isEmpty()) {
	                String[] allItems = itemIds.split(",");
	                if (allItems != null && allItems.length > 0) {
	                    for (String itemID : allItems) {
	                        List<String> metadataArray = new ArrayList<>(Arrays.asList(
	                            entryDate, ipAddress, userName, query, "", "", "", "", "", "", ""
	                        ));
	                        UUID itemUUID = UUID.fromString(itemID);
	                        try {
	                            Item item = itemService.find(context, itemUUID);
	                            if (item != null) {
	                                List<MetadataValue> metadataValues = item.getMetadata();
	                                if (metadataValues != null) {
	                                    for (MetadataValue metadataValue : metadataValues) {
	                                        String schema = metadataValue.getMetadataField().getMetadataSchema().getName();
	                                        String element = metadataValue.getMetadataField().getElement();
	                                        String qualifier = metadataValue.getMetadataField().getQualifier();
	                                        
	                                        String metadataName = schema + "." + element + 
	                                            ((qualifier != null && !qualifier.isEmpty()) ? "." + qualifier : "");
	                                        
	                                        if (metadatas.contains(metadataName)) {
	                                            metadataArray.set(metadatas.indexOf(metadataName), metadataValue.getValue());
	                                        }
	                                    }
	                                }
	                            }
	                        } catch (SQLException e) {
	                            log.error("Error in fetching Item Details of Document Search", e);
	                        }
	                        
	                        resultList.add(metadataArray.toArray(new String[0]));
	                        if(resultList.size() == 21) {
	                        	return resultList;
	                        }
	                    }
	                }
	            }
	        }
	    }
	    return resultList;
	}
	

	
	/**
	 * Get search result from Discovery-Search
	 * @param query
	 * @param dsoTypes
	 * @param dsoScope
	 * @param configuration
	 * @param searchFilters
	 * @param page
	 * @return
	 * @throws Exception
	 */
	public List<SearchResultEntryRest> getSearchObjects(String query, List<String> dsoTypes, String dsoScope,
			String configuration, List<SearchFilter> searchFilters, Pageable page) throws Exception {

		dsoTypes = emptyIfNull(dsoTypes);

		if (log.isTraceEnabled()) {
			log.trace("Searching with scope: " + StringUtils.trimToEmpty(dsoScope) + ", configuration name: "
					+ StringUtils.trimToEmpty(configuration) + ", dsoTypes: " + String.join(", ", dsoTypes)
					+ ", query: " + StringUtils.trimToEmpty(query) + ", filters: " + Objects.toString(searchFilters)
					+ ", page: " + Objects.toString(page));
		}

		// Get the Search results
		List<SearchResultEntryRest> searchResultList = new ArrayList<SearchResultEntryRest>();
		try {
			SearchResultsRest searchResultsRest = discoveryRestRepository.getSearchObjects(query, dsoTypes, dsoScope,
					configuration, searchFilters, page, utils.obtainProjection());

			searchResultList = searchResultsRest.getSearchResults();
		} catch (IllegalArgumentException e) {
			boolean isParsingException = e.getMessage().contains(SOLR_PARSE_ERROR_CLASS);
			if (isParsingException) {
				throw new UnprocessableEntityException(e.getMessage());
			} else {
				throw e;
			}
		}
		return searchResultList;
	}

	private List<String[]> handleMetadata(String reportType, List<SearchResultEntryRest> searchResultList) throws SQLException {

		List<String[]> resultList = new ArrayList<String[]>();
		
		if (!reportType.equalsIgnoreCase("datewisefileupload") && !reportType.equalsIgnoreCase("casetype") 
			&& !reportType.equalsIgnoreCase("batchnamewisefileupload") && !reportType.equalsIgnoreCase("verify")) {
			reportType = "default";
		}
		
		String metadataList[] = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("report." + reportType);
		String header[] = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("report." + reportType + ".header");
		
		resultList.add(header);
		if (searchResultList != null) {
			for (SearchResultEntryRest searchResultEntry : searchResultList) {
				 if (!(searchResultEntry.getIndexableObject() instanceof ItemRest)) {
			        continue; 
			    }
				ItemRest itemRest = (ItemRest) searchResultEntry.getIndexableObject();
				MetadataRest metadataRest = itemRest.getMetadata();

				SortedMap<String, List<MetadataValueRest>> metadataMap = metadataRest.getMap();
				List<String> metadataArray = new ArrayList<String>();
				for (String metadataName : metadataList) {
					if (metadataMap.containsKey(metadataName)) {
						metadataArray.add(
										(metadataMap.get(metadataName) != null && !metadataMap.get(metadataName).isEmpty())
										? metadataMap.get(metadataName).get(0).getValue()
										: "");
					} else {
						if (metadataName.contains("dc.batchName")) {
							metadataArray.add(itemRest.getBatchName() != null ? itemRest.getBatchName() : "");
						} else {
							metadataArray.add("");	
						}
					}
				}
				resultList.add(metadataArray.toArray(new String[0]));
			}
		}
		
		return resultList;
	}
	
	private List<String[]> handleSort20Metadata(String reportType, List<SearchResultEntryRest> searchResultList) throws SQLException {

		List<String[]> resultList = new ArrayList<>();

	    if (!reportType.equalsIgnoreCase("datewisefileupload") && !reportType.equalsIgnoreCase("casetype")
	        && !reportType.equalsIgnoreCase("batchnamewisefileupload") && !reportType.equalsIgnoreCase("verify")) {
	        reportType = "default";
	    }

	    String metadataList[] = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("report." + reportType);
	    String header[] = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("report." + reportType + ".header");

	    resultList.add(header);

	    if (searchResultList != null) {
	        String dateMetadataField = "dc.date.accessioned";

	        // Sort the searchResultList by date in descending order
	        List<SearchResultEntryRest> sortedResultList = searchResultList.stream()
	            .sorted((entry1, entry2) -> {
	                ItemRest item1 = (ItemRest) entry1.getIndexableObject();
	                ItemRest item2 = (ItemRest) entry2.getIndexableObject();
	                MetadataRest<MetadataValueRest> metadataRest1 = item1.getMetadata();
	                MetadataRest<MetadataValueRest> metadataRest2 = item2.getMetadata();

	                String date1 = metadataRest1.getMap().get(dateMetadataField).get(0).getValue();
	                String date2 = metadataRest2.getMap().get(dateMetadataField).get(0).getValue();

	                // Parsing the date strings, assuming a common format (e.g., yyyy-MM-dd)
	                Date parsedDate1 = parseDate(date1);
	                Date parsedDate2 = parseDate(date2);

	                return parsedDate2.compareTo(parsedDate1); // Descending order
	            })
	            .limit(20) // Get top 20 entries
	            .collect(Collectors.toList());

	        // Process the sorted and limited list
	        for (SearchResultEntryRest searchResultEntry : sortedResultList) {
	            ItemRest itemRest = (ItemRest) searchResultEntry.getIndexableObject();
	            MetadataRest metadataRest = itemRest.getMetadata();

	            SortedMap<String, List<MetadataValueRest>> metadataMap = metadataRest.getMap();
	            List<String> metadataArray = new ArrayList<>();
	            for (String metadataName : metadataList) {
	                if (metadataMap.containsKey(metadataName)) {
	                    metadataArray.add(
	                        (metadataMap.get(metadataName) != null && !metadataMap.get(metadataName).isEmpty())
	                        ? metadataMap.get(metadataName).get(0).getValue()
	                        : "");
	                } else {
	                    if (metadataName.contains("dc.batchName")) {
	                        metadataArray.add(itemRest.getBatchName() != null ? itemRest.getBatchName() : "");
	                    } else {
	                        metadataArray.add("");	
	                    }
	                }
	            }
	            resultList.add(metadataArray.toArray(new String[0]));
	        }
	    }

	    return resultList;
	}
	
	public List<String[]> handleMetadataForAdvanceSearchReport(String reportType, List<AuditTrail> searchResultList, String download)
	{
		List<String[]> resultList = new ArrayList<String[]>();
		String header[] = configurationService.getArrayProperty("advanceSearchPdfHeader");
		resultList.add(header);
		if (searchResultList != null) 
		{
			for (AuditTrail searchResultEntry : searchResultList) {
				List<String>  metadataArray = new ArrayList<>() ;
				String action = "";
				if(searchResultEntry.getAction()!=null)
				{
					action = searchResultEntry.getAction().split("=")[1];
				}
				metadataArray.add(action);
				metadataArray.add(searchResultEntry.getUserName());
				metadataArray.add(searchResultEntry.getIpAddress());
				metadataArray.add(searchResultEntry.getTime().toString());
	
				resultList.add(metadataArray.toArray(new String[0]));
			}
		}
		
		return resultList;
	}
	
	public List<String[]> handleSort20MetadataForAdvanceSearchReport(String reportType, List<AuditTrail> searchResultList, String download)
	{
		List<String[]> resultList = new ArrayList<String[]>();
		String header[] = configurationService.getArrayProperty("advanceSearchPdfHeader");
		resultList.add(header);
		if (searchResultList != null) 
		{
			for (AuditTrail searchResultEntry : searchResultList) {
				List<String>  metadataArray = new ArrayList<>() ;
				String action = "";
				if(searchResultEntry.getAction()!=null)
				{
					action = searchResultEntry.getAction().split("=")[1];
				}
				metadataArray.add(action);
				metadataArray.add(searchResultEntry.getUserName());
				metadataArray.add(searchResultEntry.getIpAddress());
				metadataArray.add(searchResultEntry.getTime().toString());
	
				resultList.add(metadataArray.toArray(new String[0]));
				if(resultList.size() > 21) {
					return resultList;
				}
			}
		}
		
		return resultList;
	}
	
	private Date parseDate(String dateString) {
	    // Implement your date parsing logic here based on the expected date format
	    // For example, if the date format is "yyyy-MM-dd":
	    try {
	        return new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
	    } catch (ParseException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
}