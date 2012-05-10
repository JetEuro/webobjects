package org.webobjects.store;

import org.webobjects.registry.RegistryGettable;
import org.webobjects.registry.Registries;
import org.webobjects.registry.Registry;
import org.webobjects.registry.RegistryBean;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:18 PM
 */
public class RegistryStoreUtils {
    public static <T extends RegistryBean>
    T read(RegistryStore store, long id, Class<T> clazz) {
        Registry reg = Registries.newRegistry();
        store.newStorer(reg).setId(id).load();
        return reg.bean(clazz);
    }

    public static <T extends RegistryBean>
    void write(RegistryStore store, long id, T bean) {
        if (!(bean instanceof RegistryGettable)) {
            throw new UnsupportedOperationException("write supported only for org.webobjects.beans implementing GetRegistry");
        }
        Registry registry = ((RegistryGettable) bean).getRegistry();
        store.newStorer(registry).setId(id).store();
    }

    public static <T extends RegistryBean>
    long write(RegistryStore store, T bean) {
        long id = store.newId();
        write(store, id, bean);
        return id;
    }
}
