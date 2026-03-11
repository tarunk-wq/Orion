package org.dspace.batchdetails.dao.impl;


import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

import java.util.UUID;

import org.dspace.batchdetails.Batchdetails;
import org.dspace.batchhistory.BatchHistory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Batchdetails.class)
public abstract class Batchdetails_ {
	public static volatile SingularAttribute<Batchdetails, String> batchName;
	public static volatile SingularAttribute<Batchdetails, XmlWorkflowItem> workflowId;
	public static volatile SingularAttribute<Batchdetails, BatchHistory> historyid;
	public static volatile SingularAttribute<Batchdetails, String> state;
	public static volatile SingularAttribute<Batchdetails, UUID> id;

	public static final String BATCH_NAME = "batchName";
	public static final String WORKFLOW_ID = "workflowId";
	public static final String HISTORY_ID = "historyid";
	public static final String STATE = "state";
	public static final String ID = "id";

}
