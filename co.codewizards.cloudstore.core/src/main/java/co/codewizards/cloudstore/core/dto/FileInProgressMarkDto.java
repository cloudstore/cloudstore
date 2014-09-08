package co.codewizards.cloudstore.core.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * DTO for a file left in progress, which needs to be resumed upon next sync.
 * @author Sebastian Schefczyk
 */
@XmlRootElement
public class FileInProgressMarkDto {

	private List<RepoFileDto> pathList;

	public List<RepoFileDto> getPathList() {
		return pathList;
	}
	public void setPathList(final List<RepoFileDto> pathLists) {
		this.pathList = pathLists;
	}

}
