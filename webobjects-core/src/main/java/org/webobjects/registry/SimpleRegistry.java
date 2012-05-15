package org.webobjects.registry;

import org.webobjects.utils.BasicTypeParser;

import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * User: cap_protect
 * Date: 5/6/12
 * Time: 11:14 PM
 */
class SimpleRegistry implements Registry {
    public static final String CLASS_PROPERTY = "class";
    private Map<String, SimpleRegistry> subRegistries = new TreeMap<String, SimpleRegistry>();
    private ArrayList<SimpleRegistry> indexedSubregistries = new ArrayList<SimpleRegistry>();
    private TreeMap<String, Object> store = new TreeMap<String, Object>();
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\d+");
    private volatile ExecutionContext executionContext = new ExecutionContext();

    public Set<String> getNamedSubregistriesKeys() {
        return subRegistries.keySet();
    }

    public int getIndexedSubregistriesCount() {
        return indexedSubregistries.size();
    }

    public SimpleRegistry byName(String name) {
        if (INDEX_PATTERN.matcher(name).matches()) {
            return atIndex(Integer.parseInt(name));
        }
        SimpleRegistry sr = subRegistries.get(name);
        if (sr == null) {
            sr = new SimpleRegistry();
            subRegistries.put(name, sr);
        }
        return sr;
    }

    public SimpleRegistry atIndex(int index) {
        if (index + 1 > indexedSubregistries.size()) {
            indexedSubregistries.addAll(
                    Collections.<SimpleRegistry>nCopies(index + 1
                            - indexedSubregistries.size(), null)
            );
        }

        SimpleRegistry sr = indexedSubregistries.get(index);
        if (sr == null) {
            sr = new SimpleRegistry();
            indexedSubregistries.set(index, sr);
        }
        return sr;
    }

    public void removeSubregistry(String name) {
        subRegistries.remove(name);
    }

    public void removeSubregistry(int index) {
        indexedSubregistries.remove(index);
        int idx = indexedSubregistries.size() - 1;
        while (idx >= 0 && indexedSubregistries.get(idx) == null) {
            idx--;
        }
        if (idx > 0 && idx < indexedSubregistries.size()) {
            indexedSubregistries.subList(idx, indexedSubregistries.size()).clear();
        }
    }

    public void clearAll() {
        clearSubregistires();
        clear();
    }

    public void clearSubregistires() {
        clearIndexedSubregistries();
        clearNamedSubregistries();
    }

    public void clearIndexedSubregistries() {
        indexedSubregistries.clear();
    }

    public void clearNamedSubregistries() {
        subRegistries.clear();
    }

    public <T extends RegistryBean> T bean(Class<T> clazz) {
        Object classStringObj = get(CLASS_PROPERTY);
        if (classStringObj != null
                && classStringObj instanceof String) {
            String classString = (String) classStringObj;
            try {
                Class someClass = Class.forName(classString);
                if (clazz.isAssignableFrom(someClass)) {
                    clazz = someClass;
                }
            } catch (ClassNotFoundException e) {
                // skip
            }
        }
        if (Polymorphic.class.isAssignableFrom(clazz)) {
            put(CLASS_PROPERTY, clazz.getName());
        }
        Class[]classArray = new Class[] { clazz, RegistryGettable.class, JavaDefaults.class };
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                classArray, new BeanInvocationHandler(clazz));
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext context) {
        if (context == null) {
            throw new NullPointerException("context");
        }
        executionContext = context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleRegistry that = (SimpleRegistry) o;

        if (!indexedSubregistries.equals(that.indexedSubregistries)) return false;
        if (!store.equals(that.store)) return false;
        if (!subRegistries.equals(that.subRegistries)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = subRegistries.hashCode();
        result = 31 * result + indexedSubregistries.hashCode();
        result = 31 * result + store.hashCode();
        return result;
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
            private final IndexedRegistryListType listType;

            public IndexedRegistryiesList(Method getMethod, IndexedRegistryListType listType) {
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
                    && beanClass.isAnnotationPresent(IndexedRegistryListType.class)) {

                final Method getMethod;
                try {
                    getMethod = beanClass.getMethod("get", new Class[]{int.class});
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("could not get 'get' List method");
                }

                final IndexedRegistryListType listType = beanClass.getAnnotation(IndexedRegistryListType.class);
                irl = new IndexedRegistryiesList(getMethod, listType);
            } else {
                irl = null;
            }

            acceptsGetRegistry = RegistryGettable.class.isAssignableFrom(beanClass);
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isTaskImplementation(method)) {
                return executeTask(proxy, method, args);
            } else if (isGetRegistry(method)) {
                return SimpleRegistry.this;
            } else if (isToString(method)) {
                return SimpleRegistry.this.toString();
            } else if (isEquals(method)) {
                return equals(args[0]);
            } else if (isHashCode(method)) {
                return hashCode();
            } else if (isRegistryMapType(method)) {
                return getMapObject(method);
            } else if (isIndexedListMethod(method)) {
                return invokeIndexedListMethod(method, args);
            } else if (isGetProperty(method)) {
                return getProperty(method);
            } else if (isIsProperty(method)) {
                return isProperty(method);
            } else if (isSetProperty(method)) {
                setProperty(method, args[0]);
                return proxy;
            }
            throw new UnsupportedOperationException();
        }

        private Object executeTask(Object proxy, Method method, Object[] args) {
            RegistryHandler impl = method.getAnnotation(RegistryHandler.class);
            RegistryDelegate delegate = getExecutionContext().selectDelegate(impl);
            if (delegate == null) {
                return null;
            }
            return delegate.execute((RegistryBean) proxy, args);
        }

        private Map<Class, RegistryDelegate> delegates = new HashMap();

        private RegistryDelegate getTaskClass(Class<? extends RegistryDelegate> taskClass) throws IllegalAccessException, InstantiationException {
            synchronized (delegates) {
                RegistryDelegate delegate = delegates.get(taskClass);
                if (delegate == null) {
                    delegate = taskClass.newInstance();
                    delegates.put(taskClass, delegate);
                }
                return delegate;
            }

        }

        private boolean isTaskImplementation(Method method) {
            return method.isAnnotationPresent(RegistryHandler.class);
        }

        private Map getMapObject(Method method) {
            final RegistryMapType mapType = method.getAnnotation(RegistryMapType.class);

            final BasicTypeParser caster = BasicTypeParser.create(mapType.value());

            String propertyName = getPropertyName(method);
            final SimpleRegistry registry = byName(propertyName);

            return new StringCasterMap(registry, caster);
        }

        private boolean isRegistryMapType(Method method) {
            return method.isAnnotationPresent(RegistryMapType.class);
        }

        private boolean isToString(Method method) {
            return method.getName().equals("toString")
                    && method.getParameterTypes().length == 0;
        }

        private boolean isHashCode(Method method) {
            return method.getName().equals("hashCode")
                    && method.getParameterTypes().length == 0;
        }

        private boolean isEquals(Method method) {
            return method.getName().equals("equals")
                    && method.getParameterTypes().length == 1;
        }

        private boolean isGetRegistry(Method method) {
            return method.getName().equals("getRegistry")
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

        private boolean isIsProperty(Method method) {
            String name = method.getName();
            return name.startsWith("is")
                    && name.length() > 2
                    && method.getParameterTypes().length == 0
                    && (method.getReturnType().equals(Boolean.class)
                        || method.getReturnType().equals(boolean.class));
        }

        private boolean isGetProperty(Method method) {
            String name = method.getName();
            return name.startsWith("get")
                    && name.length() > 3
                    && method.getParameterTypes().length == 0;
        }

        private boolean isSetProperty(Method method) {
            String name = method.getName();
            return name.startsWith("set")
                    && name.length() >= 4
                    && method.getParameterTypes().length == 1;
        }

        private Boolean isProperty(Method method) {
            String name = method.getName().substring("is".length());
            name = name.substring(0, 1).toLowerCase() + name.substring(1);

            return (Boolean) store.get(name);

        }

        private Object getProperty(Method method) {
            String name = getPropertyName(method);

            Class<?> retType = method.getReturnType();
            if (RegistryBean.class.isAssignableFrom(retType)) {
                return beanize(method, byName(name));
            }

            return store.get(name);
        }

        private String getPropertyName(Method method) {
            String name = method.getName().substring("get".length());
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
            return name;
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

        private Object beanize(Method method, IndexedRegistryListType listType, SimpleRegistry reg) {
            if (listType == null) {
                throw new AnnotationTypeMismatchException(
                        method,
                        "declare " + IndexedRegistryListType.class + " for " +
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
                    if (0 <= idx && idx < indexedSubregistries.size()) {
                        reg = indexedSubregistries.get(idx);
                    }
                } catch (NumberFormatException nfe){
                }
            }
            return reg;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof RegistryGettable) {
                RegistryGettable otherBean = (RegistryGettable) o;
                return SimpleRegistry.this.equals(otherBean.getRegistry());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return SimpleRegistry.this.hashCode();
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
    public String toString() {
        return Registries.stringify(this).toString();
    }

    private interface JavaDefaults {
        int hashCode();

        boolean equals(Object obj);

        Object clone() throws CloneNotSupportedException;

        String toString();
    }

    private static class StringCasterMap extends AbstractMap {
        final AbstractSet<Entry> set;
        private final SimpleRegistry registry;
        private final BasicTypeParser caster;

        public StringCasterMap(SimpleRegistry registry, BasicTypeParser caster) {
            this.registry = registry;
            this.caster = caster;
            set = new StringCasterEntrySet(registry, caster);
        }

        @Override
        public Set entrySet() {
            return set;
        }

        @Override
        public Object put(Object key, Object value) {
            String keyStr = caster.toString(key);
            return registry.put(keyStr, value);
        }

        private class StringCasterEntrySet extends AbstractSet<Entry> {
            private final SimpleRegistry registry;
            private final BasicTypeParser caster;

            public StringCasterEntrySet(SimpleRegistry registry, BasicTypeParser caster) {
                this.registry = registry;
                this.caster = caster;
            }

            @Override
            public Iterator<Entry> iterator() {
                final Iterator<Entry<String, Object>> it = registry.entrySet().iterator();

                return new StringCasterEntrySetIterator(it);
            }

            @Override
            public int size() {
                return registry.size();
            }


            private class StringCasterEntrySetIterator implements Iterator<Entry> {
                private final Iterator<Entry<String, Object>> it;

                public StringCasterEntrySetIterator(Iterator<Entry<String, Object>> it) {
                    this.it = it;
                }

                public boolean hasNext() {
                    return it.hasNext();
                }

                public Entry next() {
                    final Entry<String, Object> someEntry = it.next();
                    return new StringCasterEntry(someEntry);
                }

                public void remove() {
                }

                private class StringCasterEntry implements Entry {
                    private final Entry<String, Object> someEntry;

                    public StringCasterEntry(Entry<String, Object> someEntry) {
                        this.someEntry = someEntry;
                    }

                    public Object getKey() {
                        return caster.parse(someEntry.getKey());

                    }

                    public Object getValue() {
                        return someEntry.getValue();
                    }

                    public Object setValue(Object value) {
                        return someEntry.setValue(value);
                    }
                }
            }
        }
    }
}
