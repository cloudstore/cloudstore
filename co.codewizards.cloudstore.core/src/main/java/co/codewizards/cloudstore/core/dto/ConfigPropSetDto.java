package co.codewizards.cloudstore.core.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConfigPropSetDto {

	private List<ConfigPropDto> configPropDtos;

	public List<ConfigPropDto> getConfigPropDtos() {
		if (configPropDtos == null)
			configPropDtos = new ArrayList<>();

		return configPropDtos;
	}

	public void setConfigPropDtos(List<ConfigPropDto> configPropDtos) {
		this.configPropDtos = configPropDtos;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[configPropDtos=" + configPropDtos
				+ "]";
	}
}
