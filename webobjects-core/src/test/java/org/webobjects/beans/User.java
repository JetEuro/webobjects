package org.webobjects.beans;

import org.webobjects.registry.RegistryBean;
import org.webobjects.registry.RegistryGettable;

/**
* User: cap_protect
* Date: 5/8/12
* Time: 1:57 AM
*/
public interface User extends RegistryBean, RegistryGettable {
    int getAge();

    String getSex();

    Credentials getCredentials();

    Images getImages();

    ImageTree getTree();

    void setAge(int age);

    void setSex(String sex);
}
