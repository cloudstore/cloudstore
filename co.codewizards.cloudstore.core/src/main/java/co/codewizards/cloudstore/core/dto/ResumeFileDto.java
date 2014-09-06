package co.codewizards.cloudstore.core.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * DTO for files to resume (NormalFiles with state 'inProgress' true).
 * @author Sebastian Schefczyk
 */
@XmlRootElement
public class ResumeFileDto {

	private List<RepoFileDto> pathListDto;

	public void setPathList(final List<RepoFileDto> pathListDto) {
		this.pathListDto = pathListDto;
	}
	public List<RepoFileDto> getPathList() {
		return this.pathListDto;
	}

}
