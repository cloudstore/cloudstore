package co.codewizards.cloudstore.core.collection;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class StringWrapperListMergerTest {

	private static final class StringWrapper {
		public String string;
		public StringWrapper(String string) {
			this.string = string;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((string == null) ? 0 : string.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StringWrapper other = (StringWrapper) obj;
			return equal(this.string, other.string);
		}

		@Override
		public String toString() {
			return string;
		}
	}

	private ListMerger<StringWrapper, String> merger = new ListMerger<StringWrapper, String>() {
		@Override
		protected String getKey(StringWrapper element) {
			return element.string;
		}

		@Override
		protected void update(List<StringWrapper> dest, int index, StringWrapper sourceElement, StringWrapper destElement) {
			destElement.string = sourceElement.string;
		}
	};

	@Test
	public void addElements() {
		List<StringWrapper> source = new ArrayList<>();
		source.add(new StringWrapper("000"));
		source.add(new StringWrapper("001"));
		source.add(new StringWrapper("002"));
		source.add(new StringWrapper("003"));
		source.add(new StringWrapper("004"));
		source.add(new StringWrapper("001"));
		source.add(new StringWrapper("003"));
		source.add(new StringWrapper("003"));

		List<StringWrapper> dest = new ArrayList<>();
		dest.add(new StringWrapper("002"));
		dest.add(new StringWrapper("003"));

		merger.merge(source, dest);

		assertThat(source).hasSize(8);
		assertThat(dest).hasSize(8);
		assertThat(dest).isEqualTo(source);
	}

	@Test
	public void removeElements() {
		List<StringWrapper> source = new ArrayList<>();
		source.add(new StringWrapper("000"));
		source.add(new StringWrapper("001"));
		source.add(new StringWrapper("002"));
		source.add(new StringWrapper("003"));
		source.add(new StringWrapper("004"));
		source.add(new StringWrapper("001"));
		source.add(new StringWrapper("003"));
		source.add(new StringWrapper("003"));

		List<StringWrapper> dest = new ArrayList<>(source);
		source.remove(7);
		source.remove(6);
		source.remove(2);

		merger.merge(source, dest);

		assertThat(source).hasSize(5);
		assertThat(dest).hasSize(5);
		assertThat(dest).isEqualTo(source);
	}
}
