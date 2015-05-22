package co.codewizards.cloudstore.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil;
import co.codewizards.cloudstore.local.persistence.DeleteModification;

public class DeleteModificationDtoConverter {

	protected DeleteModificationDtoConverter() {
	}

	public static DeleteModificationDtoConverter create() {
		return ObjectFactoryUtil.createObject(DeleteModificationDtoConverter.class);
	}

	public DeleteModificationDto toDeleteModificationDto(DeleteModification deleteModification) {
		final DeleteModificationDto dto = createObject(DeleteModificationDto.class);
		dto.setId(deleteModification.getId());
		dto.setLocalRevision(deleteModification.getLocalRevision());

		// *Warning* This path is overwritten with the *unprefixed* path, before being sent to the server.
		dto.setPath(deleteModification.getPath());
		return dto;
	}
}
