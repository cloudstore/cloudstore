package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.codewizards.cloudstore.core.dto.CopyModificationDto;
import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.ModificationDto;

public class ModificationDtoSet {

	private final Map<String, List<DeleteModificationDto>> path2DeleteModificationDtos;
	private final Map<String, List<CopyModificationDto>> fromPath2CopyModificationDtos;

	public ModificationDtoSet(Collection<ModificationDto> modificationDtos) {
		assertNotNull("modificationDtos", modificationDtos);
		path2DeleteModificationDtos = new HashMap<String, List<DeleteModificationDto>>();
		fromPath2CopyModificationDtos = new HashMap<String, List<CopyModificationDto>>();
		for (ModificationDto modificationDto : modificationDtos) {
			if (modificationDto instanceof CopyModificationDto) {
				CopyModificationDto copyModificationDto = (CopyModificationDto) modificationDto;
				String fromPath = copyModificationDto.getFromPath();
				List<CopyModificationDto> list = fromPath2CopyModificationDtos.get(fromPath);
				if (list == null) {
					list = new LinkedList<CopyModificationDto>();
					fromPath2CopyModificationDtos.put(fromPath, list);
				}
				list.add(copyModificationDto);
			}
			else if (modificationDto instanceof DeleteModificationDto) {
				DeleteModificationDto deleteModificationDto = (DeleteModificationDto) modificationDto;
				String path = deleteModificationDto.getPath();
				List<DeleteModificationDto> list = path2DeleteModificationDtos.get(path);
				if (list == null) {
					list = new LinkedList<DeleteModificationDto>();
					path2DeleteModificationDtos.put(path, list);
				}
				list.add(deleteModificationDto);
			}
			else
				throw new UnsupportedOperationException("Unknown ModificationDto type: " + modificationDto);
		}
	}

	public Map<String, List<CopyModificationDto>> getFromPath2CopyModificationDtos() {
		return fromPath2CopyModificationDtos;
	}

	public Map<String, List<DeleteModificationDto>> getPath2DeleteModificationDtos() {
		return path2DeleteModificationDtos;
	}

}
