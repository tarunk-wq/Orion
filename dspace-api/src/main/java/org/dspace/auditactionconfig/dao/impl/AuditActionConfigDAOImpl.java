package org.dspace.auditactionconfig.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Query;

import org.dspace.auditactionconfig.AuditActionConfig;
import org.dspace.auditactionconfig.dao.AuditActionConfigDAO;
import org.dspace.audittrail.AuditAction;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;

/**
 * DAO implementation for AuditActionConfig.
 * Only handles saving and updating actionCode + actionStatus.
 */
public class AuditActionConfigDAOImpl extends AbstractHibernateDSODAO<AuditActionConfig> implements AuditActionConfigDAO {

	@Override
	public void initializeAuditActionConfig(Context context) throws SQLException {
		List<AuditActionConfig> existingConfigs = findAll(context);
        Set<String> existingActionCodes = existingConfigs.stream()
                .map(AuditActionConfig::getActionCode)
                .collect(Collectors.toSet());
        // Loop over all possible actions
        for (AuditAction action : AuditAction.values()) {
            if (!existingActionCodes.contains(action.getCode())) {
                // Only insert if action code doesn't exist
                AuditActionConfig config = new AuditActionConfig();
                config.setActionCode(action.getCode());
                config.setActionStatus(false);
                create(context, config);
            }
        }
	}
    
    @Override
    public AuditActionConfig findByActionCode(Context context, String actionCode) throws SQLException {
        String hql = "FROM AuditActionConfig WHERE actionCode = :actionCode";
        Query query = createQuery(context, hql);
        query.setParameter("actionCode", actionCode);
        return singleResult(query);
    }

    @Override
    public List<AuditActionConfig> findAll(Context context) throws SQLException {
        return findAll(context, AuditActionConfig.class);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<String> findEnabledActionCodes(Context context) throws SQLException {
        String hql = "SELECT actionCode FROM AuditActionConfig WHERE actionStatus = true";
        Query query = createQuery(context, hql);
        return query.getResultList();
    }
}
