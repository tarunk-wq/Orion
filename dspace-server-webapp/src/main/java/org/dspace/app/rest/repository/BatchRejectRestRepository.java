package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.BatchRejectRest;
import org.dspace.batchreject.BatchReject;
import org.dspace.batchreject.service.BatchRejectService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component(BatchRejectRest.CATEGORY + "." + BatchRejectRest.NAME)
public class BatchRejectRestRepository extends DSpaceObjectRestRepository<BatchReject, BatchRejectRest> {
	private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(BatchRejectRestRepository.class);
	
	@Autowired
	BatchRejectService batchrejectService;
	
	BatchRejectRestRepository(DSpaceObjectService<BatchReject> dsoService) {
		super(dsoService);
	}
	@Override
	public BatchRejectRest findOne(Context context, UUID id) {
		BatchReject batchrejects;
		try {
			batchrejects=batchrejectService.find(context, id);
		} catch(SQLException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
		return converter.toRest(batchrejects, utils.obtainProjection());
	}
	
	@Override
	public Page<BatchRejectRest> findAll(Context context, Pageable pageable) {
		try {
			int total=batchrejectService.countRows(context);
			List<BatchReject> batchrejects=batchrejectService.findAll(context,pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
			return converter.toRestPage(batchrejects, pageable, total,utils.obtainProjection());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
	
	@Override
	public Class<BatchRejectRest> getDomainClass() {
		return BatchRejectRest.class;
	}
	 

}
