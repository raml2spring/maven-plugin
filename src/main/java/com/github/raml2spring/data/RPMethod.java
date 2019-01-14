package com.github.raml2spring.data;

import lombok.*;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder=true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RPMethod {

    private String name;
    private String method;
    private String uri;
    private String description;
    private HttpStatus responseStatus;
    private TypeDeclaration returnType;
    private List<String> produces = new ArrayList<>();
    private List<TypeDeclaration> queryParams = new ArrayList<>();
    private List<TypeDeclaration> headerParams = new ArrayList<>();
    private List<String> uriParams = new ArrayList<>();
    private List<String> handledExceptions = new ArrayList<>();
    private TypeDeclaration body;

}
