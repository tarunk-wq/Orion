package org.dspace.batchhistory.dao.impl;

import java.util.Date;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import org.dspace.batchhistory.BatchHistory;


@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(BatchHistory.class)
public class BatchHistory_ {

	public static volatile SingularAttribute<BatchHistory, String> batchName;
	public static volatile SingularAttribute<BatchHistory, Date> time;
	public static volatile SingularAttribute<BatchHistory, String> toState;

	public static final String BATCH_NAME = "batchName";
	public static final String TIME = "time";
    public static final String TO_STATE="toState";
}
