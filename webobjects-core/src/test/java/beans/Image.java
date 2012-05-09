package beans;

import registry.RegistryGettable;
import registry.RegistryBean;

/**
* User: cap_protect
* Date: 5/8/12
* Time: 1:58 AM
*/
public interface Image extends RegistryBean, RegistryGettable {
    String getSrc();

    Image setSrc(String value);

    String getTitle();

    Image setTitle(String value);
}
