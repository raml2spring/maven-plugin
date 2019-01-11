package com.github.raml2spring.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class RPModel {

    private String basePackage;
    private String schemaLocation;

    private Map<String, RPEndpoint> endpoints = new HashMap<>();
    private Map<String, RPException> exceptions = new HashMap<>();
    private Map<String, RPEnum> enums = new HashMap<>();
    private Map<String, RPType> types = new HashMap<>();

}
