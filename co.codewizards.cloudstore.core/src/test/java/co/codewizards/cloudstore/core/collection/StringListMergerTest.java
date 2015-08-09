package co.codewizards.cloudstore.core.collection;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class StringListMergerTest {

	private Random random = new Random();

	private ListMerger<String, String> merger = new ListMerger<String, String>() {
		@Override
		protected String getKey(String element) {
			return element;
		}

		@Override
		protected void update(List<String> dest, int index, String sourceElement, String destElement) {
			dest.set(index, sourceElement);
		}
	};

	@Test
	public void addElements() {
		List<String> source = new ArrayList<>();
		source.add("000");
		source.add("001");
		source.add("002");
		source.add("003");
		source.add("004");
		source.add("001");
		source.add("003");
		source.add("003");

		List<String> dest = new ArrayList<>();
		dest.add("002");
		dest.add("003");

		merger.merge(source, dest);

		assertThat(source).hasSize(8);
		assertThat(dest).hasSize(8);
		assertThat(dest).isEqualTo(source);
	}

	@Test
	public void removeElements() {
		List<String> source = new ArrayList<>();
		source.add("000");
		source.add("001");
		source.add("002");
		source.add("003");
		source.add("004");
		source.add("001");
		source.add("003");
		source.add("003");

		List<String> dest = new ArrayList<>(source);
		source.remove(7);
		source.remove(6);
		source.remove(2);

		merger.merge(source, dest);

		assertThat(source).hasSize(5);
		assertThat(dest).hasSize(5);
		assertThat(dest).isEqualTo(source);
	}

	@Test
	public void addOrRemoveElements() {
		List<String> source = new ArrayList<>();
		source.add("000");
		source.add("001");
		source.add("002");
		source.add("003");
		source.add("004");
		source.add("001");
		source.add("003");
		source.add("003");

		List<String> dest = new ArrayList<>();
		dest.add("100");
		dest.add("004");
		dest.add("003");
		dest.add("001");
		dest.add("100");
		dest.add("101");
		dest.add("102");
		dest.add("104");
		dest.add("000");
		dest.add("103");
		dest.add("100");
		dest.add("001");
		dest.add("101");
		dest.add("003");

		merger.merge(source, dest);

		assertThat(source).hasSize(8);
		assertThat(dest).hasSize(8);
		assertThat(dest).isEqualTo(source);
	}

	@Test
	public void randomAddOrRemoveElements() {
		List<String> source = createRandomList();
		List<String> dest = createRandomList();

		merger.merge(source, dest);

		assertThat(dest).isEqualTo(source);
	}

	private List<String> createRandomList() {
		int length = random.nextInt(1000);
		List<String> result = new ArrayList<String>(length);
		for (int i = 0; i < length; ++i)
			result.add(Integer.toString(random.nextInt(1000)));

		return result;
	}
}
