package org.webobjects.store;

import org.webobjects.beans.AddCommentRequest;
import org.webobjects.beans.User;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;
import org.webobjects.registry.*;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 6:57 PM
 */
@RunWith(JDaveRunner.class)
public class RegistryStoreTest extends Specification<RegistryStore> {
    public class StoreAddUserRequest {
        RegistryStore store = RegistryStores.inMemory(IdGenerator.Type.SEQUENTIAL);

        public void addRequest() {
            AddCommentRequest bean = Registries.newBean(AddCommentRequest.class);

            bean.getUser().setAge(25);
            bean.getUser().setSex("M");

            bean.getUser().getCredentials().setUsername("user");
            bean.getUser().getCredentials().setPassword("password");

            long id = RegistryStoreUtils.write(store, bean);
            postProcess(id);
        }

        private void postProcess(long id) {
            AddCommentRequest request = RegistryStoreUtils.read(store, id, AddCommentRequest.class);

            specify(request.getUser().getAge(), must.equal(25));
            specify(request.getUser().getSex(), must.equal("M"));
            specify(request.getUser().getCredentials().getUsername(), must.equal("user"));
            specify(request.getUser().getCredentials().getPassword(), must.equal("password"));
        }
    }

    public class StoreAddUserRequestUseSubstorer {
        RegistryStore store = RegistryStores.inMemory(IdGenerator.Type.SEQUENTIAL);

        public void addRequest() {
            AddCommentRequest bean = Registries.newBean(AddCommentRequest.class);

            bean.getUser().setAge(25);
            bean.getUser().setSex("M");

            bean.getUser().getCredentials().setUsername("user");
            bean.getUser().getCredentials().setPassword("password");

            long id = store
                    .newStorer(bean.getRegistry())
                    .subStorer("requests")
                    .renewId()
                    .store()
                    .getId();
            postProcess(id);
        }

        private void postProcess(long id) {
            BeanStorer storer = store
                    .newStorer(Registries.newRegistry())
                    .subStorer("requests", "user")
                    .setId(id);
            storer.load();
            User user = storer.getBean(User.class);

            specify(user.getAge(), must.equal(25));
            specify(user.getSex(), must.equal("M"));
            specify(user.getCredentials().getUsername(), must.equal("user"));
            specify(user.getCredentials().getPassword(), must.equal("password"));
        }
    }
}
