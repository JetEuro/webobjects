package org.webobjects.registry;

import org.webobjects.beans.*;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;
import org.webobjects.beans.calculate.*;
import org.webobjects.beans.calculate.delegates.CalculateDelegates;
import org.webobjects.beans.delegates.RunRequestProcessDelegate;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 1:09 AM
 */

@RunWith(JDaveRunner.class)
public class RegistryTest extends Specification<Registry> {
    public class RegistryAsBean {
        private Registry registry = Registries.newRegistry();

        public void readBeanized() {
            Registry creds = registry.byName("credentials");
            creds.put("username", "value1");
            creds.put("password", "value2");

            registry.put("age", 25);
            registry.put("sex", "M");

            Registry images = registry.byName("images");
            images.atIndex(0).put("src", "img0");

            User userBean = registry.bean(User.class);
            specify(userBean.getAge(), must.equal(25));
            specify(userBean.getSex(), must.equal("M"));
            specify(userBean.getCredentials().getUsername(), must.equal("value1"));
            specify(userBean.getCredentials().getPassword(), must.equal("value2"));

            specify(userBean.getImages().size(), must.equal(1));
            for (Image image : userBean.getImages()) {
                specify(image.getSrc(), must.equal("img0"));
                Registry reg = image.getRegistry();
                System.out.println(reg.toString());
            }

            System.out.println(registry);
        }

        public void readWriteBeanized() {
            User userBean = Registries.newBean(User.class);
            Images images = userBean.getImages();
            Image img = images.get(0);
            img.setSrc("img0").setTitle("title");

            specify(userBean.getImages().size(), must.equal(1));
            for (Image image : userBean.getImages()) {
                specify(image.getSrc(), must.equal("img0"));
                specify(image.getTitle(), must.equal("title"));
            }

            System.out.println(userBean);
        }

        public void readTree() {
            User userBean = registry.bean(User.class);

            ImageTree tree = userBean.getTree();
            ImageTreeNode root = tree.getRoot();

            root.get(0).getImage().setSrc("img0");
            root.get(1).getImage().setSrc("img1");
            root.get(2).getImage().setSrc("img2");
            root.get(0).get(0).getImage().setSrc("img00");
            root.get(0).get(1).getImage().setSrc("img01");
            root.get(0).get(2).getImage().setSrc("img02");

            specify(userBean.getTree().getRoot().size(), 3);
            specify(userBean.getTree().getRoot().get(0).size(), 3);

            specify(root.get(0).getImage().getSrc(), "img0");
            specify(root.get(1).getImage().getSrc(), "img1");
            specify(root.get(2).getImage().getSrc(), "img2");
            specify(root.get(0).get(0).getImage().getSrc(), "img00");
            specify(root.get(0).get(1).getImage().getSrc(), "img01");
            specify(root.get(0).get(2).getImage().getSrc(), "img02");

            System.out.println(registry);
        }

        public void executeTask() {
            AddCommentRequest request = registry.bean(AddCommentRequest.class);
            ExecutionContext execCtx = registry.getExecutionContext();
            execCtx.bind("process", new RunRequestProcessDelegate());

            User userBean = request.getUser();
            request.setText("text");

            ImageTree tree = userBean.getTree();
            ImageTreeNode root = tree.getRoot();

            userBean.getCredentials().setUsername("username");
            root.get(0).getImage().setSrc("img0");
            root.get(1).getImage().setSrc("img1");
            root.get(2).getImage().setSrc("img2");
            root.get(0).get(0).getImage().setSrc("img00");
            root.get(0).get(1).getImage().setSrc("img01");
            root.get(0).get(2).getImage().setSrc("img02");

            specify(userBean.getTree().getRoot().size(), 3);
            specify(userBean.getTree().getRoot().get(0).size(), 3);

            specify(root.get(0).getImage().getSrc(), "img0");
            specify(root.get(1).getImage().getSrc(), "img1");
            specify(root.get(2).getImage().getSrc(), "img2");
            specify(root.get(0).get(0).getImage().getSrc(), "img00");
            specify(root.get(0).get(1).getImage().getSrc(), "img01");
            specify(root.get(0).get(2).getImage().getSrc(), "img02");

            StringBuilder response = new StringBuilder();
            request.process(response);
            specify(response.toString(), "username: text");
        }

        private CalculateTask calculateTask(Class<? extends CalculateTask> clazz, int a, int b) {
            CalculateTask task = Registries.newBean(clazz);
            CalculateDelegates.defaultContext(
                    task.getRegistry().getExecutionContext());
            task.setA(a);
            task.setB(b);
            return task;
        }

        public void addTask() {
            CalculateTask task = calculateTask(AddTask.class, 5, 11);
            execute(task);
            specify(task.getC(), 16);
        }


        public void subtractTask() {
            CalculateTask task = calculateTask(SubtractTask.class, 5, 11);
            execute(task);
            specify(task.getC(), -6);
        }


        public void multiplyTask() {
            CalculateTask task = calculateTask(MultiplyTask.class, 5, 11);
            execute(task);
            specify(task.getC(), 55);
        }


        public void divideTask() {
            CalculateTask task = calculateTask(DivideTask.class, 60, 5);
            execute(task);
            specify(task.getC(), 12);
        }

        private void execute(Task task) {
            task.run();
    }
    }
}