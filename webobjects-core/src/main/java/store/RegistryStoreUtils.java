package store;

import registry.RegistryGettable;
import registry.Registries;
import registry.Registry;
import registry.RegistryBean;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:18 PM
 */
public class RegistryStoreUtils {
    public static <T extends RegistryBean>
    T read(RegistryStore store, long id, Class<T> clazz) {
        Registry reg = Registries.newRegistry();
        store.load(id, reg);
        return reg.bean(clazz);
    }

    public static <T extends RegistryBean>
    void write(RegistryStore store, long id, T bean) {
        if (!(bean instanceof RegistryGettable)) {
            throw new UnsupportedOperationException("write supported only for beans implementing GetRegistry");
        }
        Registry registry = ((RegistryGettable) bean).getRegistry();
        store.store(id, registry);
    }

    public static <T extends RegistryBean>
    long write(RegistryStore store, T bean) {
        long id = store.newId();
        write(store, id, bean);
        return id;
    }
}
