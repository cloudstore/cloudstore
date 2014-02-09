package co.codewizards.cloudstore.core.repo.sync;

import static co.codewizards.cloudstore.core.util.Util.assertNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.codewizards.cloudstore.core.dto.CopyModificationDTO;
import co.codewizards.cloudstore.core.dto.DeleteModificationDTO;
import co.codewizards.cloudstore.core.dto.ModificationDTO;

public class ModificationDTOSet {

	private final Map<String, List<DeleteModificationDTO>> path2DeleteModificationDTOs;
	private final Map<String, List<CopyModificationDTO>> fromPath2CopyModificationDTOs;

	public ModificationDTOSet(Collection<ModificationDTO> modificationDTOs) {
		assertNotNull("modificationDTOs", modificationDTOs);
		path2DeleteModificationDTOs = new HashMap<String, List<DeleteModificationDTO>>();
		fromPath2CopyModificationDTOs = new HashMap<String, List<CopyModificationDTO>>();
		for (ModificationDTO modificationDTO : modificationDTOs) {
			if (modificationDTO instanceof CopyModificationDTO) {
				CopyModificationDTO copyModificationDTO = (CopyModificationDTO) modificationDTO;
				String fromPath = copyModificationDTO.getFromPath();
				List<CopyModificationDTO> list = fromPath2CopyModificationDTOs.get(fromPath);
				if (list == null) {
					list = new LinkedList<CopyModificationDTO>();
					fromPath2CopyModificationDTOs.put(fromPath, list);
				}
				list.add(copyModificationDTO);
			}
			else if (modificationDTO instanceof DeleteModificationDTO) {
				DeleteModificationDTO deleteModificationDTO = (DeleteModificationDTO) modificationDTO;
				String path = deleteModificationDTO.getPath();
				List<DeleteModificationDTO> list = path2DeleteModificationDTOs.get(path);
				if (list == null) {
					list = new LinkedList<DeleteModificationDTO>();
					path2DeleteModificationDTOs.put(path, list);
				}
				list.add(deleteModificationDTO);
			}
			else
				throw new UnsupportedOperationException("Unknown ModificationDTO type: " + modificationDTO);
		}
	}

	public Map<String, List<CopyModificationDTO>> getFromPath2CopyModificationDTOs() {
		return fromPath2CopyModificationDTOs;
	}

	public Map<String, List<DeleteModificationDTO>> getPath2DeleteModificationDTOs() {
		return path2DeleteModificationDTOs;
	}

}
