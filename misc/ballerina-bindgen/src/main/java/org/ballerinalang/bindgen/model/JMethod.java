/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.bindgen.model;

import org.ballerinalang.bindgen.utils.BindgenEnv;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.ballerinalang.bindgen.command.BindingsGenerator.getAllJavaClasses;
import static org.ballerinalang.bindgen.command.BindingsGenerator.setClassListForLooping;
import static org.ballerinalang.bindgen.command.BindingsGenerator.setExceptionList;
import static org.ballerinalang.bindgen.utils.BindgenConstants.ARRAY_BRACKETS;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BALLERINA_RESERVED_WORDS;
import static org.ballerinalang.bindgen.utils.BindgenConstants.JAVA_STRING;
import static org.ballerinalang.bindgen.utils.BindgenConstants.JAVA_STRING_ARRAY;
import static org.ballerinalang.bindgen.utils.BindgenConstants.METHOD_INTEROP_TYPE;
import static org.ballerinalang.bindgen.utils.BindgenUtils.getAlias;
import static org.ballerinalang.bindgen.utils.BindgenUtils.getBallerinaHandleType;
import static org.ballerinalang.bindgen.utils.BindgenUtils.getBallerinaParamType;
import static org.ballerinalang.bindgen.utils.BindgenUtils.getJavaType;
import static org.ballerinalang.bindgen.utils.BindgenUtils.isStaticMethod;

/**
 * Class for storing details pertaining to a specific Java method used for Ballerina bridge code generation.
 *
 * @since 1.2.0
 */
public class JMethod extends BFunction {

    private BindgenEnv env;
    private boolean isStatic;
    private boolean hasParams = true;
    private boolean hasReturn = false;
    private boolean returnError = false;
    public boolean objectReturn = false;
    private boolean reservedWord = false;
    private boolean isArrayReturn = false;
    private boolean hasException = false;
    private boolean handleException = false;
    public boolean isStringReturn = false;
    public boolean isStringArrayReturn = false;
    private boolean javaArraysModule = false;
    private boolean hasPrimitiveParam = false;
    private String parentPrefix;

    private Class parentClass;
    private Class jClass;
    private Method method;
    private String methodName;
    private String returnType;
    private String externalType;
    private String exceptionName;
    private String returnTypeJava;
    private String shortClassName;
    private String javaMethodName;
    private String exceptionConstName;
    private String returnComponentType;
    private String interopType = METHOD_INTEROP_TYPE;

    private List<JParameter> parameters = new ArrayList<>();
    private StringBuilder paramTypes = new StringBuilder();
    private Set<String> importedPackages = new HashSet<>();

    public JMethod(Method m, BindgenEnv env, String parentPrefix, Class jClass, int overloaded) {
        super(BFunctionKind.METHOD, env);
        this.env = env;
        this.parentPrefix = parentPrefix;
        method = m;
        javaMethodName = m.getName();
        if (overloaded != 0) {
            methodName = m.getName() + overloaded;
        } else {
            methodName = m.getName();
        }
        shortClassName = getAlias(m.getDeclaringClass());
        isStatic = isStaticMethod(m);
        super.setStatic(isStatic);
        this.parentClass = m.getDeclaringClass();
        setDeclaringClass(parentClass);

        // Set the attributes required to identify different return types.
        Class returnTypeClass = m.getReturnType();
        if (!returnTypeClass.equals(Void.TYPE)) {
            setReturnTypeAttributes(returnTypeClass);
        }
        setExternalFunctionName(jClass.getName().replace(".", "_") + "_" + methodName);
        // Set the attributes relevant to error returns.
        for (Class<?> exceptionType : m.getExceptionTypes()) {
            try {
                if (!this.getClass().getClassLoader().loadClass(RuntimeException.class.getCanonicalName())
                        .isAssignableFrom(exceptionType)) {
                    JError jError = new JError(exceptionType);
                    setThrowable(jError);
                    importedPackages.add(exceptionType.getPackageName());
                    exceptionName = jError.getShortExceptionName();
                    exceptionConstName = jError.getExceptionConstName();
                    if (env.getModulesFlag()) {
                        exceptionName = getPackageAlias(exceptionName, exceptionType);
                        exceptionConstName = getPackageAlias(exceptionConstName, exceptionType);
                    }
                    setExceptionList(jError);
                    hasException = true;
                    handleException = true;
                    break;
                }
            } catch (ClassNotFoundException ignore) {
            }
        }

        // Set the attributes required to identify different parameters.
        setParameters(m.getParameters());
        if (!parameters.isEmpty()) {
            JParameter lastParam = parameters.get(parameters.size() - 1);
            lastParam.setHasNext(false);
        } else {
            hasParams = false;
        }

        List<String> reservedWords = Arrays.asList(BALLERINA_RESERVED_WORDS);
        if (reservedWords.contains(methodName)) {
            reservedWord = true;
        }
        if (objectReturn && !getAllJavaClasses().contains(returnTypeClass.getName())) {
            if (isArrayReturn) {
                setClassListForLooping(returnTypeClass.getComponentType().getName());
            } else {
                setClassListForLooping(returnTypeClass.getName());
            }
        }

        if (isStatic) {
            super.setFunctionName(shortClassName + "_" + methodName);
        } else {
            super.setFunctionName(methodName);
        }
    }

    private void setReturnTypeAttributes(Class returnTypeClass) {
        hasReturn = true;
        if (!returnTypeClass.isPrimitive()) {
            if (returnTypeClass.isArray()) {
                if (!returnTypeClass.getComponentType().isPrimitive()) {
                    importedPackages.add(returnTypeClass.getComponentType().getPackageName());
                }
            } else {
                importedPackages.add(returnTypeClass.getPackageName());
            }
        }

        returnTypeJava = getJavaType(returnTypeClass);
        externalType = getBallerinaHandleType(returnTypeClass);
        setExternalReturnType(externalType);
        returnType = getBallerinaParamType(returnTypeClass);
        returnType = getExceptionName(returnTypeClass, returnType);
        if (returnTypeClass.isArray()) {
            setArrayReturnType(true);
            javaArraysModule = true;
            hasException = true;
            returnError = true;
            isArrayReturn = true;
            if (returnTypeClass.getComponentType().isPrimitive()) {
                objectReturn = false;
            } else if (getAlias(returnTypeClass).equals(JAVA_STRING_ARRAY)) {
                objectReturn = false;
                isStringArrayReturn = true;
            } else {
                returnComponentType = getAlias(returnTypeClass.getComponentType());
                returnComponentType = getExceptionName(returnTypeClass.getComponentType(), returnComponentType);
                returnType = returnComponentType + ARRAY_BRACKETS;
                if (env.getModulesFlag()) {
                    returnType = getPackageAlias(returnType, returnTypeClass.getComponentType());
                    returnComponentType = getPackageAlias(returnComponentType, returnTypeClass.getComponentType());
                }
                objectReturn = true;
            }
        } else if (returnTypeClass.isPrimitive()) {
            objectReturn = false;
        } else if (getAlias(returnTypeClass).equals(JAVA_STRING)) {
            isStringReturn = true;
        } else {
            if (env.getModulesFlag()) {
                returnType = getPackageAlias(returnType, returnTypeClass);
            }
            objectReturn = true;
        }
        setReturnType(returnType);
    }

    private String getPackageAlias(String shortClassName, Class objectType) {
        if (objectType.getPackage() != parentClass.getPackage()) {
            return objectType.getPackageName().replace(".", "") + ":" + shortClassName;
        }
        return shortClassName;
    }

    private String getExceptionName(Class exception, String name) {
        try {
            // Append the prefix "J" in front of bindings generated for Java exceptions.
            if (this.getClass().getClassLoader().loadClass(Exception.class.getCanonicalName())
                    .isAssignableFrom(exception)) {
                return "J" + name;
            }
        } catch (ClassNotFoundException ignore) {
            // Silently ignore if the exception class cannot be found.
        }
        return name;
    }

    private void setParameters(Parameter[] paramArr) {
        for (Parameter param : paramArr) {
            importedPackages.add(param.getType().getPackageName());
            paramTypes.append(getAlias(param.getType()).toLowerCase(Locale.ENGLISH));
            JParameter parameter = new JParameter(param, parentClass, env);
            parameters.add(parameter);
            if (parameter.getIsPrimitiveArray()) {
                javaArraysModule = true;
                returnError = true;
                hasPrimitiveParam = true;
                hasException = true;
            }
            if (parameter.isObjArrayParam() || parameter.getIsStringArray()) {
                javaArraysModule = true;
                returnError = true;
                hasException = true;
            }
        }
    }

    public String getJavaMethodName() {
        return javaMethodName;
    }

    String getParamTypes() {
        return paramTypes.toString();
    }

    public Boolean getHasException() {
        return hasException;
    }

    public Boolean getIsStringReturn() {
        return isStringReturn;
    }

    public Boolean getHasPrimitiveParam() {
        return hasPrimitiveParam;
    }

    public String getReturnType() {
        return returnType;
    }

    public boolean getHasReturn() {
        return hasReturn;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<JParameter> getParameters() {
        return parameters;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean hasParams() {
        return hasParams;
    }

    public String getExternalType() {
        return externalType;
    }

    public boolean isHandleException() {
        return handleException;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public boolean isReturnError() {
        return returnError;
    }

    public Method getMethod() {
        return method;
    }

    boolean requireJavaArrays() {
        return javaArraysModule;
    }

    Set<String> getImportedPackages() {
        return importedPackages;
    }

    public boolean isArrayReturn() {
        return isArrayReturn;
    }

    public List<String> getExternalFunctionCallArguments() {
        List<String> argTypes = new LinkedList<>();
        if (!isStatic) {
            argTypes.add("self.jObj");
        }
        for (JParameter jParameter : parameters) {
            argTypes.add(jParameter.getFieldName());
        }
        return argTypes;
    }

    public String getBalFunctionName() {
        return parentPrefix + "_" + methodName;
    }

    public String getAccessModifier() {
        if (env.hasPublicFlag()) {
            return "public";
        }
        return null;
    }

    public String getFunctionReturnType() {
        StringBuilder returnString = new StringBuilder();
        if (getHasReturn()) {
            returnString.append(this.returnType);
            if (getIsStringReturn()) {
                returnString.append("?");
            }
            if (getHasException()) {
                if (isHandleException()) {
                    returnString.append("|").append(getExceptionName());
                    if (isReturnError()) {
                        returnString.append("|error");
                    }
                } else {
                    returnString.append("|error");
                }
            }
        } else if (getHasException() || getHasPrimitiveParam()) {
            returnString.append("error?");
        }

        return returnString.toString();
    }

    public String externalFunctionReturnType() {
        StringBuilder returnString = new StringBuilder();
        if (getHasReturn()) {
            returnString.append(externalType);
            if (handleException) {
                returnString.append("|error");
            }
        } else if (getHasException() || getHasPrimitiveParam()) {
            returnString.append("error?");
        }
        return returnString.toString();
    }

    public String getParentPrefix() {
        return parentPrefix;
    }

    public String getReturnComponentType() {
        return returnComponentType;
    }

    public String getReturnTypeJava() {
        return returnTypeJava;
    }

    public String getExceptionConstName() {
        return exceptionConstName;
    }
}
