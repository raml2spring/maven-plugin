package com.github.raml2spring.configuration;

import com.github.raml2spring.plugin.Raml2SpringMojo;
import org.apache.maven.plugin.logging.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public class Raml2SpringConfig {

    private static Raml2SpringMojo raml2SpringMojo;
    private static String _schemaLocation;

    public static void setMojo(Raml2SpringMojo raml2SpringMojo) {
        Raml2SpringConfig.raml2SpringMojo = raml2SpringMojo;
    }

    public static String getBasePackage() {
        if(raml2SpringMojo != null) {
            return raml2SpringMojo.basePackage;
        }
        return "";
    }

    public static String getOutputPath() {
        if(raml2SpringMojo != null) {
            return raml2SpringMojo.outputPath;
        }
        return "";
    }

    public static String getSchemaLocation() {
        return _schemaLocation;
    }

    public static void setSchemaLocation(String schemaLocation) {
        _schemaLocation = schemaLocation;
    }

    public static Log getLog() {
        if(raml2SpringMojo != null) {
            return raml2SpringMojo.getLog();
        }
        return null;
    }

    public static String getDateType() {
        if(raml2SpringMojo != null && raml2SpringMojo.useOldJavaDate) {
            return "java.util.Date";
        }
        return "java.time.LocalDate";
    }

    public static String getTimeType() {
        if(raml2SpringMojo != null && raml2SpringMojo.useOldJavaDate) {
            return "java.util.Date";
        }
        return "java.time.LocalTime";
    }

    public static String getDateTimeType() {
        if(raml2SpringMojo != null && raml2SpringMojo.useOldJavaDate) {
            return "java.util.Date";
        }
        return "java.time.LocalDateTime";
    }

    public static Class getDateClass() {
        if(raml2SpringMojo != null && raml2SpringMojo.useOldJavaDate) {
            return Date.class;
        }
        return LocalDate.class;
    }

    public static Class getTimeClass() {
        if(raml2SpringMojo != null && raml2SpringMojo.useOldJavaDate) {
            return Date.class;
        }
        return LocalTime.class;
    }

    public static Class getDateTimeClass() {
        if(raml2SpringMojo != null && raml2SpringMojo.useOldJavaDate) {
            return Date.class;
        }
        return LocalDateTime.class;
    }

}
