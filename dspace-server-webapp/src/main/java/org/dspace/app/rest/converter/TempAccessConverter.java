package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.TempAccessDTO;
import org.dspace.tempaccess.TempAccess;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class TempAccessConverter extends DSpaceObjectConverter<TempAccess, TempAccessDTO> {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public TempAccessDTO convert(TempAccess entity) {
		TempAccessDTO dto = new TempAccessDTO();
		dto.setId(entity.getID().toString());
		dto.setItemUuid(entity.getItem() != null ? entity.getItem().getID() : null);
		dto.setEpersonUuid(entity.getEperson() != null ? entity.getEperson().getID() : null);
		dto.setDepartmentUuid(entity.getDepartmentUuid());
		dto.setEpersonName(entity.getEperson() != null ? entity.getEperson().getFullName() : null);

		if (entity.getStartDate() != null) {
			dto.setStartDate(entity.getStartDate().format(DATE_FORMATTER));
		}
		if (entity.getEndDate() != null) {
			dto.setEndDate(entity.getEndDate().format(DATE_FORMATTER));
		}

		return dto;
	}

	@Override
	public Class<TempAccess> getModelClass() {
		return TempAccess.class;
	}

	@Override
	protected TempAccessDTO newInstance() {
		return new TempAccessDTO();
	}
}
