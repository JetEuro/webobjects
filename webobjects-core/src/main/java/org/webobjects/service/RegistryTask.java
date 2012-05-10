package org.webobjects.service;

import org.webobjects.registry.RegistryBean;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:33 PM
 */
public abstract class RegistryTask<T extends RegistryBean> implements Runnable {
    private T bean;

    public void setBean(T bean) {
        this.bean = bean;
    }

    public T getBean() {
        return bean;
    }
}
