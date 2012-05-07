package store;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:05 PM
 */
public class RegistryStores {
    public static RegistryStore inMemory(IdGenerator.Type type) {
        return new InMemoryStore(type);
    }
}
