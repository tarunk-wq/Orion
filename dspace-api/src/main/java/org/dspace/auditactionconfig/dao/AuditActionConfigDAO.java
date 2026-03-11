package org.dspace.auditactionconfig.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.auditactionconfig.AuditActionConfig;
import org.dspace.content.dao.DSpaceObjectDAO;
import org.dspace.content.dao.DSpaceObjectLegacySupportDAO;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * DAO interface for AuditActionConfig.
 * Extends DSpaceObjectDAO for core operations and defines custom queries.
 */
public interface AuditActionConfigDAO extends DSpaceObjectDAO<AuditActionConfig>, DSpaceObjectLegacySupportDAO<AuditActionConfig>, GenericDAO<AuditActionConfig> {

    void initializeAuditActionConfig(Context context) throws SQLException;
    
    AuditActionConfig findByActionCode(Context context, String actionCode) throws SQLException;
    
    List<AuditActionConfig> findAll(Context context) throws SQLException;
    
    List<String> findEnabledActionCodes(Context context) throws SQLException;    
}
