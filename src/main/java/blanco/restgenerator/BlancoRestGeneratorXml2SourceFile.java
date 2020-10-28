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
import blanco.cg.util.BlancoCgSourceUtil;
import blanco.cg.valueobject.*;
import blanco.commons.util.BlancoNameAdjuster;
import blanco.commons.util.BlancoStringUtil;
import blanco.restgenerator.resourcebundle.BlancoRestGeneratorResourceBundle;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegram;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegramField;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegramProcess;
import blanco.valueobject.valueobject.BlancoValueObjectClassStructure;
import blanco.xml.bind.BlancoXmlBindingUtil;
import blanco.xml.bind.BlancoXmlUnmarshaller;
import blanco.xml.bind.valueobject.BlancoXmlDocument;
import blanco.xml.bind.valueobject.BlancoXmlElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 「メッセージ定義書」Excel様式からメッセージを処理するクラス・ソースコードを生成。
 *
 * このクラスは、中間XMLファイルからソースコードを自動生成する機能を担います。
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
     * このプロダクトのリソースバンドルへのアクセスオブジェクト。
     */
    private final BlancoRestGeneratorResourceBundle fBundle = new BlancoRestGeneratorResourceBundle();

    /**
     * 出力対象となるプログラミング言語。
     */
    private int fTargetLang = BlancoCgSupportedLang.JAVA;

    /**
     * 入力シートに期待するプログラミング言語
     */
    private int fSheetLang = BlancoCgSupportedLang.JAVA;

    public void setSheetLang(final int argSheetLang) {
        fSheetLang = argSheetLang;
    }

    /**
     * ソースコード生成先ディレクトリのスタイル
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
     * 内部的に利用するblancoCg用ファクトリ。
     */
    private BlancoCgObjectFactory fCgFactory = null;

    /**
     * 内部的に利用するblancoCg用ソースファイル情報。
     */
    private BlancoCgSourceFile fCgSourceFile = null;

    /**
     * 内部的に利用するblancoCg用クラス情報。
     */
    private BlancoCgClass fCgClass = null;

    /**
     * フィールド名やメソッド名の名前変形を行うかどうか。
     */
    private boolean fNameAdjust = true;

    /**
     * 要求電文のベースクラス
     */
    private String inputTelegramBase = null;
    /**
     * 応答電文のベースクラス
     */
    private String outputTelegramBase = null;

    /**
     * 自動生成するソースファイルの文字エンコーディング。
     */
    private String fEncoding = null;

    public void setEncoding(final String argEncoding) {
        fEncoding = argEncoding;
    }

    /**
     * 中間XMLファイルからソースコードを自動生成します。
     *
     * @param argMetaXmlSourceFile
     *            メタ情報が含まれているXMLファイル。
     * @param argDirectoryTarget
     *            ソースコード生成先ディレクトリ (/mainを除く部分を指定します)。
     * @param argNameAdjust
     *            名前変形を行うかどうか。(常にtrue）
     * @throws IOException
     *             入出力例外が発生した場合。
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
            // 得られた情報から Java コードを生成します。
            generate(processStructure, argDirectoryTarget);
        }
    }

    private void generate(final BlancoRestGeneratorTelegramProcess argProcessStructure, final File argDirectoryTarget) {
        if (this.isVerbose()) {
            System.out.println("generate START!!!");
        }

        // まず電文を生成します。
        Set<String> methodKeys = argProcessStructure.getListTelegrams().keySet(); // parse 時点で check しているので null はないはず
        for (String methodKey : methodKeys) {
            if (this.isVerbose()) {
                System.out.println("METHOD = " + methodKey);
            }
            HashMap<String, BlancoRestGeneratorTelegram> kindMap =
                    argProcessStructure.getListTelegrams().get(methodKey);
            Set<String> kindKeys = kindMap.keySet(); // parse 時点で check しているので null はないはず
            for (String kindKey : kindKeys) {
                if (this.isVerbose()) {
                    System.out.println("Kined = " + kindKey);
                }
                generateTelegram(kindMap.get(kindKey), argDirectoryTarget);
            }
        }

        /*
         * 次に電文処理を生成します。
         * 現時点では micronaut 向けの controller を生成します。
         * 将来的には tomcat 向けの abstract クラスにも対応します。
         */
        generateProcess(argProcessStructure, argDirectoryTarget);
    }

    /**
     * 収集された情報を元に、電文処理のソースコードを自動生成します。
     *
     * @param argProcessStructure
     *            メタファイルから収集できた処理構造データ。
     * @param argDirectoryTarget
     *            ソースコードの出力先フォルダ。
     */
    public void generateProcess(
            final BlancoRestGeneratorTelegramProcess argProcessStructure,
            final File argDirectoryTarget) {

        /*
         * 出力ディレクトリはant taskのtargetStyel引数で
         * 指定された書式で出力されます。
         * 従来と互換性を保つために、指定がない場合は blanco/main
         * となります。
         * by tueda, 2019/08/30
         */
        String strTarget = argDirectoryTarget
                .getAbsolutePath(); // advanced
        if (!this.isTargetStyleAdvanced()) {
            strTarget += "/main"; // legacy
        }
        final File fileBlancoMain = new File(strTarget);

        // パッケージ名の置き換えオプションが指定されていれば置き換え
        // Suffix があればそちらが優先です。
        String processPackage = argProcessStructure.getPackage();
        if (argProcessStructure.getPackageSuffix() != null && argProcessStructure.getPackageSuffix().length() > 0) {
            processPackage = processPackage + "." + argProcessStructure.getPackageSuffix();
        } else if (argProcessStructure.getOverridePackage() != null && argProcessStructure.getOverridePackage().length() > 0) {
            processPackage = argProcessStructure.getOverridePackage();
        }

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(processPackage, "このソースコードは blanco Frameworkによって自動生成されています。");
        fCgSourceFile.setEncoding(fEncoding);
        fCgClass = fCgFactory.createClass(BlancoRestGeneratorConstants.PREFIX_ABSTRACT + argProcessStructure.getName(),
                BlancoStringUtil.null2Blank(argProcessStructure
                        .getDescription()));
        fCgSourceFile.getClassList().add(fCgClass);

        // ApiBase クラスを継承 (parserで設定済み)
        if (BlancoStringUtil.null2Blank(argProcessStructure.getExtends())
                .length() > 0) {
            fCgClass.getExtendClassList().add(
                    fCgFactory.createType(argProcessStructure.getExtends()));
        }

        // abstrac フラグをセット
        fCgClass.setAbstract(true);

        // 説明
        if (argProcessStructure.getDescription() != null) {
            fCgSourceFile.setDescription(argProcessStructure
                    .getDescription());
        }

        /* クラスのannotation を設定します */
        List annotationList = argProcessStructure.getAnnotationList();
        if (annotationList != null && annotationList.size() > 0) {
            fCgClass.getAnnotationList().addAll(argProcessStructure.getAnnotationList());
            /* tueda DEBUG */
            if (this.isVerbose()) {
                System.out.println("generateTelegramProcess : class annotation = " + argProcessStructure.getAnnotationList().get(0));
            }
        }

        // サービスメソッドを生成します。
        // RequestHeader, ResponseHeader はここで確定しておく
        String requestHeaderClass = argProcessStructure.getRequestHeaderClass();
        String responseHeaderClass = argProcessStructure.getResponseHeaderClass();
        String requestHeaderIdSimple = null;
        if (requestHeaderClass != null && requestHeaderClass.length() > 0) {
            fCgSourceFile.getImportList().add(requestHeaderClass); // fullPackageで指定されている前提
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

        // isAuthenticationRequired メソッドの上書き
        overrideAuthenticationRequired(argProcessStructure);

        // required 文を出力しない ... 将来的には xls で指定するように？
         //fCgSourceFile.setIsImport(false);

        BlancoCgTransformerFactory.getSourceTransformer(fTargetLang).transform(
                fCgSourceFile, fileBlancoMain);
    }


    /**
     * Serviceメソッドを実装します。
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
             * デフォルトの電文IDを作成する
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
             * このクラスのパッケージ名を探す
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
                /* このメソッドは未対応 */
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

        // Processor の定義
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
         * デフォルトの例外（BlancoRestException）を throws に加える
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
         * デフォルトの例外（BlancoRestException）を throws に加える
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

        // メソッドの実装
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

        /* Excel sheet に電文定義がない場合にここにくるので、その場合は電文処理シートの定義を使う */
        cgExecutorMethod.getParameterList().add(
                fCgFactory.createParameter("argRequest", argSuperRequestId,
                        fBundle
                                .getXml2sourceFileExecutorArgLangdoc()));
        cgExecutorMethod.setReturn(fCgFactory.createReturn(argSuperResponseId,
                fBundle.getXml2sourceFileExecutorReturnLangdoc()));

        /*
         * デフォルトの例外（BlancoRestException）を throws に加える
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

        // メソッドの実装
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

        // メソッドの実装
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
         * メソッドの実装
         * 定義されていないときはnullを返す
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
         * メソッドの実装
         * 定義されていないときはnullを返す
         */
        final List<String> listLine = cgResponseIdMethod.getLineList();
        if(responseIdName == null) {
            listLine.add("return null" + BlancoCgLineUtil.getTerminator(fTargetLang));
        } else {
            listLine.add("return " + "\"" + packageName + "." + responseIdName + "\""
                    + BlancoCgLineUtil.getTerminator(fTargetLang));
        }
    }

    /**
     * 電文クラスを生成します。
     *
     * @param argTelegramStructure
     * @param argDirectoryTarget
     */
    private void generateTelegram(
            final BlancoRestGeneratorTelegram argTelegramStructure,
            final File argDirectoryTarget) {

        /*
         * 出力ディレクトリはant taskのtargetStyel引数で
         * 指定された書式で出力されます。
         * 従来と互換性を保つために、指定がない場合は blanco/main
         * となります。
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

        // パッケージ名の置き換えオプションが指定されていれば置き換え
        // Suffix があればそちらが優先です。
        String telegramPackage = argTelegramStructure.getPackage();
        if (argTelegramStructure.getPackageSuffix() != null && argTelegramStructure.getPackageSuffix().length() > 0) {
            telegramPackage = telegramPackage + "." + argTelegramStructure.getPackageSuffix();
        } else if (argTelegramStructure.getOverridePackage() != null && argTelegramStructure.getOverridePackage().length() > 0) {
            telegramPackage = argTelegramStructure.getOverridePackage();
        }

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(telegramPackage, "このソースコードは blanco Frameworkによって自動生成されています。");
        fCgSourceFile.setEncoding(fEncoding);
        fCgSourceFile.setTabs(this.getTabs());
        // クラスを作成
        fCgClass = fCgFactory.createClass(argTelegramStructure.getName(),
                BlancoStringUtil.null2Blank(argTelegramStructure
                        .getDescription()));
        fCgSourceFile.getClassList().add(fCgClass);
        // 電文クラスは常に public。
        String access = "public";
        // 継承
        if (BlancoStringUtil.null2Blank(argTelegramStructure.getExtends())
                .length() > 0) {
            fCgClass.getExtendClassList().add(
                    fCgFactory.createType(argTelegramStructure.getExtends()));
        }
        // 実装
        for (int index = 0; index < argTelegramStructure.getImplementsList()
                .size(); index++) {
            final String impl = (String) argTelegramStructure.getImplementsList()
                    .get(index);
            fCgClass.getImplementInterfaceList().add(
                    fCgFactory.createType(impl));
        }

        // クラスのJavaDocを設定します。
        fCgClass.setDescription(argTelegramStructure.getDescription());

        /* クラスのannotation を設定します */
        List annotationList = argTelegramStructure.getAnnotationList();
        if (annotationList != null && annotationList.size() > 0) {
            fCgClass.getAnnotationList().addAll(argTelegramStructure.getAnnotationList());
            /* tueda DEBUG */
            if (this.isVerbose()) {
                System.out.println("BlancoRestGeneratorXml2SourceFile#generateTelegram : class annotation = " + argTelegramStructure.getAnnotationList().get(0));
            }
        }

        /* クラスのimport を設定します */
        for (int index = 0; index < argTelegramStructure.getImportList()
                .size(); index++) {
            final String imported = (String) argTelegramStructure.getImportList()
                    .get(index);
            fCgSourceFile.getImportList().add(imported);
        }

        if (this.isVerbose()) {
            System.out.println("BlancoRestGeneratorXml2SourceFile: Start create properties : " + argTelegramStructure.getName());
        }

        // 電文定義・一覧
        for (int indexField = 0; indexField < argTelegramStructure.getListField()
                .size(); indexField++) {
            // おのおののフィールドを処理します。
            final BlancoRestGeneratorTelegramField fieldStructure =
                    argTelegramStructure.getListField().get(indexField);

            // 必須項目が未設定の場合には例外処理を実施します。
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

            // フィールドの生成。
            expandField(fieldStructure);
            // getter/setterメソッドの生成
            expandMethodSet(fieldStructure);
            expandMethodGet(fieldStructure);
            expandMethodType(fieldStructure);
        }
        expandMethodToString(argTelegramStructure);

        // 収集された情報を元に実際のソースコードを自動生成。
        BlancoCgTransformerFactory.getJavaSourceTransformer().transform(
                fCgSourceFile, fileBlancoMain);
    }

    /**
     * フィールドを展開します。
     *
     * @param fieldLook
     */
    private void expandField(
            final BlancoRestGeneratorTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        switch (fSheetLang) {
            case BlancoCgSupportedLang.PHP:
                if (fieldLook.getFieldType() == "java.lang.Integer") fieldLook.setFieldType("java.lang.Long");
                break;
            /* 対応言語を増やす場合はここに case を追記します */
        }

        final BlancoCgField cgField = fCgFactory.createField("f" + fieldName,
                fieldLook.getFieldType(), "");
        fCgClass.getFieldList().add(cgField);
        cgField.setAccess("private");

        cgField.setDescription(fBundle.getXml2sourceFileFieldName(fieldLook
                .getName()));
        cgField.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileFieldType(fieldLook.getFieldType()));
        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgField.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }
    }

    /**
     * setメソッドを展開します。
     *
     * @param fieldLook
     */
    private void expandMethodSet(
            final BlancoRestGeneratorTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("set"
                + fieldName, fBundle.getXml2sourceFileSetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileSetLangdoc02(fieldLook.getFieldType()));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        cgMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + fieldName, fieldLook
                        .getFieldType(), fBundle
                        .getXml2sourceFileSetArgLangdoc(fieldLook.getName())));

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                + "this.f" + fieldName + " = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "arg"
                + fieldName + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * getメソッドを展開します。
     *
     * @param fieldLook
     */
    private void expandMethodGet(
            final BlancoRestGeneratorTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("get"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileGetLangdoc02(fieldLook.getFieldType()));

        cgMethod.setReturn(fCgFactory.createReturn(fieldLook.getFieldType(), fBundle
                .getXml2sourceFileGetReturnLangdoc(fieldLook.getName())));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                        + "this." + "f" + fieldName
                        + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * typeメソッドを展開します
     *
     * @param fieldLook
     */
    private void expandMethodType(
            final BlancoRestGeneratorTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("type"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.setStatic(true);

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileTypeLangdoc02("java.lang.String"));

        cgMethod.setReturn(fCgFactory.createReturn("java.lang.String", fBundle
                .getXml2sourceFileTypeReturnLangdoc(fieldLook.getName())));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return " +
                        BlancoCgLineUtil.getStringLiteralEnclosure(BlancoCgSupportedLang.JAVA) +
                                fieldLook.getFieldType() +
                                BlancoCgLineUtil.getStringLiteralEnclosure(BlancoCgSupportedLang.JAVA) +
                                BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * toStringメソッドを展開します。
     *
     * @param argProcessStructure
     */
    private void expandMethodToString(
            final BlancoRestGeneratorTelegram argProcessStructure) {
        final BlancoCgMethod method = fCgFactory.createMethod("toString",
                "このバリューオブジェクトの文字列表現を取得します。");
        fCgClass.getMethodList().add(method);

        method.getLangDoc().getDescriptionList().add(
                "オブジェクトのシャロー範囲でしかtoStringされない点に注意して利用してください。");
        method
                .setReturn(fCgFactory.createReturn("java.lang.String",
                        "バリューオブジェクトの文字列表現。"));

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
                listLine.add("// TODO 配列は未対応です。");
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
