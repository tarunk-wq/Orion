package org.dspace.uploaddetails.dao.impl;

import java.util.Date;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

import org.dspace.eperson.EPerson;
import org.dspace.uploaddetails.UploadDetails;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(UploadDetails.class)
public abstract class UploadDetails_ {
	public static volatile SingularAttribute<UploadDetails, String> batchName;
	public static volatile SingularAttribute<UploadDetails, EPerson> submitter;
	public static volatile SingularAttribute<UploadDetails, String> uploadstatus;
	public static volatile SingularAttribute<UploadDetails, Date> time;

	public static final String BATCH_NAME = "batchName";
	public static final String SUBMITTER = "submitter";
	public static final String UPLOAD_STATUS ="uploadstatus";
	public static final String TIME="time";

}
