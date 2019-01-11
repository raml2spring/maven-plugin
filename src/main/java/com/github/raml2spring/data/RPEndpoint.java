package com.github.raml2spring.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class RPEndpoint {

    private String baseUri;
    private String name;
    private List<RPMethod> methods = new ArrayList<>();

}
