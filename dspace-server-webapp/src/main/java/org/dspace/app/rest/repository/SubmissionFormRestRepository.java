/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.SubmissionFormRest;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage InputForm Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(SubmissionFormRest.CATEGORY + "." + SubmissionFormRest.PLURAL_NAME)
public class SubmissionFormRestRepository extends DSpaceRestRepository<SubmissionFormRest, String> {
    private Map<Locale, DCInputsReader> inputReaders;
    private DCInputsReader defaultInputReader;
    private CollectionService collectionService;
    private CommunityService communityService;
    
    public SubmissionFormRestRepository() throws DCInputsReaderException {
        defaultInputReader = new DCInputsReader();
        Locale[] locales = I18nUtil.getSupportedLocales();
        inputReaders = new HashMap<Locale,DCInputsReader>();
        for (Locale locale : locales) {
            inputReaders.put(locale, new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
        }
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public SubmissionFormRest findOne(Context context, String submitName)  {
        try {
            Locale currentLocale = context.getCurrentLocale();
            DCInputsReader inputReader = inputReaders.get(currentLocale);
            if (inputReader == null) {
                inputReader = defaultInputReader;
            }
            DCInputSet subConfs = inputReader.getInputsByFormName(submitName);
            if (subConfs == null) {
                return null;
            }
            return converter.toRest(subConfs, utils.obtainProjection());
        } catch (DCInputsReaderException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<SubmissionFormRest> findAll(Context context, Pageable pageable) {
        try {
            Locale currentLocale = context.getCurrentLocale();
            DCInputsReader inputReader;
            if (currentLocale != null) {
                inputReader = inputReaders.get(currentLocale);
            } else {
                inputReader = defaultInputReader;
            }
            long total = inputReader.countInputs();
            List<DCInputSet> subConfs = inputReader.getAllInputs(pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(subConfs, pageable, total, utils.obtainProjection());
        } catch (DCInputsReaderException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public Class<SubmissionFormRest> getDomainClass() {
        return SubmissionFormRest.class;
    }

    /**
     * Reload the current Submission Form configuration based on the currently
     * supported locales. This method can be used to force a reload if the
     * configured supported locales change.
     *
     * @throws DCInputsReaderException
     */
    public void reload() throws DCInputsReaderException {
        this.defaultInputReader = new DCInputsReader();
        Locale[] locales = I18nUtil.getSupportedLocales();
        this.inputReaders = new HashMap<Locale, DCInputsReader>();
        for (Locale locale : locales) {
            inputReaders.put(locale, new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
        }
    }
    
	@PreAuthorize("hasAuthority('AUTHENTICATED')")
	@SearchRestMethod(name = "byDspaceObject")
	public SubmissionFormRest findByDspaceObject(@Parameter(value = "uuid", required = true) UUID uuid) {
		Context context = obtainContext();
		try {
			collectionService = ContentServiceFactory.getInstance().getCollectionService();
			communityService = ContentServiceFactory.getInstance().getCommunityService();

			DSpaceObject dso = collectionService.find(context, uuid);
			if (dso == null)
				dso = communityService.find(context, uuid);

			if (dso == null) {
				return null;
			}

			Community section = (Community) getSecondTopLevelCommunity(context, dso);
			if (section == null) {
				return null;
			}

			Locale currentLocale = context.getCurrentLocale();
			DCInputsReader inputReader = inputReaders.get(currentLocale);
			if (inputReader == null) {
				inputReader = defaultInputReader;
			}
			
			DCInputSet subConfs = null;
			try {
				subConfs = inputReader.getInputsByFormName(context, section.getHandle());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (subConfs == null) {
				return null;
			}
			return converter.toRest(subConfs, utils.obtainProjection());

		} catch (SQLException | DCInputsReaderException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
    
    public DSpaceObject getSecondTopLevelCommunity(Context context, DSpaceObject dso) throws SQLException {
	    if (dso == null) return null;
	    if (dso instanceof Collection) {
	        return getSecondTopLevelCommunity(context, collectionService.getParentObject(context, (Collection) dso));
	    } else if (dso instanceof Community) {
	        DSpaceObject parent = communityService.getParentObject(context, (Community) dso);
	        if (parent == null) return null;
	        DSpaceObject grandParent = communityService.getParentObject(context, (Community) parent);
	        return (grandParent == null) ? dso : getSecondTopLevelCommunity(context, parent);
	    }
	    return null;
	}
}
