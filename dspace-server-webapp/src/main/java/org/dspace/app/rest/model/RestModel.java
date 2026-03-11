/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A REST resource directly or indirectly (in a collection) exposed must have at
 * least a type attribute to facilitate deserialization.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface RestModel extends Serializable {

    public static final String ROOT = "root";
    public static final String CONTENT_REPORT = "contentreport";
    public static final String CORE = "core";
    public static final String EPERSON = "eperson";
    public static final String DISCOVER = "discover";
    public static final String DUPLICATES = "duplicates";
    public static final String CONFIGURATION = "config";
    public static final String INTEGRATION = "integration";
    public static final String STATISTICS = "statistics";
    public static final String SUBMISSION = "submission";
    public static final String SYSTEM = "system";
    public static final String WORKFLOW = "workflow";
    public static final String AUTHORIZATION = "authz";
    public static final String VERSIONING = "versioning";
    public static final String AUTHENTICATION = "authn";
    public static final String TOOLS = "tools";
    public static final String LDN = "ldn";
    public static final String PID = "pid";
    
	public static final String AUDITTRAIL = "audittrail";
	public static final String ANALYTICS = "analytics";
	public static final String BACKUP = "backups";
	public static final String BATCHDETAIL="batchdetailc";
    public static final String BATCHHISTORY="batchhistory";
    public static final String BATCHREJECT="batchrejectc";
    public static final String DEPARTMENT="department";
    public static final String MOVE_ITEM = "moveItem";
	public static final String SAVEDSEARCH = "savedsearch";
    public static final String SUB_DEPARTMENT="sub-department";
	public static final String THESAURUS = "thesaurusm";
    public static final String TEMP_ACCCESS = "tempaccess";
    public static final String UPLOADDETAIL="uploaddetail";


    public String getType();

    @JsonIgnore
    public String getTypePlural();
}
