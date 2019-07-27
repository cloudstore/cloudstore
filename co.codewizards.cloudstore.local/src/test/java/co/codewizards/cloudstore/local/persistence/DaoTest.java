package co.codewizards.cloudstore.local.persistence;

import static java.util.Objects.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

@SuppressWarnings("deprecation")
public class DaoTest {

	@Test
	public void buildIdRangePackages_0() {
		System.out.println();
		System.out.println("*** buildIdRangePackages_0 ***");
		final int idRangePackageSize = 3;

		SortedSet<Long> entityIds = getEntityIds(
				16,17,18,19,20,21,
				25,
				27,28,29,
				34,35,36,37,38
				);

		List<List<Dao.IdRange>> idRangePackages = Dao.buildIdRangePackages(entityIds, idRangePackageSize);
		showIdRangePackages(idRangePackages);

		assertThat(idRangePackages).hasSize(2);
		for (List<Dao.IdRange> idRangePackage : idRangePackages) {
			assertThat(idRangePackage).hasSize(idRangePackageSize);
		}

		assertIdRangePackagesEqualEntityIds(idRangePackages, entityIds);
	}

	@Test
	public void buildIdRangePackages_1() {
		System.out.println();
		System.out.println("*** buildIdRangePackages_1 ***");
		final int idRangePackageSize = 3;

		SortedSet<Long> entityIds = getEntityIds(
				1,2,3,4,5,
				7,8,9,
				11,12,13,14,
				16,17,18,19,20,21,
				25,
				27,28,29,
				34,35,36,37,38,
				40
				);

		List<List<Dao.IdRange>> idRangePackages = Dao.buildIdRangePackages(entityIds, idRangePackageSize);
		showIdRangePackages(idRangePackages);

		assertThat(idRangePackages).hasSize(3);
		for (List<Dao.IdRange> idRangePackage : idRangePackages) {
			assertThat(idRangePackage).hasSize(idRangePackageSize);
		}

		assertIdRangePackagesEqualEntityIds(idRangePackages, entityIds);
	}

	@Test
	public void buildIdRangePackages_2() {
		System.out.println();
		System.out.println("*** buildIdRangePackages_2 ***");
		final int idRangePackageSize = 3;

		SortedSet<Long> entityIds = getEntityIds(
				1,
				7,8,9,
				11,12,13,14,
				16,17,18,19,20,21,
				25,
				27,28,29,
				34,35,36,37,38
				);

		List<List<Dao.IdRange>> idRangePackages = Dao.buildIdRangePackages(entityIds, idRangePackageSize);
		showIdRangePackages(idRangePackages);

		assertThat(idRangePackages).hasSize(3);
		for (List<Dao.IdRange> idRangePackage : idRangePackages) {
			assertThat(idRangePackage).hasSize(idRangePackageSize);
		}

		assertIdRangePackagesEqualEntityIds(idRangePackages, entityIds);
	}

	@Test
	public void buildIdRangePackages_3() {
		System.out.println();
		System.out.println("*** buildIdRangePackages_3 ***");
		final int idRangePackageSize = 3;

		SortedSet<Long> entityIds = getEntityIds(
				1,2,3,4,5,
				7,8,9,
				11,12,13,14,
				16,17,18,19,20,21,
				25,
				27,28,29,
				34,35,36,37,38,
				40,41,
				45,46,47,
				48
				);

		List<List<Dao.IdRange>> idRangePackages = Dao.buildIdRangePackages(entityIds, idRangePackageSize);
		showIdRangePackages(idRangePackages);

		assertThat(idRangePackages).hasSize(3);
		for (List<Dao.IdRange> idRangePackage : idRangePackages) {
			assertThat(idRangePackage).hasSize(idRangePackageSize);
		}

		assertIdRangePackagesEqualEntityIds(idRangePackages, entityIds);
	}

	@Test
	public void buildIdRangePackages_4() {
		System.out.println();
		System.out.println("*** buildIdRangePackages_4 ***");
		final int idRangePackageSize = 3;

		SortedSet<Long> entityIds = new TreeSet<>();

		List<List<Dao.IdRange>> idRangePackages = Dao.buildIdRangePackages(entityIds, idRangePackageSize);
		showIdRangePackages(idRangePackages);

		assertThat(idRangePackages).isEmpty();;
	}

	@Test
	public void buildIdRangePackages_5() {
		System.out.println();
		System.out.println("*** buildIdRangePackages_5 ***");
		final int idRangePackageSize = 3;

		SortedSet<Long> entityIds = getEntityIds(
				666
				);

		List<List<Dao.IdRange>> idRangePackages = Dao.buildIdRangePackages(entityIds, idRangePackageSize);
		showIdRangePackages(idRangePackages);

		assertThat(idRangePackages).hasSize(1);
		for (List<Dao.IdRange> idRangePackage : idRangePackages) {
			assertThat(idRangePackage).hasSize(idRangePackageSize);
		}

		assertIdRangePackagesEqualEntityIds(idRangePackages, entityIds);
	}

	@Test
	public void buildIdRangePackages_6() {
		System.out.println();
		System.out.println("*** buildIdRangePackages_6 ***");
		final int idRangePackageSize = 3;

		SortedSet<Long> entityIds = getEntityIds(
				666,667
				);

		List<List<Dao.IdRange>> idRangePackages = Dao.buildIdRangePackages(entityIds, idRangePackageSize);
		showIdRangePackages(idRangePackages);

		assertThat(idRangePackages).hasSize(1);
		for (List<Dao.IdRange> idRangePackage : idRangePackages) {
			assertThat(idRangePackage).hasSize(idRangePackageSize);
		}

		assertIdRangePackagesEqualEntityIds(idRangePackages, entityIds);
	}

	private static void assertIdRangePackagesEqualEntityIds(List<List<Dao.IdRange>> idRangePackages, SortedSet<Long> entityIds) {
		for (long id = 0; id < 1000; ++id) {
			assertThat(isInIdRangePackages(idRangePackages, id)).isEqualTo(entityIds.contains(id));
		}
	}

	private static boolean isInIdRangePackages(List<List<Dao.IdRange>> idRangePackages, long id) {
		requireNonNull(idRangePackages, "idRangePackages");
		int matchCount = 0;
		for (List<Dao.IdRange> idRangePackage : idRangePackages) {
			for (Dao.IdRange idRange : idRangePackage) {
				if (idRange.fromIdIncl <= id && id <= idRange.toIdIncl)
					++matchCount;
			}
		}
		assertThat(matchCount).isLessThanOrEqualTo(1);
		return matchCount == 1;
	}

	private static SortedSet<Long> getEntityIds(int ... entityIds) {
		requireNonNull(entityIds, "entityIds");

		TreeSet<Long> result = new TreeSet<>();
		for (int id : entityIds) {
			result.add(Long.valueOf(id));
		}
		return result;
	}

	private static void showIdRangePackages(List<List<Dao.IdRange>> idRangePackages) {
		int idx = -1;
		for (List<Dao.IdRange> idRangePackage : idRangePackages) {
			System.out.println((++idx) + ": " + idRangePackage);
		}
	}

}
