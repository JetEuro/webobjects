package org.webobjects.web;

import org.webobjects.registry.Registries;
import org.webobjects.registry.Registry;
import org.webobjects.registry.RegistryBean;
import org.webobjects.web.BeanValidator;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;

/**
 * User: TCSDEVELOPER
 * Date: 5/15/12
 * Time: 10:04 AM
 */
public abstract class SimpleBeanValidator<T extends RegistryBean> implements BeanValidator<T> {
    protected Registry beanRegistry = Registries.newRegistry();
    protected Registry otherValues = Registries.newRegistry();
    private Map<String, String> messages = new TreeMap();
    public boolean validationResult = true;
    private final Class<T> beanClass;

    protected SimpleBeanValidator(Class<T> beanClass) {
        this.beanClass = beanClass;
    }

    protected void loadOther(HttpServletRequest request, String... parameters) {
        for (String param : parameters) {
            otherValues.put(param, request.getParameter(param));
        }
    }

    protected void load(HttpServletRequest request, String... parameters) {
        for (String param : parameters) {
            beanRegistry.put(param, request.getParameter(param));
        }
    }

    protected void checkEquals(String message, String key1, String key2) {
        Object value1 = getValue(key1);
        Object value2 = getValue(key2);
        boolean ret;
        if (value1 == null) {
            ret = value2 == null;
        } else {
            ret = value1.equals(value2);
        }
        if (!ret) {
            addMessage(key1, message, key1, key2);
            validationResult = false;
        }
    }

    protected void addMessage(String field, String message, Object ...args) {
        messages.put(field, String.format(message, args));
    }

    protected Object getValue(String key) {
        Object ret = beanRegistry.get(key);
        if (ret == null) {
            ret = otherValues.get(key);
        }
        return ret;
    }

    protected void checkExists(String message, String... keys) {
        for (String key : keys) {
            if (getValue(key) == null) {
                validationResult = false;
                addMessage(key, message, key);
            }
        }
    }

    protected void checkNotEmpty(String message, String... keys) {
        for (String key : keys) {
            Object value = getValue(key);
            if (value == null || value.toString().trim().isEmpty()) {
                validationResult = false;
                addMessage(key, message, key);
            }
        }
    }

    public String getMessages() {
        StringBuilder builder = new StringBuilder();
        for (String message : messages.values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(message);
        }
        return builder.toString();
    }

    public String getMessage(String parameter) {
        return messages.get(parameter);
    }

    public T bean() {
        return beanRegistry.bean(beanClass);
    }

    public Registry getOtherValues() {
        return otherValues;
    }
}
