package org.webobjects.registry;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 5:48 PM
 */
public interface RegistryDelegate<T extends RegistryBean> {
    Object execute(T self, Object... args);
}
