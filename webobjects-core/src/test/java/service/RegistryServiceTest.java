package service;

import beans.AddCommentRequest;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import jdave.support.Assert;
import org.junit.runner.RunWith;
import registry.RegistryGettable;
import registry.Registries;
import registry.Registry;
import registry.RegistryBean;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:36 PM
 */
@RunWith(JDaveRunner.class)
public class RegistryServiceTest extends Specification<RegistryService> {

    public class PostPoneJob {
        private RegistryService service = RegistryServices.runRegistryService();

        public void postPone() {
            RegistryTask<AddCommentRequest> task = new AddCommentRequestRegistryTask();
            AddCommentRequest bean = Registries.newBean(AddCommentRequest.class);
            bean.getUser().setAge(25);

            task.setBean(bean);

            service.submit(task);
        }
    }

    public class PostPoneJobSerialized {
        private RegistryService service = new SerializeRegistryService();

        private class SerializeRegistryService implements RegistryService {
            public <T extends RegistryBean> void submit(RegistryTask<T> task) {
                T bean = task.getBean();
                RegistryGettable gr = (RegistryGettable) bean;
                Registry registry = gr.getRegistry();
                Map<String,Object> liniarize = Registries.liniarize(registry);
                try {
                    RegistryTask newOne = (RegistryTask) Class.forName(task.getClass().getName()).newInstance();
                    Registry restoredOne = Registries.newRegistry(liniarize);
                    RegistryBean newBean = restoredOne.bean(RegistryBean.class);
                    newOne.setBean(newBean);
                    newOne.run();
                } catch (Exception e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        }

        public void postPone() {
            RegistryTask<AddCommentRequest> task = new AddCommentRequestRegistryTask();
            AddCommentRequest bean = Registries.newBean(AddCommentRequest.class);
            bean.getUser().setAge(25);

            task.setBean(bean);

            service.submit(task);
        }

    }

    public static class AddCommentRequestRegistryTask extends RegistryTask<AddCommentRequest> {
        public void run() {
            Assert.isTrue(25 == getBean().getUser().getAge(), "age should be 25");
        }
    }
}
