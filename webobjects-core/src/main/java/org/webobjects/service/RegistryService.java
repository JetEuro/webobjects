package org.webobjects.service;

import org.webobjects.registry.RegistryBean;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:33 PM
 */
public interface RegistryService {
    <T extends RegistryBean> void submit(RegistryTask<T> task);
}
