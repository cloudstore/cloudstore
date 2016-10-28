package co.codewizards.cloudstore.ls.core.dto;

import java.io.Serializable;

import co.codewizards.cloudstore.core.Uid;

public interface InverseServiceResponse extends Serializable {

	public Uid getRequestId();

}
