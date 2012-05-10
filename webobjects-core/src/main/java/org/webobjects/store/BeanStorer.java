package org.webobjects.store;

import org.webobjects.registry.Registry;
import org.webobjects.registry.RegistryBean;

/**
 * User: TCSDEVELOPER
 * Date: 5/9/12
 * Time: 6:13 PM
 */
public interface BeanStorer {
    BeanStorer renewId();

    long getId();

    BeanStorer setId(long id);

    boolean load();

    BeanStorer store();

    void dispose();

    Registry getRegistry();

    <T extends RegistryBean> T getBean(Class<T> clazz);

    BeanStorer subStorer(String ...path);
}
