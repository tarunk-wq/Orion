package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.hateoas.GroupResource;
import org.dspace.app.rest.repository.CommunityRestRepository;
import org.dspace.app.rest.service.UserPermissionService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/communities" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
public class CommunityWorkflowRestController {

	@Autowired
	private CommunityService communityService;
	
	@Autowired
	private AuthorizeService authorizeService;
	
	@Autowired
	private UserPermissionService userPermissionService;
	
	@Autowired
    private WorkflowService workflowService;
	
	@Autowired
	private CommunityRestRepository communityRestRepository;
	
	@Autowired
    private ConverterService converterService;
	
	@RequestMapping(method = RequestMethod.POST, value = "/workflowGroups/{workflowRole}")
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'READ')")
    public ResponseEntity<RepresentationModel<?>> postWorkflowGroupForRole(@PathVariable UUID uuid,
                                                                           HttpServletResponse response,                                                                           HttpServletRequest request,
                                                                           @PathVariable String workflowRole) throws Exception {
        
		Context context = ContextUtil.obtainContext(request);
        Community community = communityService.find(context, uuid);
        if (community == null) {
            throw new ResourceNotFoundException("No such community: " + uuid);
        }
        
        // only admin, department admin and section admin can manage workflow
        if (!authorizeService.isAdmin(context) && !userPermissionService.isDepOrSectionAdmin(context, Utils.extractDepAndSub(community)[0])) {
            throw new AuthorizeException(
                "Only system admin, department admin and section admin are allowed to manage community workflow");
        }
        
        Group group = workflowService.getWorkflowRoleGroup(context, community, workflowRole, null);
        if (group != null) {
            throw new UnprocessableEntityException("WorkflowGroup already exists for the role: " + workflowRole +
                                                       " in community with UUID: " + community.getID());
        }
        GroupRest groupRest = communityRestRepository
            .createWorkflowGroupForRole(context, request, community, workflowRole);
        context.complete();
        GroupResource groupResource = converterService.toResource(groupRest);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), groupResource);
    }
	
	@RequestMapping(method = RequestMethod.GET, value = "/workflowGroups/{workflowRole}")
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'READ')")
    public ResponseEntity<RepresentationModel<?>> getWorkflowGroupForRole(@PathVariable UUID uuid,
                                                                          HttpServletResponse response,
                                                                          HttpServletRequest request,
                                                                          @PathVariable String workflowRole) throws Exception {
        
		Context context = ContextUtil.obtainContext(request);
        Community community = communityService.find(context, uuid);
        if (community == null) {
            throw new ResourceNotFoundException("No such community: " + uuid);
        }

        // only admin, department admin and section admin can manage workflow
        if (!authorizeService.isAdmin(context) && !userPermissionService.isDepOrSectionAdmin(context, Utils.extractDepAndSub(community)[0])) {
            throw new AuthorizeException(
                "Only system admin, department admin and section admin are allowed to manage community workflow");
        }
        GroupRest groupRest = communityRestRepository.getWorkflowGroupForRole(context, community, workflowRole);
        if (groupRest == null) {
            return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
        }
        GroupResource groupResource = converterService.toResource(groupRest);
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), groupResource);
    }
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/workflowGroups/{workflowRole}")
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'READ')")
    public ResponseEntity<RepresentationModel<?>> deleteWorkflowGroupForRole(@PathVariable UUID uuid,
                                                                             HttpServletResponse response,
                                                                             HttpServletRequest request,
                                                                             @PathVariable String workflowRole)
        throws Exception {
        Context context = ContextUtil.obtainContext(request);
        Community community = communityService.find(context, uuid);
        if (community == null) {
            throw new ResourceNotFoundException("No such community: " + uuid);
        }
        
        // only admin, department admin and section admin can manage workflow
        if (!authorizeService.isAdmin(context) && !userPermissionService.isDepOrSectionAdmin(context, Utils.extractDepAndSub(community)[0])) {
            throw new AuthorizeException(
                "Only system admin, department admin and section admin are allowed to manage community workflow");
        }
        communityRestRepository.deleteWorkflowGroupForRole(context, request, community, workflowRole);
        context.complete();
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }
}
