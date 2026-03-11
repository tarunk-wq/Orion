package org.dspace.app.rest.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class UserService {
	
	@Autowired(required = false)
	private ObjectMapper mapper;
	
	@Autowired
	private GroupService groupService;
	
    public ArrayNode getFilteredUser(Context context, List<EPerson> users){
    	ArrayNode nodes = mapper.createArrayNode();
    	try {			    		
    		Group adminGroup = groupService.findByName(context, Group.ADMIN);
    		Set<EPerson> adminUsers = new HashSet<>(adminGroup.getMembers());
        	EPerson currentUser = context.getCurrentUser();  
        	adminUsers.add(currentUser);
        	users.removeAll(adminUsers);
        	for(EPerson user : users) {
        		ObjectNode node = mapper.createObjectNode();
        		node.put("id", user.getID().toString());
        		node.put("name", user.getFullName());
        		nodes.add(node);
        	}
		} catch (Exception e) {
			// TODO: handle exception
			throw new RuntimeException(e.getMessage(), e);
		}
    	
    	return nodes;
    }

}
