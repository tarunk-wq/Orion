package org.dspace.audittrail;

public enum AuditAction {
	//Authentication
    LOGIN("LOGIN", "Logged in"),
    LOGOUT("LOGOUT", "Logged out"),
    
    // EPerson Management Actions
    EPERSON_CREATE("EPERSON_CREATE", "Created a new EPerson %s"),
    EPERSON_REGISTER("EPERSON_REGISTER", "Registered a new EPerson %s"),
    EPERSON_UPDATE_PROFILE("EPERSON_UPDATE_PROFILE", "Updated EPerson %s"),
    EPERSON_ADMIN_CHANGED_PASSWORD("EPERSON_ADMIN_CHANGED_PASSWORD", "Changed EPerson password by admin"),
    EPERSON_ADMIN_DISABLED_LOGIN("EPERSON_ADMIN_DISABLED_LOGIN","Login has been disabled for EPerson by admin"),
    EPERSON_PASSWORD_RESET_REQUEST("EPERSON_PASSWORD_RESET_REQUEST", "Requested password reset email"),
    
    // Group Membership Management
    GROUP_CREATE("GROUP_CREATED","Group Create: %s"),
    GROUP_DELETE("GROUP_DELETED","Group Delete: %s"),
    EPERSON_ADD_TO_GROUP("EPERSON_ADD_TO_GROUP", "Added EPerson to group: %s"),
    EPERSON_REMOVE_FROM_GROUP("EPERSON_REMOVE_FROM_GROUP", "Removed EPerson from group: %s"),
    GROUP_SUBGROUP_ADD("GROUP_SUBGROUP_ADD", "Added Subgroup to group %s"),
    GROUP_SUBGROUP_REMOVE("GROUP_SUBGROUP_REMOVE", "Removed Subgroup %s from group %s"),
	
    // Metadata
    METADATA_EDIT("METADATA_EDIT", "Edited metadata in %s field"),
    METADATA_ADD("METADATA_ADD", "Added metadata field %s with value %s"),
    METADATA_REMOVE("METADATA_REMOVE", "Removed metadata field %s"),
    
    // Discovery Search Actions
    SEARCH("SEARCH", "Performed search with query: %s"),
    DISCOVERY_FACETED_SEARCH("DISCOVERY_FACETED_SEARCH", "Performed faceted search with facets: %s"),
    DISCOVERY_PROXIMITY_SEARCH("DISCOVERY_PROXIMITY_SEARCH", "Performed proximity search with query: %s"),
    DISCOVERY_PHONETIC_SEARCH("DISCOVERY_PHONETIC_SEARCH", "Performed phonetic search with query: %s"),
    DISCOVERY_TEXT_FUZZY_SEARCH("DISCOVERY_TEXT_FUZZY_SEARCH", "Performed fuzzy text search with query: %s"),

	// Analytics
	DOWNLOAD_PDF_REPORT("DOWNLOAD_PDF_REPORT", "Downloaded PDF report: %s"),
	DOWNLOAD_CSV_REPORT("DOWNLOAD_CSV_REPORT", "Downloaded CSV report: %s"),
  
    // Item
	ITEM_CREATED("ITEM_CREATED_IN_COLLECTION", "Item %s created in folder %s with combination metadata value %s"),
    ITEM_DELETE("ITEM_DELETE", "Deleted item %s in collection %s"),
    BATCH_UPLOAD("BATCH_UPLOAD", "Batch uploaded %s file to collection %s using %s script"),
    ITEM_MOVED("ITEM_MOVED", "Moved item %s to %s"),
    ITEM_RETRIEVED("ITEM_RETRIEVED","Retrieved item %s from collection %s"),
    
    //Bitstream
    BITSTREAM_DOWNLOAD_VIEWED("BITSTREAM_DOWNLOAD_VIEWED", "Viewed or download Bitstream %s"),
    BITSTREAM_DELETE("BITSTREAM_DELETE", "Deleted bitstream %s"),
    BITSTREAM_UPDATE_FORMAT("BITSTREAM_UPDATE_FORMAT", "Updated bitstream format for %s"),
	BITSTREAM_UPLOAD_ITEM_SUBMISSION("BITSTREAM_UPLOAD_ITEM_SUBMISSION", "Uploaded bitstream during item submission"),
	BITSTREAN_UPLOAD("BITSTREAM_UPLOAD", "Upload file - %s"),
	
	//Record Management
	RECORD_CREATED("RECORD_CREATED","Create Record ID %s"),
	RECORD_UPDATED("RECORD_UPDATED","Update Record ID %s"),
	RECORD_DELETED("RECORD_DELETED","Delete Record ID %s"),
	RECORD_ARCHIEVED("RECORD_ARCHIEVED","Archieved Record ID %s"),
	RECORD_NON_ARCHIEVED("RECORD_NON_ARCHIEVED","Non Archieved Record ID %s"),
	
	//Department
	DEPARTMENT_CREATED("DEPARTMENT_CREATED","Department Created - %s"),
	DEPARTMENT_DELETED("DEPARTMENT_DELETED","Department Deleted - %s"),
	ADMIN_ASSIGN_TO_DEPARTMENT("ASSIGN_ADMIN_TO_DEPARTMENT", "Assigned admin %s to Department %s"),
	ADMIN_REMOVED_FROM_DEPARTMENT("ADMIN_REMOVED_FROM_DEPARTMENT", "Admin %s removed from Department %s"),
	
	//SubDepartment
	SUB_DEPARTMENT_CREATED("SUB_DEPARTMENT_CREATED","Sub Department Created - %s"),
	SUB_DEPARTMENT_DELETED("SUB_DEPARTMENT_DELETED","Sub Department Deleted - %s"),
	ADMIN_ASSIGN_TO_SUB_DEPARTMENT("ASSIGN_ADMIN_TO_SUB_DEPARTMENT", "Assigned admin %s to Sub Department %s"),
	ADMIN_REMOVED_FROM_SUB_DEPARTMENT("ADMIN_REMOVED_FROM_SUB_DEPARTMENT", "Admin %s removed from Sub Department %s"),
	
	// Folder
	FOLDER_CREATED("FOLDER_CREATED","Folder Created - %s"),
	FOLDER_DELETED("FOLDER_DELETED","Folder Deleted - %s"),
	FOLDER_RENAMED("FOLDER_RENAMED","Folder renamed from %s to %s"),
	REVOKED_FOLDER_PERMISSION("REVOKED_FOLDER_PERMISSION","Revoked %s permission from %s for folder %s"),
	GRANT_FOLDER_PERMISSION("GRANT_FOLDER_PERMISSION","Grant %s permission to %s for folder %s"),
	
	//Temp Access
	TEMP_ACCESS_CREATED("TEMP_ACCESS_CREATED", "Temporary access created for item %s in collection %s"),
	TEMP_ACCESS_UPDATED("TEMP_ACCESS_UPDATED", "Temporary access updated for item %s in collection %s"),
	TEMP_ACCESS_RETRIEVED("TEMP_ACCESS_RETRIEVED", "Retrieved list of temporary access records for item %s in collection %s"),
	TEMP_ACCESS_DELETED("TEMP_ACCESS_DELETED", "Deleted temporary access for item %s in collection %s"),
	
	//Move Item
	MOVE_ITEM_GET_COLLECTIONS_SUBDEPARTMENT("MOVE_ITEM_GET_COLLECTIONS_SUBDEPARTMENT", "Listed all collections for subdepartment %s");

    private final String code;
    private final String template;

    AuditAction(String code, String template) {
        this.code = code;
        this.template = template;
    }

    public String getCode() {
        return code;
    }

    public String getTemplate() {
        return template;
    }

    public String formatDescription(Object... args) {
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template; // fallback if incorrect number of args
        }
    }

    @Override
    public String toString() {
        return code;
    }
}
