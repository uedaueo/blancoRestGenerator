/*
 * blanco Framework
 * Copyright (C) 2004-2006 IGA Tosiki
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package blanco.restgenerator;

import blanco.cg.BlancoCgObjectFactory;
import blanco.cg.BlancoCgSupportedLang;
import blanco.cg.transformer.BlancoCgTransformerFactory;
import blanco.cg.util.BlancoCgLineUtil;
import blanco.cg.valueobject.*;
import blanco.commons.util.BlancoNameAdjuster;
import blanco.commons.util.BlancoStringUtil;
import blanco.restgenerator.resourcebundle.BlancoRestGeneratorResourceBundle;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegram;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegramField;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegramProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Generates class source code to process messages from "Message Definition Form" Excel format.
 *
 * This class is responsible for generation of source code from intermediate XML files.
 *
 * @author IGA Tosiki
 * @author tueda
 */
public class BlancoRestGeneratorXml2SourceFile {

    private boolean fVerbose = false;
    public void setVerbose(boolean argVerbose) {
        this.fVerbose = argVerbose;
    }
    public boolean isVerbose() {
        return fVerbose;
    }

    /**
     * An access object to the resource bundle for this product.
     */
    private final BlancoRestGeneratorResourceBundle fBundle = new BlancoRestGeneratorResourceBundle();

    /**
     * Target programming language.
     */
    private int fTargetLang = BlancoCgSupportedLang.JAVA;

    /**
     * Programming language expected for the input sheet.
     */
    private int fSheetLang = BlancoCgSupportedLang.JAVA;

    public void setSheetLang(final int argSheetLang) {
        fSheetLang = argSheetLang;
    }

    /**
     * Style of the destination directory.
     */
    private boolean fTargetStyleAdvanced = false;
    public void setTargetStyleAdvanced(boolean argTargetStyleAdvanced) {
        this.fTargetStyleAdvanced = argTargetStyleAdvanced;
    }
    public boolean isTargetStyleAdvanced() {
        return this.fTargetStyleAdvanced;
    }

    private int fTabs = 4;
    public int getTabs() {
        return fTabs;
    }
    public void setTabs(int fTabs) {
        this.fTabs = fTabs;
    }

    /**
     * A factory for blancoCg to be used internally.
     */
    private BlancoCgObjectFactory fCgFactory = null;

    /**
     * Source file information for blancoCg to be used internally.
     */
    private BlancoCgSourceFile fCgSourceFile = null;

    /**
     * Class information for blancoCg to be used internally.
     */
    private BlancoCgClass fCgClass = null;

    /**
     * Whether to adjust name of fields or methods.
     */
    private boolean fNameAdjust = true;

    /**
     * Base class for request telegrams.
     */
    private String inputTelegramBase = null;
    /**
     * Base class for response telegrams.
     */
    private String outputTelegramBase = null;

    /**
     * Character encoding of auto-generated source files.
     */
    private String fEncoding = null;

    public void setEncoding(final String argEncoding) {
        fEncoding = argEncoding;
    }

    /**
     * Auto-generates source code from intermediate XML files.
     *
     * @param argMetaXmlSourceFile
     *            An XML file that contains meta-information.
     * @param argDirectoryTarget
     *            Output directory of the generated source code (specify the part excluding /main).
     * @param argNameAdjust
     *            Whether to adjust the name. (Always true)
     * @throws IOException
     *             If an I/O exception occurs.
     */
    public void process(final File argMetaXmlSourceFile,
            final boolean argNameAdjust, final File argDirectoryTarget)
            throws IOException {

        System.out.println("BlancoRestGeneratorXml2SourceFile#process file = " + argMetaXmlSourceFile.getName());

        fNameAdjust = argNameAdjust;

        BlancoRestGeneratorXmlParser parser = new BlancoRestGeneratorXmlParser();
        parser.setVerbose(this.isVerbose());
        BlancoRestGeneratorTelegramProcess [] processStructures = parser.parse(argMetaXmlSourceFile);

        if (processStructures == null) {
            System.out.println("!!! SKIP !!!! " + argMetaXmlSourceFile.getName());
            return;
        }

        if (this.isVerbose()) {
            System.out.println("Parse done. Now generate!!! process number =  " + processStructures.length);
        }

        for (int index = 0; index < processStructures.length; index++) {
            BlancoRestGeneratorTelegramProcess processStructure = processStructures[index];
            // Generates Java code from the obtained information.
            generate(processStructure, argDirectoryTarget);
        }
    }

    private void generate(final BlancoRestGeneratorTelegramProcess argProcessStructure, final File argDirectoryTarget) {
        if (this.isVerbose()) {
            System.out.println("generate START!!!");
        }

        // The first step is to generate a telegram.
        Set<String> methodKeys = argProcessStructure.getListTelegrams().keySet(); // It should not be null because it is checked at time of parse.
        for (String methodKey : methodKeys) {
            if (this.isVerbose()) {
                System.out.println("METHOD = " + methodKey);
            }
            HashMap<String, BlancoRestGeneratorTelegram> kindMap =
                    argProcessStructure.getListTelegrams().get(methodKey);
            Set<String> kindKeys = kindMap.keySet(); // It should not be null because it is checked at time of parse.
            for (String kindKey : kindKeys) {
                if (this.isVerbose()) {
                    System.out.println("Kined = " + kindKey);
                }
                generateTelegram(kindMap.get(kindKey), argDirectoryTarget);
            }
        }

        /*
         * Next, it generates the telegram processing.
         * Generates a controller for micronaut for now.
         * In the future, abstract classes for tomcat will also be supported.
         */
        if (!BlancoRestGeneratorUtil.telegramsOnly) {
            generateProcess(argProcessStructure, argDirectoryTarget);
        }
    }

    /**
     * Auto-generates source code of the telegram processing based on the collected information.
     *
     * @param argProcessStructure
     *            Process structure data collected from metafiles.
     * @param argDirectoryTarget
     *            Output directory of the generated source code.
     */
    public void generateProcess(
            final BlancoRestGeneratorTelegramProcess argProcessStructure,
            final File argDirectoryTarget) {

        /*
         * The output directory will be in the format specified by the targetStyle argument of the ant task.
         * To maintain compatibility with the previous version, it will be blanco/main if not specified.
         * by tueda, 2019/08/30
         */
        String strTarget = argDirectoryTarget
                .getAbsolutePath(); // advanced
        if (!this.isTargetStyleAdvanced()) {
            strTarget += "/main"; // legacy
        }
        final File fileBlancoMain = new File(strTarget);

        // Replaces the package name if the replace package name option is specified.
        // If there is Suffix, that is the priority.
        String processPackage = argProcessStructure.getPackage();
        if (argProcessStructure.getPackageSuffix() != null && argProcessStructure.getPackageSuffix().length() > 0) {
            processPackage = processPackage + "." + argProcessStructure.getPackageSuffix();
        } else if (argProcessStructure.getOverridePackage() != null && argProcessStructure.getOverridePackage().length() > 0) {
            processPackage = argProcessStructure.getOverridePackage();
        }

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(processPackage, "This source code has been generated by blanco Framework.");
        fCgSourceFile.setEncoding(fEncoding);
        fCgClass = fCgFactory.createClass(BlancoRestGeneratorConstants.PREFIX_ABSTRACT + argProcessStructure.getName(),
                BlancoStringUtil.null2Blank(argProcessStructure
                        .getDescription()));
        fCgSourceFile.getClassList().add(fCgClass);

        // Inherits ApiBase class. (Already configured in parser)
        if (BlancoStringUtil.null2Blank(argProcessStructure.getExtends())
                .length() > 0) {
            fCgClass.getExtendClassList().add(
                    fCgFactory.createType(argProcessStructure.getExtends()));
        }

        // Sets the abstrac flag.
        fCgClass.setAbstract(true);

        // Description.
        if (argProcessStructure.getDescription() != null) {
            fCgSourceFile.setDescription(argProcessStructure
                    .getDescription());
        }

        /* Sets the annotation for the class. */
        List annotationList = argProcessStructure.getAnnotationList();
        if (annotationList != null && annotationList.size() > 0) {
            fCgClass.getAnnotationList().addAll(argProcessStructure.getAnnotationList());
            /* tueda DEBUG */
            if (this.isVerbose()) {
                System.out.println("generateTelegramProcess : class annotation = " + argProcessStructure.getAnnotationList().get(0));
            }
        }

        // Generates a service method.
        // Determines RequestHeader and ResponseHeader here.
        String requestHeaderClass = argProcessStructure.getRequestHeaderClass();
        String responseHeaderClass = argProcessStructure.getResponseHeaderClass();
        String requestHeaderIdSimple = null;
        if (requestHeaderClass != null && requestHeaderClass.length() > 0) {
            fCgSourceFile.getImportList().add(requestHeaderClass); // Assuming that fullPackage is specified.
            requestHeaderIdSimple = BlancoRestGeneratorUtil.getSimpleClassName(requestHeaderClass);
        }
        String responseHeaderIdSimple = null;
        if (responseHeaderClass != null && responseHeaderClass.length() > 0) {
            fCgSourceFile.getImportList().add(responseHeaderClass);
            responseHeaderIdSimple = BlancoRestGeneratorUtil.getSimpleClassName(responseHeaderClass);
        }
        if (BlancoRestGeneratorUtil.createServiceMethod) {
            createServiceMethods(argProcessStructure, requestHeaderIdSimple, responseHeaderIdSimple);
        } else {
            if (this.isVerbose()) {
                System.out.println("BlancoRestGeneratorKtXml2SourceFile#generateTelegramProcess: SKIP SERVICE METHOD!");
            }
        }

        // Sets a location.
        createLocationId(argProcessStructure);

        // Overrides isAuthenticationRequired method.
        overrideAuthenticationRequired(argProcessStructure);

        // Doesn't output "required" statement ... In the future, specifies in xls?
         //fCgSourceFile.setIsImport(false);

        BlancoCgTransformerFactory.getSourceTransformer(fTargetLang).transform(
                fCgSourceFile, fileBlancoMain);
    }


    /**
     * Implements Service method.
     *  @param argProcessStructure
     */
    private void createServiceMethods(
            final BlancoRestGeneratorTelegramProcess argProcessStructure,
            final String argRequestHeaderIdSimple,
            final String argResponseHeaderIdSimple) {

        List<String> httpMethods = new ArrayList<>();
        httpMethods.add(BlancoRestGeneratorConstants.HTTP_METHOD_GET);
        httpMethods.add(BlancoRestGeneratorConstants.HTTP_METHOD_POST);
        httpMethods.add(BlancoRestGeneratorConstants.HTTP_METHOD_PUT);
        httpMethods.add(BlancoRestGeneratorConstants.HTTP_METHOD_DELETE);
//        List<String> telegramKind = new ArrayList<>();
//        telegramKind.add(BlancoRestGeneratorConstants.TELEGRAM_INPUT);
//        telegramKind.add(BlancoRestGeneratorConstants.TELEGRAM_OUTPUT);

        for (String method : httpMethods) {
            HashMap<String, BlancoRestGeneratorTelegram> telegrams = argProcessStructure.getListTelegrams().get(method);

            /*
             * Creates a default telegram ID.
             */
            String superRequestId = "";
            String superResponseId = "";
            if (BlancoRestGeneratorConstants.HTTP_METHOD_DELETE.equals(method)) {
                superRequestId = BlancoRestGeneratorConstants.DEFAULT_API_DELETE_REQUESTID;
                superResponseId = BlancoRestGeneratorConstants.DEFAULT_API_DELETE_RESPONSEID;
            } else if (BlancoRestGeneratorConstants.HTTP_METHOD_PUT.equals(method)) {
                superRequestId = BlancoRestGeneratorConstants.DEFAULT_API_PUT_REQUESTID;
                superResponseId = BlancoRestGeneratorConstants.DEFAULT_API_PUT_RESPONSEID;
            } else if (BlancoRestGeneratorConstants.HTTP_METHOD_GET.equals(method)) {
                superRequestId = BlancoRestGeneratorConstants.DEFAULT_API_GET_REQUESTID;
                superResponseId = BlancoRestGeneratorConstants.DEFAULT_API_GET_RESPONSEID;
            } else {
                superRequestId = BlancoRestGeneratorConstants.DEFAULT_API_POST_REQUESTID;
                superResponseId = BlancoRestGeneratorConstants.DEFAULT_API_POST_RESPONSEID;
            }
            /*
             * Finds the package name for this class.
             */
            if (BlancoStringUtil.null2Blank(BlancoRestGeneratorUtil.telegramPackage).length() == 0) {
                String packageName = null;
                packageName = BlancoRestGeneratorUtil.searchPackageBySimpleName(superRequestId);
                packageName = BlancoStringUtil.null2Blank(packageName).length() == 0 ? "" : packageName + ".";
                superRequestId = packageName + superRequestId;
                packageName = BlancoRestGeneratorUtil.searchPackageBySimpleName(superResponseId);
                packageName = BlancoStringUtil.null2Blank(packageName).length() == 0 ? "" : packageName + ".";
                superResponseId = packageName + superResponseId;
            } else {
                superRequestId = BlancoRestGeneratorUtil.telegramPackage + "." + superRequestId;
                superResponseId = BlancoRestGeneratorUtil.telegramPackage + "." + superResponseId;
            }

            if (telegrams == null) {
                /* This method is not supported. */
                String defaultRequestId = argProcessStructure.getName() + BlancoNameAdjuster.toClassName(method.toLowerCase()) + "Request";
                createExecuteMethodNotImplemented(method, defaultRequestId, superRequestId, superResponseId);
            } else {
                createAbstractMethod(telegrams, method);
                createExecuteMethod(telegrams, method, superRequestId, superResponseId);
            }
        }
    }

    private void createAbstractMethod(
            final HashMap<String, BlancoRestGeneratorTelegram> argTelegrams,
            final String argMethod
    ) {

        // Defines Processor.
        final BlancoCgMethod cgProcessorMethod = fCgFactory.createMethod(
                BlancoRestGeneratorConstants.API_PROCESS_METHOD, fBundle.getXml2sourceFileProcessorDescription());

        fCgClass.getMethodList().add(cgProcessorMethod);
        cgProcessorMethod.setAccess("protected");
        cgProcessorMethod.setAbstract(true);

        BlancoRestGeneratorTelegram input = argTelegrams.get(BlancoRestGeneratorConstants.TELEGRAM_INPUT);
        BlancoRestGeneratorTelegram output = argTelegrams.get(BlancoRestGeneratorConstants.TELEGRAM_OUTPUT);
        //System.out.println("### type = " + input.getTelegramType());
//        System.out.println("(createAbstractMethod)### method = " + output.getTelegramMethod());

        String packageNameIn = input.getPackage();
        String packageNameOut = output.getPackage();
        if (BlancoStringUtil.null2Blank(BlancoRestGeneratorUtil.packageSuffix).length() > 0) {
            packageNameIn = input.getPackage() + "." + BlancoRestGeneratorUtil.packageSuffix;
            packageNameOut = output.getPackage() + "." + BlancoRestGeneratorUtil.packageSuffix;
        } else if (BlancoStringUtil.null2Blank(BlancoRestGeneratorUtil.overridePackage).length() > 0) {
            packageNameIn = packageNameOut = BlancoRestGeneratorUtil.overridePackage;
        }

        String requestSubId = input.getName();
        String requestId = packageNameIn + "." + requestSubId;
        String responseSubId = output.getName();
        String responseId = packageNameOut + "." + responseSubId;
        cgProcessorMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + requestSubId, requestId,
                        fBundle.getXml2sourceFileProsessorArgLangdoc()));
        cgProcessorMethod.setReturn(fCgFactory.createReturn(responseId,
                fBundle.getXml2sourceFileProsessorReturnLangdoc()));

        /*
         * Adds a default exception (BlancoRestException) to throws.
         */
        BlancoCgType blancoCgType = new BlancoCgType();
        blancoCgType.setName(BlancoRestGeneratorUtil.getDefaultExceptionId());
        blancoCgType.setDescription(fBundle.getXml2sourceFileDefaultExceptionTypeLangdoc());

        BlancoCgException blancoCgException = new BlancoCgException();
        blancoCgException.setType(blancoCgType);
        blancoCgException.setDescription(fBundle.getXml2sourceFileDefaultExceptionLangdoc());

        ArrayList<BlancoCgException> arrayBlancoCgException = new ArrayList<>();
        arrayBlancoCgException.add(blancoCgException);

        cgProcessorMethod.setThrowList(arrayBlancoCgException);
    }

    private void createExecuteMethod(
            final HashMap<String, BlancoRestGeneratorTelegram> argTelegrams,
            final String argMethod,
            final String argSuperRequestId,
            final String argSuperResponseId
    ) {
        final BlancoCgMethod cgExecutorMethod = fCgFactory.createMethod(
                BlancoRestGeneratorConstants.BASE_EXECUTOR_METHOD, fBundle.getXml2sourceFileExecutorDescription());
        fCgClass.getMethodList().add(cgExecutorMethod);
        cgExecutorMethod.setAccess("protected");

        BlancoRestGeneratorTelegram input = argTelegrams.get(BlancoRestGeneratorConstants.TELEGRAM_INPUT);
        BlancoRestGeneratorTelegram output = argTelegrams.get(BlancoRestGeneratorConstants.TELEGRAM_OUTPUT);
        //System.out.println("### type = " + input.getTelegramType());
//        System.out.println("(createExecuteMethod)### method = " + output.getTelegramMethod());

        String packageNameIn = input.getPackage();
        String packageNameOut = output.getPackage();
        if (BlancoStringUtil.null2Blank(BlancoRestGeneratorUtil.packageSuffix).length() > 0) {
            packageNameIn = input.getPackage() + "." + BlancoRestGeneratorUtil.packageSuffix;
            packageNameOut = output.getPackage() + "." + BlancoRestGeneratorUtil.packageSuffix;
        } else if (BlancoStringUtil.null2Blank(BlancoRestGeneratorUtil.overridePackage).length() > 0) {
            packageNameIn = packageNameOut = BlancoRestGeneratorUtil.overridePackage;
        }
        String requestSubId = input.getName();
        String requestId = packageNameIn + "." + requestSubId;
        String responseSubId = output.getName();
        String responseId = packageNameOut + "." + responseSubId;

        cgExecutorMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + requestSubId, argSuperRequestId,
                        fBundle
                                .getXml2sourceFileExecutorArgLangdoc()));
        cgExecutorMethod.setReturn(fCgFactory.createReturn(argSuperResponseId,
                fBundle.getXml2sourceFileExecutorReturnLangdoc()));

        /*
         * Adds a default exception (BlancoRestException) to throws.
         */
        BlancoCgType blancoCgType = new BlancoCgType();
        blancoCgType.setName(BlancoRestGeneratorUtil.getDefaultExceptionId());
        blancoCgType.setDescription(fBundle.getXml2sourceFileDefaultExceptionTypeLangdoc());

        BlancoCgException blancoCgException = new BlancoCgException();
        blancoCgException.setType(blancoCgType);
        blancoCgException.setDescription(fBundle.getXml2sourceFileDefaultExceptionLangdoc());

        ArrayList<BlancoCgException> arrayBlancoCgException = new ArrayList<>();
        arrayBlancoCgException.add(blancoCgException);

        cgExecutorMethod.setThrowList(arrayBlancoCgException);

        // Implements the method.
        final List<String> ListLine = cgExecutorMethod.getLineList();
        ListLine.add(
                argSuperResponseId + " " + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "ret" + responseSubId + " = "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "this." + BlancoRestGeneratorConstants.API_PROCESS_METHOD
                        + "( " +  "("+requestId+ ")" +  BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "arg" + requestSubId + " )"
                        + BlancoCgLineUtil.getTerminator(fTargetLang));

        ListLine.add("\n");
        ListLine.add("return "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "ret" + responseSubId
                + BlancoCgLineUtil.getTerminator(fTargetLang));

    }

    private void createExecuteMethodNotImplemented(
            final String argMethod,
            final String argDefaultRequestId,
            final String argSuperRequestId,
            final String argSuperResponseId
    ) {
        final BlancoCgMethod cgExecutorMethod = fCgFactory.createMethod(
                BlancoRestGeneratorConstants.BASE_EXECUTOR_METHOD, fBundle.getXml2sourceFileExecutorDescription());
        fCgClass.getMethodList().add(cgExecutorMethod);
        cgExecutorMethod.setAccess("protected");
        final List<String> ListLine = cgExecutorMethod.getLineList();

        /*
         * If there is no telegram definition in the Excel sheet, it will come here.
         * Uses definition in the telegram processing sheet in that case.
         */
        cgExecutorMethod.getParameterList().add(
                fCgFactory.createParameter("argRequest", argSuperRequestId,
                        fBundle
                                .getXml2sourceFileExecutorArgLangdoc()));
        cgExecutorMethod.setReturn(fCgFactory.createReturn(argSuperResponseId,
                fBundle.getXml2sourceFileExecutorReturnLangdoc()));

        /*
         * Adds a default exception (BlancoRestException) to throws.
         */
        BlancoCgType blancoCgType = new BlancoCgType();
        blancoCgType.setName(BlancoRestGeneratorUtil.getDefaultExceptionId());
        blancoCgType.setDescription(fBundle.getXml2sourceFileDefaultExceptionTypeLangdoc());

        BlancoCgException blancoCgException = new BlancoCgException();
        blancoCgException.setType(blancoCgType);
        blancoCgException.setDescription(fBundle.getXml2sourceFileDefaultExceptionLangdoc());

        ArrayList<BlancoCgException> arrayBlancoCgException = new ArrayList<>();
        arrayBlancoCgException.add(blancoCgException);

        cgExecutorMethod.setThrowList(arrayBlancoCgException);

        // Implements the method.
        //throw new BlancoRestException("GetMethod is not implemented in this api");
        ListLine.add(
                "throw new " + BlancoRestGeneratorUtil.getDefaultExceptionId() + "( " + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang) +
                        fBundle.getBlancorestErrorMsg05(argDefaultRequestId) + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)  +")" + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void overrideAuthenticationRequired(BlancoRestGeneratorTelegramProcess argStructure) {
        String methodName = BlancoRestGeneratorConstants.API_AUTHENTICATION_REQUIRED;

        final BlancoCgMethod cgAuthenticationRequiredMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileAuthflagDescription());
        fCgClass.getMethodList().add(cgAuthenticationRequiredMethod);
        cgAuthenticationRequiredMethod.setAccess("public");

        cgAuthenticationRequiredMethod.setReturn(fCgFactory.createReturn("java.lang.Boolean",
                fBundle.getXml2sourceFileAuthflagReturnLangdoc()));

        // Implements the method.
        final List<String> listLine = cgAuthenticationRequiredMethod.getLineList();

        String retval = "true";
        if (argStructure.getNoAuthentication()) {
            retval = "false";
        }

        listLine.add("return " + retval
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void createRequestIdMethod(String methodName, String requestIdName, String packageName) {
        final BlancoCgMethod cgRequestIdMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileRequestidDesctiption());
        fCgClass.getMethodList().add(cgRequestIdMethod);

        cgRequestIdMethod.setAccess("protected");

        List<String> annotators = new ArrayList<>();
        annotators.add("Override");
        cgRequestIdMethod.setAnnotationList(annotators);

        cgRequestIdMethod.setReturn(fCgFactory.createReturn("java.lang.String",
                fBundle.getXml2sourceFileRequestidReturnLangdoc()));

        /*
         * Implements the method.
         * Returns null if not defined.
         */
        final List<String> listLine = cgRequestIdMethod.getLineList();
        if(requestIdName == null) {
            listLine.add("return null" + BlancoCgLineUtil.getTerminator(fTargetLang));
        } else {
            listLine.add("return " + "\"" + packageName + "." + requestIdName + "\""
                    + BlancoCgLineUtil.getTerminator(fTargetLang));
        }
    }

    private void createResponseIdMethod(String methodName, String responseIdName, String packageName) {

        final BlancoCgMethod cgResponseIdMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileRequestidDesctiption());
        fCgClass.getMethodList().add(cgResponseIdMethod);
        cgResponseIdMethod.setAccess("protected");

        List<String> annotators = new ArrayList<>();
        annotators.add("Override");
        cgResponseIdMethod.setAnnotationList(annotators);

        cgResponseIdMethod.setReturn(fCgFactory.createReturn("java.lang.String",
                fBundle.getXml2sourceFileRequestidReturnLangdoc()));

        /*
         * Implements the method.
         * Returns null if not defined.
         */
        final List<String> listLine = cgResponseIdMethod.getLineList();
        if(responseIdName == null) {
            listLine.add("return null" + BlancoCgLineUtil.getTerminator(fTargetLang));
        } else {
            listLine.add("return " + "\"" + packageName + "." + responseIdName + "\""
                    + BlancoCgLineUtil.getTerminator(fTargetLang));
        }
    }

    private void createLocationId(BlancoRestGeneratorTelegramProcess argProcessStructure) {
        String location = argProcessStructure.getLocation();
        String overrideLocation = argProcessStructure.getOverrideLocation();
        if (overrideLocation != null && overrideLocation.length() > 0) {
            location = overrideLocation;
        }
        String serviceId = argProcessStructure.getServiceId();
        String locationId = location + "/" + serviceId;

        BlancoCgMethod cgMethod = fCgFactory.createMethod("getLocationId", "The URL to call this API.");
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.getAnnotationList().add("Override");
        cgMethod.setAccess("protected");
        BlancoCgReturn cgReturn = fCgFactory.createReturn("java.lang.String", "The URL to call this API.");
        cgMethod.setReturn(cgReturn);
        List<String> lineList = cgMethod.getLineList();
        lineList.add("return \"" + locationId + "\";");
    }

    /**
     * Generates a telegram class.
     *
     * @param argTelegramStructure
     * @param argDirectoryTarget
     */
    private void generateTelegram(
            final BlancoRestGeneratorTelegram argTelegramStructure,
            final File argDirectoryTarget) {

        /*
         * The output directory will be in the format specified by the targetStyle argument of the ant task.
         * To maintain compatibility with the previous version, it will be blanco/main if not specified.
         * by tueda, 2019/08/30
         */
        String strTarget = argDirectoryTarget
                .getAbsolutePath(); // advanced
        if (!this.isTargetStyleAdvanced()) {
            strTarget += "/main"; // legacy
        }
        final File fileBlancoMain = new File(strTarget);

        /* tueda DEBUG */
        if (this.isVerbose()) {
            System.out.println("BlancoRestGeneratorXml2SourceFile#generateTelegram START with argDirectoryTarget : " + argDirectoryTarget.getAbsolutePath());
        }

        // Replaces the package name if the replace package name option is specified.
        // If there is Suffix, that is the priority.
        String telegramPackage = argTelegramStructure.getPackage();
        if (argTelegramStructure.getPackageSuffix() != null && argTelegramStructure.getPackageSuffix().length() > 0) {
            telegramPackage = telegramPackage + "." + argTelegramStructure.getPackageSuffix();
        } else if (argTelegramStructure.getOverridePackage() != null && argTelegramStructure.getOverridePackage().length() > 0) {
            telegramPackage = argTelegramStructure.getOverridePackage();
        }

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(telegramPackage, "This source code has been generated by blanco Framework.");
        fCgSourceFile.setEncoding(fEncoding);
        fCgSourceFile.setTabs(this.getTabs());
        // Creates a class.
        fCgClass = fCgFactory.createClass(argTelegramStructure.getName(),
                BlancoStringUtil.null2Blank(argTelegramStructure
                        .getDescription()));
        fCgSourceFile.getClassList().add(fCgClass);
        // The telegram class is always public.
        String access = "public";
        // Inheritance
        if (BlancoStringUtil.null2Blank(argTelegramStructure.getExtends())
                .length() > 0) {
            fCgClass.getExtendClassList().add(
                    fCgFactory.createType(argTelegramStructure.getExtends()));
        }
        // Implementation
        for (int index = 0; index < argTelegramStructure.getImplementsList()
                .size(); index++) {
            final String impl = (String) argTelegramStructure.getImplementsList()
                    .get(index);
            fCgClass.getImplementInterfaceList().add(
                    fCgFactory.createType(impl));
        }

        // Sets the JavaDoc for the class.
        fCgClass.setDescription(argTelegramStructure.getDescription());

        /* Sets the annotation for the class. */
        List annotationList = argTelegramStructure.getAnnotationList();
        if (annotationList != null && annotationList.size() > 0) {
            fCgClass.getAnnotationList().addAll(argTelegramStructure.getAnnotationList());
            /* tueda DEBUG */
            if (this.isVerbose()) {
                System.out.println("BlancoRestGeneratorXml2SourceFile#generateTelegram : class annotation = " + argTelegramStructure.getAnnotationList().get(0));
            }
        }

        /* Sets the import of the class. */
        for (int index = 0; index < argTelegramStructure.getImportList()
                .size(); index++) {
            final String imported = (String) argTelegramStructure.getImportList()
                    .get(index);
            fCgSourceFile.getImportList().add(imported);
        }

        if (this.isVerbose()) {
            System.out.println("BlancoRestGeneratorXml2SourceFile: Start create properties : " + argTelegramStructure.getName());
        }

        // A list of telegram definitions.
        for (int indexField = 0; indexField < argTelegramStructure.getListField()
                .size(); indexField++) {
            // Processes each field.
            final BlancoRestGeneratorTelegramField fieldStructure =
                    argTelegramStructure.getListField().get(indexField);

            // Performs exception processing if a required field is not set.
            if (fieldStructure.getName() == null) {
                throw new IllegalArgumentException(fBundle
                        .getXml2sourceFileErr004(argTelegramStructure.getName()));
            }
            if (fieldStructure.getFieldType() == null) {
                throw new IllegalArgumentException(fBundle.getXml2sourceFileErr003(
                        argTelegramStructure.getName(), fieldStructure.getName()));
            }

            if (this.isVerbose()) {
                System.out.println("property : " + fieldStructure.getName());
            }

            // Creates fields.
            expandField(fieldStructure);
            // Creates getter/setter methods.
            expandMethodSet(fieldStructure);
            expandMethodGet(fieldStructure);
            expandMethodType(fieldStructure);
        }
        expandMethodToString(argTelegramStructure);

        // Auto-generates the actual source code based on the collected information.
        BlancoCgTransformerFactory.getJavaSourceTransformer().transform(
                fCgSourceFile, fileBlancoMain);
    }

    /**
     * Expands fields.
     *
     * @param argFieldStructure
     */
    private void expandField(
            final BlancoRestGeneratorTelegramField argFieldStructure) {
        String fieldName = argFieldStructure.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        switch (fSheetLang) {
            case BlancoCgSupportedLang.PHP:
                if (argFieldStructure.getFieldType() == "java.lang.Integer") {
                    argFieldStructure.setFieldType("java.lang.Long");
                }
                break;
            /* Adds the case here if you want to add more languages. */
        }

        final BlancoCgField cgField = fCgFactory.createField("f" + fieldName,
                argFieldStructure.getFieldType(), "");
        fCgClass.getFieldList().add(cgField);
        /* Corresponds generics. */
        if (BlancoStringUtil.null2Blank(argFieldStructure.getFieldGeneric()).length() > 0) {
            cgField.getType().setGenerics(argFieldStructure.getFieldGeneric());
        }

        cgField.setAccess("private");

        cgField.setDescription(fBundle.getXml2sourceFileFieldName(argFieldStructure
                .getName()));
        cgField.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileFieldType(argFieldStructure.getFieldType()));
        if (BlancoStringUtil.null2Blank(argFieldStructure.getDescription()).length() > 0) {
            cgField.getLangDoc().getDescriptionList().add(
                    argFieldStructure.getDescription());
        }

        /* Sets the annotation for the method. */
        List annotationList = argFieldStructure.getFieldAnnotationList();
        if (annotationList != null && annotationList.size() > 0) {
            cgField.getAnnotationList().addAll(annotationList);
            System.out.println("/* tueda */ method annotation = " + cgField.getAnnotationList().get(0));
        }
    }

    /**
     * Expands the set method.
     *
     * @param argFieldStructure
     */
    private void expandMethodSet(
            final BlancoRestGeneratorTelegramField argFieldStructure) {
        String fieldName = argFieldStructure.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("set"
                + fieldName, fBundle.getXml2sourceFileSetLangdoc01(argFieldStructure
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileSetLangdoc02(argFieldStructure.getFieldType()));

        if (BlancoStringUtil.null2Blank(argFieldStructure.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    argFieldStructure.getDescription());
        }

        BlancoCgParameter cgParameter = fCgFactory.createParameter("arg" + fieldName, argFieldStructure
                .getFieldType(), fBundle
                .getXml2sourceFileSetArgLangdoc(argFieldStructure.getName()));
        cgMethod.getParameterList().add(cgParameter);
        if (BlancoStringUtil.null2Blank(argFieldStructure.getFieldGeneric()).length() > 0) {
            cgParameter.getType().setGenerics(argFieldStructure.getFieldGeneric());
        }

        // Implements the method.
        final List<String> listLine = cgMethod.getLineList();

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                + "this.f" + fieldName + " = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "arg"
                + fieldName + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * Expands the get method.
     *
     * @param argFieldStructure
     */
    private void expandMethodGet(
            final BlancoRestGeneratorTelegramField argFieldStructure) {
        String fieldName = argFieldStructure.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("get"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(argFieldStructure
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileGetLangdoc02(argFieldStructure.getFieldType()));

        cgMethod.setReturn(fCgFactory.createReturn(argFieldStructure.getFieldType(), fBundle
                .getXml2sourceFileGetReturnLangdoc(argFieldStructure.getName())));

        if (BlancoStringUtil.null2Blank(argFieldStructure.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    argFieldStructure.getDescription());
        }
        if (BlancoStringUtil.null2Blank(argFieldStructure.getFieldGeneric()).length() > 0) {
            cgMethod.getReturn().getType().setGenerics(argFieldStructure.getFieldGeneric());
        }

        // Implements the method.
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                        + "this." + "f" + fieldName
                        + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * Expands the type method.
     *
     * @param argFieldStructure
     */
    private void expandMethodType(
            final BlancoRestGeneratorTelegramField argFieldStructure) {
        String fieldName = argFieldStructure.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("type"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(argFieldStructure
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.setStatic(true);

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileTypeLangdoc02("java.lang.String"));

        cgMethod.setReturn(fCgFactory.createReturn("java.lang.String", fBundle
                .getXml2sourceFileTypeReturnLangdoc(argFieldStructure.getName())));

        if (BlancoStringUtil.null2Blank(argFieldStructure.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    argFieldStructure.getDescription());
        }

        // Implements the method.
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return " +
                        BlancoCgLineUtil.getStringLiteralEnclosure(BlancoCgSupportedLang.JAVA) +
                                argFieldStructure.getFieldType() +
                                (BlancoStringUtil.null2Blank(argFieldStructure.getFieldGeneric()).length() > 0 ?
                                        "<" + argFieldStructure.getFieldGeneric() + ">" : "") +
                                BlancoCgLineUtil.getStringLiteralEnclosure(BlancoCgSupportedLang.JAVA) +
                                BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * Expands the toString method.
     *
     * @param argProcessStructure
     */
    private void expandMethodToString(
            final BlancoRestGeneratorTelegram argProcessStructure) {
        final BlancoCgMethod method = fCgFactory.createMethod("toString",
                "Gets the string representation of this value object.");
        fCgClass.getMethodList().add(method);

        method.getLangDoc().getDescriptionList().add(
                "Note that only the shallow range of the object will be toString.");
        method
                .setReturn(fCgFactory.createReturn("java.lang.String",
                        "String representation of a value obejct."));

        List<String> annotators = new ArrayList<>();
        annotators.add("Override");
        method.setAnnotationList(annotators);

        final List<String> listLine = method.getLineList();

        listLine.add(BlancoCgLineUtil.getVariableDeclaration(fTargetLang,
                "buf", "java.lang.String", BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang)
                        + BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang))
                + BlancoCgLineUtil.getTerminator(fTargetLang));

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf "
                + BlancoCgLineUtil.getStringConcatenationOperator(fTargetLang)
                + " " + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + argProcessStructure.getPackage() + "."
                + argProcessStructure.getName() + "["
                + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + BlancoCgLineUtil.getTerminator(fTargetLang));
        for (int indexField = 0; indexField < argProcessStructure
                .getListField().size(); indexField++) {
            final BlancoRestGeneratorTelegramField fieldLook = argProcessStructure
                    .getListField().get(indexField);

            String fieldName = fieldLook.getName();
            if (fNameAdjust) {
                fieldName = BlancoNameAdjuster.toClassName(fieldName);
            }

            if (fieldLook.getFieldType().equals("array") == false) {
                String strLine = BlancoCgLineUtil
                        .getVariablePrefix(fTargetLang)
                        + "buf = "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                        + "buf "
                        + BlancoCgLineUtil
                                .getStringConcatenationOperator(fTargetLang)
                        + " "
                        + BlancoCgLineUtil
                                .getStringLiteralEnclosure(fTargetLang)
                        + (indexField == 0 ? "" : ",")
                        + fieldLook.getName()
                        + "="
                        + BlancoCgLineUtil
                                .getStringLiteralEnclosure(fTargetLang)
                        + " "
                        + BlancoCgLineUtil
                                .getStringConcatenationOperator(fTargetLang)
                        + " ";
                if (fieldLook.getFieldType().equals("java.lang.String")) {
                    strLine += BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this.f" + fieldName;
                } else if (fieldLook.getFieldType().equals("boolean")) {
                    strLine += "("
                            + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this.f" + fieldName + " ? 'true' : 'false')";
                } else {
                    strLine += " "
                            + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this.f" + fieldName;
                }
                strLine += BlancoCgLineUtil.getTerminator(fTargetLang);
                listLine.add(strLine);
            } else {
                listLine.add("// TODO Arrays are not supported.");
            }
        }

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf "
                + BlancoCgLineUtil.getStringConcatenationOperator(fTargetLang)
                + " " + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + "]" + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + BlancoCgLineUtil.getTerminator(fTargetLang));
        listLine.add("return "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf"
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }
}
