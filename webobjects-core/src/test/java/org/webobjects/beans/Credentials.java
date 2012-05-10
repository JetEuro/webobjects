package org.webobjects.beans;

import org.webobjects.registry.RegistryBean;

/**
* User: cap_protect
* Date: 5/8/12
* Time: 1:57 AM
*/
public interface Credentials extends RegistryBean {
    String getUsername();

    String getPassword();

    void setUsername(String username);

    void setPassword(String password);
}
