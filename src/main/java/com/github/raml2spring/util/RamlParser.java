package com.github.raml2spring.util;

import com.github.raml2spring.data.*;
import com.github.raml2spring.util.comparator.TypeComparator;
import com.sun.codemodel.*;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.api.Library;
import org.raml.v2.api.model.v10.bodies.Response;
import org.raml.v2.api.model.v10.datamodel.JSONTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.raml.v2.api.model.v10.methods.Method;
import org.raml.v2.api.model.v10.resources.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RamlParser {

    private Api ramlApi;

    public RamlParser(String ramlPath) {
        RamlModelResult ramlModel = new RamlModelBuilder().buildApi(ramlPath);
        if(ramlModel.hasErrors()) {
            StringBuilder validationErros = new StringBuilder();
            ramlModel.getValidationResults().forEach(result -> validationErros.append(result.getMessage()));
            throw new RuntimeException("RAML validation error: " + validationErros.toString());
        }
        ramlApi = ramlModel.getApiV10();
    }

    public RPModel readModel(RPModel rpModel) {
        rpModel.setTypes(getTypes(rpModel));
        rpModel = getEndpoints(rpModel);
        return rpModel;
    }

    public Map<String, RPType> getTypes(RPModel rpModel) {
        readUses(rpModel, ramlApi.uses());
        readTypes(rpModel, ramlApi.types());

        return rpModel.getTypes();
    }

    private void readUses(RPModel rpModel, List<Library> uses) {
        uses.forEach(library -> {
            readUses(rpModel, library.uses());
            readTypes(rpModel, library.types());
        });
    }

    private void readTypes(RPModel rpModel, List<TypeDeclaration> typeDeclarations) {
        //sort object and json types
        typeDeclarations.sort(new TypeComparator());

        typeDeclarations.forEach(typeDeclaration -> {

            JCodeModel typeModel = new JCodeModel();
            String typeName = typeDeclaration.name();  //NamingHelper.getClassName(typeDeclaration.name());
            JType pojo;
            if(typeDeclaration instanceof JSONTypeDeclaration && !typeDeclaration.type().startsWith("{")) {
                pojo = RamlTypeHelper.extendFromJSON(typeModel, typeDeclaration, rpModel, rpModel.getBasePackage() + ".model");
            } else {
                pojo = RamlTypeHelper.generateType(typeModel, typeDeclaration, rpModel, rpModel.getBasePackage() + ".model", typeName);
            }

            rpModel.getTypes().put(typeName, new RPType(typeName, pojo, typeModel, typeDeclaration));
        });
    }

    public RPModel getEndpoints(RPModel rpModel) {
        Map<String, RPEndpoint> endpoints = rpModel.getEndpoints();
        //String baseUri;
        AtomicReference<String> baseUri = new AtomicReference<>();
        if(ramlApi.baseUri() != null) {
            //baseUri = ramlApi.baseUri().value();
            baseUri.set(ramlApi.baseUri().value());
            if(ramlApi.version() != null) {
                //baseUri = baseUri.replaceAll("\\{version\\}", ramlApi.version().value());
                baseUri.set(baseUri.get().replaceAll("\\{version\\}", ramlApi.version().value()));
            }
        }

        ramlApi.resources().forEach(resource -> {

            //TODO Naming
            String endpointName = NamingHelper.getClassName(resource.displayName().value()) + "Api";

            RPEndpoint endpoint = new RPEndpoint();
            endpoint.setName(endpointName);
            endpoint.setBaseUri(baseUri.get());
            readResource(resource, endpoint, rpModel);
            endpoints.put(endpointName, endpoint);

        });

        return rpModel;
    }

    private void readResource(Resource resource, RPEndpoint endpoint, RPModel model) {

        List<String> uriParams = getPathParameters(resource.resourcePath());
        Map<String, RPException> exceptions = model.getExceptions();

        String description = null;
        if(resource.description() != null && StringUtils.hasText(resource.description().value())) {
            description = resource.description().value();
        }

        //get methods
        for (Method method : resource.methods()) {
            RPMethod rpMethod = new RPMethod();
            //TODO Naming
            String methodName = method.method().toLowerCase() + NamingHelper.getClassName(resource.displayName().value());

            rpMethod.setName(methodName);
            rpMethod.setUri(resource.resourcePath());
            rpMethod.setMethod(method.method().toUpperCase());
            rpMethod.setDescription(description);

            //add uriParams
            rpMethod.setUriParams(uriParams);

            //read queryParams
            if(method.queryParameters() != null) {
                rpMethod.setQueryParams(method.queryParameters());
            }

            //read headerParams
            if(method.headers() != null) {
                rpMethod.setHeaderParams(method.headers());
            }

            //read body
            if(method.body() != null && method.body().size() > 0) {
                rpMethod.setBody(method.body().get(0));
            }

            //read responses
            List<String> producesCommon = new ArrayList<>();
            for(Response response : method.responses()) {
                int code = Integer.parseInt(response.code().value());

                for(TypeDeclaration type : response.body()) {
                    if(!producesCommon.contains(type.name())) {
                        producesCommon.add(type.name());
                    }
                }

                if(rpMethod.getResponseStatus() == null) {
                    rpMethod.setResponseStatus(HttpStatus.valueOf(code));
                    rpMethod.setReturnType(getReturnTypeDeclaration(response.body()));
                } else if(rpMethod.getResponseStatus().value() != HttpStatus.OK.value()) {
                    if(rpMethod.getResponseStatus().value() > code) {
                        rpMethod.setResponseStatus(HttpStatus.valueOf(code));
                        rpMethod.setReturnType(getReturnTypeDeclaration(response.body()));
                    }
                }

                if(HttpStatus.valueOf(code).is4xxClientError() || HttpStatus.valueOf(code).is5xxServerError()) {
                    TypeDeclaration typeDeclaration = getReturnTypeDeclaration(response.body());
                    if(typeDeclaration instanceof JSONTypeDeclaration ||
                            typeDeclaration instanceof ObjectTypeDeclaration) {
                        String name = "Error".concat(response.code().value()); //methodName.concat(response.code().value());
                        if(response.description() != null && StringUtils.hasText(response.description().value())) {
                            name = response.description().value();
                        }
                        name = NamingHelper.getClassName(name) + "Exception";

                        if(exceptions.containsKey(name)) {
                            if(!exceptions.get(name).getTypeDeclaration().type().equals(typeDeclaration.type())) {
                                name = NamingHelper.getClassName(name, exceptions.keySet());
                            }
                        }
                        exceptions.put(name, new RPException(name, HttpStatus.valueOf(code), typeDeclaration, null));
                        rpMethod.getHandledExceptions().add(name);
                    } /*else {
                        //"WARNING: response error should be of type Object or JSON"
                    }*/

                }
            }

            rpMethod.setProduces(producesCommon);

            endpoint.getMethods().add(rpMethod);

            if(method.body().size() > 1) {
                for(int i = 1; i < method.body().size(); i++) {

                    RPMethod rpMethodClone = rpMethod.toBuilder().body(method.body().get(i)).build();
                    endpoint.getMethods().add(rpMethodClone);
                }
            }
        }
        resource.resources().forEach(childResource ->
            readResource(childResource, endpoint, model)
        );
    }

    private List<String> getPathParameters(String path) {
        List<String> pathParams = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{([a-zA-Z0-9]*)\\}");
        Matcher matcher = pattern.matcher(path);

        while(matcher.find()) {
            pathParams.add(matcher.group(1));
        }
        return pathParams;
    }

    private TypeDeclaration getReturnTypeDeclaration(List<TypeDeclaration> typeDeclarations) {
        final String preferedType = "application/json";
        TypeDeclaration returnType = null;
        for(TypeDeclaration typeDeclaration : typeDeclarations) {
            if(preferedType.equals(typeDeclaration.name())) {
                returnType = typeDeclaration;
                break;
            }
        }
        if(returnType == null) {
            returnType = typeDeclarations.get(0);
        }
        return returnType;
    }


}
