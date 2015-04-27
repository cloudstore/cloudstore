package co.codewizards.cloudstore.ls.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TestDto {

	private String bla;

	public String getBla() {
		return bla;
	}
	public void setBla(String bla) {
		this.bla = bla;
	}

}
