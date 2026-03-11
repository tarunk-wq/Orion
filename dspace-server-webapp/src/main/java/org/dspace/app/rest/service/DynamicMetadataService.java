package org.dspace.app.rest.service;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tools.ant.util.FileUtils;
import org.dspace.app.rest.converter.UserMetadataConverter;
import org.dspace.app.rest.exception.DuplicateEntityException;
import org.dspace.app.rest.model.UserMetadataDto;
import org.dspace.app.rest.utils.DiscoveryXMLGenerator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DynamicMetadataService {
   private static final Logger log = LoggerFactory.getLogger(DynamicMetadataService.class);
   @Autowired
   private CommunityService communityService;
   @Autowired
   private MetadataSchemaService schemaService;
   @Autowired
   private MetadataFieldService metadataFieldService;
   @Autowired
   private UserMetadataFieldsService userMetadataFieldService;
   @Autowired
   private CollectionService collectionService;
   @Autowired
   private UserMetadataConverter userMetadataConverter;
   @Autowired
   private ItemService itemService;
   
   @Transactional
   public String addMetadata(Context context, UUID uuid, UserMetadataDto[] userMetadataList) throws SQLException {
      Community subDepartment = null;

      try {
         subDepartment = (Community) communityService.find(context, uuid);
         if (subDepartment == null) {
            return "SubCommunity Not Found!";
         }
      } catch (Exception var9) {
         log.error("Unable to find Community " + String.valueOf(var9));
      }

      Community department = subDepartment != null && subDepartment.getParentCommunities() != null ? (Community)subDepartment.getParentCommunities().get(0) : null;
      if (department == null) {
         return "Parent Community Not Found!";
      } else {
         List<UserMetadataFields> exisitingMetadataFieldsList = userMetadataFieldService.getMetadataFieldsBySubDept(context, subDepartment.getHandle());
         if (exisitingMetadataFieldsList == null) {
            exisitingMetadataFieldsList = new ArrayList<UserMetadataFields>();
         }

         ArrayList<UserMetadataFields> userMetadataFieldsList = (ArrayList<UserMetadataFields>) updateDynamicUserMetadataInSubDepartment(context, department, subDepartment, userMetadataList, exisitingMetadataFieldsList);
         String msg = generateDiscoveryXMLFile(context, userMetadataFieldsList, subDepartment);
         return !"success".equalsIgnoreCase(msg) ? msg : "Successfully Added";
      }
   }
   
   @Transactional
   public String deleteMetadata(Context context, UUID subDepartmentId, UserMetadataFields userMetadata) throws SQLException, AuthorizeException {
      if (userMetadata == null) {
         return "Invalid Operation: Metadata record is null";
      }
      
      try {
    	  deleteAssociatedMetadataFields(context, userMetadata);
      } catch (Exception e) {
    	  throw new DuplicateEntityException(String.format("Cannot delete '%s': This field is still assigned to existing items."
    	  		+ "Please remove these references before deleting.", userMetadata.getUserFieldName()));
      }

      try {
    	 Community subDepartment = (Community) communityService.find(context, subDepartmentId);
         
         if (subDepartment == null) {
             log.warn("Section not found for ID: ", subDepartmentId);
             return "Section not found";
         }
         
         ArrayList<UserMetadataFields> userMetadataList = (ArrayList<UserMetadataFields>) userMetadataFieldService.getMetadataFieldsBySubDept(context, subDepartment.getHandle());
         String msg = generateDiscoveryXMLFile(context, userMetadataList, subDepartment);
         if (!"success".equalsIgnoreCase(msg)) {
            return msg;
         }

         return "Successfully Deleted";
      } catch (SQLException e) {
          log.error("Database error during metadata deletion: ", e);
          return "Database error: " + e.getMessage();
      } catch (Exception e) {
          log.error("Unexpected error during metadata deletion and discovery generation: ", e);
          return "Unexpected error during metadata deletion and discovery generation";
      }
   
   }
   
   private void deleteAssociatedMetadataFields(Context context, UserMetadataFields userMetadata) throws SQLException, AuthorizeException {
	    String systemFieldName = userMetadata.getSystemFieldName();
	    MetadataField metadataField = metadataFieldService.findByString(context, systemFieldName, '.');
	    
	    if (metadataField != null) {
	        metadataFieldService.delete(context, metadataField);
	    }
	    
	    userMetadataFieldService.delete(context, userMetadata);
   }

public static String toCamelCase(String input) {
	    if (input == null || input.trim().isEmpty()) {
	        return "";
	    }

	    input = input.trim().replaceAll("\\s+", "-");

	    String[] parts = input.split("-");
	    StringBuilder camelCase = new StringBuilder();

	    camelCase.append(parts[0].toLowerCase());

	    for (int i = 1; i < parts.length; i++) {
	        String word = parts[i].toLowerCase();
	        camelCase.append(Character.toUpperCase(word.charAt(0)))
	                 .append(word.substring(1));
	    }

	    return camelCase.toString();
	}

   public List<UserMetadataFields> updateDynamicUserMetadataInSubDepartment(Context context, Community department, Community subDepartment, UserMetadataDto[] userMetadataList, List<UserMetadataFields> userMetadataFieldsList) throws SQLException {
      String departmentName = (department != null && department.getName() != null) ? department.getName() : "";
	  String metadataNamePrefix = departmentName.replaceAll("\\s", "-") + "_" + subDepartment.getName().replaceAll("\\s", "-");
      MetadataSchema schema = schemaService.find(context, 1);
      Locale locale = context.getCurrentLocale();
      ResourceBundle labels = ResourceBundle.getBundle("Messages", locale);
      Map<String, String> duplicateMetadataList = new HashMap<String, String>();
      int i = 0;
      int fieldPosition = 1;

      while(true) {
         while(i < userMetadataList.length) {
        	
            String element = null;
            String qual = null;
            String scope = null;
            int fieldType = 0;
            String metadataSystemElement = null;
            UserMetadataDto entry = userMetadataList[i];
            i++;
            if (entry != null) {
               int metadataType = entry.getType();
               String metadataName = toCamelCase(entry.getName());
               String metadataValue = entry.getValue();
               if (metadataName != null && metadataType != -1 && !metadataName.trim().isEmpty()) {
                  if (metadataType == 2) {
                     element = "date";
                     qual = metadataName;
                     fieldType = metadataType;
                     metadataSystemElement = schema.getName() + "." + element + "." + metadataNamePrefix + "_" + metadataName;
                  } else {
                     element = metadataName;
                     fieldType = metadataType;
                     metadataSystemElement = schema.getName() + "." + metadataNamePrefix + "_" + metadataName;
                  }
               }

               String sanityCheckMessage = sanityCheck(labels, element, qual);
               if (!sanityCheckMessage.equals("OK")) {
                  log.error(sanityCheckMessage);
                  continue;
               }

               try {
            	   MetadataField dc = null;
            	   if(metadataType == 2) {
            		   dc = metadataFieldService.create(context, schema, element , metadataNamePrefix + "_" + qual, (String)scope);
            	   }else {
            		   dc = metadataFieldService.create(context, schema, metadataNamePrefix + "_" + element, qual, (String)scope);
            	   }
                  metadataFieldService.update(context, dc);
               } catch (Exception e) {
                  duplicateMetadataList.put(metadataNamePrefix + "_" + element, e.getMessage());
                  log.error("Exception Occurs to add metadata in metadataFieledRedistry " + e);
                  continue;
               }
               
               Boolean uniqueMetadataValue = entry.getIsUniqueMetadata();
               if(uniqueMetadataValue == null) {
            	   uniqueMetadataValue = false;
               }

               UserMetadataFields userMetadataFields = new UserMetadataFields();
               userMetadataFields.setUserFieldName(metadataName);
               userMetadataFields.setSystemFieldName(metadataSystemElement);
               userMetadataFields.setFieldType(fieldType);
               userMetadataFields.setFieldPosition(fieldPosition);
               userMetadataFields.setUserFieldValue(metadataValue);
               userMetadataFields.setIsUniqueMetadata(uniqueMetadataValue);

               try {
                  userMetadataFieldService.savePeMetadataFields(context, userMetadataFields, subDepartment.getHandle());
                  userMetadataFieldsList.add(userMetadataFields);
                  ++fieldPosition;
               } catch (SQLException e) {
                  log.error("Exception Occurs while add metadata fields in DB " + e);
               }
            }
         }
         context.commit();
         return userMetadataFieldsList;
      }
   }

   private String sanityCheck(ResourceBundle labels, String element, String qualifier) {
      Pattern special = Pattern.compile("[._\\s]");
      Matcher elementMatcher = special.matcher(element);
      Matcher qualifierMatcher = qualifier != null ? special.matcher(qualifier) : special.matcher("") ;
      if (elementMatcher.find()) {
         return labels.getString("Element Special Character Occurs " + element);
      } else if (element.length() > 64) {
         return labels.getString("Element too long " + element);
      } else if (qualifier != null && qualifier.length() > 64) {
         return labels.getString("Qualifier too long " + qualifier);
      } else {
         return qualifierMatcher.find() ? labels.getString("Qualifier Special Character Occurs " + qualifier) : "OK";
      }
   }

   private String generateDiscoveryXMLFile(Context context, ArrayList<UserMetadataFields> userMetadataFieldsList, Community subDepartment) {
      if (userMetadataFieldsList.size() > 0 && context != null && context.isValid()) {
         context.turnOffAuthorisationSystem();
         File newDiscoveryFile = null;

         try {
            String subCommunityHandle = subDepartment.getHandle();
            String[] handleVals = subCommunityHandle.split("/");
            String uniqueHandlePart = handleVals[1];
            String newDiscoveryFilePath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir") + "/config/spring/api/discovery-" + uniqueHandlePart + ".xml";
            DiscoveryXMLGenerator discoveryXMLGenerator = new DiscoveryXMLGenerator();
            newDiscoveryFile = new File(newDiscoveryFilePath);
            if (newDiscoveryFile.exists()) {
               FileUtils.delete(newDiscoveryFile);
               newDiscoveryFile = new File(newDiscoveryFilePath);
            }

            discoveryXMLGenerator.generateXMLFile(subCommunityHandle, userMetadataFieldsList, newDiscoveryFile);
            log.info("Load Discovery XML File");
            DiscoveryConfigurationService.loadDiscoveryConfiguration(newDiscoveryFile.toURI().toString());
         } catch (Exception e) {
            log.error("Error generating configuration file! " + e);
            if (newDiscoveryFile.exists()) {
               newDiscoveryFile.delete();
            }

            return "Error generating configuration file! " + e.toString();
         }

         context.restoreAuthSystemState();
      }

      return "success";
   }

   public String addMetadata(Context context, Community department, Community subDepartment, UserMetadataDto[] userMetadataList) throws SQLException {
	   department = subDepartment.getParentCommunities().get(0);
      ArrayList<UserMetadataFields> userMetadataFieldsList = (ArrayList<UserMetadataFields>) updateDynamicUserMetadataInSubDepartment(context, department, subDepartment, userMetadataList, new ArrayList<UserMetadataFields>());
      if (!userMetadataFieldsList.isEmpty()) {
         String msg = generateDiscoveryXMLFile(context, userMetadataFieldsList, subDepartment);
         if (!"success".equalsIgnoreCase(msg)) {
            return msg;
         }
      }

      return "Succesfully Added";
   }
   
   public List<UserMetadataDto> getFiltersForDspaceObject(Context context, UUID dspaceObjectId) throws SQLException {
	   DSpaceObject dso = collectionService.find(context, dspaceObjectId);
       if (dso == null) dso = communityService.find(context, dspaceObjectId);
       if(dso == null) dso = itemService.find(context, dspaceObjectId);
       
       if (dso == null) {
           throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found: " + dspaceObjectId);
       }
       
       Community section = (Community) getSecondTopLevelCommunity(context, dso);
       if (section == null) {
           throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Section not found for DSpace object: " + dspaceObjectId);
       }
       
       List<UserMetadataFields> fields = userMetadataFieldService.getMetadataFieldsBySubDept(context, section.getHandle());
       return userMetadataConverter.convert(fields, null);
       
   }
   
   public DSpaceObject getSecondTopLevelCommunity(Context context, DSpaceObject dso) throws SQLException {
	    if (dso == null) return null;
	    if (dso instanceof Item) {
	        return getSecondTopLevelCommunity(context, ((Item) dso).getOwningParent());
	    }else if (dso instanceof Collection) {
	        return getSecondTopLevelCommunity(context, collectionService.getParentObject(context, (Collection) dso));
	    } else if (dso instanceof Community) {
	        DSpaceObject parent = communityService.getParentObject(context, (Community) dso);
	        if (parent == null) return null;
	        DSpaceObject grandParent = communityService.getParentObject(context, (Community) parent);
	        return (grandParent == null) ? dso : getSecondTopLevelCommunity(context, parent);
	    }
	    return null;
	}
}
