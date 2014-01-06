package co.codewizards.cloudstore.rest.client.ssl;

public class CheckServerTrustedCertificateExceptionResult {

	private boolean trusted;
	private boolean permanent = true;

	public CheckServerTrustedCertificateExceptionResult() { }

	public boolean isTrusted() {
		return trusted;
	}
	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}
	public CheckServerTrustedCertificateExceptionResult trusted(boolean trusted) {
		setTrusted(trusted);
		return this;
	}

	public boolean isPermanent() {
		return permanent;
	}
	public void setPermanent(boolean permanent) {
		this.permanent = permanent;
	}
	public CheckServerTrustedCertificateExceptionResult permanent(boolean permanent) {
		setPermanent(permanent);
		return this;
	}
}
