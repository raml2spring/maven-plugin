package com.github.raml2spring.configuration;

import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;

public class Jsonschema2pojoConfig extends DefaultGenerationConfig {

    @Override
    public boolean isGenerateBuilders() { // set config option by
        return true;
    }

    @Override
    public boolean isIncludeAdditionalProperties() {
        return false;
    }

    @Override
    public boolean isIncludeDynamicAccessors() {
        return false;
    }

    @Override
    public boolean isSerializable() { // set config option by
        return true;
    }

    @Override
    public String getDateTimeType() {
        return Raml2SpringConfig.getDateTimeType();
    }

    @Override
    public String getDateType() {
        return Raml2SpringConfig.getDateType();
    }

    @Override
    public String getTimeType() {
        return Raml2SpringConfig.getTimeType();
    }

//    @Override
//    public boolean isUseLongIntegers() {

//    }

    public SchemaMapper getDefaultSchemaMapper() {
        RuleFactory ruleFactory = new RuleFactory(this, new Jackson2Annotator(this), new SchemaStore());
        return new SchemaMapper(ruleFactory, new SchemaGenerator(
        ));
    }

}
