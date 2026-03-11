package org.dspace.xmlworkflow.storedcomponents.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.storedcomponents.CommunityRole;

public interface CommunityRoleService {

	/**
     * This is the default name of the role equivalent in the default configuration to the "legacy" workflow step1. Old
     * piece of code will expect to use it in place of the workflow step1
     */
    public final String LEGACY_WORKFLOW_STEP1_NAME = "reviewer";

    /**
     * This is the default name of the role equivalent in the default configuration to the "legacy" workflow step2. Old
     * piece of code will expect to use it in place of the workflow step2
     */
    public final String LEGACY_WORKFLOW_STEP2_NAME = "editor";

    /**
     * This is the default name of the role equivalent in the default configuration to the "legacy" workflow step3. Old
     * piece of code will expect to use it in place of the workflow step3
     */
    public final String LEGACY_WORKFLOW_STEP3_NAME = "finaleditor";

    public CommunityRole find(Context context, int id) throws SQLException;

    public CommunityRole find(Context context, UUID communityId, String role) throws SQLException;

    /**
     * 
     * @param context
     *            DSpace context
     * @param group
     *            EPerson Group
     * @return the list of CommunityRole assigned to the specified group
     * @throws SQLException
     */
    public List<CommunityRole> findByGroup(Context context, UUID groupId) throws SQLException;

    public List<CommunityRole> findByCommunity(Context context, UUID communityId) throws SQLException;

    public CommunityRole create(Context context, Community community, String roleId, Group group)
        throws SQLException;

    public void update(Context context, CommunityRole communityRole) throws SQLException;

    public void delete(Context context, CommunityRole communityRole) throws SQLException;

    public void deleteByCommunity(Context context, UUID communityId) throws SQLException;
    
	void deleteByCommunity(Context context, Community community) throws SQLException;
}
