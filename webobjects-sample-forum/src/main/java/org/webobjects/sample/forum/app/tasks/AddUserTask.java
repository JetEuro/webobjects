package org.webobjects.sample.forum.app.tasks;

import org.webobjects.service.RegistryTask;

/**
 * User: cap_protect
 * Date: 5/13/12
 * Time: 1:41 PM
 */
public interface AddUserTask extends RegistryTask {
    long getUserId();

    void setUserId(long id);
}
