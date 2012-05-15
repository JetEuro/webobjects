package org.webobjects.web;

import org.webobjects.registry.Registry;
import org.webobjects.registry.RegistryBean;

import javax.servlet.http.HttpServletRequest;

/**
 * User: cap_protect
 * Date: 5/13/12
 * Time: 1:37 PM
 */
public interface BeanValidator<T extends RegistryBean> {
    void loadDefaults();

    void loadParameters(HttpServletRequest request);

    boolean validate();

    String getMessages();

    String getMessage(String parameter);

    T bean();

    Registry getOtherValues();
}
