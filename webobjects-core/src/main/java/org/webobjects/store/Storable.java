package org.webobjects.store;

import org.webobjects.registry.RegistryHandler;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 6:40 PM
 */
public interface Storable {
    void newId();

    void setId(long id);

    long getId();

    @RegistryHandler("store")
    void store();

    @RegistryHandler("load")
    void load();
}
