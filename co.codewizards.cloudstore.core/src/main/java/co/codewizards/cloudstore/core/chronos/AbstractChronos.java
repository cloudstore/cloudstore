package co.codewizards.cloudstore.core.chronos;

import java.util.Date;

public abstract class AbstractChronos implements Chronos {

	@Override
	public Date nowAsDate() {
		return new Date(nowAsMillis());
	}
}
