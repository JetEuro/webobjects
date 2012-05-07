package registry;

import java.util.ArrayList;
import java.util.List;
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

    public static <T extends RegistryBean> T newBean(Class<T> clazz) {
        return new SimpleRegistry().bean(clazz);
    }

    public static Map<String, Registry> iterate(Registry root) {
        Map<String, Registry> treeMap = new TreeMap<String, Registry>();
        iterate0(root, "", treeMap);
        return treeMap;
    }

    public static Map<String, Object> liniarize(Registry root) {
        Map<String, Object> treeMap = new TreeMap<String, Object>();
        liniarize0(root, "", treeMap);
        return treeMap;
    }

    public static Map<String, String> stringify(Registry root) {
        Map<String, Object> lin = liniarize(root);
        Map<String, String> treeMap = new TreeMap<String, String>();
        for (String name : lin.keySet()) {
            treeMap.put(name, lin.get(name).toString());
        }
        return treeMap;
    }

    private static void iterate0(Registry node,
                                 String pathString,
                                 Map<String, Registry> result) {
        for (String name : node.getSubkeys()) {
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
        for (String name : node.getSubkeys()) {
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

    private static String pathToName(String pathString, String name) {
        return pathString.isEmpty() ? name : pathString + "." + name;
    }

    public static void putMassivly(Registry registry, Map<String, Object> values) {
        for (String path : values.keySet()) {
            String []arr = path.split("\\.");
            Registry reg = registry;
            for (int i = 0; i < arr.length; i++) {
                if (i == arr.length - 1) {
                    reg.put(arr[i], values.get(path));
                } else {
                    reg = reg.byName(arr[i]);
                }
            }
        }
    }
}
