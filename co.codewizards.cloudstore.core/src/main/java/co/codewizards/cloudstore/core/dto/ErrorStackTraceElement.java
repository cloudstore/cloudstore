package co.codewizards.cloudstore.core.dto;

import java.io.Serializable;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class ErrorStackTraceElement
implements Serializable
{
	private static final long serialVersionUID = 2L;

	private String className;
	private String fileName;
	private int lineNumber;
	private String methodName;

	public ErrorStackTraceElement() { }

	public ErrorStackTraceElement(StackTraceElement stackTraceElement)
	{
		if (stackTraceElement != null) {
			this.className = stackTraceElement.getClassName();
			this.fileName = stackTraceElement.getFileName();
			this.lineNumber = stackTraceElement.getLineNumber();
			this.methodName = stackTraceElement.getMethodName();
		}
	}

	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public int getLineNumber() {
		return lineNumber;
	}
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
}
