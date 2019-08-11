//package co.codewizards.cloudstore.local.persistence;
//
//import java.util.UUID;
//
//import javax.jdo.annotations.Column;
//import javax.jdo.annotations.Index;
//import javax.jdo.annotations.NullValue;
//import javax.jdo.annotations.PersistenceCapable;
//import javax.jdo.annotations.Persistent;
//import javax.jdo.annotations.Queries;
//import javax.jdo.annotations.Query;
//import javax.jdo.annotations.Unique;
//
//import co.codewizards.cloudstore.core.repo.transport.TransferDoneMarkerType;
//
//@PersistenceCapable
//@Unique(
//		name="TransferDoneMarker_fromRepositoryId_toRepositoryId_transferDoneMarkerType_fromEntityId",
//		members={"fromRepositoryId", "toRepositoryId", "transferDoneMarkerType", "fromEntityId"})
//@Index(
//		name="TransferDoneMarker_fromRepositoryId_toRepositoryId",
//		members={"fromRepositoryId", "toRepositoryId"})
//@Queries({
//	@Query(
//			name="getTransferDoneMarker_fromRepositoryId_toRepositoryId_transferDoneMarkerType_fromEntityId",
//			value="SELECT UNIQUE WHERE"
//					+ " this.fromRepositoryId == :fromRepositoryId"
//					+ " && this.toRepositoryId == :toRepositoryId"
//					+ " && this.transferDoneMarkerType == :transferDoneMarkerType"
//					+ " && this.fromEntityId == :fromEntityId"),
//	@Query(
//			name="getTransferDoneMarkers_fromRepositoryId_toRepositoryId",
//			value="SELECT WHERE this.fromRepositoryId == :fromRepositoryId && this.toRepositoryId == :toRepositoryId")
//})
//public class TransferDoneMarker extends Entity {
//
//	@Persistent(nullValue = NullValue.EXCEPTION)
//	private String fromRepositoryId;
//
//	@Persistent(nullValue = NullValue.EXCEPTION)
//	private String toRepositoryId;
//
//	@Persistent(nullValue = NullValue.EXCEPTION)
//	@Column(jdbcType="INTEGER")
//	private TransferDoneMarkerType transferDoneMarkerType;
//
//	private long fromEntityId;
//
//	private long fromLocalRevision;
//
//	public UUID getFromRepositoryId() {
//		return fromRepositoryId == null ? null : UUID.fromString(fromRepositoryId);
//	}
//	public void setFromRepositoryId(final UUID fromRepositoryId) {
//		this.fromRepositoryId = fromRepositoryId == null ? null : fromRepositoryId.toString();
//	}
//
//	public UUID getToRepositoryId() {
//		return toRepositoryId == null ? null : UUID.fromString(toRepositoryId);
//	}
//	public void setToRepositoryId(final UUID toRepositoryId) {
//		this.toRepositoryId = toRepositoryId == null ? null : toRepositoryId.toString();
//	}
//
//	public TransferDoneMarkerType getTransferDoneMarkerType() {
//		return transferDoneMarkerType;
//	}
//	public void setTransferDoneMarkerType(final TransferDoneMarkerType transferDoneMarkerType) {
//		this.transferDoneMarkerType = transferDoneMarkerType;
//	}
//
//	public long getFromEntityId() {
//		return fromEntityId;
//	}
//	public void setFromEntityId(final long fromEntityId) {
//		this.fromEntityId = fromEntityId;
//	}
//
//	public long getFromLocalRevision() {
//		return fromLocalRevision;
//	}
//	public void setFromLocalRevision(final long fromLocalRevision) {
//		this.fromLocalRevision = fromLocalRevision;
//	}
//
//}
