package org.dspace.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.usermetadata.UserMetadataFields;
import org.dspace.usermetadata.factory.UserMetadataFieldsServiceFactory;
import org.dspace.usermetadata.service.UserMetadataFieldsService;
import org.dspace.web.ContextUtil;

public class DynamicInputSetFactory {
	
    private UserMetadataFieldsService userMetadataFieldsService = UserMetadataFieldsServiceFactory.getInstance().getUserMetadataFieldsService();

    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    public DCInputSet buildForSection(InProgressSubmission source, SubmissionStepConfig stepConf) throws SQLException {
    	Context context = ContextUtil.obtainCurrentRequestContext();
    	DSpaceObject subDept = getSecondTopLevelCommunity(context, source.getParent());
        String subDeptHandle = subDept.getHandle();
        List<UserMetadataFields> defs = new ArrayList<>();
		try {
			defs = userMetadataFieldsService.getMetadataFieldsBySubDept(context, subDeptHandle);
		} catch (SQLException e) {
			e.printStackTrace();
		}

        // Convert to DCInputSet rows + listMap
        List<List<Map<String,String>>> rows = new ArrayList<>();
        Map<String, List<String>> listMap = new HashMap<>();

        // We’ll put everything in a single "row" for simplicity
        List<Map<String,String>> row = new ArrayList<>();
        for (UserMetadataFields def : defs) {
            Map<String,String> field = toDCInputFieldMap(context, def, listMap);
            if (field != null) {
                row.add(field);
            }
        }
        rows.add(row);

        return new DCInputSet(stepConf.getId(), rows, listMap);
    }
    
    
    public List<DCInputSet> buildForSection(Context context, String sectionHandle) throws SQLException {
        // Load your dynamic fields for this collection
    	List<UserMetadataFields> defs = new ArrayList<>();
		try {
			defs = userMetadataFieldsService.getMetadataFieldsBySubDept(context, sectionHandle);
		} catch (SQLException e) {
			e.printStackTrace();
		}

        Map<String, List<String>> listMap = new HashMap<>();
        List<Map<String, String>> row = new ArrayList<>();

        for (UserMetadataFields def : defs) {
            Map<String, String> m = toDCInputFieldMap(context, def, listMap);
            if (m != null) row.add(m);
        }

        List<List<Map<String, String>>> rows = new ArrayList<>();
        rows.add(row);

        String uniqueHandlePart = sectionHandle.substring(sectionHandle.lastIndexOf('/') + 1);
        String formName = "dynamic-" + uniqueHandlePart;

        DCInputSet set = new DCInputSet(formName, rows, listMap);
        return Collections.singletonList(set);
    }
    
    public DSpaceObject getSecondTopLevelCommunity(Context context, DSpaceObject dso) throws SQLException {
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

    /**
     * Convert one dynamic field definition to a DCInput field map.
     * @throws SQLException 
     */
    private Map<String,String> toDCInputFieldMap(Context context,
                                                 UserMetadataFields def,
                                                 Map<String, List<String>> listMap) throws SQLException {
        String key = def.getSystemFieldName();
        String[] parts = key.split("\\.");

        if (parts.length < 2) {
            // invalid key
            return null;
        }

        String schema = parts[0];
        String element = parts[1];
        String qualifier = (parts.length >= 3) ? parts[2] : null;

        // Validate the field exists in the registry
        if (metadataFieldService.findByString(context, key, '.') == null) {
            // either create it in registry first, or skip/throw
            return null;
        }

        Map<String,String> m = new HashMap<>();
        m.put("dc-schema", schema);
        m.put("dc-element", element);
        if (qualifier != null) {
            m.put("dc-qualifier", qualifier);
        }

        m.put("label", StringUtils.defaultIfBlank(def.getUserFieldName(), key));
        m.put("hint",   "Enter the " + m.get("label"));

        String inputType = mapFieldType(def.getFieldType());
        m.put("input-type", inputType);
        m.put("repeatable",    "false");
//        m.put("required",      "false");

        return m;
    }

    private String mapFieldType(Integer fieldType) {
        if (fieldType == null) return "onebox";
        switch (fieldType) {
            case 2: return "date";
            default: return "onebox";
        }
    }
}
