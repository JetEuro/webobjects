package registry;

import java.util.ListIterator;
import java.util.Set;
import java.util.SortedMap;

/**
 * User: cap_protect
 * Date: 5/6/12
 * Time: 4:27 PM
 */
public interface Registry extends SortedMap<String, Object> {
    Set<String> getSubkeys();

    int getIndexedSubregistriesCount();

    Registry byName(String name);

    Registry atIndex(int index);

    void removeSubRegistry(String name);

    void removeSubRegistry(int index);

    <T extends RegistryBean> T bean(Class<T> clazz);
}
