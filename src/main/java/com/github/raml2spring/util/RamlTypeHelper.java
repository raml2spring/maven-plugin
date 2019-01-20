package com.github.raml2spring.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.raml2spring.configuration.Jsonschema2pojoConfig;
import com.github.raml2spring.configuration.Raml2SpringConfig;
import com.github.raml2spring.data.RPEnum;
import com.github.raml2spring.data.RPModel;
import com.github.raml2spring.exception.RamlParseException;
import com.github.raml2spring.data.RPType;
import com.sun.codemodel.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jsonschema2pojo.Jsonschema2Pojo;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.exception.GenerationException;
import org.raml.v2.api.model.v10.datamodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

class RamlTypeHelper {

    private static JType getType(JCodeModel codeModel, TypeDeclaration typeDeclaration, RPModel rpModel) {
        JType type = null;
        Map<String, RPType> types = rpModel.getTypes();
        if (typeDeclaration instanceof ArrayTypeDeclaration) {
            ArrayTypeDeclaration arrayTypeDeclaration = (ArrayTypeDeclaration) typeDeclaration;
            JType baseType = getType(codeModel, arrayTypeDeclaration.items(), rpModel);
            JClass list = codeModel.ref(List.class);

            type = list.narrow(baseType);
            //throw new RuntimeException("type unsupported:" + typeDeclaration.name());
        }else if(typeDeclaration instanceof JSONTypeDeclaration) {
            if (types.containsKey(typeDeclaration.type())) {
                type = types.get(typeDeclaration.type()).getType();
            } /*else {
                //throw new RuntimeException("JSONType not found");
            }*/
        } else if (typeDeclaration instanceof ObjectTypeDeclaration) {
            String objectType = "object".equals(typeDeclaration.type()) ? typeDeclaration.name() : typeDeclaration.type();
            if (types.containsKey(objectType)) {
                type = types.get(objectType).getType();
            }/* else {
                //throw new RuntimeException("ObjectType not found");
            }*/
        //} else if (typeDeclaration == null || typeDeclaration instanceof NullTypeDeclaration) {
            //throw new RuntimeException("error paramType: null");
            //type = new JCodeModel().VOID;
        } else if (typeDeclaration instanceof UnionTypeDeclaration) {
            //for example "number?" or "number | nil" supported
            UnionTypeDeclaration unionTypeDeclaration = (UnionTypeDeclaration)typeDeclaration;
            if(unionTypeDeclaration.of().size() != 2 || !(unionTypeDeclaration.of().get(1) instanceof NullTypeDeclaration)) {
                throw new RamlParseException("Unsupported type: ".concat(unionTypeDeclaration.type()));
            }
            type = getType(codeModel, unionTypeDeclaration.of().get(0), rpModel);
        } else if (typeDeclaration instanceof StringTypeDeclaration) {
            StringTypeDeclaration stringTypeDeclaration = (StringTypeDeclaration) typeDeclaration;
            if(stringTypeDeclaration.enumValues().size() > 0) {
                Map<String, RPEnum> enums = rpModel.getEnums();
                RPEnum foundEnum = null;

                for(RPEnum rpEnum : enums.values()) {
                    if (new HashSet(rpEnum.getItems()).equals(new HashSet(((StringTypeDeclaration) typeDeclaration).enumValues()))) {
                        foundEnum = rpEnum;
                        break;
                    }
                }

                if(foundEnum == null) {
                    try {
                        //JCodeModel enumCodeModel = new JCodeModel();
                        String enumName = NamingHelper.getClassName(stringTypeDeclaration.name()) + "Enum";
                        JPackage jp; // = codeModel.packages().next(); //codeModel._package(SpringRamlPluginConfig.getBasePackage() + ".model");
                        if(codeModel.packages().hasNext()) {
                            jp = codeModel.packages().next();
                        } else {
                            jp = codeModel._package(Raml2SpringConfig.getBasePackage() + ".model");
                        }

                        JDefinedClass enumClass = jp._enum(enumName);
                        stringTypeDeclaration.enumValues().forEach(enumClass::enumConstant);//enumClass.enumConstant(value));
                        foundEnum = new RPEnum(enumName, enumClass, codeModel, stringTypeDeclaration.enumValues());
                        enums.put(enumName, foundEnum);
                    } catch (JClassAlreadyExistsException e) {
                        throw new RuntimeException("error creating enum");
                    }
                }

                type = foundEnum.getType();

            } else {
                type = codeModel.directClass("String");
            }
        } else if (typeDeclaration instanceof BooleanTypeDeclaration) {
            type = codeModel.directClass("Boolean");
        } else if (typeDeclaration instanceof NumberTypeDeclaration) {
            NumberTypeDeclaration numberTypeDeclaration = (NumberTypeDeclaration) typeDeclaration;
            type = codeModel.directClass(getNumberClass(numberTypeDeclaration));
        } else if (typeDeclaration instanceof FileTypeDeclaration) {
            type = codeModel._ref(MultipartFile.class);
        } else if (typeDeclaration instanceof DateTypeDeclaration) {
            //type = codeModel._ref(Date.class);
            type = codeModel._ref(Raml2SpringConfig.getDateClass());
        } else if(typeDeclaration instanceof TimeOnlyTypeDeclaration) {
            //type = codeModel._ref(Date.class);
            type = codeModel._ref(Raml2SpringConfig.getTimeClass());
        } else if(typeDeclaration instanceof DateTimeOnlyTypeDeclaration ||
                    typeDeclaration instanceof DateTimeTypeDeclaration) {
            //type = codeModel._ref(Date.class);
            type = codeModel._ref(Raml2SpringConfig.getDateTimeClass());
        } else {
            //throw new RuntimeException("type unsupported:" + typeDeclaration.name());
            throw new RamlParseException("Unsupported type: ".concat(typeDeclaration.name()));
        }
        return type;
    }

    private static String getNumberClass(NumberTypeDeclaration numberTypeDeclaration) {
        String format = numberTypeDeclaration.format() != null ? numberTypeDeclaration.format().toLowerCase() : "";
        if (format.equals("int64") || format.equals("long")) {
            return "Long";
        } else if (format.equals("int32") || format.equals("int")) {
            return "Integer";
        } else if (format.equals("int16") || format.equals("int8")) {
            return "Short";
        } else if (format.equals("double") || format.equals("float")) {
            return "Double";
        } else if (numberTypeDeclaration instanceof IntegerTypeDeclaration) {
            return "Long";
        } else {
            return "Double";
        }
    }

    static JType generateType(JCodeModel typeModel, TypeDeclaration typeDeclaration, RPModel rpModel, String packagePath, String typeName) {
        if(typeDeclaration instanceof JSONTypeDeclaration) {
            return buildModel(typeModel, packagePath, typeName, typeDeclaration.type(), Raml2SpringConfig.getSchemaLocation());
        } else if (typeDeclaration instanceof ObjectTypeDeclaration){
            return RamlTypeHelper.getObject(typeModel, typeDeclaration, rpModel, packagePath, typeName);
        } else if (typeDeclaration instanceof StringTypeDeclaration) {
            return RamlTypeHelper.getType(typeModel, typeDeclaration, rpModel);
        }
        return null;
    }

    static JType getTypeSave(JCodeModel codeModel, TypeDeclaration typeDeclaration, RPModel rpModel, String newTypeName) {
        //TODO add warning???
        JType type = getType(codeModel, typeDeclaration, rpModel);
        if(type == null) {
            //Raml2SpringConfig.getLog().warn("Please use types:".concat(typeDeclaration.name()));
            String newTypeNameChecked = NamingHelper.getClassName(newTypeName, rpModel.getTypes().keySet());
            type = generateType(codeModel, typeDeclaration, rpModel,
                    Raml2SpringConfig.getBasePackage() + ".model", newTypeNameChecked);
            rpModel.getTypes().put(newTypeNameChecked, new RPType(newTypeNameChecked, type, codeModel, typeDeclaration));
        }
        return type;
    }

    static JClass extendFromJSON(JCodeModel codeModel, TypeDeclaration typeDeclaration, RPModel rpModel, String packagePath) {
        try {
            JPackage jp = codeModel._package(packagePath);
            JDefinedClass jc = jp._class(NamingHelper.getClassName(typeDeclaration.name()));
            addSerializeable(jc);

            RPType objType = rpModel.getTypes().get(typeDeclaration.type());
            jc._extends(objType.getType().boxify());

            //ObjectStreamClass.lookup(jc.getClass());
            //jc.field(JMod.STATIC | JMod.FINAL, Long.class, "serialVersionUID", JExpr.lit(new Random(System.currentTimeMillis()).nextLong()));
            return jc;
        } catch (JClassAlreadyExistsException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void addSerializeable(JDefinedClass jclass) {
        jclass._implements(Serializable.class);
        jclass.field(JMod.STATIC | JMod.FINAL, Long.class, "serialVersionUID", JExpr.lit(new Random(System.currentTimeMillis()).nextLong()));
    }

    private static JClass getObject(JCodeModel codeModel, TypeDeclaration typeDeclaration, RPModel rpModel, String packagePath, String typeName) {
        if(typeDeclaration instanceof  ObjectTypeDeclaration) {
            try {
                ObjectTypeDeclaration objectTypeDeclaration = (ObjectTypeDeclaration) typeDeclaration;
                //JCodeModel codeModel = new JCodeModel();
                JPackage jp = codeModel._package(packagePath);
                JDefinedClass jc = jp._class(NamingHelper.getClassName(typeName));

                addSerializeable(jc);
                final Collection<String> parentFields = new ArrayList<>();
                boolean hasSuperclass = false;

                if(!"object".equals(objectTypeDeclaration.type())) {
                    hasSuperclass = true;
                    RPType objType = rpModel.getTypes().get(objectTypeDeclaration.type());
                    jc._extends(objType.getType().boxify());
                    readParentFields(objType, parentFields, rpModel);
//                    parentFields.addAll(objType.getModel()
//                            .packages().next()
//                            .classes().next()
//                            .fields().values()
//                            .stream().map(jFieldVar -> jFieldVar.name()).collect(Collectors.toList()));

                }

                final Collection<JFieldVar> properties = new ArrayList<>();
                objectTypeDeclaration.properties().forEach(property -> {
                    //TODO Naming
                    if(parentFields.contains(property.name())) {
                        return;
                    }
                    JType propType = getType(codeModel, property, rpModel);
                    JFieldVar prop = jc.field(JMod.PRIVATE, propType, property.name());

                    JMethod getMethod = jc.method(JMod.PUBLIC, propType, "get" + NamingHelper.getClassName(property.name()));
                    getMethod.body()._return(prop);

                    JMethod setMethod = jc.method(JMod.PUBLIC, codeModel.VOID, "set" + NamingHelper.getClassName(property.name()));
                    setMethod.param(prop.type(), prop.name());
                    setMethod.body().assign(JExpr._this().ref(prop.name()), JExpr.ref(prop.name()));

                    properties.add(prop);
                });
                addToStringMethod(jc, properties, objectTypeDeclaration, hasSuperclass);
                addHashCodeMethod(jc, properties, objectTypeDeclaration, hasSuperclass);
                addEqualsMethod(jc, properties, objectTypeDeclaration, hasSuperclass);
                return jc;

            } catch (JClassAlreadyExistsException e) {
                e.printStackTrace();
            }

            return null;
        } else {
            throw new RuntimeException("typeDeclaration not object");
        }
    }

    private static void readParentFields(RPType objType, Collection<String> parentFields, RPModel rpModel) {
        parentFields.addAll(objType.getModel()
                .packages().next()
                .classes().next()
                .fields().values()
                .stream().map(jFieldVar -> jFieldVar.name()).collect(Collectors.toList()));
        if(!"object".equals(objType.getTypeDeclaration().type())) {
            RPType pobjType = rpModel.getTypes().get(objType.getTypeDeclaration().type());
            readParentFields(pobjType, parentFields, rpModel);
        }

    }

    private static void addToStringMethod(JDefinedClass jc, Collection<JFieldVar> properties, ObjectTypeDeclaration objectTypeDeclaration, boolean hasSuperclass) {
        JMethod hashCode = jc.method(JMod.PUBLIC, String.class, "toString");
        hashCode.annotate(Override.class);
        //TODO commons3???
        JClass hashCodeBuilderRef = jc.owner().ref(ToStringBuilder.class);
        JInvocation invocation = JExpr._new(hashCodeBuilderRef).arg(JExpr._this());
        if(hasSuperclass) {
            invocation = invocation.invoke("appendSuper").arg(JExpr._super().invoke("toString"));
        }
        for(JFieldVar property : properties) {
            invocation = invocation.invoke("append").arg(JExpr.lit(property.name())).arg(property);
        }
        hashCode.body()._return(invocation.invoke("toString"));
    }

    private static void addHashCodeMethod(JDefinedClass jc, Collection<JFieldVar> properties, ObjectTypeDeclaration objectTypeDeclaration, boolean hasSuperclass) {
        JMethod hashCode = jc.method(JMod.PUBLIC, int.class, "hashCode");
        hashCode.annotate(Override.class);
        //TODO commons3???
        JClass hashCodeBuilderRef = jc.owner().ref(HashCodeBuilder.class);
        JInvocation invocation = JExpr._new(hashCodeBuilderRef);
        if(hasSuperclass) {
            invocation = invocation.invoke("appendSuper").arg(JExpr._super().invoke("hashCode"));
        }
        for(JFieldVar property : properties) {
            invocation = invocation.invoke("append").arg(property);
        }
        hashCode.body()._return(invocation.invoke("toHashCode"));
    }

    private static void addEqualsMethod(JDefinedClass jc, Collection<JFieldVar> properties, ObjectTypeDeclaration objectTypeDeclaration, boolean hasSuperclass) {
        JMethod equals = jc.method(JMod.PUBLIC, boolean.class, "equals");
        equals.annotate(Override.class);
        JVar otherObject = equals.param(Object.class, "other");

        JBlock body = equals.body();

        body._if(otherObject.eq(JExpr._null()))._then()._return(JExpr.FALSE);
        body._if(otherObject.eq(JExpr._this()))._then()._return(JExpr.TRUE);
        body._if(JExpr._this().invoke("getClass").ne(otherObject.invoke("getClass")))._then()._return(JExpr.FALSE);

        JVar otherObjectVar = body.decl(jc, "otherObject").init(JExpr.cast(jc, otherObject));

        //TODO commons3???
        JClass equalsBuilderRef = jc.owner().ref(EqualsBuilder.class);

        JInvocation invocation = JExpr._new(equalsBuilderRef);
        if(hasSuperclass) {
            invocation = invocation.invoke("appendSuper").arg(JExpr._super().invoke("equals").arg(otherObject));
        }
        for(JFieldVar property : properties) {
            invocation = invocation.invoke("append").arg(JExpr.lit(property.name())).arg(otherObjectVar.ref(property.name()));
        }

        body._return(invocation.invoke("isEquals"));
    }

    private static JType buildModel(JCodeModel codeModel, String basePackage, String name, String schema, String schemaLocation) {

        Jsonschema2pojoConfig jsonschema2pojoConfig = new Jsonschema2pojoConfig();
        SchemaMapper mapper = jsonschema2pojoConfig.getDefaultSchemaMapper(); //new SchemaMapper(ruleFactory, new SchemaGenerator());
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(schema);
            // ************************
            // * workaround jsonschema2pojo issue -
            // * we have to remove extends-property and handle it at our own
            // ************************
            if(jsonNode.has("extends")) {
                JsonNode extendsNode = jsonNode.get("extends");
                Schema extendsSchema = new Schema(new URI(schemaLocation), null, null);
                extendsSchema = new SchemaStore().create(extendsSchema, extendsNode.get("$ref").asText(), jsonschema2pojoConfig.getRefFragmentPathDelimiters());
                ((ObjectNode)extendsSchema.getContent()).remove("extends");
                JType refType = mapper.generate(codeModel, nameFromRef(extendsNode.get("$ref").asText(), jsonschema2pojoConfig),
                        basePackage, extendsSchema.getContent().toString(), new URI(schemaLocation));

                ((ObjectNode)jsonNode).remove("extends");
                JType returnType = mapper.generate(codeModel, name, basePackage, jsonNode.toString(), new URI(schemaLocation));

                JDefinedClass defClass = codeModel._getClass(returnType.fullName());
                defClass._extends(refType.boxify());
                defClass.methods().removeIf(method -> "toString".equals(method.name()) || "hashCode".equals(method.name()) || "equals".equals(method.name()));
                Collection<JFieldVar> fields = getFieldsFromClass(defClass);
                addToStringMethod(defClass, fields, null, true);
                addHashCodeMethod(defClass, fields, null, true);
                addEqualsMethod(defClass, fields, null, true);
                return defClass;
            }
            return mapper.generate(codeModel, name, basePackage, jsonNode.toString(), new URI(schemaLocation));
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        //return codeModel;
        return null;
    }

    private static String nameFromRef(String ref, Jsonschema2pojoConfig jsonschema2pojoConfig) {
        if ("#".equals(ref)) {
            return null;
        } else {
            String nameFromRef;
            if (!StringUtils.contains(ref, "#")) {
                nameFromRef = Jsonschema2Pojo.getNodeName(ref, jsonschema2pojoConfig);
            } else {
                String[] nameParts = StringUtils.split(ref, "/\\#");
                nameFromRef = nameParts[nameParts.length - 1];
            }

            try {
                return URLDecoder.decode(nameFromRef, "utf-8");
            } catch (UnsupportedEncodingException var4) {
                throw new GenerationException("Failed to decode ref: " + ref, var4);
            }
        }
    }

    private static Collection<JFieldVar> getFieldsFromClass(JDefinedClass clazz) {
        final Collection<JFieldVar> temp = new ArrayList<JFieldVar>();
        clazz.fields().forEach((name, field) -> {
            if((field.mods().getValue() & JMod.STATIC) == 0) {
                temp.add(field);
            }
        });
        return temp;
    }
}
