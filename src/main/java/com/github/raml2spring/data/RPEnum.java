package com.github.raml2spring.data;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class RPEnum {

    private final String name;
    private final JType type;
    private final JCodeModel model;
    private List<String> items = new ArrayList<>();
}
