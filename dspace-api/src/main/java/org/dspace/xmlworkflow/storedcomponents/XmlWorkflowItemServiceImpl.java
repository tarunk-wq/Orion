/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchdetails.factory.BatchdetailsServiceFactory;
import org.dspace.batchdetails.service.BatchdetailsService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.storedcomponents.dao.XmlWorkflowItemDAO;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the XmlWorkflowItem object.
 * This class is responsible for all business logic calls for the XmlWorkflowItem object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class XmlWorkflowItemServiceImpl implements XmlWorkflowItemService {

    @Autowired(required = true)
    protected XmlWorkflowItemDAO xmlWorkflowItemDAO;


    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired(required = true)
    protected PoolTaskService poolTaskService;
    @Autowired(required = true)
    protected WorkflowRequirementsService workflowRequirementsService;
    @Autowired(required = true)
    protected WorkflowItemRoleService workflowItemRoleService;

    /*
     * The current step in the workflow system in which this workflow item is present
     */
    private Logger log = org.apache.logging.log4j.LogManager.getLogger(XmlWorkflowItemServiceImpl.class);

    private BatchdetailsService batchdetailsService = BatchdetailsServiceFactory.getInstance().getBatchdetailsService();

    protected XmlWorkflowItemServiceImpl() {

    }

    @Override
    public XmlWorkflowItem create(Context context, Item item, DSpaceObject parent)
        throws SQLException, AuthorizeException {
        XmlWorkflowItem xmlWorkflowItem = xmlWorkflowItemDAO.create(context, new XmlWorkflowItem());
        UUID[] a= new UUID[1];
        a[0]=item.getID();
        xmlWorkflowItem.setItem_ids(a);
        xmlWorkflowItem.setItem(item);
        xmlWorkflowItem.setParent(parent);
        return xmlWorkflowItem;
    }

    @Override
	public XmlWorkflowItem create(Context context, String batchname) throws SQLException, AuthorizeException {
		XmlWorkflowItem xmlWorkflowItem = null;
		Batchdetails batchdetails = null;
		List<Batchdetails> batchList = batchdetailsService.findAllByBatchName(context, batchname);
		if (batchList != null && !batchList.isEmpty()) {
			batchdetails = batchList.get(0);
			if (batchdetails.getItemIds() != null && batchdetails.getItemIds().length > 0) {
				xmlWorkflowItem = xmlWorkflowItemDAO.create(context, new XmlWorkflowItem());
				xmlWorkflowItem.setItem_ids(batchdetails.getItemIds());
				if(batchdetails.getItemIds()[0] != null) {
					Item item = itemService.find(context, batchdetails.getItemIds()[0]);
					xmlWorkflowItem.setItem(item);
				}
			}
		}
		return xmlWorkflowItem;
	}
    
    @Override
    public XmlWorkflowItem find(Context context, int id) throws SQLException {
        XmlWorkflowItem workflowItem = xmlWorkflowItemDAO.findByID(context, XmlWorkflowItem.class, id);

        if (workflowItem == null) {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_workflow_item",
                                               "not_found,workflowitem_id=" + id));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(LogHelper.getHeader(context, "find_workflow_item",
                                               "workflowitem_id=" + id));
            }
        }
        return workflowItem;
    }

    @Override
    public List<XmlWorkflowItem> findAll(Context context) throws SQLException {
        return xmlWorkflowItemDAO.findAll(context, XmlWorkflowItem.class);
    }

    @Override
    public List<XmlWorkflowItem> findAll(Context context, Integer page, Integer pagesize) throws SQLException {
        return findAllInCollection(context, page, pagesize, null);
    }

    @Override
    public List<XmlWorkflowItem> findAllInCollection(Context context, Integer page, Integer pagesize,
                                                     Collection collection) throws SQLException {
        Integer offset = null;
        if (page != null && pagesize != null) {
            offset = page * pagesize;
        }
        return xmlWorkflowItemDAO.findAllInCollection(context, offset, pagesize, collection);
    }

    @Override
    public int countAll(Context context) throws SQLException {
        return xmlWorkflowItemDAO.countAll(context);
    }

    @Override
    public int countAllInCollection(Context context, Collection collection) throws SQLException {
        return xmlWorkflowItemDAO.countAllInCollection(context, collection);
    }

    @Override
    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep) throws SQLException {
        return xmlWorkflowItemDAO.findBySubmitter(context, ep);
    }

    @Override
    public List<XmlWorkflowItem> findBySubmitter(Context context, EPerson ep, Integer pageNumber, Integer pageSize)
            throws SQLException {
        Integer offset = null;
        if (pageNumber != null && pageSize != null) {
            offset = pageNumber * pageSize;
        }
        return xmlWorkflowItemDAO.findBySubmitter(context, ep, pageNumber, pageSize);
    }

    @Override
    public int countBySubmitter(Context context, EPerson ep) throws SQLException {
        return xmlWorkflowItemDAO.countBySubmitter(context, ep);
    }

    @Override
    public void deleteByCollection(Context context, Collection collection)
        throws SQLException, IOException, AuthorizeException {
        List<XmlWorkflowItem> xmlWorkflowItems = findByCollection(context, collection);
        Iterator<XmlWorkflowItem> iterator = xmlWorkflowItems.iterator();
        while (iterator.hasNext()) {
            XmlWorkflowItem workflowItem = iterator.next();
            iterator.remove();
            delete(context, workflowItem);
        }
    }
    
    @Override
    public void deleteByCommunity(Context context, Community community)
        throws SQLException, IOException, AuthorizeException {
        List<XmlWorkflowItem> xmlWorkflowItems = findByCommunity(context, community);
        Iterator<XmlWorkflowItem> iterator = xmlWorkflowItems.iterator();
        while (iterator.hasNext()) {
            XmlWorkflowItem workflowItem = iterator.next();
            iterator.remove();
            delete(context, workflowItem);
        }
    }

    @Override
    public void delete(Context context, XmlWorkflowItem workflowItem)
        throws SQLException, AuthorizeException, IOException {
        UUID[] item_uuids = workflowItem.getItem_ids();
        // Need to delete the workspaceitem row first since it refers
        // to item ID
        deleteWrapper(context, workflowItem);

        // Delete item
        for(UUID uuid : item_uuids) {
        	Item item = itemService.find(context, uuid);
        	itemService.delete(context, item);
        }
    }

    @Override
    public List<XmlWorkflowItem> findByCollection(Context context, Collection collection) throws SQLException {
        return xmlWorkflowItemDAO.findByCollection(context, collection);
    }
    
    @Override
    public List<XmlWorkflowItem> findByCommunity(Context context, Community community) throws SQLException {
        return xmlWorkflowItemDAO.findByCommunity(context, community);
    }
    @Override
    public XmlWorkflowItem findByItem(Context context, Item item) throws SQLException {
        return xmlWorkflowItemDAO.findByItem(context, item);
    }

    @Override
    public void update(Context context, XmlWorkflowItem workflowItem) throws SQLException, AuthorizeException {
        // FIXME check auth
        log.info(LogHelper.getHeader(context, "update_workflow_item",
                                      "workflowitem_id=" + workflowItem.getID()));

        // Update the item
        itemService.update(context, workflowItem.getItem());

        xmlWorkflowItemDAO.save(context, workflowItem);
    }
    
    @Override
   	public void updateBatch(Context context, XmlWorkflowItem workflowItem) throws SQLException, AuthorizeException {
   		// FIXME check auth
   		log.info(LogHelper.getHeader(context, "update_workflow_item", "workflowitem_id=" + workflowItem.getID()));

   		UUID[] itemIDs = workflowItem.getItem_ids();
   		if (itemIDs != null && itemIDs.length > 0) {
   			for (UUID uuid : itemIDs) {
   				log.info("Item UUID from WorkflowItem" + uuid);
   				if (uuid != null) {
   					Item item = itemService.find(context, uuid);
   					if (item != null) {
   						itemService.update(context, item);
   					}
   				}
   			}
   		} else {
   			log.info("No item found for update in batchUpdate ", "workflowitem_id" + workflowItem.getID());
   		}
   		xmlWorkflowItemDAO.save(context, workflowItem);
   	}

    @Override
    public void deleteWrapper(Context context, XmlWorkflowItem workflowItem) throws SQLException, AuthorizeException {
    	XmlWorkflowItem managed = find(context, workflowItem.getID());
        List<WorkflowItemRole> roles = workflowItemRoleService.findByWorkflowItem(context, managed);
        Iterator<WorkflowItemRole> workflowItemRoleIterator = roles.iterator();
        while (workflowItemRoleIterator.hasNext()) {
            WorkflowItemRole workflowItemRole = workflowItemRoleIterator.next();
            workflowItemRoleIterator.remove();
            workflowItemRoleService.delete(context, workflowItemRole);
        }

        poolTaskService.deleteByWorkflowItem(context, managed);
        workflowRequirementsService.clearInProgressUsers(context, managed);
        claimedTaskService.deleteByWorkflowItem(context, managed);

        Batchdetails batchdetails = batchdetailsService.findByWorkFlowId(context, managed);
        if (batchdetails != null) {      	
        	batchdetails.setWorkflowId(null);
        	batchdetailsService.update(context, batchdetails);
        }
        // FIXME - auth?
        xmlWorkflowItemDAO.delete(context, managed);
    }


    @Override
    public void move(Context context, XmlWorkflowItem inProgressSubmission, Collection fromCollection,
                     Collection toCollection) {
        // TODO not implemented yet
    }
}
