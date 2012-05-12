package org.webobjects.registry;

import java.util.Set;
import java.util.SortedMap;

/**
 * User: cap_protect
 * Date: 5/6/12
 * Time: 4:27 PM
 */
public interface Registry extends SortedMap<String, Object> {
    Set<String> getNamedSubregistriesKeys();

    int getIndexedSubregistriesCount();

    Registry byName(String name);

    Registry atIndex(int index);

    void removeSubregistry(String name);

    void removeSubregistry(int index);

    void clearAll();

    void clearIndexedSubregistries();

    void clearNamedSubregistries();

    void clearSubregistires();

    <T extends RegistryBean> T bean(Class<T> clazz);

    ExecutionContext getExecutionContext();

    void setExecutionContext(ExecutionContext context);
}
