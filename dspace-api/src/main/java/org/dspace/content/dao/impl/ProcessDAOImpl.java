/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import static org.dspace.scripts.Process_.CREATION_TIME;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.ProcessStatus;
import org.dspace.content.dao.ProcessDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessQueryParameterContainer;
import org.dspace.scripts.Process_;

/**
 *
 * Implementation class for {@link ProcessDAO}
 */
public class ProcessDAOImpl extends AbstractHibernateDAO<Process> implements ProcessDAO {

    @Override
    public List<Process> findAllSortByScript(Context context) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        criteriaQuery.orderBy(criteriaBuilder.asc(processRoot.get(Process_.name)));

        return list(context, criteriaQuery, false, Process.class, -1, -1);

    }

    @Override
    public List<Process> findAllSortByStartTime(Context context) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        criteriaQuery.orderBy(criteriaBuilder.desc(processRoot.get(Process_.startTime)),
                              criteriaBuilder.desc(processRoot.get(Process_.processId)));

        return list(context, criteriaQuery, false, Process.class, -1, -1);
    }

    @Override
    public List<Process> findAll(Context context, int limit, int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        criteriaQuery.orderBy(criteriaBuilder.desc(processRoot.get(Process_.processId)));

        return list(context, criteriaQuery, false, Process.class, limit, offset);
    }

    @Override
    public int countRows(Context context) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(criteriaBuilder.count(processRoot));

        return count(context, criteriaQuery, criteriaBuilder, processRoot);

    }

    @Override
    public List<Process> search(Context context, ProcessQueryParameterContainer processQueryParameterContainer,
                                int limit, int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);

        handleProcessQueryParameters(processQueryParameterContainer, criteriaBuilder, criteriaQuery, processRoot);
        return list(context, criteriaQuery, false, Process.class, limit, offset);

    }

    @Override
    public List<Process> search(Context context, ProcessQueryParameterContainer processQueryParameterContainer,
            int limit, int offset, Date startDate, Date endDate) throws SQLException {
    	Map<String, Object> filters = processQueryParameterContainer.getQueryParameterMap();
    	Object scriptName = filters.get(Process_.NAME);
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
		CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
		Root<Process> processRoot = criteriaQuery.from(Process.class);
		criteriaQuery.select(processRoot);
		handleProcessQueryParameters(processQueryParameterContainer, criteriaBuilder, criteriaQuery, processRoot);
		
        if (startDate != null && endDate != null) {
        	List<Predicate> andPredicates = new ArrayList<>();
            andPredicates.add(criteriaBuilder.equal(processRoot.get(Process_.NAME), scriptName));
            andPredicates.add(criteriaBuilder.between(processRoot.get(Process_.CREATION_TIME), startDate, endDate));
            criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[0])));
        }
		return list(context, criteriaQuery, false, Process.class, limit, offset);
    }
    
    /**
     * This method will ensure that the params contained in the {@link ProcessQueryParameterContainer} are transferred
     * to the ProcessRoot and that the correct conditions apply to the query
     * @param processQueryParameterContainer    The object containing the conditions that need to be met
     * @param criteriaBuilder                   The criteriaBuilder to be used
     * @param criteriaQuery                     The criteriaQuery to be used
     * @param processRoot                       The processRoot to be used
     */
    private void handleProcessQueryParameters(ProcessQueryParameterContainer processQueryParameterContainer,
                                              CriteriaBuilder criteriaBuilder, CriteriaQuery criteriaQuery,
                                              Root<Process> processRoot) {
        addProcessQueryParameters(processQueryParameterContainer, criteriaBuilder, criteriaQuery, processRoot);
        if (StringUtils.equalsIgnoreCase(processQueryParameterContainer.getSortOrder(), "asc")) {
            criteriaQuery
                .orderBy(criteriaBuilder.asc(processRoot.get(processQueryParameterContainer.getSortProperty())));
        } else if (StringUtils.equalsIgnoreCase(processQueryParameterContainer.getSortOrder(), "desc")) {
            criteriaQuery
                .orderBy(criteriaBuilder.desc(processRoot.get(processQueryParameterContainer.getSortProperty())));
        }
    }

    /**
     * This method will apply the variables in the {@link ProcessQueryParameterContainer} as criteria for the
     * {@link Process} objects to the given CriteriaQuery.
     * They'll need to adhere to these variables in order to be eligible for return
     * @param processQueryParameterContainer    The object containing the variables for the {@link Process}
     *                                          to adhere to
     * @param criteriaBuilder                   The current CriteriaBuilder
     * @param criteriaQuery                     The current CriteriaQuery
     * @param processRoot                       The processRoot
     */
    private void addProcessQueryParameters(ProcessQueryParameterContainer processQueryParameterContainer,
                                           CriteriaBuilder criteriaBuilder, CriteriaQuery criteriaQuery,
                                           Root<Process> processRoot) {
        List<Predicate> andPredicates = new LinkedList<>();

        for (Map.Entry<String, Object> entry : processQueryParameterContainer.getQueryParameterMap().entrySet()) {
            andPredicates.add(criteriaBuilder.equal(processRoot.get(entry.getKey()), entry.getValue()));
        }
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[]{})));
    }

    @Override
    public int countTotalWithParameters(Context context, ProcessQueryParameterContainer processQueryParameterContainer)
        throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(criteriaBuilder.count(processRoot));

        addProcessQueryParameters(processQueryParameterContainer, criteriaBuilder, criteriaQuery, processRoot);
        return count(context, criteriaQuery, criteriaBuilder, processRoot);
    }
    
    @Override
    public int countTotalWithParameters(Context context, ProcessQueryParameterContainer processQueryParameterContainer, Date startDateObj, Date endDateObj)
            throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);

        addProcessQueryParameters(processQueryParameterContainer, criteriaBuilder, criteriaQuery, processRoot);
        if (startDateObj != null && endDateObj != null) {
        	List<Predicate> andPredicates = new LinkedList<>();
        	andPredicates.add(criteriaBuilder.between(processRoot.get(Process_.CREATION_TIME), startDateObj, endDateObj));
        	criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[]{})));
        }
        return count(context, criteriaQuery, criteriaBuilder, processRoot);
    }


    @Override
    public List<Process> findByStatusAndCreationTimeOlderThan(Context context, List<ProcessStatus> statuses,
        Instant date) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);

        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);

        Predicate creationTimeLessThanGivenDate = criteriaBuilder.lessThan(processRoot.get(CREATION_TIME), date);
        Predicate statusIn = processRoot.get(Process_.PROCESS_STATUS).in(statuses);
        criteriaQuery.where(criteriaBuilder.and(creationTimeLessThanGivenDate, statusIn));

        return list(context, criteriaQuery, false, Process.class, -1, -1);
    }

    @Override
    public List<Process> findByUser(Context context, EPerson user, int limit, int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);

        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        criteriaQuery.where(criteriaBuilder.equal(processRoot.get(Process_.E_PERSON), user));

        List<jakarta.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(processRoot.get(Process_.PROCESS_ID)));
        criteriaQuery.orderBy(orderList);

        return list(context, criteriaQuery, false, Process.class, limit, offset);
    }

    @Override
    public int countByUser(Context context, EPerson user) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);

        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(criteriaBuilder.count(processRoot));
        criteriaQuery.where(criteriaBuilder.equal(processRoot.get(Process_.E_PERSON), user));
        return count(context, criteriaQuery, criteriaBuilder, processRoot);
    }
    
    @Override
    public List<Process> findByBatchName(Context context, String batchName) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        Predicate batchNameEqual = criteriaBuilder.equal(processRoot.get(Process_.BATCH_NAME), batchName);
        criteriaQuery.where(batchNameEqual).orderBy(criteriaBuilder.desc(processRoot.get(Process_.CREATION_TIME)));

        return list(context, criteriaQuery, false, Process.class, -1, -1);

    }
    
    @Override
	public List<Process> findByBatchNameCompleted(Context context, String batchName) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        Predicate batchNameEqual = criteriaBuilder.equal(processRoot.get(Process_.BATCH_NAME), batchName);
        Predicate isCompletedEqual = criteriaBuilder.equal(processRoot.get(Process_.PROCESS_STATUS), ProcessStatus.COMPLETED);
        criteriaQuery.where(criteriaBuilder.and(batchNameEqual, isCompletedEqual)).orderBy(criteriaBuilder.desc(processRoot.get(Process_.CREATION_TIME)));

        return list(context, criteriaQuery, false, Process.class, -1, -1);
	}

	@Override
	public List<Process> findByBatchNameImportCompleted(Context context, String batchName) throws SQLException {
		CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        Predicate batchNameEqual = criteriaBuilder.equal(processRoot.get(Process_.BATCH_NAME), batchName);
        Predicate isimportEqual = criteriaBuilder.equal(processRoot.get(Process_.NAME), "import");
        Predicate isCompletedEqual = criteriaBuilder.equal(processRoot.get(Process_.PROCESS_STATUS), ProcessStatus.COMPLETED);
        criteriaQuery.where(criteriaBuilder.and(batchNameEqual, isimportEqual, isCompletedEqual)).orderBy(criteriaBuilder.desc(processRoot.get(Process_.CREATION_TIME)));

        return list(context, criteriaQuery, false, Process.class, -1, -1);
	}

	@Override
	public List<Process> findByStatusCollectionAndBatchName(Context context, String batchName, String scriptName,
			Collection collection, Date startDate, Date endDate, ProcessStatus status) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Process> criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);

        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);

        List<Predicate> predicates = new ArrayList<>();
        
        if (scriptName != null) {
            predicates.add(criteriaBuilder.equal(processRoot.get(Process_.NAME), scriptName));
        }

        if (batchName != null) {
            predicates.add(criteriaBuilder.equal(processRoot.get(Process_.BATCH_NAME), batchName));
        }
        
        if(status!=null)
        {
        	Predicate statusEquals = criteriaBuilder.equal(processRoot.get(Process_.PROCESS_STATUS), status);
        	predicates.add(statusEquals);
        }
        
//        if(collection != null) {
//        	predicates.add(criteriaBuilder.equal(processRoot.get(Process_.COLLECTION), collection));
//        }
        
        if (startDate != null && endDate != null) {
            predicates.add(criteriaBuilder.between(processRoot.get(Process_.CREATION_TIME), startDate, endDate));
        }
        
        criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(criteriaBuilder.desc(processRoot.get(Process_.CREATION_TIME)));
        return list(context, criteriaQuery, false, Process.class, -1, -1);
	}

}


