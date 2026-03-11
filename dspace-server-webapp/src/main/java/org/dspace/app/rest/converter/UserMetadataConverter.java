package org.dspace.app.rest.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.rest.model.UserMetadataDto;
import org.dspace.app.rest.projection.Projection;
import org.dspace.usermetadata.UserMetadataFields;
import org.springframework.stereotype.Component;

@Component
public class UserMetadataConverter implements DSpaceConverter<UserMetadataFields, UserMetadataDto>{

	@Override
	public UserMetadataDto convert(UserMetadataFields modelObject, Projection projection) {
		UserMetadataDto userMetadataDto = new UserMetadataDto();
		userMetadataDto.setId(modelObject.getID());
		userMetadataDto.setType(modelObject.getFieldType());
		userMetadataDto.setName(modelObject.getUserFieldName());
		userMetadataDto.setValue(modelObject.getUserFieldValue());
		userMetadataDto.setSystemFieldName(modelObject.getSystemFieldName());
		userMetadataDto.setIsUniqueMetadata(modelObject.getIsUniqueMetadata());
		return userMetadataDto;
	}

	@Override
	public Class<UserMetadataFields> getModelClass() {
		// TODO Auto-generated method stub
		return UserMetadataFields.class;
	}
	
	public List<UserMetadataDto> convert(List<UserMetadataFields> modelObjects, Projection projection) {
        if (modelObjects == null) {
            return null;
        }
        return modelObjects.stream()
                .map(model -> convert(model, projection))
                .collect(Collectors.toList());
    }
	
}