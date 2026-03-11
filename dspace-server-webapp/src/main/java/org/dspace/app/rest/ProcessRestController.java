package org.dspace.app.rest;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.batchhistory.service.BatchHistoryService;
import org.dspace.batchreject.service.BatchRejectService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Department;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DepartmentService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/processes")
public class ProcessRestController {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ProcessService processService;
    
    @Autowired
    private CollectionService collectionService;
    
    @Autowired
	private CommunityService communityService;
    
    @Autowired
	private ItemService itemService;
    
    @Autowired
    private RequestService requestService;
    
    @Autowired
    private BatchHistoryService batchhistoryService;
    
    @Autowired(required = true)
    protected BatchRejectService batchRejectService;
    
    @Autowired(required = true)
    private AuthorizeService authorizeService;
    
    @Autowired(required = true)
    private ConfigurationService configurationService;
    
	@Autowired(required=true)
	protected static EPersonService epersonService;
	
	@Autowired(required = true)
    protected MetadataFieldService metadataFieldService;
	
	@Autowired(required = true)
    private UserMetadataFieldsService userMetadataFieldsService;
    
    @Autowired(required = true)
    private DepartmentService departmentService;


	static String adminEmailId = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.default.admin.user.email");

    private static final DateTimeFormatter INPUT_FMT =  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter OUTPUT_FMT =  DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

	@GetMapping("/generatecsv")
	public void generateProcessCSVReport(HttpServletResponse response,
											HttpServletRequest request,
											@RequestParam(name = "collection") String collectionID,
											@RequestParam(name = "startdate") String startDate,
											@RequestParam(name = "enddate") String endDate,
											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
		try {
			Context context = ContextUtil.obtainContext(request);
			List<Process> processList = new ArrayList<Process>();
			String reportFileName = "Report-"+ scriptName.toUpperCase() +" %s";
			
			Collection collection = null;
			Date startDateObj = null;
			Date endDateObj = null;
			if (collectionID != null && !collectionID.isEmpty()) {
				UUID collectionUUid = UUID.fromString(collectionID);
				collection = collectionService.find(context, collectionUUid);
			}
			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				startDateObj = formatter.parse(startDate + " 00:00:00");
				endDateObj = formatter.parse(endDate + " 23:59:59");
				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
			} else {
				reportFileName = String.format(reportFileName,  "");
			}
//			processList = processService.getProcess(context, scriptName, collection, startDateObj, endDateObj);
			
			// creating Rows entry for Table record
			List<String[]> metadata = fetchMetadataResponseFormat(context, processList);
			// generating and flushing PDF file, context completion handled inside
			Utils.handleCSV(context, request, response, metadata, reportFileName);
		} catch (Exception e) {
			log.error("Error in generating pdf for batch-import report", e);
		}
	}
    
    @GetMapping("/generatepdf")
	public void generateProcessPDFReport(HttpServletResponse response,
											HttpServletRequest request,
											@RequestParam(name = "collection") String collectionID,
											@RequestParam(name = "startdate") String startDate,
											@RequestParam(name = "enddate") String endDate,
											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
		try {
			Context context = ContextUtil.obtainContext(request);
			List<Process> processList = new ArrayList<Process>();
			String reportFileName = "Report-"+ scriptName.toUpperCase() +" %s";
			
			Collection collection = null;
			Date startDateObj = null;
			Date endDateObj = null;
			if (collectionID != null && !collectionID.isEmpty()) {
				UUID collectionUUid = UUID.fromString(collectionID);
				collection = collectionService.find(context, collectionUUid);
			}
			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				startDateObj = formatter.parse(startDate + " 00:00:00");
				endDateObj = formatter.parse(endDate+ " 23:59:59");
				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
			} else {
				reportFileName = String.format(reportFileName,  "");
			}
//			processList = processService.getProcess(context, scriptName, collection, startDateObj, endDateObj);
			
			// creating Rows entry for Table record
			List<String[]> metadata = fetchMetadataResponseFormat(context, processList);
			// generating and flushing PDF file, context completion handled inside
			Utils.handlePdf(context, request, response, metadata, reportFileName);
		} catch (Exception e) {
			log.error("Error in generating pdf for batch-import report", e);
		}
	}
    
    @GetMapping("/generate")
	public List<String[]> generateProcessReport(HttpServletResponse response,
											HttpServletRequest request,
											@RequestParam(name = "collection") String collectionID,
											@RequestParam(name = "startdate") String startDate,
											@RequestParam(name = "enddate") String endDate,
											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
		try {
			Context context = ContextUtil.obtainContext(request);
			List<Process> processList = new ArrayList<Process>();
			
			Collection collection = null;
			Date startDateObj = null;
			Date endDateObj = null;
			if (collectionID != null && !collectionID.isEmpty()) {
				UUID collectionUUid = UUID.fromString(collectionID);
				collection = collectionService.find(context, collectionUUid);
			}
			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				startDateObj = formatter.parse(startDate + " 00:00:00");
				endDateObj = formatter.parse(endDate + " 23:59:59");
			}
//			processList = processService.getProcess(context, scriptName, collection, startDateObj, endDateObj);
			
			// creating Rows entry for Table record
			List<String[]> metadata = fetchMetadataSort20ResponseFormat(context, processList);
			// generating and flushing PDF file, context completion handled inside
			return metadata;
		} catch (Exception e) {
			log.error("Error in generating batch-import report", e);
		}
		return null;
	}
	
    @RequestMapping(method = RequestMethod.GET, value = "/verify/generatecsv")
	public void generateCSVReport(HttpServletResponse response,
			HttpServletRequest request,
			@RequestParam(name = "subDepartment") String subDepartment,
			@RequestParam(name = "startdate") String startDate,
			@RequestParam(name = "enddate") String endDate) throws Exception {

		try {
			Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
			String reportFileName = "Report-BatchImport-";
			UUID sectionId = UUID.fromString(subDepartment);
			Community community =  communityService.find(context, sectionId);
			if(community != null) {
				reportFileName += community.getName();
			}
			reportFileName = reportFileName+" "+ startDate + " to " + endDate;
			
			Iterator<Object[]> itr = getBatchUploadReport(context, community);
			List<String[]> metadata = handleSortBatchUploadMetadata(context, itr, community, startDate, endDate);
			Utils.handleCSV(context, request, response, metadata, reportFileName);
		} catch (Exception e) {
			log.error("Error in generating csv for batch-upload report", e);
		}
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/verify/generatepdf")
	public void generatePDFReport(HttpServletResponse response,
			HttpServletRequest request,
			@RequestParam(name = "subDepartment") String subDepartment,
			@RequestParam(name = "startdate") String startDate,
			@RequestParam(name = "enddate") String endDate) throws Exception {
		
		try {
			Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
			String reportFileName = "Report-BatchImport-";
			UUID sectionId = UUID.fromString(subDepartment);
			Community community =  communityService.find(context, sectionId);
			if(community != null) {
				reportFileName += community.getName();
			}
			reportFileName = reportFileName+" "+ startDate + " to " + endDate;
			
			Iterator<Object[]> itr = getBatchUploadReport(context, community);
			List<String[]> metadata = handleSortBatchUploadMetadata(context, itr, community, startDate, endDate);
			Utils.handlePdf(context, request, response, metadata, reportFileName);
		} catch (Exception e) {
			log.error("Error in generating pdf for batch-upload report", e);
		}
	}
	
	
	@RequestMapping(method = RequestMethod.GET, value = "/verify/generate")
	public List<String[]> generateReport(HttpServletResponse response,
			HttpServletRequest request,
			@RequestParam(name = "subDepartment") String subDepartment,
			@RequestParam(name = "startdate") String startDate,
			@RequestParam(name = "enddate") String endDate) throws Exception {
		
		try {
			Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
			UUID sectionId = UUID.fromString(subDepartment);
			Community community =  communityService.find(context, sectionId);
			Iterator<Object[]> itr = getBatchUploadReport(context, community);
			List<String[]> metadata = handleSort20BatchUploadMetadata(context, itr, community, startDate, endDate);
			return metadata;
		} catch (Exception e) {
			log.error("Error in generating batch-upload report", e);
		}
		return new ArrayList<String[]>();
	}
	
	private Iterator<Object[]> getBatchUploadReport(Context context, Community selectedCommunity) throws SQLException {
		Set<MetadataField> topUserFieldsSet = new LinkedHashSet<MetadataField>();
		List<Community> subCommunities = new ArrayList<Community>();
		List<Collection> subCollections = new ArrayList<Collection>();
		MetadataField uploadmf = metadataFieldService.findByString(context, "dc.date.accessioned", '.');
		if (uploadmf != null) {
			topUserFieldsSet.add(uploadmf);
		}
		MetadataField batchmf = metadataFieldService.findByString(context, "dc.batch-number", '.');
		if (batchmf != null) {
			topUserFieldsSet.add(batchmf);
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
		return itemService.getBatchUploadReport(context, subCommunities, subCollections, topUserFields.toArray(new MetadataField[0]));
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
	
	private String[] getBatchUploadHeading(Context context, Community selectedCommunity) throws SQLException {
	    Set<String> headings = new LinkedHashSet<String>();
	    headings.add("UPLOAD DATE");
	    headings.add("BATCH NAME");
	    headings.add("TOTAL ITEMS");
//		if(selectedCommunity != null) {
//	        addUserFieldHeadings(context, selectedCommunity, headings);
//	   } else {
//			List<Community> communities = communityService.findAllTop(context);
//			for(Community community : communities) {
//				Department department = departmentService.findByCommunity(context, community);
//				if(department != null) {
//					for (Community com : community.getSubcommunities()) {
//	                    addUserFieldHeadings(context, com, headings);
//					}
//				} else {
//					addUserFieldHeadings(context, community, headings);
//				}
//			}
//		}
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

	private List<String[]> handleSortBatchUploadMetadata(Context context, Iterator<Object[]> itr, Community selectedCommunity, String startDate, String endDate) throws SQLException {
		LocalDateTime startDateOb = LocalDateTime.parse(startDate + " 00:00:00",INPUT_FMT);
		String startdate= startDateOb.format(OUTPUT_FMT);
		LocalDateTime startDateObj=LocalDateTime.parse(startdate, OUTPUT_FMT);
		LocalDateTime endDateOb = LocalDateTime.parse(endDate + " 23:59:59",INPUT_FMT);
		String enddate= endDateOb.format(OUTPUT_FMT);
		LocalDateTime endDateObj=LocalDateTime.parse(enddate, OUTPUT_FMT);	
		
		ZoneId istZone = ZoneId.of("Asia/Kolkata");

		List<String[]> metadata =  new ArrayList<String[]>();
		String[] header = getBatchUploadHeading(context, selectedCommunity);
		metadata.add(header);
		Set<String> uniquemetadata = new HashSet<>();
		while(itr.hasNext()) {
			Object[] row = itr.next();
			String[] metadataArray = new String[header.length];
			//Convert date format
		    if (row.length > 0 && row[0] != null) {
		    	String uniqueCombo =
		    		    (row.length > 1 ? row[1] : "") + "_" +
		    		    (row.length > 0 ? row[0] : "");
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
		    
		    for (int i = 1; i < row.length; i++) {
		        metadataArray[i] = row[i] != null ? row[i].toString() : null;
		    }
		    metadata.add(metadataArray);
		}
		return metadata;
	}
	
	private List<String[]> handleSort20BatchUploadMetadata(Context context, Iterator<Object[]> itr, Community selectedCommunity, String startDate, String endDate) throws SQLException {
		LocalDateTime startDateOb = LocalDateTime.parse(startDate + " 00:00:00",INPUT_FMT);
		String startdate= startDateOb.format(OUTPUT_FMT);
		LocalDateTime startDateObj=LocalDateTime.parse(startdate, OUTPUT_FMT);
		LocalDateTime endDateOb = LocalDateTime.parse(endDate + " 23:59:59",INPUT_FMT);
		String enddate= endDateOb.format(OUTPUT_FMT);
		LocalDateTime endDateObj=LocalDateTime.parse(enddate, OUTPUT_FMT);	
		
		ZoneId istZone = ZoneId.of("Asia/Kolkata");

		List<String[]> metadata =  new ArrayList<String[]>();
		String[] header = getBatchUploadHeading(context, selectedCommunity);
		metadata.add(header);
		Set<String> uniquemetadata = new HashSet<>();
		while(itr.hasNext()) {
			Object[] row = itr.next();
			String[] metadataArray = new String[header.length];
			//Convert date format
		    if (row.length > 0 && row[0] != null) {
		    	String uniqueCombo =
		    		    (row.length > 1 ? row[1] : "") + "_" +
		    		    (row.length > 0 ? row[0] : "");
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
		    
		    for (int i = 1; i < row.length; i++) {
		        metadataArray[i] = row[i] != null ? row[i].toString() : null;
		    }
		    metadata.add(metadataArray);
		    if(metadata.size() == 21) {
            	return metadata;
            }
		}
		return metadata;
	}

//	@GetMapping("/generatecsvReject")
//	public void generateProcessCSVReportRejected(HttpServletResponse response,
//											HttpServletRequest request,
//											@RequestParam(name = "collection") String collectionID,
//											@RequestParam(name = "startdate") String startDate,
//											@RequestParam(name = "enddate") String endDate,
//											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
//		try {
//			Context context = ContextUtil.obtainContext(request);
//			String reportFileName = "Report-REJECT-" +" %s";
//			Collection collection = null;
//			Date startDateObj = null;
//			Date endDateObj = null;
//			List<BatchReject> batchRejectList = null;
//			if (collectionID != null && !collectionID.isEmpty()) {
//				UUID collectionUUid = UUID.fromString(collectionID);
//				collection = collectionService.find(context, collectionUUid);
//				reportFileName = "Report-REJECT-" +collection.getName() + "-" + " %s";
//			}
//			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
//				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				startDateObj = formatter.parse(startDate + " 00:00:00");
//				endDateObj = formatter.parse(endDate + " 23:59:59");
//				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
//			} else {
//				reportFileName = String.format(reportFileName,  "");
//			}
//			
//			if(collection!=null) {
//				
//			}
//			batchRejectList = batchRejectService.findByTimeRange(context, startDateObj, endDateObj);
//			List<String[]> metadata = fetchBatchMetadataResponseFormat(context, batchRejectList,collection,startDateObj,endDateObj,false);
//			Utils.handleCSV(context, request, response, metadata, reportFileName);
//		} catch (Exception e) {
//			log.error("Error in generating pdf for batch-reject report", e);
//		}
//	}
//	
//	
//	@GetMapping("/generatepdfReject")
//	public void generateProcessPDFReportRejected(HttpServletResponse response,
//											HttpServletRequest request,
//											@RequestParam(name = "collection") String collectionID,
//											@RequestParam(name = "startdate") String startDate,
//											@RequestParam(name = "enddate") String endDate,
//											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
//		try {
//			Context context = ContextUtil.obtainContext(request);
//			String reportFileName = "Report-REJECT-" +" %s";
//			Collection collection = null;
//			Date startDateObj = null;
//			Date endDateObj = null;
//			List<BatchReject> batchRejectList = null;
//			if (collectionID != null && !collectionID.isEmpty()) {
//				UUID collectionUUid = UUID.fromString(collectionID);
//				collection = collectionService.find(context, collectionUUid);
//				reportFileName = "Report-REJECT-" +collection.getName() + "-" + " %s";
//			}
//			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
//				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				startDateObj = formatter.parse(startDate + " 00:00:00");
//				endDateObj = formatter.parse(endDate + " 23:59:59");
//				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
//			} else {
//				reportFileName = String.format(reportFileName,  "");
//			}
//			
//            if(collection!=null)
//            {
//            	batchRejectList = batchRejectService.findByTimeRange(context, startDateObj, endDateObj);
//    			List<String[]> metadata = fetchBatchMetadataResponseFormat(context, batchRejectList,collection,startDateObj,endDateObj,false);
//				Utils.handlePdf(context, request, response, metadata, reportFileName);
//            }
//		} catch (Exception e) {
//			log.error("Error in generating pdf for batch-reject report", e);
//		}
//	}
//	 
//	@GetMapping("/generateReject")
//	public List<String[]> generateProcessReportRejected(HttpServletResponse response,
//											HttpServletRequest request,
//											@RequestParam(name = "collection") String collectionID,
//											@RequestParam(name = "startdate") String startDate,
//											@RequestParam(name = "enddate") String endDate,
//											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
//		try {
//			Context context = ContextUtil.obtainContext(request);
//			String reportFileName = "Report-REJECT-" +" %s";
//			Collection collection = null;
//			Date startDateObj = null;
//			Date endDateObj = null;
//			List<BatchReject> batchRejectList = null;
//			if (collectionID != null && !collectionID.isEmpty()) {
//				UUID collectionUUid = UUID.fromString(collectionID);
//				collection = collectionService.find(context, collectionUUid);
//				reportFileName = "Report-REJECT-" +collection.getName() + "-" + " %s";
//			}
//			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
//				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				startDateObj = formatter.parse(startDate + " 00:00:00");
//				endDateObj = formatter.parse(endDate + " 23:59:59");
//				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
//			} else {
//				reportFileName = String.format(reportFileName,  "");
//			}
//			
//            if(collection!=null)
//            {
//            	batchRejectList = batchRejectService.findByTimeRange(context, startDateObj, endDateObj);
//    			List<String[]> metadata = fetchBatchMetadataResponseFormat(context, batchRejectList,collection,startDateObj,endDateObj,true);
//    			return metadata;
//            }
//		} catch (Exception e) {
//			log.error("Error in generating pdf for batch-reject report", e);
//		}
//		return null;
//	}
//	 
//	@GetMapping("/generatecsvApprove")
//	public void generateProcessCSVReportApproved(HttpServletResponse response,
//											HttpServletRequest request,
//											@RequestParam(name = "collection") String collectionID,
//											@RequestParam(name = "startdate") String startDate,
//											@RequestParam(name = "enddate") String endDate,
//											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
//		try {
//			Context context = ContextUtil.obtainContext(request);
//			String reportFileName = "Report-APPROVE-" +" %s";
//			Collection collection = null;
//			Date startDateObj = null;
//			Date endDateObj = null;
//			if (collectionID != null && !collectionID.isEmpty()) {
//				UUID collectionUUid = UUID.fromString(collectionID);
//				collection = collectionService.find(context, collectionUUid);
//				reportFileName = "Report-APPROVE-" +collection.getName() + "-" + " %s";
//			}
//			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
//				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				startDateObj = formatter.parse(startDate + " 00:00:00");
//				endDateObj = formatter.parse(endDate + " 23:59:59");
//				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
//			} else {
//				reportFileName = String.format(reportFileName,  "");
//			}
//			List<BatchHistory> batchHistoryList = batchhistoryService.getByTimeandState(context, startDateObj, endDateObj, "approved");
//			List<String[]> metadata = fetchBatchMetadataResponseFormat(context, batchHistoryList,collection,startDateObj,endDateObj,false);
//			Utils.handleCSV(context, request, response, metadata, reportFileName);
//		} catch (Exception e) {
//			log.error("Error in generating pdf for batch-approve report", e);
//		}
//	}
//	 
//	@GetMapping("/generatepdfApprove")
//	public void generateProcessPDFReportApproved(HttpServletResponse response,
//											HttpServletRequest request,
//											@RequestParam(name = "collection") String collectionID,
//											@RequestParam(name = "startdate") String startDate,
//											@RequestParam(name = "enddate") String endDate,
//											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
//		try {
//			Context context = ContextUtil.obtainContext(request);
//			String reportFileName = "Report-APPROVE-" +" %s";
//			Collection collection = null;
//			Date startDateObj = null;
//			Date endDateObj = null;
//			if (collectionID != null && !collectionID.isEmpty()) {
//				UUID collectionUUid = UUID.fromString(collectionID);
//				collection = collectionService.find(context, collectionUUid);
//				reportFileName = "Report-APPROVE-" +collection.getName() + "-" + " %s";
//			}
//			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
//				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				startDateObj = formatter.parse(startDate + " 00:00:00");
//				endDateObj = formatter.parse(endDate + " 23:59:59");
//				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
//			} else {
//				reportFileName = String.format(reportFileName,  "");
//			}
//			List<BatchHistory> batchHistoryList = batchhistoryService.getByTimeandState(context, startDateObj, endDateObj, "approved");
//			List<String[]> metadata = fetchBatchMetadataResponseFormat(context, batchHistoryList,collection,startDateObj,endDateObj,false);
//			Utils.handlePdf(context, request, response, metadata, reportFileName);
//		} catch (Exception e) {
//			log.error("Error in generating pdf for batch-approve report", e);
//		}
//	}
//	 
//	@GetMapping("/generateApprove")
//	public List<String[]> generateProcessReportApproved(HttpServletResponse response,
//											HttpServletRequest request,
//											@RequestParam(name = "collection") String collectionID,
//											@RequestParam(name = "startdate") String startDate,
//											@RequestParam(name = "enddate") String endDate,
//											@RequestParam(value = "scriptName") String scriptName) throws SQLException {
//		try {
//			Context context = ContextUtil.obtainContext(request);
//			String reportFileName = "Report-APPROVE-" +" %s";
//			Collection collection = null;
//			Date startDateObj = null;
//			Date endDateObj = null;
//			if (collectionID != null && !collectionID.isEmpty()) {
//				UUID collectionUUid = UUID.fromString(collectionID);
//				collection = collectionService.find(context, collectionUUid);
//				reportFileName = "Report-APPROVE-" +collection.getName() + "-" + " %s";
//			}
//			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
//				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				startDateObj = formatter.parse(startDate + " 00:00:00");
//				endDateObj = formatter.parse(endDate + " 23:59:59");
//				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
//			} else {
//				reportFileName = String.format(reportFileName,  "");
//			}
//			List<BatchHistory> batchHistoryList = batchhistoryService.getByTimeandState(context, startDateObj, endDateObj, "approved");
//			List<String[]> metadata = fetchBatchMetadataResponseFormat(context, batchHistoryList,collection,startDateObj,endDateObj,true);
//			return metadata;
//		} catch (Exception e) {
//			log.error("Error in generating pdf for batch-approve report", e);
//		}
//		return null;
//	}
//	 
	 	 
	private List<String[]> fetchMetadataResponseFormat(Context context, List<Process> processList) {
		ArrayList<String[]> metadataList = new ArrayList<String[]>();		
		String[] header = {"BATCH NUMBER","UPLOAD BY","UPLOAD TIME","RECORD COUNT","PAGE COUNT"};
		//Adding header to pdf
		metadataList.add(header);
		//inserting data into metadatlist for pdf generation
		try {
			for(int rowNum = 0; rowNum < processList.size(); rowNum++){
				Process process = processList.get(rowNum);
				SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				Date creationTime = Date.from(process.getCreationTime());
//				Integer recordCounts = process.getTotalFiles();
//				Integer totalPageCounts = process.getTotalPages();

				ArrayList<String> metadataArray = new ArrayList<String>();
				metadataArray.add(process.getBatchName());
				metadataArray.add(process.getEPerson().getFullName());
				metadataArray.add(dateFormatter.format(creationTime));
//				metadataArray.add(String.valueOf(recordCounts));
//				metadataArray.add(String.valueOf(totalPageCounts));
				metadataList.add(metadataArray.toArray(new String[0]));
			}
		} catch (Exception e) {
			log.error("Error in formatting audittrail data", e);
		}
		return metadataList;
	}
	
//private List<String[]> fetchMetadataResponseFormat(Context context, List<Process> processList) {
//	ArrayList<String[]> metadataList = new ArrayList<String[]>();		
//	String[] header = {"BATCH NUMBER","UPLOAD BY","UPLOAD TIME","RECORD COUNT","PAGE COUNT"};
//	//Adding header to pdf
//	metadataList.add(header);
//	//inserting data into metadatlist for pdf generation
//	try {
//		for(int rowNum = 0; rowNum < processList.size(); rowNum++){
//			Process process = processList.get(rowNum);
//			SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
//			Date creationTime = process.getCreationTime();
//			Integer recordCounts = process.getTotalFiles();
//			Integer totalPageCounts = process.getTotalPages();
//
//			ArrayList<String> metadataArray = new ArrayList<String>();
//			metadataArray.add(process.getBatchName());
//			metadataArray.add(process.getEPerson().getFullName());
//			metadataArray.add(dateFormatter.format(creationTime));
//			metadataArray.add(String.valueOf(recordCounts));
//			metadataArray.add(String.valueOf(totalPageCounts));
//			metadataList.add(metadataArray.toArray(new String[0]));
//		}
//	} catch (Exception e) {
//		log.error("Error in formatting audittrail data", e);
//	}
//	return metadataList;
//}
	
	private List<String[]> fetchMetadataSort20ResponseFormat(Context context, List<Process> processList) {
		 ArrayList<String[]> metadataList = new ArrayList<String[]>();
		    String[] header = {"BATCH NUMBER", "UPLOAD BY", "UPLOAD TIME", "RECORD COUNT", "PAGE COUNT"};
		    
		    // Adding header to PDF
		    metadataList.add(header);

		    // Sorting processList based on creation date in descending order
		    Collections.sort(processList, new Comparator<Process>() {
		        @Override
		        public int compare(Process p1, Process p2) {
		            return p2.getCreationTime().compareTo(p1.getCreationTime());
		        }
		    });

		    // Limiting to top 20 entries
		    int limit = Math.min(processList.size(), 20);
		    
		    try {
		        for (int rowNum = 0; rowNum < limit; rowNum++) {
		            Process process = processList.get(rowNum);
		            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
					Date creationTime = Date.from(process.getCreationTime());
//		            Integer recordCounts = process.getTotalFiles();
//		            Integer totalPageCounts = process.getTotalPages();

		            ArrayList<String> metadataArray = new ArrayList<String>();
		            metadataArray.add(process.getBatchName());
		            metadataArray.add(process.getEPerson().getFullName());
		            metadataArray.add(dateFormatter.format(creationTime));
//		            metadataArray.add(String.valueOf(recordCounts));
//		            metadataArray.add(String.valueOf(totalPageCounts));
		            metadataList.add(metadataArray.toArray(new String[0]));
		        }
		    } catch (Exception e) {
		        log.error("Error in formatting audit trail data", e);
		    }
		    return metadataList;
	}
	
	private String getMetadataValue(Item item, String element, String qualifier) {
	    return itemService.getMetadataFirstValue(item, MetadataSchemaEnum.DC.getName(), element, qualifier, Item.ANY);
	}


//	private <T> List<String[]> fetchBatchMetadataResponseFormat(Context context, List<T> entityList, 
//	        Collection collection, Date startDateObj, Date endDateObj, boolean isOffsetRequired) {
//	    
//	    List<String[]> metadataList = new ArrayList<>();
//	    
//	    if (entityList.isEmpty()) {
//	        return metadataList;
//	    }
//	    
//	    boolean isBatchReject = entityList.get(0) instanceof BatchReject;
//	    
//	    String[] header = isBatchReject 
//	        ? DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("report.batch.reject.header")
//	        : DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("report.batch.approve.header");
//	    
//	    metadataList.add(header);
//
//	    int limit = isOffsetRequired ? Math.min(entityList.size(), 20) : entityList.size();
//
//	    try {
//	        for (int rowNum = 0; rowNum < limit; rowNum++) {
//	            String[] metadata = isBatchReject 
//	                ? getBatchRejectMetadata((BatchReject) entityList.get(rowNum), context, collection, startDateObj, endDateObj) 
//	                : getBatchHistoryMetadata((BatchHistory) entityList.get(rowNum), context, collection, startDateObj, endDateObj);
//	            
//	            if (metadata != null) {
//	                metadataList.add(metadata);
//	            }
//	        }
//	    } catch (Exception e) {
//	        log.error("Error in formatting approval/rejection data", e);
//	    }
//
//	    return metadataList;
//	}

//	private String[] getBatchRejectMetadata(BatchReject batchReject, Context context, Collection collection, Date startDate, Date endDate) throws SQLException {
//	    if (batchReject == null || batchReject.getBatchName() == null) {
//	        return null;
//	    }
//
//	    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
//	    Date rejectionTime = batchReject.getTime();
//
//	    List<Process> processList = processService.findByStatusCollectionAndBatchName(context, batchReject.getBatchName(),
//	            "import", collection, startDate, endDate, ProcessStatus.REJECTED);
//	    
//	    int recordCount = processList.isEmpty() ? 0 : processList.get(0).getTotalFiles();
//
//	    return new String[]{
//	        batchReject.getBatchName(),
//	        dateFormatter.format(rejectionTime),
//	        String.valueOf(recordCount),
//	        batchReject.getUser().getFullName(),
//	        batchReject.getReason()
//	    };
//	}

//	private String[] getBatchHistoryMetadata(BatchHistory batchHistory, Context context, Collection collection, Date startDate, Date endDate) throws SQLException {
//	    if (batchHistory == null || batchHistory.getBatchName() == null) {
//	        return null;
//	    }
//
//	    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
//	    Date approvalTime = batchHistory.getTime();
//
//	    List<Process> processList = processService.findByStatusCollectionAndBatchName(context, batchHistory.getBatchName(),
//	            "import", collection, startDate, endDate, ProcessStatus.COMPLETED);
//	    
//	    int recordCount = processList.isEmpty() ? 0 : processList.get(0).get();
//
//	    return new String[]{
//	        batchHistory.getBatchName(),
//	        dateFormatter.format(approvalTime),
//	        String.valueOf(recordCount),
//	        batchHistory.getOwner().getFullName()
//	    };
//	}
	
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.HEAD }, value = "storage-checker")
	public static Double[] UbuntuStorageTracker(HttpServletRequest request,HttpServletResponse response) throws SQLException {
	        // Detect OS
	    	String dspaceInstallConfigPath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir");

	    	File installer = new File(dspaceInstallConfigPath);
	    	File root = installer.getParentFile();
	    	Double[] values = new Double[4];
	    	// Get total memory size
	        long totalSpace = root.getTotalSpace(); // Total disk space in bytes
	        long freeSpace = root.getFreeSpace();   // Free disk space in bytes
	        long usedSpace = totalSpace - freeSpace;

	        // Convert bytes to GB for readability
	        double totalSpaceGB = totalSpace / (1024.0 * 1024 * 1024);
	        double freeSpaceGB = freeSpace / (1024.0 * 1024 * 1024);
	        double usedSpaceGB = usedSpace / (1024.0 * 1024 * 1024);

	        log.info("Total Storage ( " + root.getAbsolutePath() + "): " + totalSpaceGB + " GB\n");
	        log.info("Available Storage ( " + root.getAbsolutePath() + "): " + freeSpaceGB + " GB\n");
	        
	        values[0] = totalSpaceGB;
	        values[1] = usedSpaceGB;
	        values[2] = freeSpaceGB;
	        values[3] = (usedSpaceGB * 100.0) / totalSpaceGB;

	        return values;
	}
	
	@PostMapping("/shutdown-alert")
	public ResponseEntity<?> alertForShutdown(HttpServletRequest request,HttpServletResponse response, @RequestBody Map<String, String> requestMap) throws SQLException {

		Context context =  ContextUtil.obtainContext(request);
		if (context.getCurrentUser() == null) {
			
			if(epersonService == null) {
				epersonService = EPersonServiceFactory.getInstance().getEPersonService();
			}
			
			EPerson eperson = epersonService.findByEmail(context, adminEmailId);
			context.setCurrentUser(eperson);
		}

	    String reason = requestMap.get("reason");
	    String date = requestMap.get("date");   // e.g., "2025-05-30"
	    String time = requestMap.get("time");   // e.g., "16:00"

	    // You can now log or store these values
	    log.info("Shutdown Alert Received:");
	    log.info("Reason: " + reason);
	    log.info("Date: " + date);
	    log.info("Time: " + time);
		try {

			Locale supportedLocale = I18nUtil.getEPersonLocale(context.getCurrentUser());
			Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "send_alert"));
			List<EPerson> epersonList = epersonService.findAll(context, EPerson.EMAIL, -1, -1);
			for(EPerson ep : epersonList) {
				email.addRecipient(ep.getEmail());
			}
			
			email.addArgument(reason);
			email.addArgument(date);
			email.addArgument(time);
			email.addArgument(context.getCurrentUser().getFullName());
			email.send();
			return ResponseEntity.status(HttpStatus.OK).body("Alert sent Successfully");
		} catch (IOException | MessagingException e) {
			log.error("Error while sending Alert " + e);
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Folder path cannot be empty.");
		}
	}

	
//	@RequestMapping(method = { RequestMethod.GET, RequestMethod.HEAD }, value = "datatrendBar")
//   	public String[][] getDataTrendBarResult(@RequestParam(value = "typeName") String typeName,
//   											HttpServletRequest request,HttpServletResponse response,
//   											Pageable pageable) throws SQLException {
//		List<String[]> info = new ArrayList<String[]>();
//		List<Object[]> itr = new ArrayList<Object[]>();
//		Community community = null;
//		if(itemService == null) {
//			itemService = ContentServiceFactory.getInstance().getItemService();
//		}
//       	try {
//    		Context context = ContextUtil.obtainContext(request);
//	       	List<Group> groups = context.getCurrentUser().getGroups();
//
//    		boolean isAdmin = authorizeService.isAdmin(context);
//    		
//   			if (typeName == null || typeName.isEmpty()) {
//   				log.error("Type Name is Empty");
//   			}
//   			
//   			String metadataField = null;
//   			switch (typeName.toLowerCase()) {
//   			    case "casetype":
//   			        metadataField = "dc.casetype";
//   			        break;
//   			    case "judgename":
//   			        metadataField = "dc.contributor.author";
//   			        break;
//   			    case "caseyear":
//   			        metadataField = "dc.caseyear";
//   			        break;
//   			    default:
//   			        log.error("Invalid typeName: " + typeName);
//   			        return new String[0][0];
//   			}
//   			
//   	        if (metadataField == null) return new String[0][0];
//   	        
//   	        MetadataField mf = metadataFieldService.findByString(context, metadataField, '.');
//    		if(isAdmin) {
//                itr = itemService.findByEachTypeCount(context, null, null, mf);
//    		} else {
//                	Set<Collection> collectionIds = new HashSet<Collection>();
//	    			for( Group g:groups) {
//    		        	String uploadGroupName = g.getName();
//    		        	String uploadGroupNameLower = uploadGroupName.toLowerCase();
//    		        	if(uploadGroupName.contains("_")) {
//    		        		if(uploadGroupNameLower.contains("community")) {
//    		        			String communityID = uploadGroupNameLower.split("_")[1];
//    		        			UUID communityUUID = UUID.fromString(communityID);
//    		        			community = communityService.find(context, communityUUID);
//    		   					List<Collection> collections = community.getCollections();
//    		   					for(Collection coll : collections) {
//    	                            collectionIds.add(coll);
//    		   					}
//    		        		} 
//    		        	}	
//    		        }
//	    			
//	                if (!collectionIds.isEmpty()) {
//	                    itr = itemService.findByEachTypeCount(context, null, new ArrayList<>(collectionIds), mf);
//	                } else {
//	                    log.warn("No valid collection IDs found for user groups.");
//	                }
//    		}   			
//   			
//   			for(Object[] row : itr) {
//			    String[] metadataArray = new String[row.length];
//			    if(row[0] != null) {
//			    	metadataArray[0] = row[0].toString();
//			    } else {
//			    	continue;
//			    }
//	            metadataArray[1] = row[1] != null ? row[1].toString() : "0";
//	            info.add(metadataArray);			    
//   			}
//   		} catch (Exception e) {
//   			log.error("Error in fetching data-trend result", e);
//   		}
//   		return info.toArray(new String[0][0]);
//   	}
}
