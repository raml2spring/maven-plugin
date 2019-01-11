package com.github.raml2spring.util;

import com.github.raml2spring.data.RPModel;
import com.sun.codemodel.*;
import org.raml.v2.api.model.v10.datamodel.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.Map;

class AnnotationHelper {

    static void annotateQueryParam(JCodeModel codeModel, JMethod method, TypeDeclaration typeDeclaration, RPModel rpModel) {

        JType type = RamlTypeHelper.getTypeSave(codeModel, typeDeclaration, rpModel, method.name() + "Request");
        annotateRequestParam(type, method, typeDeclaration,false, null);
    }

    static void annotateRequestHeader(JCodeModel codeModel, JMethod method, TypeDeclaration typeDeclaration, RPModel rpModel) {

        JType type = RamlTypeHelper.getTypeSave(codeModel, typeDeclaration, rpModel, method.name() + "Request");
        JVar param = method.param(type, typeDeclaration.name());
        JAnnotationUse requestParam = param.annotate(RequestHeader.class);
        requestParam.param("required", typeDeclaration.required());
    }

    private static void annotateRequestParam(JType type, JMethod method, TypeDeclaration typeDeclaration, boolean noParams, String name) {

        JVar param = method.param(type, name != null ? name : typeDeclaration.name());
        JAnnotationUse requestParam = param.annotate(RequestParam.class);
        if(!noParams) {
            requestParam.param("value", typeDeclaration.name());
            requestParam.param("required", typeDeclaration.required());
        }
        if(typeDeclaration instanceof DateTypeDeclaration ||
                typeDeclaration instanceof DateTimeTypeDeclaration ||
                typeDeclaration instanceof TimeOnlyTypeDeclaration ||
                typeDeclaration instanceof DateTimeOnlyTypeDeclaration) {
            annotateDateFormat(param, typeDeclaration);
        } else if(typeDeclaration instanceof NumberTypeDeclaration) {
            annotateNumberFormat(param,(NumberTypeDeclaration)typeDeclaration);
        } else if(typeDeclaration instanceof StringTypeDeclaration) {
            annotateStringFormat(param,(StringTypeDeclaration)typeDeclaration);
        }
    }

    private static void annotateNumberFormat(JVar param, NumberTypeDeclaration typeDeclaration) {
        if(typeDeclaration.minimum() != null) {
            param.annotate(Min.class).param("value", typeDeclaration.minimum().longValue());
        }
        if(typeDeclaration.maximum() != null) {
            param.annotate(Max.class).param("value", typeDeclaration.maximum().longValue());
        }
    }

    private static void annotateStringFormat(JVar param, StringTypeDeclaration typeDeclaration) {
        if(typeDeclaration.minLength() != null || typeDeclaration.maxLength() != null) {
            JAnnotationUse size = param.annotate(Size.class);

            if(typeDeclaration.minLength() != null) {
                size.param("min", typeDeclaration.minLength());
            }
            if(typeDeclaration.maxLength() != null) {
                size.param("max", typeDeclaration.maxLength());
            }
        }
    }

    private static void annotateDateFormat(JVar param, TypeDeclaration typeDeclaration) {
        JAnnotationUse requestParam = param.annotate(DateTimeFormat.class);
        String date = typeDeclaration.type().toLowerCase();
        switch (date) {
            case "date-only":
                //2018-11-13
                requestParam.param("pattern", "yyyy-MM-dd");
                break;
            case "time-only":
                //10:22:36
                requestParam.param("pattern", "HH:mm:ss");
                break;
            case "datetime-only":
                //2018-11-13T10:22:36
                requestParam.param("pattern", "yyyy-MM-dd'T'HH:mm:ss");
                break;
            case "datetime":
                DateTimeTypeDeclaration dateTimeTypeDeclaration = (DateTimeTypeDeclaration) typeDeclaration;
                if ("rfc2616".equalsIgnoreCase(dateTimeTypeDeclaration.format())) {
                    //Tue, 13 Nov 2018 10:22:36 GMT
                    requestParam.param("pattern", "EEE, dd MMM yyyy HH:mm:ss z");
                } else {
                    requestParam.param("pattern", "yyyy-MM-dd'T'HH:mm:ssXXX");
                }
                break;
            default:
                requestParam.param("pattern", "yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    static void annotateBodyParam(JCodeModel codeModel, JMethod method, TypeDeclaration typeDeclaration, RPModel rpModel) {
        String contentType = typeDeclaration.name().toLowerCase();

        if("application/json".equals(contentType)) {
            JType type = RamlTypeHelper.getTypeSave(codeModel, typeDeclaration, rpModel, method.name() + "Request");
            annotateRequestParam(type, method, typeDeclaration, true, "body");
        } else if("multipart/form-data".equals(contentType)) {
            if(typeDeclaration instanceof ObjectTypeDeclaration) {
                ObjectTypeDeclaration objectTypeDeclaration = (ObjectTypeDeclaration) typeDeclaration;
                objectTypeDeclaration.properties().forEach(property -> {
                    JType type = RamlTypeHelper.getTypeSave(codeModel, property, rpModel, method.name() + "Request");
                    annotateRequestParam(type, method, property,false, null);
                });
            } else {
                throw new RuntimeException("error: unsupported format in multipart/form-data");
            }
        } else if("application/x-www-form-urlencoded".equals(contentType)) {
            JClass clazz = codeModel.ref(Map.class);
            clazz = clazz.narrow(codeModel.directClass("String"), codeModel.directClass("String"));
            annotateRequestParam(clazz, method, typeDeclaration, true, "body");
        } else {
            annotateRequestParam(codeModel.directClass("String"), method, typeDeclaration, true, "body");
        }
        addHeaderParamToRequestParam(method, contentType);
    }

    private static void addHeaderParamToRequestParam(JMethod method, String contentType) {
        method.annotations().forEach(annotation -> {
            if("RequestMapping".equals(annotation.getAnnotationClass().name())) {
                JAnnotationArrayMember jAnnotationArrayMember = annotation.paramArray("headers");
                jAnnotationArrayMember.param("content-type=" + contentType);
            }
        });
    }

}
