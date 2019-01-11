package com.github.raml2spring.util;

import com.github.raml2spring.configuration.Jsonschema2pojoConfig;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JVar;
import org.jsonschema2pojo.util.NameHelper;
import org.raml.v2.internal.utils.Inflector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class NamingHelper {

    private static String DEFAULT_NAME_SUFFIX = "_1";

    private static int MODE_PLURALIZE = 1;
    private static int MODE_SINGULARIZE = 2;

    private static NameHelper nameHelper;

    static {
        nameHelper = new NameHelper(new Jsonschema2pojoConfig());
    }

    static String getClassName(String name,Set<String> reservedList) {
        String newName = getClassName(name);
        if(reservedList != null) {
            while(reservedList.contains(newName)) {
                newName = addSuffix(newName);
            }
        }
        return newName;
    }

    static String getClassName(String name) {
        String newName = getJavaConformName(name);
        return newName.substring(0,1).toUpperCase().concat(newName.substring(1));
    }

    static String getMethodName(String name) {
        String newName = getJavaConformName(name);
        return newName.substring(0,1).toLowerCase().concat(newName.substring(1));
    }

    private static String getFieldName(String name) {
        String newName = getJavaConformName(name);
        return newName.substring(0,1).toLowerCase().concat(newName.substring(1));
    }

    static String getFieldNameForMethod(String name, JMethod method) {
        return getFieldNameForMethod(name, method, 0);
    }

    private static String getFieldNameForMethod(String name, JMethod method, int mode) {
        String newName = getFieldName(name);

        if((mode & MODE_SINGULARIZE) != 0) {
            newName = Inflector.singularize(newName);
        } else if((mode & MODE_PLURALIZE) != 0) {
            newName = Inflector.pluralize(newName);
        }

        if(method != null) {
            List<String> paramNames = method.params().stream().map(JVar::name).collect(Collectors.toList());
            while(paramNames.contains(newName)) {
                newName = addSuffix(newName);
            }
        }

        return newName;
    }

    static String getFieldNameForClass(String name, JDefinedClass clazz) {
        return getFieldNameForClass(name, clazz, 0);
    }

    private static String getFieldNameForClass(String name, JDefinedClass clazz, int mode) {
        String newName = getFieldName(name);

        if((mode & MODE_SINGULARIZE) != 0) {
            newName = Inflector.singularize(newName);
        } else if((mode & MODE_PLURALIZE) != 0) {
            newName = Inflector.pluralize(newName);
        }

        if(clazz != null) {

            List<String> paramNames = clazz.fields().entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
            while(paramNames.contains(newName)) {
                newName = addSuffix(newName);
            }
        }

        return newName;
    }

    private static String addSuffix(String name) {
        return addSuffix(name, DEFAULT_NAME_SUFFIX);
    }

    private static String addSuffix( String name, String suffix) {
        return name.concat(suffix);
    }

    private static String getJavaConformName(String name) {
        String newName = name;
        newName = nameHelper.replaceIllegalCharacters(newName);
        newName = nameHelper.normalizeName(newName);
        return newName;
    }

}
