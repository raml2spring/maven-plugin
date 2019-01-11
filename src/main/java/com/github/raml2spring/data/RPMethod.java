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
    private HttpStatus responseStatus;
    private TypeDeclaration returnType;
    //@Builder.Default
    //@Singular
    private List<String> produces = new ArrayList<>();
    //@Builder.Default
    //@Singular
    private List<TypeDeclaration> queryParams = new ArrayList<>();
    //@Builder.Default
    //@Singular
    private List<TypeDeclaration> headerParams = new ArrayList<>();
    //@Builder.Default
    //@Singular
    private List<String> uriParams = new ArrayList<>();
    //@Builder.Default
    //@Singular
    private List<String> handledExceptions = new ArrayList<>();
    private TypeDeclaration body;

}
