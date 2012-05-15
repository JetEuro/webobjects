package org.webobjects.sample.forum.app.beans;

import org.webobjects.registry.RegistryBean;

import java.util.Date;
import java.util.List;

/**
 * User: cap_protect
 * Date: 5/13/12
 * Time: 12:32 PM
 */
public interface Topic extends RegistryBean {
    void setTitle(String title);

    String getTitle();

    void setText(String text);

    String getText();

    void setCreateDate(Date date);

    Date getCreateDate();
}
