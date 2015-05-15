package co.codewizards.cloudstore.test.model;

import java.util.HashMap;
import java.util.Map;

public class ExampleServiceRegistryImpl implements ExampleServiceRegistry {

	private static final ExampleServiceRegistryImpl instance = new ExampleServiceRegistryImpl();

	public static ExampleServiceRegistryImpl getInstance() {
		return instance;
	}

	private final Map<Integer, ExampleService> id2ExampleService = new HashMap<>();

	private ExampleServiceRegistryImpl() {
	}

	@Override
	public synchronized ExampleService getExampleServiceOrCreate(int id) {
		ExampleService exampleService = id2ExampleService.get(id);
		if (exampleService == null) {
			exampleService = new ExampleServiceImpl();
			id2ExampleService.put(id, exampleService);
		}
		return exampleService;
	}
}
