package org.webobjects.service;

import org.webobjects.registry.Polymorphic;
import org.webobjects.registry.RegistryBean;
import org.webobjects.registry.RegistryGettable;
import org.webobjects.registry.RegistryHandler;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:33 PM
 */
public interface RegistryTask extends RegistryBean, Polymorphic, RegistryGettable, Runnable {
    @RegistryHandler("run()")
    void run();
}
