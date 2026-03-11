package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.converter.DepartmentHierarchyConverter;
import org.dspace.app.rest.model.DepartmentDTO;
import org.dspace.app.rest.model.DepartmentHierarchyDTO;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Department;
import org.dspace.content.service.DepartmentService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ArrayNode;

@RestController
@RequestMapping("/api/core/department")
public class DepartmentRestController {

	@Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private DepartmentHierarchyConverter departmentHierarchyConverter;
    
    @GetMapping
    public List<DepartmentHierarchyDTO> findAll(HttpServletRequest request) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        List<Department> departments = departmentService.findAllDepartment(context);
        return departmentHierarchyConverter.filterDepartments(context, departments);
    }

    @GetMapping("/{uuid}")
    public DepartmentHierarchyDTO findOne(@PathVariable String uuid, HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        Department department = departmentService.findbyUuid(context, uuid);
        return departmentHierarchyConverter.convert(context, department);
    }

    @GetMapping("allUser/{uuid}")
    public ResponseEntity<?> findAllEPerson(@PathVariable String uuid, HttpServletRequest request) throws SQLException {
    	Context context = ContextUtil.obtainContext(request);
    	return ResponseEntity.ok(departmentService.getEpersonsArrayNode(context, uuid));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DepartmentDTO deptRest, 
    						HttpServletRequest request) throws SQLException, AuthorizeException {
        try {
        	Context context = ContextUtil.obtainContext(request);
        	
        	Department department = departmentService.create(context, deptRest.getDepartmentName(), deptRest.getAdminGroupName(),
        			deptRest.getAbbreviation(), deptRest.getCommunityId());
        	DepartmentHierarchyDTO dto = departmentHierarchyConverter.convert(context, department);
        	context.complete();
        	return new ResponseEntity<>(dto, HttpStatus.CREATED);
			
		} catch (SQLException e) {
			if (e.getMessage().equals("DUPLICATE")) {
				return new ResponseEntity<>("Department name already exists", HttpStatus.CONFLICT);
			}
			throw e;
		}
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<String> delete(@PathVariable String uuid, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        try {			
        	Context context = ContextUtil.obtainContext(request);
        	Department dept = departmentService.findbyUuid(context, uuid);
        	if(dept != null) {
        		departmentService.delete(context, dept);
        	}
        	context.complete();
        	return new ResponseEntity<>("Department deleted successfully", HttpStatus.OK);
		} catch (RuntimeException e) {
			if (e.getMessage().equals("CANNOT-DELETE")) {
				return new ResponseEntity<>("Cannot delete ! department is not empty.", HttpStatus.CONFLICT);
			}
			throw e;
		}
    }
    
    @GetMapping("/findAllLeaf/{depCommId}")
    public ResponseEntity<ArrayNode> findAllLeaf(@PathVariable UUID depCommId, HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        ArrayNode response = departmentService.findAllLeaf(context, depCommId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    @PostMapping("/assignAdmin/{depId}")
    public ResponseEntity<String> assignAdmin(@PathVariable UUID depId, @RequestBody String userId, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        Context context = ContextUtil.obtainContext(request);
        departmentService.assignAdmin(context, depId, userId);
        context.complete();
        return new ResponseEntity<>("Admin assigned successfully", HttpStatus.OK);
    }
    
    @PatchMapping("/removeAdmin/{depId}")
    public ResponseEntity<String> removeAdmin(@PathVariable UUID depId, @RequestBody List<UUID> userIds, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
    	Context context = ContextUtil.obtainContext(request);
    	departmentService.removeAdmin(context, depId, userIds);
    	context.complete();
    	return new ResponseEntity<>("Admin removed successfully", HttpStatus.OK);
    }
    
    @GetMapping("/find-all-folder/{communityUuid}")
    public ResponseEntity<ArrayNode> findAllFolders(@PathVariable UUID communityUuid, @RequestParam(required = false) String query, HttpServletRequest request) throws SQLException, AuthorizeException, SearchServiceException {
    	Context context = ContextUtil.obtainContext(request);
    	ArrayNode result = departmentService.findAllFolders(context, communityUuid, query);
    	return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
