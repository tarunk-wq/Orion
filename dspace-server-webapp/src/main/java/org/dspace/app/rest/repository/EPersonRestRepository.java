/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.EPersonRegistrationRestController;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.EPersonNameNotProvidedException;
import org.dspace.app.rest.exception.OldPasswordReusedException;
import org.dspace.app.rest.exception.PasswordNotValidException;
import org.dspace.app.rest.exception.RESTEmptyWorkflowGroupException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ValidatePasswordService;
import org.dspace.content.Community;
import org.dspace.content.Department;
import org.dspace.content.service.DepartmentService;
import org.dspace.core.Constants.GroupPermission;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EmptyWorkflowGroupException;
import org.dspace.eperson.Group;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.PasswordHistoryService;
import org.dspace.eperson.service.RegistrationDataService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


/**
 * This is the repository responsible to manage EPerson Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(EPersonRest.CATEGORY + "." + EPersonRest.PLURAL_NAME)
public class EPersonRestRepository extends DSpaceObjectRestRepository<EPerson, EPersonRest>
                                   implements InitializingBean {

    private static final Logger log = LogManager.getLogger();

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ValidatePasswordService validatePasswordService;

    @Autowired
    private RegistrationDataService registrationDataService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ObjectMapper mapper;

    private final EPersonService es;
    
    @Autowired
    private EPersonService epersonService;
    
    @Autowired
	private DepartmentService departmentService;
    
    @Autowired
    private PasswordHistoryService passwordHistoryService;

	@Autowired
    private AuditTrailService auditTrailService;


    public EPersonRestRepository(EPersonService dsoService) {
        super(dsoService);
        this.es = dsoService;
    }

    @Override
    protected EPersonRest createAndReturn(Context context)
            throws AuthorizeException {
        // this need to be revisited we should receive an EPersonRest as input
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        EPersonRest epersonRest = null;
        try {
            epersonRest = mapper.readValue(req.getInputStream(), EPersonRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("error parsing the body... maybe this is not the right error code");
        }
        String token = req.getParameter("token");
        // If a token is available, we'll swap to the execution that is token based
        if (StringUtils.isNotBlank(token)) {
            try {
                return createAndReturn(context, epersonRest, token);
            } catch (SQLException e) {
                log.error("Something went wrong in the creation of an EPerson with token: " + token, e);
                throw new RuntimeException("Something went wrong in the creation of an EPerson with token: " + token);
            }
        }
        // If no token is present, we simply do the admin execution
        EPerson eperson = createEPersonFromRestObject(context, epersonRest);

        return converter.toRest(eperson, utils.obtainProjection());
    }

    private EPerson createEPersonFromRestObject(Context context, EPersonRest epersonRest) throws AuthorizeException {
        EPerson eperson = null;
        try {
            eperson = es.create(context);

            // this should be probably moved to the converter (a merge method?)
            eperson.setCanLogIn(epersonRest.isCanLogIn());
            eperson.setRequireCertificate(epersonRest.isRequireCertificate());
            eperson.setEmail(epersonRest.getEmail());
            eperson.setNetid(epersonRest.getNetid());
            if (epersonRest.getPassword() != null) {
                if (!validatePasswordService.isPasswordValid(epersonRest.getPassword())) {
                    throw new PasswordNotValidException();
                }
                passwordHistoryService.addPasswordHistory(context, eperson, epersonRest.getPassword());
                es.setPassword(eperson, epersonRest.getPassword());
            }
            
            //number of days after which password should expire
            int passwordExpiryTime = DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getIntProperty("eperson.password.expiry.days");
            		
    		Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, passwordExpiryTime);
            
            eperson.setPasswordExpiryDate(calendar.getTime());
            
            es.update(context, eperson);
            auditTrailService.logAction(context, eperson.getEmail(), AuditAction.EPERSON_CREATE, eperson.getName());
            metadataConverter.setMetadata(context, eperson, epersonRest.getMetadata());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return eperson;
    }

    /**
     * This method will perform checks on whether or not the given Request was valid for the creation of an EPerson
     * with a token or not.
     * It'll check that the token exists, that the token doesn't yet resolve to an actual eperson already,
     * that the email in the given json is equal to the email for the token and that other properties are set to
     * what we expect in this creation.
     * It'll check if all of those constraints hold true and if we're allowed to register new accounts.
     * If this is the case, we'll create an EPerson without any authorization checks and delete the token
     * @param context       The DSpace context
     * @param epersonRest   The EPersonRest given to be created
     * @param token         The token to be used
     * @return              The EPersonRest after the creation of the EPerson object
     * @throws AuthorizeException   If something goes wrong
     * @throws SQLException         If something goes wrong
     */
    private EPersonRest createAndReturn(Context context, EPersonRest epersonRest, String token)
        throws AuthorizeException, SQLException {
        if (!AuthorizeUtil.authorizeNewAccountRegistration(context, requestService
            .getCurrentRequest().getHttpServletRequest())) {
            throw new DSpaceBadRequestException(
                "Registration is disabled, you are not authorized to create a new Authorization");
        }
        RegistrationData registrationData = registrationDataService.findByToken(context, token);
        if (registrationData == null) {
            throw new DSpaceBadRequestException("The token given as parameter: " + token + " does not exist" +
                                                " in the database");
        }
        if (es.findByEmail(context, registrationData.getEmail()) != null) {
            throw new DSpaceBadRequestException("The token given already contains an email address that resolves" +
                                                " to an eperson");
        }
        String emailFromJson = epersonRest.getEmail();
        if (StringUtils.isNotBlank(emailFromJson)) {
            if (!StringUtils.equalsIgnoreCase(registrationData.getEmail(), emailFromJson)) {
                throw new DSpaceBadRequestException("The email resulting from the token does not match the email given"
                                                        + " in the json body. Email from token: " +
                                                    registrationData.getEmail() + " email from the json body: "
                                                    + emailFromJson);
            }
        }
        if (epersonRest.isSelfRegistered() != null && !epersonRest.isSelfRegistered()) {
            throw new DSpaceBadRequestException("The self registered property cannot be set to false using this method"
                                                    + " with a token");
        }
        checkRequiredProperties(registrationData, epersonRest);
        // We'll turn off authorisation system because this call isn't admin based as it's token based
        context.turnOffAuthorisationSystem();
        EPerson ePerson = createEPersonFromRestObject(context, epersonRest);
        context.restoreAuthSystemState();
        // Restoring authorisation state right after the creation call
        accountService.deleteToken(context, token);
        if (context.getCurrentUser() == null) {
            context.setCurrentUser(ePerson);
        }
        return converter.toRest(ePerson, utils.obtainProjection());
    }

    private void checkRequiredProperties(RegistrationData registration, EPersonRest epersonRest) {
        MetadataRest<MetadataValueRest> metadataRest = epersonRest.getMetadata();
        if (metadataRest != null) {
            List<MetadataValueRest> epersonFirstName = metadataRest.getMap().get("eperson.firstname");
            List<MetadataValueRest> epersonLastName = metadataRest.getMap().get("eperson.lastname");
            if (epersonFirstName == null || epersonLastName == null ||
                epersonFirstName.isEmpty() || epersonLastName.isEmpty()) {
                throw new EPersonNameNotProvidedException();
            }
        }

        String password = epersonRest.getPassword();
        String netId = epersonRest.getNetid();
        if (StringUtils.isBlank(password) && StringUtils.isBlank(netId)) {
            throw new DSpaceBadRequestException(
                "You must provide a password or register using an external account"
            );
        }

        if (StringUtils.isBlank(password) && !canRegisterExternalAccount(registration, epersonRest)) {
            throw new DSpaceBadRequestException(
                "Cannot register external account with netId: " + netId
            );
        }
    }

    private boolean canRegisterExternalAccount(RegistrationData registration, EPersonRest epersonRest) {
        return accountService.isTokenValidForCreation(registration) &&
            StringUtils.equals(registration.getNetId(), epersonRest.getNetid());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'EPERSON', 'READ')")
    public EPersonRest findOne(Context context, UUID id) {
        EPerson eperson = null;
        try {
            eperson = es.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (eperson == null) {
            return null;
        }
        return converter.toRest(eperson, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<EPersonRest> findAll(Context context, Pageable pageable) {
        try {
            long total = es.countTotal(context);
            List<EPerson> epersons = es.findAll(context, EPerson.EMAIL, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(epersons, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Find the eperson with the provided email address if any. The search is delegated to the
     * {@link EPersonService#findByEmail(Context, String)} method
     *
     * @param email
     *            is the *required* email address
     * @return a Page of EPersonRest instances matching the user query
     */
    @SearchRestMethod(name = "byEmail")
    public EPersonRest findByEmail(@Parameter(value = "email", required = true) String email) {
        EPerson eperson = null;
        try {
            Context context = obtainContext();
            eperson = es.findByEmail(context, email);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (eperson == null) {
            return null;
        }
        return converter.toRest(eperson, utils.obtainProjection());
    }

    /**
     * Find the epersons matching the query parameter. The search is delegated to the
     * {@link EPersonService#search(Context, String, int, int)} method
     *
     * @param query
     *            is the *required* query string
     * @param pageable
     *            contains the pagination information
     * @return a Page of EPersonRest instances matching the user query
     */
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MANAGE_ACCESS_GROUP')")
    @SearchRestMethod(name = "byMetadata")
    public Page<EPersonRest> findByMetadata(@Parameter(value = "query", required = true) String query,@Parameter(value = "isFiltered") boolean isFiltered,
            Pageable pageable) {

        try {
            Context context = obtainContext();
            long total = es.searchResultCount(context, query);
            List<EPerson> epersons = es.search(context, query, Math.toIntExact(pageable.getOffset()),
                                                               Math.toIntExact(pageable.getPageSize()));
            if(isFiltered) {
            	filterUsers(context, epersons);
            }
            return converter.toRestPage(epersons, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    private void filterUsers( Context context, List<EPerson> epersons){
    	try {
    		Group adminGroup = groupService.findByName(context, Group.ADMIN);
    		Set<EPerson> adminUsers = new HashSet<>(adminGroup.getMembers());
        	adminUsers.add(context.getCurrentUser());
        	epersons.removeAll(adminUsers);
		} catch (Exception e) {
			throw new RuntimeException("Error Filtering admin and current user from eperson list ", e);
		}
    	
    	return ;
    }
    /**
     * Find the EPersons matching the query parameter which are NOT a member of the given Group.
     * The search is delegated to the
     * {@link EPersonService#searchNonMembers(Context, String, Group, int, int)}  method
     *
     * @param groupUUID the *required* group UUID to exclude results from
     * @param query    is the *required* query string
     * @param pageable contains the pagination information
     * @return a Page of EPersonRest instances matching the user query
     */
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MANAGE_ACCESS_GROUP')")
    @SearchRestMethod(name = "isNotMemberOf")
    public Page<EPersonRest> findIsNotMemberOf(@Parameter(value = "group", required = true) UUID groupUUID,
                                             @Parameter(value = "query", required = true) String query,
                                             Pageable pageable) {

        try {
            Context context = obtainContext();
            Group excludeGroup = groupService.find(context, groupUUID);
            long total = es.searchNonMembersCount(context, query, excludeGroup);
            List<EPerson> epersons = es.searchNonMembers(context, query, excludeGroup,
                                                     Math.toIntExact(pageable.getOffset()),
                                                     Math.toIntExact(pageable.getPageSize()));
            return converter.toRestPage(epersons, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#uuid, 'EPERSON', #patch)")
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID uuid,
                         Patch patch) throws AuthorizeException, SQLException {
        boolean passwordChangeFound = false;
        EPerson eperson = es.find(context, uuid);
        for (Operation operation : patch.getOperations()) {
            if (StringUtils.equalsIgnoreCase(operation.getPath(), "/password")) {
                passwordChangeFound = true;
            }
        }
        
        if (StringUtils.isNotBlank(request.getParameter("token"))) {
            if (!passwordChangeFound) {
                throw new AccessDeniedException("Refused to perform the EPerson patch based on a token without " +
                                                    "changing the password");
            }
//        } else {
//            if (passwordChangeFound && !StringUtils.equals(context.getAuthenticationMethod(), "password")) {
//                throw new AccessDeniedException("Refused to perform the EPerson patch based to change the password " +
//                                                        "for non \"password\" authentication");
//            }
        }
        
        if (passwordChangeFound){
        	String password = "";
        	for (Operation operation : patch.getOperations()) {
                if (StringUtils.equalsIgnoreCase(operation.getPath(), "/password")) {
                	Object pass = operation.getValue();
                	if (pass instanceof JsonValueEvaluator) {
                		JsonNode jsonNode = ((JsonValueEvaluator) pass).getValueNode();
                		password = jsonNode.get("new_password").asText();
                	} else {
                		password = (String)operation.getValue();
                	}
                }
        	// We'll turn off authorisation system because this call isn't admin based as it's token based
        	boolean isPasswordReused = passwordHistoryService.isPasswordReused(context, eperson, password);
            if(!isPasswordReused) {
	            context.turnOffAuthorisationSystem();
	           
	        	es.setPassword(eperson, password);
	        	
	        	int passwordExpiryTime = DSpaceServicesFactory.getInstance().getConfigurationService()
	                    .getIntProperty("eperson.password.expiry.days");
	            		
	    		Calendar calendar = Calendar.getInstance();
	            calendar.setTime(new Date());
	            calendar.add(Calendar.DAY_OF_YEAR, passwordExpiryTime);
	            
	            eperson.setPasswordExpiryDate(calendar.getTime());
	            
	            passwordHistoryService.addPasswordHistory(context, eperson, password);
	        	
	        	es.update(context, eperson);
                context.restoreAuthSystemState();

                boolean isSelf = context.getCurrentUser() != null && context.getCurrentUser().equals(eperson);

                try {
                    if (!isSelf && passwordChangeFound) {
                        auditTrailService.logAction(context, eperson.getEmail(),
                                AuditAction.EPERSON_ADMIN_CHANGED_PASSWORD);
                    }else {
                        auditTrailService.logAction(context, eperson.getEmail(),
                                AuditAction.EPERSON_UPDATE_PROFILE,
                                eperson.getName());
                    }
                } catch (Exception e) {
                    log.error("Audit logging failed for eperson patch", e);
                }

                if (context.getCurrentUser() == null) {
                    context.setCurrentUser(eperson);
                }

            } else {
            	request.setAttribute("oldPasswordError", true);
            	log.warn("Old Password detected for the user");
            	throw new OldPasswordReusedException("Old Password Reused!");
            }
        }
        }
        patch.getOperations().removeIf(op -> StringUtils.equalsIgnoreCase(op.getPath(), "/password"));
        
        patchDSpaceObject(apiCategory, model, uuid, patch);
    }

    @Override
    protected void delete(Context context, UUID id) throws AuthorizeException {
        EPerson eperson = null;
        try {
            eperson = es.find(context, id);
            
            // Remove permanent delete 
            //es.delete(context, eperson);
            
            // just toggle canLogin
            eperson.setCanLogIn(!eperson.canLogIn());
            eperson.setUnsuccessfulAttempts(0);
            es.update(context, eperson);
            
            auditTrailService.logAction(context, eperson.getEmail(), AuditAction.EPERSON_ADMIN_DISABLED_LOGIN, eperson.getName());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (EmptyWorkflowGroupException e) {
            throw new RESTEmptyWorkflowGroupException(e);
        } catch (IllegalStateException e) {
            throw new UnprocessableEntityException(e.getMessage(), e);
        }
    }

    @Override
    public Class<EPersonRest> getDomainClass() {
        return EPersonRest.class;
    }

    /**
     * This method tries to merge the details coming from the {@link EPersonRegistrationRestController} of a given
     * {@code uuid} eperson. <br/>
     *
     * @param context - The Dspace Context
     * @param uuid - The uuid of the eperson
     * @param token - A valid registration token
     * @param override - An optional list of metadata fields that will be overwritten
     * @return a EPersonRest entity updated with the registration data.
     * @throws AuthorizeException
     */
    public EPersonRest mergeFromRegistrationData(
        Context context, UUID uuid, String token, List<String> override
    ) throws AuthorizeException {
        try {

            if (uuid == null) {
                throw new DSpaceBadRequestException("The uuid of the person cannot be null");
            }

            if (token == null) {
                throw new DSpaceBadRequestException("You must provide a token for the eperson");
            }

            return converter.toRest(
                accountService.mergeRegistration(context, uuid, token, override),
                utils.obtainProjection()
            );
        } catch (SQLException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(
                Link.of("/api/" + EPersonRest.CATEGORY + "/registrations", EPersonRest.NAME + "-registration")));
    }
    
    /*
     * Index - 
     * 0 : FirstName
     * 1 : LastName
     * 2 : password
     * 3 : email
     * 4 : can-login
     * 5 : super-admin
     * 6 : department
     * 7 : department/section (Hierarchy)
     * 8 : role 
     */
    
    public Map<Integer,String> importEperson(Context context, List<String[]> epersonList) throws Exception{
    	Map<Integer,String> errorMap = new HashMap<>();
    	for(int i=1;i < epersonList.size();i++) {// Skip the header(0) always
    		String[] epersonDetails = epersonList.get(i);
    		if(epersonDetails.length < 5 || checkIfEPersonAlreadyExist(context,epersonDetails[3])) { // email
    			if(epersonDetails.length < 5) {    				
    				errorMap.put(i, "Please Fill all mandatory fields");
    			}else {    				
    				errorMap.put(i, "User already exist with this email");
    			}
    			continue;
    		}
    		if(!EMAIL_PATTERN.matcher(epersonDetails[3]).matches()) {
    			errorMap.put(i, "Invalid Email format"); 
    			continue;
    		}
    		if(validatePasswordService.isPasswordValid(epersonDetails[4])) { // password
    			errorMap.put(i, "Password validation failed"); 
    			continue;
    		}
    		EPerson ep = createEperson(context, epersonDetails);
    		ep.setFirstName(context, epersonDetails[0]);
    		ep.setLastName(context, epersonDetails[1]);
    		
    		if(epersonDetails.length > 5 && !epersonDetails[5].isBlank() && Boolean.valueOf(epersonDetails[5])) {
    			addToAdminGRoup(context, ep);
    		}else if(epersonDetails.length > 6 && !epersonDetails[6].isBlank()) {
    			String status = addToDepartment(context,epersonDetails[6],ep); 
    			if(!status.equalsIgnoreCase("")) {    				
    				errorMap.put(i, status);
    			};
    		}else if(epersonDetails.length > 7 && !epersonDetails[7].isBlank() && epersonDetails.length > 8 && !epersonDetails[8].isBlank()) {
    			String status =  addMemberToSection(context, epersonDetails[7],ep,epersonDetails[8].split("\\|"));
    			if(!status.equalsIgnoreCase("")) {    				
    				errorMap.put(i, status);
    			};
    		}
    	}
    	
    	return errorMap;
    }
    
    private boolean checkIfEPersonAlreadyExist(Context context, String epersonEmail) {
    	try {
			EPerson eperson = epersonService.findByEmail(context, epersonEmail);
			if(eperson == null) {
				return false;
			}
		} catch (Exception e) {
			// TODO: handle exception			
		}
    	return true;
    }
    
    private String addMemberToSection(Context context, String depAndSection, EPerson ep,String[] permission) throws Exception {   
    	if(depAndSection.indexOf("|") == -1) {
    		return "Invalid Section department hierarchy format";
    	}
    	String[] depSection = depAndSection.split("\\|");
    	Department department = departmentService.findByName(context, depSection[0].toLowerCase()); // department name    	    	
    	List<Community> allCommunities = department.getCommunity().getSubcommunities();
    	Community requiredCommunity = null;
    	for(Community section : allCommunities) {
    		if(section.getName().equalsIgnoreCase(depSection[1])) { // section Name
    			requiredCommunity = section;
    			break;
    		}
    	}
    	if(requiredCommunity == null) {
    		return "No section found for with name: "+depSection[1]; // section name
    	}
    	for(String perm : permission) {
    		String communityGroupName = String.join("_", requiredCommunity.getName(),requiredCommunity.getID().toString(),perm.toUpperCase());
        	Group group = groupService.findByName(context, communityGroupName);    
        	if(group == null) {
        		continue;
        	}
        	groupService.addMember(context, group, ep);	
    	}	
    	
    	return "";
    }
    
    private String addToDepartment(Context context, String depName,EPerson ep) throws Exception {
    	Department department = departmentService.findByName(context, depName.toLowerCase());
    	if(department == null) {
    		return "No department found for name: "+depName ;
    	}
    	String departmentGroupName = String.join("_", department.getDepartmentName(),department.getCommunity().getID().toString(),GroupPermission.ADMIN.toString());
    	Group group = groupService.findByName(context, departmentGroupName);    	
    	if(group == null) {
    		return "No admin group for name: "+departmentGroupName;
    	}
    	groupService.addMember(context, group, ep);		
    	
    	return "";
    }
    	
    	
	private void addToAdminGRoup(Context context, EPerson eperson) throws Exception {
		Group adminGroup = groupService.findByName(context, Group.ADMIN);
		groupService.addMember(context, adminGroup, eperson);		
	}
	
	public EPerson createEperson(Context context , String[] epersonDetails) {
    	EPerson ep = null;
    	try {
    		EPersonRest epersonRest = new EPersonRest();
    		epersonRest.setPassword(epersonDetails[2]);
    		epersonRest.setEmail(epersonDetails[3]);
    		epersonRest.setCanLogIn(Boolean.valueOf(epersonDetails[4]));
    		
    		ep = createEPersonFromRestObject(context, epersonRest);
		} catch (Exception e) {
			// TODO: handle exception
		}
    	
    	return ep;
    }
}
