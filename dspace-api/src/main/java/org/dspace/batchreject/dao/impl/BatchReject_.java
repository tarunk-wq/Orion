package org.dspace.batchreject.dao.impl;

import java.util.Date;
import java.util.UUID;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

import org.dspace.batchreject.BatchReject;
import org.dspace.eperson.EPerson;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(BatchReject.class)
public abstract class BatchReject_{

	public static volatile SingularAttribute<BatchReject, String> batchName;
	public static volatile SingularAttribute<BatchReject, String> reason;
	public static volatile SingularAttribute<BatchReject, UUID[]> itemIds;
	public static volatile SingularAttribute<BatchReject, Date> time;
	public static volatile SingularAttribute<BatchReject, EPerson> user;

	public static final String BATCH_NAME = "batchName";
	public static final String REASON = "reason";
	public static final String ITEM_IDS = "itemIds";
	public static final String TIME = "time";
	public static final String USER = "user";

}

