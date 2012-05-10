package org.webobjects.registry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 8:14 AM
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface IndexedRegistryListType {
    Class value();
}
