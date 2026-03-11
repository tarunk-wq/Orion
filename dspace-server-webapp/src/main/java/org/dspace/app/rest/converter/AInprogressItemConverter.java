/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.AInprogressSubmissionRest;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.submit.DataProcessingStep;
import org.dspace.app.rest.submit.RestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.step.LicenseStep;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.service.SubmissionConfigService;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Abstract implementation providing the common functionalities for all the inprogressSubmission Converter
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <T>
 *            the DSpace API inprogressSubmission object
 * @param <R>
 *            the DSpace REST inprogressSubmission representation
 */
public abstract class AInprogressItemConverter<T extends InProgressSubmission,
                            R extends AInprogressSubmissionRest>
        implements IndexableObjectConverter<T, R> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AInprogressItemConverter.class);

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Autowired
    private SubmissionSectionConverter submissionSectionConverter;

    protected SubmissionConfigService submissionConfigService;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    private WorkspaceItemService wiService;
    
    @Autowired
   	WorkflowItemService<XmlWorkflowItem> wfService;
       
    @Autowired
    private ItemService itemService;
    
    public AInprogressItemConverter() throws SubmissionConfigReaderException {
        submissionConfigService = SubmissionServiceFactory.getInstance().getSubmissionConfigService();
    }

    protected void fillFromModel(T obj, R witem, Projection projection) {
    	Context context = ContextUtil.obtainCurrentRequestContext();
    	if(obj == null) {
   		 return;
    	}
    	Item item = null;
    	try {
    		if (obj instanceof WorkspaceItem) {
    			obj = (T) wiService.find(context, obj.getID());
	    		item = obj.getItem();
			} else if (obj instanceof XmlWorkflowItem) {
				obj = (T) wfService.find(context, obj.getID());
				UUID[] itemIDs = ((XmlWorkflowItem) obj).getItem_ids();
				if (itemIDs != null && itemIDs.length > 0) {
					item = itemService.find(context, itemIDs[0]);
				}
			} else {
	    		item = obj.getItem();
				log.info("object is not re-initialized using  any service (WorkspaceItemService / WorkflowItemService<XmlWorkflowItem>)");
			}
		} catch (Exception e) {
			log.error("Error intializing dspace object ", e);
		}
        Collection collection = obj.getCollection();
        Community community = obj.getCommunity();
        EPerson submitter = null;
        submitter = obj.getSubmitter();

        witem.setId(obj.getID());
        witem.setCollection(collection != null ? converter.toRest(collection, projection) : null);
        witem.setCommunity(community != null ? converter.toRest(community, projection) : null);

        if(item != null) {
        	witem.setItem(converter.toRest(item, projection));        
        }
        if (submitter != null) {
            witem.setSubmitter(converter.toRest(submitter, projection));
        }
        
        // 1. retrieve the submission definition
        // 2. iterate over the submission section to allow to plugin additional
        // info

        if (obj.getParent() != null) {
            SubmissionDefinitionRest def = converter.toRest(
                    submissionConfigService.getSubmissionConfigByCollection(obj.getParent().getHandle()), projection);
            witem.setSubmissionDefinition(def);
            for (SubmissionSectionRest sections : def.getPanels()) {
                SubmissionStepConfig stepConfig = submissionSectionConverter.toModel(sections);

                if (stepConfig.isHiddenForInProgressSubmission(obj)) {
                    continue;
                }

                /*
                 * First, load the step processing class (using the current
                 * class loader)
                 */
                ClassLoader loader = this.getClass().getClassLoader();
                Class stepClass;
                try {
                    stepClass = loader.loadClass(stepConfig.getProcessingClassName());

                    Object stepInstance = stepClass.newInstance();

                    if (stepInstance instanceof DataProcessingStep) {
                    	// skip license process
                    	if (stepInstance instanceof LicenseStep) {
                    		continue;
                    	}
                        // load the interface for this step
                        DataProcessingStep stepProcessing =
                            (DataProcessingStep) stepClass.newInstance();
                        for (ErrorRest error : stepProcessing.validate(submissionService, obj, stepConfig)) {
                            addError(witem.getErrors(), error);
                        }   
                        witem.getSections()
                            .put(sections.getId(), stepProcessing.getData(submissionService, obj, stepConfig));
                    } else if (!(stepInstance instanceof RestProcessingStep)) {
                        log.warn("The submission step class specified by '" + stepConfig.getProcessingClassName() +
                                 "' does not implement the interface org.dspace.app.rest.submit.RestProcessingStep!" +
                                 " Therefore it cannot be used by the Configurable Submission as the " +
                                 "<processing-class>!");
                    }

                } catch (Exception e) {
                    log.error("An error occurred during the unmarshal of the data for the section " + sections.getId()
                            + " - reported error: " + e.getMessage(), e);
                }

            }
        }
    }

    private void addError(List<ErrorRest> errors, ErrorRest toAdd) {

        boolean found = false;
        String i18nKey = toAdd.getMessage();
        if (StringUtils.isNotBlank(i18nKey)) {
            for (ErrorRest error : errors) {
                if (i18nKey.equals(error.getMessage())) {
                    error.getPaths().addAll(toAdd.getPaths());
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            errors.add(toAdd);
        }
    }

}
