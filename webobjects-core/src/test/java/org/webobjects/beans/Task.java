package org.webobjects.beans;

import org.webobjects.registry.RegistryBean;
import org.webobjects.registry.RegistryHandler;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 6:29 PM
 */
public interface Task extends RegistryBean, Runnable {
    void run();
}
