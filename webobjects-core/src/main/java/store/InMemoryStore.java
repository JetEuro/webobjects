package store;

import registry.Registries;
import registry.Registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 8:26 PM
 */
class InMemoryStore implements RegistryStore {
    private IdGenerator generator;

    InMemoryStore(IdGenerator.Type type) {
        generator = IdGenerator.getGenerator(type);
    }

    private Map<Long, Map<String, Object>> backendMap = new ConcurrentHashMap<Long, Map<String, Object>>();

    public long newId() {
        return generator.newId();
    }

    public void store(long id, Registry registry) {
        Map<String, Object> liniarizedData = Registries.liniarize(registry);

        Map<String, Object> data = backendMap.get(id);
        if (data == null) {
            backendMap.put(id, liniarizedData);
        } else {
            data.putAll(liniarizedData);
        }
    }

    public boolean load(long id, Registry registry) {
        Map<String, Object> map = backendMap.get(id);
        if (map == null) {
            return false;
        }
        Registries.putMassivly(registry, map);
        return true;
    }

}
