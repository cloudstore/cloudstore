package co.codewizards.cloudstore.shared;

import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CloudStoreResponse
{
	private static final long serialVersionUID = 1L;

	private UUID uuid;

	private String revision;

	public CloudStoreResponse() { }

	public CloudStoreResponse(UUID uuid, String revision)
	{
		this.uuid = uuid;
		this.revision = revision;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
}
