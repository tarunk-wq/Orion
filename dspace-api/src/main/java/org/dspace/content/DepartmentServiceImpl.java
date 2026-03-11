package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.constants.enums.FolderPermission;
import org.dspace.constants.enums.NodeType;
import org.dspace.content.dao.DepartmentDAO;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DepartmentService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.util.ObjectMapperUtil;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DepartmentServiceImpl extends DSpaceObjectServiceImpl<Department> implements DepartmentService {
	
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DepartmentServiceImpl.class);

	@Autowired
	private DepartmentDAO departmentDAO;
	
	@Autowired
	private CommunityService communityService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private AuthorizeService authorizeService;
	
	@Autowired
	private EPersonService ePersonService;
	
	@Autowired
	private AuditTrailService auditTrailService;
	
	@Autowired(required = true)
    protected SearchService searchService;
	
	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public Department find(Context context, UUID uuid) throws SQLException {
		return departmentDAO.findByID(context, Department.class, uuid);
	}

	@Override
	public void updateLastModified(Context context, Department dso) throws SQLException, AuthorizeException {
		departmentDAO.delete(context, dso);
	}

	@Override
	public void delete(Context context, Department dso) throws SQLException, AuthorizeException, IOException {
		String handle = dso.getCommunity().getID().toString();
		String name = dso.getDepartmentName();
		
		Community community = dso.getCommunity();
		if (community.getSubcommunities().size() > 0) {
			throw new RuntimeException("CANNOT-DELETE");
		}
		// delete permission groups
		deleteRelatedGroup(context, community);
		
		departmentDAO.delete(context, dso);
		communityService.delete(context, community);
		auditTrailService.logAction(context, handle, AuditAction.DEPARTMENT_DELETED, name);
	}

	@Override
	public int getSupportsTypeConstant() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Department> findAllDepartment(Context context) throws SQLException {
		return departmentDAO.findAll(context);
	}

	@Override
	public Department findByDepartmentAbbreviation(Context context, String departmentAbbr) throws SQLException {
		return departmentDAO.findByDepartmentAbbreviation(context, departmentAbbr);
	}

	@Override
	public Department findByCommunity(Context context, Community community) throws SQLException {
		return departmentDAO.findByCommunity(context, community);
	}

	@Override
	public Department create(Context context, String departmentName, String adminGroupName, String abbreviation,
			String communityId) throws SQLException, AuthorizeException {
		if (findByName(context, departmentName.toLowerCase()) != null) {
			throw new SQLException("DUPLICATE");
		}
		
		Community newCommunity = communityService.create(null, context);
		newCommunity.setName(departmentName);
		
		// create groups and add permissions
		addResourcePolicies(context, newCommunity, departmentName);
		communityService.update(context, newCommunity);		
		
		// create department
		Department department = new Department();
		
		department.setAbbreviation(abbreviation);
		department.setAdminGroupName(adminGroupName);
		department.setCommunity(communityService.find(context, newCommunity.getID()));
		department.setCreationTime(new Date());
		department.setCreatedBy(context.getCurrentUser());
		department.setDepartmentName(departmentName);
		
		department = departmentDAO.create(context, department);
		auditTrailService.logAction(context, newCommunity.getID().toString(), AuditAction.DEPARTMENT_CREATED,department.getDepartmentName());
				
		return department;
	}

	private void addResourcePolicies(Context context, Community community, String departmentName) throws SQLException, AuthorizeException {
		String groupNamePrefix = String.join("_", departmentName, community.getID().toString());
		
		// add admin group
		String adminGroupName = String.join("_", groupNamePrefix, FolderPermission.ADMIN.name());
		Group adminGroup = groupService.createGroupByName(context, adminGroupName);
		authorizeService.createResourcePolicy(context, community, adminGroup, null, Constants.ADMIN, ResourcePolicy.TYPE_CUSTOM);
		
		communityService.save(context, community);
	}
	
	private void deleteRelatedGroup(Context context, Community community) throws SQLException, AuthorizeException, IOException {
		String groupNamePrefix = String.join("_", community.getName(), community.getID().toString());
		
		// add admin group
		String adminGroupName = String.join("_", groupNamePrefix, FolderPermission.ADMIN.name());
		Group adminGroup = groupService.findByName(context, adminGroupName);
		groupService.delete(context, adminGroup);
		
		authorizeService.removeAllPolicies(context, community);
	}

	@Override
	public Department findbyUuid(Context context, String uuid) throws SQLException {
		UUID departmentuuid = UUID.fromString(uuid);
		return departmentDAO.findByUuid(context, departmentuuid);
	}

	@Override
	public Department findByName(Context context, String name) throws SQLException {
		return departmentDAO.findByName(context, name);
	}

	@Override
	public ArrayNode findAllLeaf(Context context, UUID depCommId) throws SQLException {
		Community department = communityService.find(context, depCommId);
		
		ArrayNode nodes = ObjectMapperUtil.createArrayNode();
		addLeafs(department, nodes);		
		
		return nodes;
	}

	private void addLeafs(Community community, ArrayNode nodes) {
		for (Collection collection : community.getCollections()) {
			nodes.add(convertCollection(collection));
		}
		
		for (Community subCommunity : community.getSubcommunities()) {
			addLeafs(subCommunity, nodes);			
		}
	}

	private ObjectNode convertCollection(Collection collection) {
		ObjectNode node = ObjectMapperUtil.createObjectNode();
		
		node.put("id", collection.getID().toString());
		node.put("name", collection.getName());
		
		return node;
	}

	@Override
	public void assignAdmin(Context context, UUID depId, String userId) throws SQLException {
		Department department = find(context, depId);		
		Community parent = department.getCommunity();

		// find admin group
		String adminGroupName = String.join("_", parent.getName(), parent.getID().toString(), FolderPermission.ADMIN.name());
		Group adminGroup = groupService.findByName(context, adminGroupName);
		if (adminGroup == null) {
			log.error("Admin group is null");
			throw new SQLException("Admin group is not found");
		}
		EPerson ePerson =  ePersonService.find(context, UUID.fromString(userId));
		groupService.addMember(context, adminGroup, ePerson);
		auditTrailService.logAction(context,parent.getID().toString(), AuditAction.ADMIN_ASSIGN_TO_DEPARTMENT, ePerson.getEmail(), department.getDepartmentName());
	}

	@Override
	public void removeAdmin(Context context, UUID depId, List<UUID> userIds) throws SQLException {
		Department department = find(context, depId);		
		Community parent = department.getCommunity();
		
		// find admin group
		String adminGroupName = String.join("_", parent.getName(), parent.getID().toString(), FolderPermission.ADMIN.name());
		Group adminGroup = groupService.findByName(context, adminGroupName);
		if (adminGroup == null) {
			log.error("Admin group is null");
			throw new SQLException("Admin group is not found");
		}
		for(UUID uuid : userIds) {			
			EPerson ePerson =  ePersonService.find(context, uuid);
			groupService.removeMember(context, adminGroup, ePerson);
			auditTrailService.logAction(context,parent.getID().toString(), AuditAction.ADMIN_REMOVED_FROM_DEPARTMENT, ePerson.getEmail(), department.getDepartmentName());
		}
	}
	
	@Override
	public ArrayNode getEpersonsArrayNode(Context context, String deptId) {
		ArrayNode nodes = mapper.createArrayNode();
		try {			
			Department department = find(context, UUID.fromString(deptId));
			String adminGroupName = String.join("_", department.getDepartmentName(), department.getCommunity().getID().toString(), FolderPermission.ADMIN.name());
			Group adminGroup = groupService.findByName(context, adminGroupName);
			List<EPerson> allMembers = groupService.allMembers(context, adminGroup);
			for(EPerson eperson : allMembers) {
				ObjectNode node = mapper.createObjectNode();
        		node.put("id", eperson.getID().toString());
        		node.put("name", eperson.getFullName());
        		nodes.add(node);
			}
		} catch (Exception e) {
			// TODO: handle exception
			log.error("Error fetching department epersons list", e);
		}
		return nodes;
	}

	@Override
	public ArrayNode findAllFolders(Context context, UUID communityUuid, String query) throws SQLException, AuthorizeException, SearchServiceException {
		ArrayNode result = ObjectMapperUtil.createArrayNode();
		Community department = communityService.find(context, communityUuid);
		Set<UUID> subDepartments = department.getSubcommunities()
											 .stream()
									         .map(Community::getID)
									         .collect(Collectors.toSet());
		
		if (!authorizeService.isAdmin(context) && !isDepOrSectionAdmin(context, department)) {
			throw new AuthorizeException("User cant access folders");
        }

        DiscoverQuery discoverQuery = new DiscoverQuery();
        DiscoverResult resp = retrieveFolders(context, discoverQuery, communityUuid, query);

        for (IndexableObject solrResult : resp.getIndexableObjects()) {
        	if (solrResult instanceof IndexableCollection) {      		
        		Collection c = ((IndexableCollection) solrResult).getIndexedObject();
        		
        		ObjectNode node = ObjectMapperUtil.createObjectNode();
        		node.put("id", c.getID().toString());
        		node.put("name", c.getName());
        		node.put("type", NodeType.LEAF.name());
        		
        		result.add(node);
        	
        	} else if (solrResult instanceof IndexableCommunity) {      		
        		Community c = ((IndexableCommunity) solrResult).getIndexedObject();
        		
        		// skip sub departments
        		if (subDepartments.contains(c.getID())) {
        			continue;
        		}
        		
        		ObjectNode node = ObjectMapperUtil.createObjectNode();
        		node.put("id", c.getID().toString());
        		node.put("name", c.getName());
        		node.put("type", NodeType.BRANCH.name());
        		
        		result.add(node);
        	}
        }
        return result;
	}
	
	private boolean hasCommunityPermission(Context context, Community community, EPerson user, String permission) throws SQLException {
		// find group which corresponding to given permision
		String groupName = String.join("_", community.getName(), community.getID().toString(), permission);
		Group group = groupService.findByName(context, groupName);
		return groupService.isMember(context, user, group);
	}
	
	public boolean isDepOrSectionAdmin(Context context, Community department) throws SQLException {
		// Check if the user is an admin of the department's community
		if (hasCommunityPermission(context, department, context.getCurrentUser(), FolderPermission.ADMIN.name())) {
			return true;
		}
		
		// Check user's permissions within each subdepartment of the department
		for (Community community : department.getSubcommunities()) {
			if (hasCommunityPermission(context, community, context.getCurrentUser(), FolderPermission.ADMIN.name())) {
				return true;
			}
		}
		return false;
	}

	private DiscoverResult retrieveFolders(Context context, DiscoverQuery discoverQuery, UUID communityUuid, String q)
			throws SQLException, SearchServiceException {

		discoverQuery.addFilterQueries("location.comm:" + communityUuid.toString());
		
		// Allow only Collection and Community
	    discoverQuery.addFilterQueries("(" 
	        + SearchUtils.RESOURCE_TYPE_FIELD + ":" + Collection.class.getSimpleName()
	        + " OR "
	        + SearchUtils.RESOURCE_TYPE_FIELD + ":" + Community.class.getSimpleName()
	        + ")"
	    );

		if (StringUtils.isNotBlank(q)) {
			StringBuilder buildQuery = new StringBuilder();
			String escapedQuery = ClientUtils.escapeQueryChars(q);
			buildQuery.append("(").append(escapedQuery).append(" OR ").append(escapedQuery).append("*").append(")");
			discoverQuery.setQuery(buildQuery.toString());
		}
		DiscoverResult resp = searchService.search(context, discoverQuery);
		return resp;
	}
}
