package org.webobjects.registry;

import java.util.Map;
import java.util.TreeMap;

/**
 * User: cap_protect
 * Date: 5/7/12
 * Time: 9:25 AM
 */
public class Registries {
    public static Registry newRegistry() {
        return new SimpleRegistry();
    }

    public static Registry newRegistry(Map<String,Object> liniarizedData) {
        Registry registry = newRegistry();
        putMassivly(registry, liniarizedData);
        return registry;
    }

    public static <T extends RegistryBean> T newBean(Class<T> clazz) {
        return new SimpleRegistry().bean(clazz);
    }

    public static Map<String, Registry> iterate(Registry root) {
        Map<String, Registry> treeMap = new TreeMap<String, Registry>();
        iterate0(root, "", treeMap);
        return treeMap;
    }

    public static Map<String, Object> liniarize(Registry root, String[] path) {
        Map<String, Object> treeMap = new TreeMap<String, Object>();
        liniarize0(root, pathToName("", path), treeMap);
        return treeMap;
    }

    public static Map<String, Object> liniarize(Registry root) {
        return liniarize(root, new String[0]);
    }

    public static Map<String, String> stringify(Registry root) {
        Map<String, Object> lin = liniarize(root, new String[0]);
        Map<String, String> treeMap = new TreeMap<String, String>();
        for (String name : lin.keySet()) {
            treeMap.put(name, lin.get(name).toString());
        }
        return treeMap;
    }

    private static void iterate0(Registry node,
                                 String pathString,
                                 Map<String, Registry> result) {
        for (String name : node.getNamedSubregistriesKeys()) {
            Registry value = node.byName(name);
            String newPath = pathToName(pathString, name);
            result.put(newPath, value);
            iterate0(value, newPath, result);
        }
        int count = node.getIndexedSubregistriesCount();
        for (int i = 0; i < count; i++) {
            String newPath = pathToName(pathString, Integer.toString(i));
            Registry value = node.atIndex(i);
            result.put(newPath, value);
            iterate0(value, newPath, result);
        }
    }



    private static void liniarize0(Registry node,
                                 String pathString,
                                 Map<String, Object> result) {
        for (String name : node.getNamedSubregistriesKeys()) {
            String newPath = pathToName(pathString, name);
            Registry value = node.byName(name);
            liniarize0(value, newPath, result);
        }
        int count = node.getIndexedSubregistriesCount();
        for (int i = 0; i < count; i++) {
            String newPath = pathToName(pathString, Integer.toString(i));
            Registry value = node.atIndex(i);
            liniarize0(value, newPath, result);
        }

        for (String key : node.keySet()) {
            String newPath = pathToName(pathString, key);
            result.put(newPath, node.get(key));
        }
    }

    private static String pathToName(String pathString, String ...names) {
        StringBuilder builder = new StringBuilder();
        builder.append(pathString);
        for (String name : names) {
            if (pathString.length() > 0 || builder.length() > 0) {
                builder.append(".");
            }
            builder.append(name);
        }
        return builder.toString();
    }

    public static void putMassivly(Registry registry, Map<String, Object> liniarizedData) {
        for (String path : liniarizedData.keySet()) {
            String []arr = path.split("\\.");
            Registry reg = registry;
            for (int i = 0; i < arr.length; i++) {
                if (i == arr.length - 1) {
                    reg.put(arr[i], liniarizedData.get(path));
                } else {
                    reg = reg.byName(arr[i]);
                }
            }
        }
    }

    public static void putMassivlyWithPrefix(Registry registry, Map<String, Object> liniarizedData, String[] prefix) {
        for (String path : liniarizedData.keySet()) {
            String []arr = path.split("\\.");
            if (!isSubPath(arr, prefix)) {
                continue;
            }
            Registry reg = registry;
            for (int i = prefix.length; i < arr.length; i++) {
                if (i >= prefix.length && i == arr.length - 1) {
                    reg.put(arr[i], liniarizedData.get(path));
                } else {
                    reg = reg.byName(arr[i]);
                }
            }
        }

    }

    private static boolean isSubPath(String[] arr, String []path) {
        for (int i = 0; i < path.length; i++) {
            if (i > arr.length) {
                return false;
            } else if (!arr[i].equals(path[i])) {
                return false;
            }
        }

        return true;
    }
}
