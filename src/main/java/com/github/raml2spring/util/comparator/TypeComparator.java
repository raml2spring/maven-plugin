package com.github.raml2spring.util.comparator;

import com.github.raml2spring.exception.RamlParseException;
import org.raml.v2.api.model.v10.datamodel.JSONTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TypeComparator implements Comparator<TypeDeclaration> {

    @Override
    public int compare(TypeDeclaration t1, TypeDeclaration t2) {
        if(t1 instanceof ObjectTypeDeclaration && t2 instanceof ObjectTypeDeclaration) {
            ObjectTypeDeclaration o1 = (ObjectTypeDeclaration)t1;
            ObjectTypeDeclaration o2 = (ObjectTypeDeclaration)t2;

            List<String> o1Types = getTypes(o1);
            List<String> o2Types = getTypes(o2);

            if(o1Types.contains(o2.name()) && o2Types.contains(o1.name())) {
                throw new RamlParseException("error: cycling dependency in types");
            }
            if(o1Types.contains(o2.name())) {
                return 1;
            } else if(o2Types.contains(o1.name())) {
                return -1;
            }

        } else if(t1 instanceof JSONTypeDeclaration && t2 instanceof JSONTypeDeclaration) {
            if(t1.type().equals(t2.name())) {
                return 1;
            } else if(t2.type().equals(t1.name())) {
                return -1;
            }
        }
        return 0;
    }

    private List<String> getTypes(ObjectTypeDeclaration o) {
        final List<String> oTypes = new ArrayList<>();
        if(!"object".equals(o.type())) {
            oTypes.add(o.type());
        }
        o.properties().forEach(property -> {
            if(property instanceof ObjectTypeDeclaration) {
                oTypes.add(property.type());
            }
        });
        return oTypes;
    }
}
