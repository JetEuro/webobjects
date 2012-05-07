package store;

import registry.Registry;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 6:58 PM
 */
public interface RegistryStore {
    long newId();

    void store(long id, Registry registry);

    boolean load(long id, Registry registry);
}
