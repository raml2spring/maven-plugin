package com.github.raml2spring.data;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

@Data
@AllArgsConstructor
public class RPType {

    private final String name;
    private final JType type;
    private final JCodeModel model;
    private final TypeDeclaration typeDeclaration;

}
