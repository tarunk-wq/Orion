package org.dspace.app.rest;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.auditactionconfig.AuditActionConfig;
import org.dspace.auditactionconfig.service.AuditActionConfigService;
import org.dspace.audittrail.AuditTrail;
import org.dspace.audittrail.service.AuditTrailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audittrail/audittrails")
public class AudittrailRestController {

	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AudittrailRestController.class);
	
	@Autowired
	private AuditTrailService audittrailService;
	
	@Autowired
	private AuditActionConfigService auditActionConfigService;
	
	@GetMapping("/generatepdf")
	public void generateAudittrailPDFReport(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(name = "username") String userName, @RequestParam(name = "startdate") String startDate,
			@RequestParam(name = "enddate") String endDate) throws SQLException {
		Context context = null;
		try {
			context = ContextUtil.obtainContext(request);
			
	        List<String> enabledActions = auditActionConfigService.findEnabledActionCodes(context);  // fetch allowed actions
			List<AuditTrail> auditTrails;
			String reportFileName = "Report-AuditTrail %s";
			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date startDateObj = formatter.parse(startDate + " 00:00:00");
				Date endDateObj = formatter.parse(endDate+ " 23:59:59");
				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
				if (userName != null && !userName.isEmpty()) {
					auditTrails = audittrailService.getEnabledAuditTrailByUserName(context, userName, startDateObj, endDateObj,enabledActions);
				} else {
					auditTrails = audittrailService.getEnabledAuditTrailByDateRange(context, startDateObj, endDateObj,enabledActions);
				}
			} else {
				reportFileName = String.format(reportFileName,  "").trim();
				auditTrails = audittrailService.getAuditTrailUserName(context, userName);
			}
			// creating Rows entry for Table record
			List<String[]> metadata = fetchMetadataResponseFormat(auditTrails);
			// generating and flushing PDF file, context completion handled inside
			Utils.handlePdf(context, request, response, metadata, reportFileName);
		} catch (Exception e) {
			log.error("Error in generating pdf for audittrail-reports", e);
		} finally {
			context.complete();  // Ensure commit
		}
	}
	
	@GetMapping("/generatecsv")
	public void generateAudittrailCSVReport(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(name = "username") String userName, @RequestParam(name = "startdate") String startDate,
			@RequestParam(name = "enddate") String endDate) throws SQLException {
		Context context = null;
		try {
			context = ContextUtil.obtainContext(request);
	        List<String> enabledActions = auditActionConfigService.findEnabledActionCodes(context);  // fetch allowed actions
			List<AuditTrail> auditTrails;
			String reportFileName = "Report-AuditTrail %s";
			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date startDateObj = formatter.parse(startDate + " 00:00:00");
				Date endDateObj = formatter.parse(endDate+ " 23:59:59");
				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
				if (userName != null && !userName.isEmpty()) {
					auditTrails = audittrailService.getEnabledAuditTrailByUserName(context, userName, startDateObj, endDateObj,enabledActions);
				} else {
					auditTrails = audittrailService.getEnabledAuditTrailByDateRange(context, startDateObj, endDateObj,enabledActions);
				}
			} else {
				reportFileName = String.format(reportFileName,  "").trim();
				auditTrails = audittrailService.getAuditTrailUserName(context, userName);
			}
			// creating Rows entry for Table record
			List<String[]> metadata = fetchMetadataResponseFormat(auditTrails);
			// generating and flushing PDF file, context completion handled inside
			Utils.handleCSV(context, request, response, metadata, reportFileName);
		} catch (Exception e) {
			log.error("Error in generating pdf for audittrail-reports", e);
		} finally {
			context.complete();  // Ensure commit
		}
	}
	
	@GetMapping("/doc-access-report")
	public void docAccessReport(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(required = false) String username, @RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate, @RequestParam(defaultValue = "pdf") String format) throws SQLException {

		try {
			Context context = ContextUtil.obtainContext(request);
			
	        List<String> enabledActions = auditActionConfigService.findEnabledActionCodes(context);  // fetch allowed actions
			List<AuditTrail> auditTrails;
			String reportFileName = "Report-AuditTrail %s";
			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date startDateObj = formatter.parse(startDate + " 00:00:00");
				Date endDateObj = formatter.parse(endDate+ " 23:59:59");
				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
				if (username != null && !username.isEmpty()) {
					auditTrails = audittrailService.getEnabledAuditTrailByUserName(context, username, startDateObj, endDateObj,enabledActions);
				} else {
					auditTrails = audittrailService.getEnabledAuditTrailByDateRange(context, startDateObj, endDateObj,enabledActions);
				}
			} else {
				reportFileName = String.format(reportFileName,  "").trim();
				auditTrails = audittrailService.getAuditTrailUserName(context, username);
			}
	        // adding entry in audit trail
			// creating Rows entry for Table record
			List<String[]> metadata = fetchMetadataForDocAccessReport(auditTrails);
			
			// generating and flushing file, context completion handled inside
			if (format.equalsIgnoreCase("pdf")) {				
				Utils.handlePdf(context, request, response, metadata, reportFileName);
			
			} else if (format.equalsIgnoreCase("csv")) {				
				Utils.handleCSV(context, request, response, metadata, reportFileName);
			
			} else {
				throw new Exception("Invalid format: " + format);
			}
			context.complete();  // Ensure commit
		
		} catch (Exception e) {
			log.error("Error in generating pdf for audittrail-reports", e);
		}
	}
	
	
	private List<String[]> fetchMetadataResponseFormat(List<AuditTrail> auditTrails) {
		ArrayList<String[]> metadataList = new ArrayList<String[]>();		
		String[] header = {"DATE","IP ADDRESS","USERNAME","ACTION", "HANDLE", "DETAILS"};
		//Adding header to pdf
		metadataList.add(header);
		//inserting data into metadatlist for pdf generation
		try {
			for(int rowNum = 0; rowNum < auditTrails.size(); rowNum++){
				AuditTrail auditTrail = auditTrails.get(rowNum);
	            if(!auditTrail.getAction().toLowerCase().contains("configuration") && !auditTrail.getAction().toLowerCase().contains("facet") && !auditTrail.getAction().toLowerCase().contains("resourceid")) {
					SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
					Date date = auditTrail.getTime();
					String entryDate = dateFormatter.format(date);
	
					ArrayList<String> metadataArray = new ArrayList<String>();
					metadataArray.add(entryDate);
					metadataArray.add(auditTrail.getIpAddress());
					metadataArray.add(auditTrail.getUserName());
					metadataArray.add(auditTrail.getAction());
					metadataArray.add(auditTrail.getHandle());
		            if(auditTrail.getDescription().contains("server/api")) {
		            	metadataArray.add("");
		            } else {
		            	metadataArray.add(auditTrail.getDescription());
		            }
					metadataList.add(metadataArray.toArray(new String[0]));
				}
			}
		} catch (Exception e) {
			log.error("Error in formatting audittrail data", e);
		}
		return metadataList;
	}
	
	private List<String[]> fetchMetadataForDocAccessReport(List<AuditTrail> auditTrails) {
		ArrayList<String[]> metadataList = new ArrayList<String[]>();		
		String[] header = {"DATE", "TIME", "IP ADDRESS","USERNAME","ACTION", "SECTION", "DOCUMENT PATH"};
		//Adding header to pdf
		metadataList.add(header);
		//inserting data into metadatlist for pdf generation
		try {
			for(int rowNum = 0; rowNum < auditTrails.size(); rowNum++){
				AuditTrail auditTrail = auditTrails.get(rowNum);
	            if(auditTrail.getAction().toLowerCase().contains("BITSTREAM_DOWNLOAD_VIEWED")) {
					SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
					Date date = auditTrail.getTime();
					String entryDate = dateFormatter.format(date);
					
					SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
					String entryTime = timeFormatter.format(date);
					
					ArrayList<String> metadataArray = new ArrayList<String>();
					metadataArray.add(entryDate);
					metadataArray.add(entryTime);
					metadataArray.add(auditTrail.getIpAddress());
					metadataArray.add(auditTrail.getUserName());
					metadataArray.add(auditTrail.getAction());
					metadataArray.add("");
					metadataArray.add("");
					
					metadataList.add(metadataArray.toArray(new String[0]));
				}
			}
		} catch (Exception e) {
			log.error("Error in formatting audittrail data", e);
		}
		return metadataList;
		
	}
	
	@GetMapping("/generate")
	public List<String[]> generateAudittrailReport(HttpServletResponse response, HttpServletRequest request,
			@RequestParam(name = "username") String userName, @RequestParam(name = "startdate") String startDate,
			@RequestParam(name = "enddate") String endDate) throws SQLException {
		try {
			Context context = ContextUtil.obtainContext(request);
	        List<String> enabledActions = auditActionConfigService.findEnabledActionCodes(context);  // fetch allowed actions
			List<AuditTrail> auditTrails;
			String reportFileName = "Report-AuditTrail %s";
			if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date startDateObj = formatter.parse(startDate + " 00:00:00");
				Date endDateObj = formatter.parse(endDate+ " 23:59:59");
				reportFileName = String.format(reportFileName,  startDate + " to " + endDate);
				if (userName != null && !userName.isEmpty()) {
					auditTrails = audittrailService.getEnabledAuditTrailByUserName(context, userName, startDateObj, endDateObj,enabledActions);
				} else {
					auditTrails = audittrailService.getEnabledAuditTrailByDateRange(context, startDateObj, endDateObj,enabledActions);
				}
			} else {
				reportFileName = String.format(reportFileName,  "").trim();
				auditTrails = audittrailService.getAuditTrailUserName(context, userName);
			}
			
			// creating Rows entry for Table record
			List<String[]> metadata = fetchSort20MetadataResponseFormat(context, auditTrails);
			// generating and flushing PDF file, context completion handled inside
			return metadata;
		} catch (Exception e) {
			log.error("Error in generating audittrail-reports", e);
		}
		return null;
	}
	
	private List<String[]> fetchSort20MetadataResponseFormat(Context context, List<AuditTrail> auditTrails) {
	    ArrayList<String[]> metadataList = new ArrayList<String[]>();
	    String[] header = {"DATE", "IP ADDRESS", "USERNAME", "ACTION", "HANDLE", "DETAILS"};
	    // Adding header to PDF
	    metadataList.add(header);

	    try {
	        // Sort the auditTrails by date in descending order
	        Collections.sort(auditTrails, new Comparator<AuditTrail>() {
	            @Override
	            public int compare(AuditTrail a1, AuditTrail a2) {
	                return a2.getTime().compareTo(a1.getTime());
	            }
	        });

	        // Process only the top 20 entries
	        for (int rowNum = 0; rowNum < Math.min(auditTrails.size(), 20); rowNum++) {
	            AuditTrail auditTrail = auditTrails.get(rowNum);
	            if(!auditTrail.getAction().toLowerCase().contains("configuration") && !auditTrail.getAction().toLowerCase().contains("facet") && !auditTrail.getAction().toLowerCase().contains("resourceid")) {
	            	SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		            Date date = auditTrail.getTime();
		            String entryDate = dateFormatter.format(date);
	
		            ArrayList<String> metadataArray = new ArrayList<String>();
		            metadataArray.add(entryDate);
		            metadataArray.add(auditTrail.getIpAddress());
		            metadataArray.add(auditTrail.getUserName());
		            metadataArray.add(auditTrail.getAction());
		            metadataArray.add(auditTrail.getHandle());
		            if(auditTrail.getDescription().contains("server/api")) {
		            	metadataArray.add("");
		            } else {
		            	metadataArray.add(auditTrail.getDescription());
		            }
		            metadataList.add(metadataArray.toArray(new String[0]));
		        }
	        }
	    } catch (Exception e) {
	        log.error("Error in formatting audittrail data", e);
	    }
	    return metadataList;
	}
	
	@PostMapping("/sync-actions")
	public void syncAuditActions(HttpServletRequest request) throws SQLException {
	    Context context = null;
	    try {
	        context = ContextUtil.obtainContext(request);

	        auditActionConfigService.initializeAuditActionConfig(context);

	        context.complete();
	    } catch (Exception e) {
	        context.abort();
	        throw new RuntimeException("Failed to sync audit actions", e);
	    }
	}

	@PutMapping("/update-status")
	public void updateAuditActionStatuses(HttpServletRequest request, @RequestBody Map<String, Boolean> actionStatusMap) throws SQLException {
	    Context context = null;
	    try {
	        context = ContextUtil.obtainContext(request);

	        for (Map.Entry<String, Boolean> entry : actionStatusMap.entrySet()) {
	            String actionCode = entry.getKey();
	            boolean newStatus = entry.getValue();

	            AuditActionConfig config = auditActionConfigService.findByActionCode(context, actionCode);
	            if (config == null) {
	                throw new IllegalArgumentException("No AuditActionConfig found for actionCode: " + actionCode);
	            }

	            config.setActionStatus(newStatus);
	        }

	        context.complete();

	    } catch (Exception e) {
	        if (context != null && context.isValid()) {
	            context.abort();
	        }
	        throw new RuntimeException("Failed to update audit action statuses", e);
	    }
	}
	
	@GetMapping("/fetch-actions")
	public List<Map<String, Object>> getAllAuditActionConfigs(HttpServletRequest request) throws SQLException {
	    Context context = null;
	    try {
	        context = ContextUtil.obtainContext(request);

	        List<AuditActionConfig> configs = auditActionConfigService.findAll(context);

	        // Convert to List<Map<String, Object>> for a clean JSON response
	        List<Map<String, Object>> responseList = configs.stream().map(config -> {
	            Map<String, Object> map = new HashMap<>();
	            map.put("actionCode", config.getActionCode());
	            map.put("actionStatus", config.getActionStatus());
	            return map;
	        }).collect(Collectors.toList());

	        context.complete();
	        return responseList;

	    } catch (Exception e) {
	        if (context != null && context.isValid()) {
	            context.abort();
	        }
	        throw new RuntimeException("Failed to fetch audit action configs", e);
	    }
	}
	


	@DeleteMapping("/purge")
	public ResponseEntity<?> purgeAuditReport(HttpServletRequest request, @RequestBody Map<String, String> requestPayload) throws SQLException {
	    Context context = null;
	    try {
	        context = ContextUtil.obtainContext(request);

	        String action = requestPayload.getOrDefault("action", "all");
	        String startDate = requestPayload.getOrDefault("startDate", "");
	        String endDate = requestPayload.getOrDefault("endDate", "");
	        
	        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        Date startDateObj = null;
	        Date endDateObj = null;
	        try {
				if (!StringUtils.isBlank(startDate)) {
					startDateObj = dateFormatter.parse(startDate + " 00:00:00");
				}
				if (!StringUtils.isBlank(endDate)) {
					endDateObj = dateFormatter.parse(endDate + " 23:59:59");
				}
			} catch (Exception e) {
				// Error in date parsing
			}
	        
	        if (startDateObj == null && endDateObj == null) {
	        	return ResponseEntity.badRequest().body("Invalide Start Date & End Date");
	        }
	        
	        long record = audittrailService.purgeAuditTrail(context, action, startDateObj, endDateObj);

	        context.complete();
	        return ResponseEntity.ok().body(record + " Audit Records Purged Successfully.");

	    } catch (Exception e) {
	    	if (context != null && context.isValid()) {
	            context.abort();
	        }
	    	log.error("Failed to update audit action statuses",e);
	    	return ResponseEntity.internalServerError().body("Failed to update audit action statuses");
	    }
	}

}
