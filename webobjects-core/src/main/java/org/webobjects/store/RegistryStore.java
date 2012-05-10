package org.webobjects.store;

import org.webobjects.registry.Registry;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 6:58 PM
 */
public interface RegistryStore {
    long newId();

    BeanStorer newStorer(Registry registry);
}
