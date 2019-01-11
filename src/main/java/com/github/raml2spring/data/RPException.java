package com.github.raml2spring.data;

import com.sun.codemodel.JDefinedClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class RPException {

    private String name;
    private HttpStatus code;
    private TypeDeclaration typeDeclaration;
    private JDefinedClass definedClass;

}
