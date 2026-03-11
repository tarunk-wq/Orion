package org.dspace.auditactionconfig.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.List;

import org.dspace.auditactionconfig.AuditActionConfig;
import org.dspace.auditactionconfig.dao.AuditActionConfigDAO;
import org.dspace.auditactionconfig.service.AuditActionConfigService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class AuditActionConfigServiceImpl extends DSpaceObjectServiceImpl<AuditActionConfig> implements AuditActionConfigService {

    @Autowired(required = true)
    protected AuditActionConfigDAO auditActionConfigDAO;


    @Override
    public void initializeAuditActionConfig(Context context) throws SQLException {
        auditActionConfigDAO.initializeAuditActionConfig(context);
    }

    @Override
    public AuditActionConfig findByActionCode(Context context, String actionCode) throws SQLException {
    	return auditActionConfigDAO.findByActionCode(context, actionCode);
    }
    
    @Override
    public List<AuditActionConfig> findAll(Context context) throws SQLException {
        return auditActionConfigDAO.findAll(context);
    }
    
    @Override
    public List<String> findEnabledActionCodes(Context context) throws SQLException {
        return auditActionConfigDAO.findEnabledActionCodes(context);
    }


    // Required by DSpaceObjectServiceImpl, but not implemented in your question's context
    @Override
    public AuditActionConfig findByIdOrLegacyId(Context context, String id) throws SQLException {
        return null;
    }

    @Override
    public AuditActionConfig findByLegacyId(Context context, int legacyId) throws SQLException {
        return null;
    }

    @Override
    public int getSupportsTypeConstant() {
        return 0;
    }

	@Override
	public AuditActionConfig find(Context context, UUID uuid) throws SQLException {
		return null;
	}

	@Override
	public void updateLastModified(Context context, AuditActionConfig dso) throws SQLException, AuthorizeException {		
	}

	@Override
	public void delete(Context context, AuditActionConfig dso) throws SQLException, AuthorizeException, IOException {		
	}
}
