/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.File;

//import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.DiscoveryXMLGenerator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.service.UserMetadataFieldsService;

/**
 * This RestController will take care of all the calls for a specific
 * collection's special group This is handled by calling
 * "/api/core/collections/{uuid}/{group}" with the correct RequestMethod This
 * works for specific WorkflowGroups as well given that their role is supplied
 * by calling "/api/core/collections/{uuid}/workflowGroups/{workflowRole}"
 */
@RestController
@RequestMapping("/api/section/management")
public class DynamicCollectionCreator {

	@Autowired
	private CollectionService collectionService;
	
	@Autowired
	private UserMetadataFieldsService userMetadataFieldsService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private MetadataSchemaService schemaService;

	@Autowired
	private MetadataFieldService fieldService;

	@Autowired
	private AuthorizeService authorizeService;

	private static final String clazz = "org.dspace.app.webui.servlet.CreateSubDepartmentServlet";

	private static final Logger log = LogManager.getLogger();

	/**
	 * This method creates and returns an AdminGroup object for the given collection
	 * This is called by using RequestMethod.POST on the /adminGroup value
	 * 
	 * @param uuid     The UUID of the collection for which we'll create an
	 *                 adminGroup
	 * @param response The current response
	 * @param request  The current request
	 * @return The created AdminGroup
	 * @throws SQLException       If something goes wrong
	 * @throws AuthorizeException If something goes wrong
	 * @throws IOException
	 */

	@GetMapping(value = "/demo")
	public String checkStatus() {
		return "Working";
	}
	
	@GetMapping(value="/getusermetadata")
	public List<UserMetadataFields> listOfUserMetadata(@RequestParam(name="collectionHandle", required=true) String collectionHandle, HttpServletRequest request) throws SQLException{
		List<UserMetadataFields> userMetadataList = new ArrayList<UserMetadataFields>();
		Context context = ContextUtil.obtainContext(request);
		try {
			userMetadataList = userMetadataFieldsService.getMetadataFieldBySubDeptHandle(context, collectionHandle);
			
		} catch (Exception e) {
			log.error("Error while fetching user metadata list ", e);
			return null;
		} finally {
			if (context != null) {
				context.complete();
			}
		}
		
		return userMetadataList;
	}

//	@PreAuthorize("hasPermission(#uuid, 'COLLECTION', 'WRITE')")
	@PostMapping(value = "/demo/{uuid}")
	public void dynamicCollectionCreation(@PathVariable(name = "uuid") String uuid, @RequestBody Map<String, Object> requestBody, HttpServletResponse response,
			HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
		log.info("Dynamic Collection started");
		Context context = ContextUtil.obtainContext(request);
		UUID uuid1 = UUID.fromString(uuid);
		Community community = communityService.find(context, uuid1);
		log.info("context is here:",context); 
		log.info("uuid is here:",uuid); 
		log.info("Requestbody is here:",requestBody); 
		if (community != null) {

			Collection newSectionCollection = null;
			String collectionName = null;
			for(String keyName : requestBody.keySet()) {
				if (keyName.equalsIgnoreCase("collectionName")) {
					String value = (String) requestBody.get(keyName);
					collectionName = value;
				}
			}
			log.info("Collection created: ", newSectionCollection);
			try {
				context.turnOffAuthorisationSystem();

				if (community.getHandle() != null) {

					List<Collection> subCollectionsList = community
							.getCollections();
					// Check if collection already exits
					if (subCollectionsList != null) {
						for (Collection childColl : subCollectionsList) {
							if (childColl.getName().equalsIgnoreCase(collectionName)) {
								context.abort();
								log.error("Collection name already exitst");
								return;
							}
						}
					}
					newSectionCollection = collectionService.create(context, community);
					collectionService.setMetadataSingleValue(context, newSectionCollection, "dc", "title", "", null, collectionName);
					collectionService.update(context, newSectionCollection);
					createGroups(context, community, newSectionCollection);
					ArrayList<UserMetadataFields> userMetadataList = userMetadataCreate(context, community, newSectionCollection, requestBody, request);
					log.info("UserMetadata is getting created:",userMetadataList);
					if(userMetadataList.size() > 0) {
						createDiscoveryXML(context, newSectionCollection, userMetadataList);
					}
				}

			} finally {
				log.info("context is getting completed here");
				context.complete();
			}
			return;
		}

	}
	
	private Map<String, String[]> convertMap(@RequestBody Map<String, Object> requestBody) {
        Map<String, String[]> paramMap = new HashMap<>();

        // Iterate through the original map
        for (String key : requestBody.keySet()) {
            Object value = requestBody.get(key);

            // Check if the value is an array or a single string
            if (value instanceof String) {
                // If it's a single string, convert it to a string array with one element
                paramMap.put(key, new String[]{(String) value});
            } else if (value instanceof String[]) {
                // If it's already a string array, leave it as is
                paramMap.put(key, (String[]) value);
            } else {
                // Handle other data types as needed; here, we're converting them to string arrays
                paramMap.put(key, new String[]{String.valueOf(value)});
            }
        }

        return paramMap;
    }
	
	/* This method creates user metadata */
	private ArrayList<UserMetadataFields> userMetadataCreate(Context context, Community community, Collection collection, Map<String, Object> requestBody, HttpServletRequest request) {

		String sectionNamePrefix = null;
		String sectionName = community.getName();
		String collectionName = collection.getName();
		ArrayList<UserMetadataFields> userMetadataFieldsList = new ArrayList<UserMetadataFields>();

		if (sectionName.contains(" ")) {
			sectionNamePrefix = sectionName.replaceAll("\\s", "-");
		} else {
			sectionNamePrefix = sectionName;
		}
		sectionNamePrefix = sectionNamePrefix + "_" + collectionName;

		String message = "";
		try {
			MetadataSchema schema = schemaService.find(context, 1);
			// Get access to the localized resource bundle
			Locale locale = context.getCurrentLocale();
			ResourceBundle labels = ResourceBundle.getBundle("Messages", locale);

			Map<String, String[]> paramMap = convertMap(requestBody);
			Set<Entry<String, String[]>> entrySet = paramMap.entrySet();
			
			Iterator<?> it = entrySet.iterator();
			while (it.hasNext()) {
				String element = null;
				String qual = null;
				String scope = null;
				String countString = null;
				String metadataType = null;
				String metadataSystemElement = null;
				String metadataTypeKey = null;

				@SuppressWarnings("unchecked")
				Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>) it.next();

				String key = entry.getKey();

				if (key.contains("section-metadataname")) {
					
					countString = key.substring("section-metadataname".length());
					metadataTypeKey = "section-metadata-type" + countString;
					metadataType = (String) requestBody.get(metadataTypeKey);
					
					
					String metadataName = (String) requestBody.get(key);

					if (metadataName == null || (metadataName != null && metadataName.trim().isEmpty())) {
						context.abort();
						return null;
					}

					if (metadataType != null && metadataType.equals("2")) {
						element = "date";
						qual = (String) requestBody.get("section-metadataname" + countString);
						if(qual == null) {
							request.setAttribute("message", "qualifier is Null");
							context.abort();
							return null;
						}
						metadataSystemElement = schema.getName() + '.' + element + "." + sectionNamePrefix + "_"
								+ qual;
					} else {
						element = (String) requestBody.get(key);
						metadataSystemElement = schema.getName() + "." + sectionNamePrefix + "_" + element;
					}

					message = sanityCheck(labels, element, qual);

					if (!message.equals("OK")) {
						request.setAttribute("message", message);
						context.abort();
						return null;
					}

					try {
						// Creating new Metadata in metadata registry;
						if(element.equalsIgnoreCase("date")) {
							MetadataField dc = fieldService.create(context, schema, element,sectionNamePrefix + "_" + qual, scope);
		                    fieldService.update(context, dc);
						} else {
							MetadataField dc = fieldService.create(context, schema, sectionNamePrefix + "_" + element, qual, scope);
		                    fieldService.update(context, dc);
						}
						

					} catch (NonUniqueMetadataException e) {
						// Record the exception as a warning
						log.error(e);

						// Show the page again but with an error message to inform the
						// user that the metadata field was not created and why
						message = "Metadata with given input already exists!";
						request.setAttribute("message", message);
						context.abort();
						return null;
					}

					// Store metadata field and value in DB
					try {
						int countInt = Integer.parseInt(countString);
	                	int fieldTypeInt = Integer.parseInt(metadataType);

	                    UserMetadataFields userMetadataFields = new UserMetadataFields();
	            		
	                    if(element.equalsIgnoreCase("date")) {
	                    	userMetadataFields.setUserFieldName(qual);
	                    } else {
	                    	userMetadataFields.setUserFieldName(element);
	                    }
	            		userMetadataFields.setSystemFieldName(metadataSystemElement);
	            		userMetadataFields.setFieldType(fieldTypeInt);
	            		userMetadataFields.setFieldPosition(countInt);
	                    userMetadataFieldsList.add(userMetadataFields);
	                    if(element.equalsIgnoreCase("date")) {
	                    	userMetadataFieldsService.savePeMetadataFields(context, qual, metadataSystemElement, 
		                    		fieldTypeInt, countInt, collection.getHandle());
	                    } else {
	                    	userMetadataFieldsService.savePeMetadataFields(context, element, metadataSystemElement, 
		                    		fieldTypeInt, countInt, collection.getHandle());
	                    }

					} catch (NumberFormatException e) {

						log.error(e);
						message = "Internal Error. Please contact system administrator!";
						request.setAttribute("message", message);
						context.abort();
						return null;
					}
				}
			}
		} catch (Exception e) {
			log.error("Error while adding metadatas", e);
		}
		return userMetadataFieldsList;
		
	}
	
	/* This method creates an unique XML for each of the collections */
	private void createDiscoveryXML(Context context, Collection collection, ArrayList<UserMetadataFields> userMetadataFieldsList) {
		if(context != null && context.isValid()) {

			File newDiscoveryFile = null;
			String message = "";
			try {
				
    			String collectionHandle = collection.getHandle();
    			String[] handleVals = collectionHandle.split("/");
    			String uniqueHandlePart = handleVals[1];
        	
    			String newDiscoveryFilePath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
                    + "/config/spring/api/discovery-" + uniqueHandlePart + ".xml";

        	
    			DiscoveryXMLGenerator discoveryXMLGenerator = new DiscoveryXMLGenerator();
    			log.info("creating new discovery file");
    			newDiscoveryFile = new File(newDiscoveryFilePath);
    			discoveryXMLGenerator.generateXMLFile(collectionHandle, userMetadataFieldsList, newDiscoveryFile);

    			log.info("Before loading new discovery confuration");
    		
    			if(SystemUtils.IS_OS_WINDOWS) {
    				log.info("File path on Windows: " + "file:/" + newDiscoveryFile);
    				DiscoveryConfigurationService.loadDiscoveryConfiguration("file:/" + newDiscoveryFile);
    			} else {
    				log.info("File path on Linux: " + newDiscoveryFilePath);
    				DiscoveryConfigurationService.loadDiscoveryConfiguration("file://" + newDiscoveryFilePath);
    			}

			} catch (Exception e) {
				
				log.error(e);
				//delete the discovery xml created
				if(newDiscoveryFile.exists()) {
					newDiscoveryFile.delete();
				}
            	message = "Error generating configuration file!";
            	context.abort();
    			return;
			} finally {
				context.restoreAuthSystemState();
//				context.complete();
			}
        	

        	if(message.isEmpty() || (!message.isEmpty() && message.equalsIgnoreCase("OK"))) {
    			message = "Sub department created successfully.";
        		log.info(message);
    		}
        	
    	}
	}
	
	private void createGroups(Context context, Community community, Collection newSectionCollection) {
		try {
			String collectionName = newSectionCollection.getName();
			String groupNamePrefix = collectionName + "_" + newSectionCollection.getID();
			List<Group> collectionOfGroups = new ArrayList<>();

			Group viewGroup = groupService.create(context);
			groupService.setName(viewGroup, groupNamePrefix + "_VIEW");
			groupService.update(context, viewGroup);

			Group downloadGroup = groupService.create(context);
			groupService.setName(downloadGroup, groupNamePrefix + "_DOWNLOAD");
			groupService.update(context, downloadGroup);

			Group uploadGroup = groupService.create(context);
			groupService.setName(uploadGroup, groupNamePrefix + "_UPLOAD");
			groupService.update(context, uploadGroup);
			
			Group reviewerGroup = groupService.create(context);
			groupService.setName(reviewerGroup, groupNamePrefix + "_WORKFLOW_REVIEWER");
			groupService.update(context, reviewerGroup);

			Group deleteGroup = groupService.create(context);
			groupService.setName(deleteGroup, groupNamePrefix + "_DELETE");
			groupService.update(context, deleteGroup);

			Group adminGroup = groupService.create(context);
			groupService.setName(adminGroup, groupNamePrefix + "_ADMIN");
			groupService.update(context, adminGroup);
			
			for (Group group : collectionOfGroups) {
				setBasicPermissions(context, newSectionCollection, group);
				
			} 
		} catch(Exception e) {
			log.error("Error while creating basic groups for collection: ", e);
		}
		
	}

	private void setBasicPermissions(Context context, DSpaceObject dso, Group group) throws SQLException, AuthorizeException {
		authorizeService.addPolicy(context, dso, Constants.READ, group);
		authorizeService.addPolicy(context, dso, Constants.DEFAULT_ITEM_READ, group);
		authorizeService.addPolicy(context, dso, Constants.DEFAULT_BITSTREAM_READ, group);
		authorizeService.addPolicy(context, dso, Constants.ADD, group);
	}
	
	private String sanityCheck(ResourceBundle labels, String element, String qualifier) {

		for (int ii = 0; ii < element.length(); ii++) {
			if (element.charAt(ii) == '.' || element.charAt(ii) == '_' || element.charAt(ii) == ' ') {
				return labels.getString(clazz + ".badelemchar");
			}
		}
		if (element.length() > 64) {
			return labels.getString(clazz + ".elemtoolong");
		}

		if (qualifier != null) {
			if (qualifier.length() > 64) {
				return labels.getString(clazz + ".qualtoolong");
			}
			for (int ii = 0; ii < qualifier.length(); ii++) {
				if (qualifier.charAt(ii) == '.' || qualifier.charAt(ii) == '_' || qualifier.charAt(ii) == ' ') {

					return labels.getString(clazz + ".badqualchar");
				}
			}
		}

		return "OK";
	}

}
