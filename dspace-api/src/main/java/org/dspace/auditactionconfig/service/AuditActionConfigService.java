package org.dspace.auditactionconfig.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.auditactionconfig.AuditActionConfig;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;

/**
 * Service interface for managing AuditActionConfig objects.
 * Provides methods to initialize and update audit action configurations.
 * 
 * Extends DSpace core services to support standard DSO operations and legacy ID support.
 */
public interface AuditActionConfigService extends DSpaceObjectService<AuditActionConfig>, DSpaceObjectLegacySupportService<AuditActionConfig> {

    /**
     * Initializes the audit_action_config table with all available AuditAction enum values.
     * Sets the initial status of each action to false.
     *
     * @param context the DSpace context
     * @throws SQLException 
     */
    void initializeAuditActionConfig(Context context) throws SQLException;

    /**
     * Updates the status (enabled/disabled) of a specific AuditActionConfig entry.
     *
     * @param context    the DSpace context
     * @param actionCode the code of the audit action to update
     * @param status     the new status to set (true = enabled, false = disabled)
     * @throws SQLException if any database error occurs
     */
    AuditActionConfig findByActionCode(Context context, String actionCode) throws SQLException;
    
    /**
     * Fetches all AuditActionConfig entries in the table.
     *
     * @param context the DSpace context
     * @return a list of all AuditActionConfig entries
     * @throws SQLException if any database error occurs
     */
    List<AuditActionConfig> findAll(Context context) throws SQLException;
    
    /**
     * Fetches all action codes from AuditActionConfig where the actionStatus is true.
     * These represent the enabled audit actions that should be considered in filtering audit trail queries.
     *
     * @param context the DSpace context
     * @return a list of enabled action codes
     * @throws SQLException if any database error occurs
     */
    List<String> findEnabledActionCodes(Context context) throws SQLException;

}
