package co.codewizards.cloudstore.shared.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
@XmlRootElement
public class Error
implements Serializable
{
	private static final long serialVersionUID = 1L;

	private String className;
	private String message;
	private List<ErrorStackTraceElement> stackTraceElements;
	private Error cause;

	public Error() { }

	public Error(Throwable throwable) {
		if (throwable != null) {
			this.message = throwable.getMessage();
			this.className = throwable.getClass().getName();
		}
	}

	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	public List<ErrorStackTraceElement> getStackTraceElements() {
		if (stackTraceElements == null)
			stackTraceElements = new ArrayList<ErrorStackTraceElement>();

		return stackTraceElements;
	}
	public void setStackTraceElements(List<ErrorStackTraceElement> stackTraceElements) {
		this.stackTraceElements = stackTraceElements;
	}

	public Error getCause() {
		return cause;
	}
	public void setCause(Error cause) {
		this.cause = cause;
	}
}
