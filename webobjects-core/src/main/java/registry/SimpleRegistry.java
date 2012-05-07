package registry;

import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.reflect.*;
import java.util.*;

/**
 * User: cap_protect
 * Date: 5/6/12
 * Time: 11:14 PM
 */
class SimpleRegistry implements Registry {
    private Map<String, SimpleRegistry> subRegistries = new TreeMap<String, SimpleRegistry>();
    private ArrayList<SimpleRegistry> indexedSubRegistries = new ArrayList<SimpleRegistry>();
    private TreeMap<String, Object> store = new TreeMap<String, Object>();

    public Set<String> getSubkeys() {
        return subRegistries.keySet();
    }

    public int getIndexedSubregistriesCount() {
        return indexedSubRegistries.size();
    }

    public SimpleRegistry byName(String name) {
        SimpleRegistry sr = subRegistries.get(name);
        if (sr == null) {
            sr = new SimpleRegistry();
            subRegistries.put(name, sr);
        }
        return sr;
    }

    public SimpleRegistry atIndex(int index) {
        if (index + 1 > indexedSubRegistries.size()) {
            indexedSubRegistries.addAll(
                    Collections.<SimpleRegistry>nCopies(index + 1
                            - indexedSubRegistries.size(), null)
            );
        }

        SimpleRegistry sr = indexedSubRegistries.get(index);
        if (sr == null) {
            sr = new SimpleRegistry();
            indexedSubRegistries.set(index, sr);
        }
        return sr;
    }

    public void removeSubRegistry(String name) {
        subRegistries.remove(name);
    }

    public void removeSubRegistry(int index) {
        indexedSubRegistries.remove(index);
        int idx = indexedSubRegistries.size() - 1;
        while (idx >= 0 && indexedSubRegistries.get(idx) == null) {
            idx--;
        }
        if (idx > 0 && idx < indexedSubRegistries.size()) {
            indexedSubRegistries.subList(idx, indexedSubRegistries.size()).clear();
        }
    }

    public <T extends RegistryBean> T bean(Class<T> clazz) {
        Class[]classArray = new Class[] { clazz };
        return (T) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                classArray, new BeanInvocationHandler(clazz));
    }

    private static final class IRLSignature {
        private final String name;
        private final Class []params;

        private IRLSignature(String name, Class[] params) {
            this.name = name;
            this.params = params;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IRLSignature that = (IRLSignature) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (!Arrays.equals(params, that.params)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (params != null ? Arrays.hashCode(params) : 0);
            return result;
        }
    }

    private static Map<IRLSignature, Method> IRL_SIGNATURES = new HashMap();
    static {
        for (Method method : BeanInvocationHandler.IndexedRegistryiesList.class.getMethods()) {
            IRL_SIGNATURES.put(new IRLSignature(method.getName(), method.getParameterTypes()), method);
        }
    }

    private final class BeanInvocationHandler implements InvocationHandler {

        private final class IndexedRegistryiesList extends AbstractList<Object> {
            private final Method getMethod;
            private final RegistryListType listType;

            public IndexedRegistryiesList(Method getMethod, RegistryListType listType) {
                this.getMethod = getMethod;
                this.listType = listType;
            }

            @Override
            public Object get(int index) {
                return beanize(getMethod, listType,
                        atIndex(index));
            }

            @Override
            public int size() {
                return getIndexedSubregistriesCount();
            }
        }


        private final Class<?> beanClass;
        private final IndexedRegistryiesList irl;
        private final boolean acceptsGetRegistry;

        public BeanInvocationHandler(Class<?> beanClass) {
            this.beanClass = beanClass;
            if (List.class.isAssignableFrom(beanClass)
                    && beanClass.isAnnotationPresent(RegistryListType.class)) {

                final Method getMethod;
                try {
                    getMethod = beanClass.getMethod("get", new Class[]{int.class});
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("could not get 'get' List method");
                }

                final RegistryListType listType = beanClass.getAnnotation(RegistryListType.class);
                irl = new IndexedRegistryiesList(getMethod, listType);
            } else {
                irl = null;
            }

            acceptsGetRegistry = GetRegistry.class.isAssignableFrom(beanClass);
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isGetRegistry(method)) {
                return SimpleRegistry.this;
            } else if (isIndexedListMethod(method)) {
                return invokeIndexedListMethod(method, args);
            } else if (isGetProperty(method)) {
                return getProperty(method);
            } else if (isSetProperty(method)) {
                setProperty(method, args[0]);
                return proxy;
            }
            throw new UnsupportedOperationException();
        }

        private boolean isGetRegistry(Method method) {
            return acceptsGetRegistry
                    && method.getName().equals("getRegistry")
                    && method.getParameterTypes().length == 0
                    && Registry.class.isAssignableFrom(method.getReturnType());
        }

        private Object invokeIndexedListMethod(Method method, Object[] args) {
            Method declMethod = IRL_SIGNATURES.get(
                    new IRLSignature(method.getName(), method.getParameterTypes()));
            try {
                return declMethod.invoke(irl, args);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("RegistryListType wrapper error", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("RegistryListType wrapper error", e);
            }
        }

        private boolean isIndexedListMethod(Method method) {
            Method declMethod = IRL_SIGNATURES.get(
                    new IRLSignature(method.getName(), method.getParameterTypes()));
            return declMethod != null;
        }

        private boolean isGetProperty(Method method) {
            String name = method.getName();
            return name.startsWith("get")
                    && name.length() >= 4
                    && method.getParameterTypes().length == 0;
        }

        private boolean isSetProperty(Method method) {
            String name = method.getName();
            return name.startsWith("set")
                    && name.length() >= 4
                    && method.getParameterTypes().length == 1;
        }

        private Object getProperty(Method method) {
            String name = method.getName().substring("get".length());
            name = name.substring(0, 1).toLowerCase() + name.substring(1);


            Class<?> retType = method.getReturnType();
            if (RegistryBean.class.isAssignableFrom(retType)) {
                return beanize(method, byName(name));
            }

            return store.get(name);
        }

        private void setProperty(Method method, Object value) {
            String name = method.getName().substring("set".length());
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
            store.put(name, value);
        }

        private Object beanize(Method method, SimpleRegistry reg) {
            Class<RegistryBean> returnType;
            returnType = (Class<RegistryBean>) method.getReturnType();
            if (RegistryBean.class.isAssignableFrom(returnType)) {
                return reg.bean(returnType);
            }
            throw new UnsupportedOperationException();

        }

        private Object beanize(Method method, RegistryListType listType, SimpleRegistry reg) {
            if (listType == null) {
                throw new AnnotationTypeMismatchException(
                        method,
                        "declare " + RegistryListType.class + " for " +
                        "for bean types that implements " + List.class);
            }

            Class value = listType.value();
            return reg.bean(value);
        }

        private SimpleRegistry getRegByIndex(String name) {
            SimpleRegistry reg = null;
            if (name.matches("\\d+")) {
                int idx;
                try {
                    idx = Integer.parseInt(name);
                    if (0 <= idx && idx < indexedSubRegistries.size()) {
                        reg = indexedSubRegistries.get(idx);
                    }
                } catch (NumberFormatException nfe){
                }
            }
            return reg;
        }
    }

    public void clear() {
        store.clear();
    }

    @Override
    public Object clone() {
        return store.clone();
    }

    public Comparator<? super String> comparator() {
        return store.comparator();
    }

    public boolean containsKey(Object key) {
        return store.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return store.containsValue(value);
    }

    public Set<Entry<String, Object>> entrySet() {
        return store.entrySet();
    }

    public String firstKey() {
        return store.firstKey();
    }

    public Object get(Object key) {
        return store.get(key);
    }

    public SortedMap<String, Object> headMap(String toKey) {
        return store.headMap(toKey);
    }

    public NavigableMap<String, Object> headMap(String toKey, boolean inclusive) {
        return store.headMap(toKey, inclusive);
    }

    public Set<String> keySet() {
        return store.keySet();
    }

    public String lastKey() {
        return store.lastKey();
    }

    public Object put(String key, Object value) {
        return store.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Object> map) {
        store.putAll(map);
    }

    public Object remove(Object key) {
        return store.remove(key);
    }

    public int size() {
        return store.size();
    }

    public SortedMap<String, Object> subMap(String fromKey, String toKey) {
        return store.subMap(fromKey, toKey);
    }

    public NavigableMap<String, Object> subMap(String fromKey, boolean fromInclusive, String toKey, boolean toInclusive) {
        return store.subMap(fromKey, fromInclusive, toKey, toInclusive);
    }

    public SortedMap<String, Object> tailMap(String fromKey) {
        return store.tailMap(fromKey);
    }

    public NavigableMap<String, Object> tailMap(String fromKey, boolean inclusive) {
        return store.tailMap(fromKey, inclusive);
    }

    public Collection<Object> values() {
        return store.values();
    }

    public Entry<String, Object> firstEntry() {
        return store.firstEntry();
    }

    public Entry<String, Object> lastEntry() {
        return store.lastEntry();
    }

    public Entry<String, Object> pollFirstEntry() {
        return store.pollFirstEntry();
    }

    public Entry<String, Object> pollLastEntry() {
        return store.pollLastEntry();
    }

    public Entry<String, Object> lowerEntry(String key) {
        return store.lowerEntry(key);
    }

    public String lowerKey(String key) {
        return store.lowerKey(key);
    }

    public Entry<String, Object> floorEntry(String key) {
        return store.floorEntry(key);
    }

    public String floorKey(String key) {
        return store.floorKey(key);
    }

    public Entry<String, Object> ceilingEntry(String key) {
        return store.ceilingEntry(key);
    }

    public String ceilingKey(String key) {
        return store.ceilingKey(key);
    }

    public Entry<String, Object> higherEntry(String key) {
        return store.higherEntry(key);
    }

    public String higherKey(String key) {
        return store.higherKey(key);
    }

    public NavigableSet<String> navigableKeySet() {
        return store.navigableKeySet();
    }

    public NavigableMap<String, Object> descendingMap() {
        return store.descendingMap();
    }

    public NavigableSet<String> descendingKeySet() {
        return store.descendingKeySet();
    }

    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        return store.equals(o);
    }

    @Override
    public int hashCode() {
        return store.hashCode();
    }

    @Override
    public String toString() {
        return Registries.stringify(this).toString();
    }
}