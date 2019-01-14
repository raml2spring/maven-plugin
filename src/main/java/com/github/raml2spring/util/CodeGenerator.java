package com.github.raml2spring.util;

import com.github.raml2spring.configuration.Raml2SpringConfig;
import com.github.raml2spring.data.RPMethod;
import com.github.raml2spring.data.RPModel;
import com.github.raml2spring.exception.RamlIOException;
import com.github.raml2spring.data.RPType;
import com.sun.codemodel.*;
import org.raml.v2.api.model.v10.datamodel.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CodeGenerator {

    public static void writeCodeToDisk(RPModel model, String targetPath) {
        File dir = new File(targetPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        //createNumberFormatAnnotation(model.getBasePackage() + ".validator", dir, true);
        LogOutputStream logOutputStream = new LogOutputStream(Raml2SpringConfig.getLog());

        Map<String, RPType> types = model.getTypes();

        //write types
        types.forEach((name, type) -> {
            try {
                if(type.getModel() != null) {
                    type.getModel().build(dir, logOutputStream);
                }
            } catch (IOException e) {
                throw new RamlIOException("error writing types: ".concat(e.getMessage()));
            }
        });

        //write enums
        model.getEnums().forEach((name, cEnum) -> {
            try {
                cEnum.getModel().build(dir, logOutputStream);
            } catch (IOException e) {
                throw new RamlIOException("error writing enums: ".concat(e.getMessage()));
            }
        });

        //write Exceptions
        model.getExceptions().forEach((name, exception) -> {
            try {
                TypeDeclaration typeDeclaration;
                RPType type = types.get(exception.getTypeDeclaration().type());
                if(type == null) { //not defined in types
                    typeDeclaration = exception.getTypeDeclaration();
                } else {
                    typeDeclaration = type.getTypeDeclaration();
                }
                JCodeModel typeModel = new JCodeModel(); //cloner.deepClone(type.getModel());

                RamlTypeHelper.generateType(typeModel, typeDeclaration, model, model.getBasePackage() + ".exception", exception.getName());

                JDefinedClass dclass = typeModel.packages().next().classes().next();
                dclass._extends(RuntimeException.class);
                if(exception.getCode() != null) {
                    JAnnotationUse responseStatus = dclass.annotate(ResponseStatus.class);
                    responseStatus.param("value", exception.getCode());
                }
                exception.setDefinedClass(dclass);
                typeModel.build(dir, logOutputStream);
            } catch (IOException e) {
                throw new RamlIOException("error writing exceptions: ".concat(e.getMessage()));
            }
        });


        //write endpoints
        model.getEndpoints().forEach((name, endpoint) -> {
            try {
                JCodeModel codeModel = new JCodeModel();
                JPackage jp = codeModel._package(model.getBasePackage());
                JDefinedClass jc = jp._interface(endpoint.getName());
                jc.javadoc().add("Generated with raml2spring");
                jc.annotate(RestController.class);

                if(endpoint.getBaseUri() != null) {
                    JAnnotationUse requestMapping = jc.annotate(RequestMapping.class);
                    requestMapping.param("value",endpoint.getBaseUri());
                }

                for(RPMethod method : endpoint.getMethods()) {
                    JType returnType;
                    if ( method.getReturnType() == null ||
                            method.getReturnType() instanceof FileTypeDeclaration ||
                            method.getReturnType() instanceof NullTypeDeclaration) {
                        returnType = codeModel.VOID;
                    } else {
                        returnType = RamlTypeHelper.getTypeSave(codeModel,  method.getReturnType(), model, endpoint.getName() + "Response");
                    }

                    JMethod restcall = jc.method(JMod.PUBLIC, returnType, method.getName());
                    if(StringUtils.hasText(method.getDescription())) {
                        JDocComment doc = restcall.javadoc();
                        doc.add(method.getDescription());
                        doc.add("test");
                    }
                    JAnnotationUse requestMapping = restcall.annotate(RequestMapping.class);
                    requestMapping.param("value", method.getUri());
                    requestMapping.param("method", RequestMethod.valueOf(method.getMethod()));

                    if(method.getProduces().size() > 0) {
                        JAnnotationArrayMember jAnnotationArrayMember = requestMapping.paramArray("produces");
                        method.getProduces().forEach(jAnnotationArrayMember::param);
                    }

                    if(method.getResponseStatus() != null) {
                        JAnnotationUse responseStatus = restcall.annotate(ResponseStatus.class);
                        responseStatus.param("value", method.getResponseStatus());
                    }


                    method.getUriParams().forEach(param -> {
                        JVar pathParam = restcall.param(codeModel.directClass("String"), param);
                        JAnnotationUse pathParamAnn = pathParam.annotate(PathVariable.class);
                        pathParamAnn.param("value", param);
                    });

                    method.getQueryParams().forEach(param -> AnnotationHelper.annotateQueryParam(codeModel, restcall, param, model));

                    method.getHeaderParams().forEach(param -> AnnotationHelper.annotateRequestHeader(codeModel, restcall, param, model));

                    if(method.getBody() != null) {
                        AnnotationHelper.annotateBodyParam(codeModel, restcall, method.getBody(), model);
                    }

                    JType httpServletRequest = codeModel._ref(HttpServletRequest.class);
                    JType httpServletResponse = codeModel._ref(HttpServletResponse.class);
                    restcall.param(httpServletRequest, "httpServletRequest");
                    restcall.param(httpServletResponse, "httpServletResponse");

                    method.getHandledExceptions().forEach(exception -> restcall._throws(model.getExceptions().get(exception).getDefinedClass()));

                }
                codeModel.build(dir, logOutputStream);
            } catch(Exception e) {
                //throw new RuntimeException(e.getMessage());
                throw new RamlIOException("error writing endpoints: ", e);
            }
        });


    }

//    public static JDefinedClass createNumberFormatAnnotation(String packagePath, File outputDir, boolean write) {
//        try {
//            JCodeModel codeModel2 = new JCodeModel();
//            JPackage jp = codeModel2._package(packagePath);
//            //JType jc = codeModel2._ref(NumberFormat.class);
//            //jp._class(jc);
//            JDefinedClass jcClass = jp._class(JMod.PUBLIC, "NumberFormatValidator");
//            JDefinedClass jcInterface = jp._class(JMod.PUBLIC, "NumberFormat", ClassType.ANNOTATION_TYPE_DECL);
//
//            JClass consVal = codeModel2.ref(ConstraintValidator.class);
//            JClass consValNar = consVal.narrow(jcInterface, codeModel2.directClass("Number"));
//            jcClass._implements(consValNar);
//
//            jcClass.constructor(JMod.PUBLIC);
//
//            JMethod isValid = jcClass.method(JMod.PUBLIC, codeModel2.BOOLEAN, "isValid");
//            isValid.param(codeModel2.directClass("Number"), "numberField");
//            JType context = codeModel2._ref(ConstraintValidatorContext.class);
//            isValid.param(context, "context");
//            isValid.body()._return(JExpr.lit(false));
//            isValid.annotate(Override.class);
//
//            JMethod init = jcClass.method(JMod.PUBLIC, codeModel2.VOID, "initialize");
//            init.param(jcInterface, "number");
//            init.annotate(Override.class);
//
//            jcInterface.annotate(Documented.class);
//
//            JAnnotationUse constraint = jcInterface.annotate(Constraint.class);
//            JAnnotationArrayMember constraintParams = constraint.paramArray("validatedBy");
//            constraintParams.param(jcClass);
//
//            JAnnotationUse target = jcInterface.annotate(Target.class);
//            JAnnotationArrayMember targetParams = target.paramArray("value");
//            targetParams.param(ElementType.PARAMETER);
//            //targetParams.param(ElementType.METHOD);
//            //targetParams.param(ElementType.FIELD);
//            //targetParams.param(ElementType.TYPE);
//            //targetParams.param(ElementType.TYPE_PARAMETER);
//            //targetParams.param(ElementType.ANNOTATION_TYPE);
//
//            JAnnotationUse retention = jcInterface.annotate(Retention.class);
//            retention.param("value", RetentionPolicy.RUNTIME);
//
//            JMethod message = jcInterface.method(JMod.NONE, codeModel2.directClass("String"), "message");
//            message.declareDefaultValue(JExpr.lit("invalid number"));
//
//            JClass anyClass = codeModel2.directClass("Class").narrow(codeModel2.directClass("?")).array();
//            JMethod groups = jcInterface.method(JMod.NONE, anyClass, "groups");
//            groups.declareDefaultValue(JExpr.ref("{}"));
//
//            //codeModel2._ref(Payload.class);
//            JFieldVar dummy = jcInterface.field(JMod.NONE, Payload.class, "dummy", JExpr._null());
//            //JDefinedClass j = new JCodeModel().anonymousClass(Class.class);
//            //JTypeVar tv = j.generify("?", Payload.class);
//            //JClas
//
//            JClass retPaylod = codeModel2.directClass("Class").narrow(codeModel2.directClass("? extends Payload"));
//            JMethod payload = jcInterface.method(JMod.NONE, retPaylod.array(), "payload");
//            payload.declareDefaultValue(JExpr.ref("{}"));
//
//            if(write) {
//                codeModel2.build(outputDir);
//            }
//            return jcInterface;
//        } catch (Exception e) {
//            e.printStackTrace();
//            //throw new Exception(e.getMessage());
//        }
//        return null;
//    }
}
