package co.codewizards.cloudstore.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConfigPropSetDto {

	private List<ConfigPropDto> configPropDtos;

	public ConfigPropSetDto() {
	}

	public ConfigPropSetDto(final Properties properties) {
		assertNotNull(properties, "properties");
		configPropDtos = new ArrayList<>(properties.size());
		for (final Map.Entry<Object, Object> me : properties.entrySet()) {
			final ConfigPropDto configPropDto = new ConfigPropDto();
			configPropDto.setKey((String) me.getKey());
			configPropDto.setValue((String) me.getValue());
			configPropDtos.add(configPropDto);
		}
	}

	public List<ConfigPropDto> getConfigPropDtos() {
		if (configPropDtos == null)
			configPropDtos = new ArrayList<>();

		return configPropDtos;
	}

	public void setConfigPropDtos(List<ConfigPropDto> configPropDtos) {
		this.configPropDtos = configPropDtos;
	}

	public Properties toProperties() {
		final Properties properties = new Properties();
		for (final ConfigPropDto configPropDto : getConfigPropDtos())
			properties.setProperty(configPropDto.getKey(), configPropDto.getValue());

		return properties;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[configPropDtos=" + configPropDtos
				+ "]";
	}
}
