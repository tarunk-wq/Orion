package org.dspace.auditactionconfig;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.dspace.content.CacheableDSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;

@Entity
@Table(name = "audit_action_config")
public class AuditActionConfig extends CacheableDSpaceObject implements DSpaceObjectLegacySupport {

    @Column(name = "action_code", nullable = false, unique = true)
    private String actionCode;

    @Column(name = "action_status", nullable = false)
    private boolean actionStatus;

    public AuditActionConfig() {
    }

    public AuditActionConfig(String actionCode, boolean actionStatus) {
        this.actionCode = actionCode;
        this.actionStatus = actionStatus;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public boolean getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(boolean actionStatus) {
        this.actionStatus = actionStatus;
    }

	@Override
	public Integer getLegacyId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
}
