package registry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * User: cap_protect
 * Date: 5/9/12
 * Time: 11:05 AM
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RegistryMapType {
    Class value();
}
