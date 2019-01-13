# Raml2Spring Maven Plugin [![Build Status](https://travis-ci.org/raml2spring/maven-plugin.svg?branch=master)](https://travis-ci.org/raml2spring/maven-plugin) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.raml2spring/raml2spring-maven-plugin/badge.svg)](https://mvnrepository.com/artifact/com.github.raml2spring/raml2spring-maven-plugin) 
Generates Spring code from RAML.

## Prerequisites
[Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

## Usage 

```
<plugin>
<groupId>com.github.raml2spring</groupId>
<artifactId>raml2spring-maven-plugin</artifactId>
<version>x.x.x</version>
<configuration>
    <ramlPath>src/main/resources/api.raml</ramlPath>
    <basePackage>com.example</basePackage>
</configuration>
<executions>
    <execution>
        <id>generate-spring-endpoints</id>
        <phase>generate-sources</phase>
        <goals>
            <goal>generate</goal>
        </goals>
    </execution>
</executions>
</plugin>
```
            
