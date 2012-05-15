package org.webobjects.sample.forum.app.beans;

import org.webobjects.registry.RegistryBean;

/**
 * User: cap_protect
 * Date: 5/13/12
 * Time: 12:29 PM
 */
public interface User extends RegistryBean {
    String getUsername();

    void setUsername(String username);

    String getPassword();

    void setPassword(String password);

    String getEmail();

    void setEmail(String email);


}
