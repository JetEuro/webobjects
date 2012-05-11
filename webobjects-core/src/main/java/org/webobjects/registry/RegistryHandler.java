package org.webobjects.registry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * User: cap_protect
 * Date: 5/11/12
 * Time: 5:50 PM
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RegistryHandler {
    String value();
}
