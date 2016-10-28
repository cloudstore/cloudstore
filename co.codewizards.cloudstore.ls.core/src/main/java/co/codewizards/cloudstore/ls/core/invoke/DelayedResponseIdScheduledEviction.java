package co.codewizards.cloudstore.ls.core.invoke;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import co.codewizards.cloudstore.core.Uid;

public class DelayedResponseIdScheduledEviction implements Comparable<DelayedResponseIdScheduledEviction> {

	private final long scheduledEvictionTimestamp;
	private final Uid delayedResponseId;

	public DelayedResponseIdScheduledEviction(final long scheduledEvictionTimestamp, final Uid delayedResponseId) {
		this.scheduledEvictionTimestamp = scheduledEvictionTimestamp;
		this.delayedResponseId = assertNotNull("delayedResponseId", delayedResponseId);
	}

	public Uid getDelayedResponseId() {
		return delayedResponseId;
	}

	public long getScheduledEvictionTimestamp() {
		return scheduledEvictionTimestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (scheduledEvictionTimestamp ^ (scheduledEvictionTimestamp >>> 32));
		result = prime * result + ((delayedResponseId == null) ? 0 : delayedResponseId.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DelayedResponseIdScheduledEviction other = (DelayedResponseIdScheduledEviction) obj;
		return equal(this.scheduledEvictionTimestamp, other.scheduledEvictionTimestamp)
				&& equal(this.delayedResponseId, other.delayedResponseId);
	}

	@Override
	public int compareTo(final DelayedResponseIdScheduledEviction other) {
		int res = Long.compare(this.scheduledEvictionTimestamp, other.scheduledEvictionTimestamp);
		if (res != 0)
			return res;

		res = this.delayedResponseId.compareTo(other.delayedResponseId);
		return res;
	}
}
