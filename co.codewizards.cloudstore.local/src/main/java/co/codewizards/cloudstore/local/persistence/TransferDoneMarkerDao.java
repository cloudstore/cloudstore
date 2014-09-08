package co.codewizards.cloudstore.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.transport.TransferDoneMarkerType;
import co.codewizards.cloudstore.core.util.AssertUtil;

public class TransferDoneMarkerDao extends Dao<TransferDoneMarker, TransferDoneMarkerDao> {
	private static final Logger logger = LoggerFactory.getLogger(TransferDoneMarkerDao.class);

	public TransferDoneMarker getTransferDoneMarker(final UUID fromRepositoryId, final UUID toRepositoryId, final TransferDoneMarkerType transferDoneMarkerType, final long fromEntityId) {
		AssertUtil.assertNotNull("fromRepositoryId", fromRepositoryId);
		AssertUtil.assertNotNull("toRepositoryId", toRepositoryId);
		AssertUtil.assertNotNull("transferDoneMarkerType", transferDoneMarkerType);
		final Query query = pm().newNamedQuery(getEntityClass(), "getTransferDoneMarker_fromRepositoryId_toRepositoryId_transferDoneMarkerType_fromEntityId");
		try {
			final Map<String, Object> m = new HashMap<String, Object>(4);
			m.put("fromRepositoryId", fromRepositoryId.toString());
			m.put("toRepositoryId", toRepositoryId.toString());
			m.put("transferDoneMarkerType", transferDoneMarkerType);
			m.put("fromEntityId", fromEntityId);
			final TransferDoneMarker result = (TransferDoneMarker) query.executeWithMap(m);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<TransferDoneMarker> getRepoFileTransferDones(final UUID fromRepositoryId, final UUID toRepositoryId) {
		AssertUtil.assertNotNull("fromRepositoryId", fromRepositoryId);
		AssertUtil.assertNotNull("toRepositoryId", toRepositoryId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getTransferDoneMarkers_fromRepositoryId_toRepositoryId");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<TransferDoneMarker> result = (Collection<TransferDoneMarker>) query.execute(
					fromRepositoryId.toString(), toRepositoryId.toString());
			logger.debug("getRepoFileTransferDones: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getRepoFileTransferDones: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public void deleteRepoFileTransferDones(final UUID fromRepositoryId, final UUID toRepositoryId) {
		final Collection<TransferDoneMarker> transferDoneMarkers = getRepoFileTransferDones(fromRepositoryId, toRepositoryId);
		deletePersistentAll(transferDoneMarkers);
	}

	@Override
	protected Collection<TransferDoneMarker> load(final Collection<TransferDoneMarker> entities) {
		// no sub-classes => no need for real load method
		return new ArrayList<>(entities);
	}
}
