package service;

import beans.AddCommentRequest;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;
import registry.Registries;

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
            RegistryTask<AddCommentRequest> task = new RegistryTask<AddCommentRequest>() {
                public void run() {
                    specify(getBean().getUser().getAge(), 25);
                }
            };

            AddCommentRequest bean = Registries.newBean(AddCommentRequest.class);
            bean.getUser().setAge(25);

            task.setBean(bean);

            service.submit(task);
        }

    }
}
