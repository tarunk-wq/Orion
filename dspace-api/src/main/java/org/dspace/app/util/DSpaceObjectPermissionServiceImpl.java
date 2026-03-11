/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.util.service.DSpaceObjectPermissionService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.constants.enums.FolderPermission;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceObjectPermissionServiceImpl implements DSpaceObjectPermissionService {

    @Autowired
    private ContentServiceFactory contentServiceFactory;
    
	@Autowired
	private AuthorizeService authorizeService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private CollectionService collectionService;
	
	@Autowired
	private CommunityService communityService;

    @Override
    public DSpaceObject findDSpaceObject(Context context, UUID uuid) throws SQLException {
        for (DSpaceObjectService<? extends DSpaceObject> dSpaceObjectService :
            contentServiceFactory.getDSpaceObjectServices()) {
            DSpaceObject dso = dSpaceObjectService.find(context, uuid);
            if (dso != null) {
                return dso;
            }
        }
        return null;
    }
    
    @Override
    public boolean hasAdminPermission(Context context, DSpaceObject dso) throws Exception {
        if (authorizeService.isAdmin(context)) return true;
        
        Community topLevel = (Community) getTopLevelCommunity(context, dso);
        if (topLevel != null && isDepartmentAdmin(context, topLevel)) return true;
        
        Community subLevel = (Community) getSecondTopLevelCommunity(context, dso);
        if (subLevel != null && isSubDepartmentAdmin(context, subLevel)) return true;
        
        return false;
    }
    
    public boolean isDepartmentAdmin(Context context, Community community) throws Exception {
        if (community == null) return false;
        String adminGroupName = String.join("_", community.getName(), community.getID().toString(), FolderPermission.ADMIN.name());
        Group adminGroup = groupService.findByName(context, adminGroupName);
        return adminGroup != null && groupService.isMember(context, context.getCurrentUser(), adminGroup);
    }

    public boolean isSubDepartmentAdmin(Context context, Community community) throws Exception {
        if (community == null) return false;
        String adminGroupName = String.join("_", community.getName(), community.getID().toString(), FolderPermission.ADMIN.name());
        Group adminGroup = groupService.findByName(context, adminGroupName);
        return adminGroup != null && groupService.isMember(context, context.getCurrentUser(), adminGroup);
    }
    
    @Override
    public DSpaceObject getTopLevelCommunity(Context context, DSpaceObject dso) throws Exception {
        if (dso == null) return null;
        
        if (dso instanceof Item) {
        	Item item = (Item) dso;
        	if (item.getOwningCollection() != null) return getTopLevelCommunity(context, item.getOwningCollection());
        	else if (item.getOwningCommunity() != null) return getTopLevelCommunity(context, item.getOwningCommunity());
        	return null;
        } else if (dso instanceof Collection) {
            return getTopLevelCommunity(context, collectionService.getParentObject(context, (Collection) dso));
        } else if (dso instanceof Community) {
        	Community currentCommunity = (Community) dso;
            DSpaceObject parent = communityService.getParentObject(context, currentCommunity);
            return (parent == null) ? currentCommunity : getTopLevelCommunity(context, parent);
        }
        return null;
    }
    
    @Override
    public DSpaceObject getSecondTopLevelCommunity(Context context, DSpaceObject dso) throws Exception {
        if (dso == null) return null;
        
        if (dso instanceof Item) {
        	Item item = (Item) dso;
        	if (item.getOwningCollection() != null) return getSecondTopLevelCommunity(context, item.getOwningCollection());
        	else if (item.getOwningCommunity() != null) return getSecondTopLevelCommunity(context, item.getOwningCommunity());
        	return null;
        } else if (dso instanceof Collection) {
            return getSecondTopLevelCommunity(context, collectionService.getParentObject(context, (Collection) dso));
        } else if (dso instanceof Community) {
        	Community currentCommunity = (Community) dso;
            DSpaceObject parentDso = communityService.getParentObject(context, currentCommunity);
            if (!(parentDso instanceof Community)) return null;
            Community parentComm = (Community) parentDso;
            DSpaceObject grandParentDso = communityService.getParentObject(context, parentComm);
            return (grandParentDso == null) ? currentCommunity : getSecondTopLevelCommunity(context, parentComm);
        }
        return null;
    }
}
