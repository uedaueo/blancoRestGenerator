/*
 * blanco Framework
 * Copyright (C) 2004-2009 IGA Tosiki
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package blanco.restgenerator.task;

import blanco.cg.BlancoCgSupportedLang;
import blanco.restgenerator.BlancoRestGeneratorConstants;
import blanco.restgenerator.BlancoRestGeneratorMeta2Xml;
import blanco.restgenerator.BlancoRestGeneratorUtil;
import blanco.restgenerator.BlancoRestGeneratorXml2SourceFile;
import blanco.restgenerator.resourcebundle.BlancoRestGeneratorResourceBundle;
import blanco.restgenerator.task.valueobject.BlancoRestGeneratorProcessInput;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;

public class BlancoRestGeneratorProcessImpl implements
        BlancoRestGeneratorProcess {
    /**
     * このプロダクトのリソースバンドルへのアクセスオブジェクト。
     */
    private final BlancoRestGeneratorResourceBundle fBundle = new BlancoRestGeneratorResourceBundle();

    /**
     * {@inheritDoc}
     */
    public int execute(final BlancoRestGeneratorProcessInput input) {
        System.out.println("- " + BlancoRestGeneratorConstants.PRODUCT_NAME
                + " (" + BlancoRestGeneratorConstants.VERSION + ")" + " for " + input.getSheetType());

        try {
            final File fileMetadir = new File(input.getMetadir());
            if (fileMetadir.exists() == false) {
                throw new IllegalArgumentException(fBundle
                        .getAnttaskErr001(input.getMetadir()));
            }

            /*
             * 改行コードを決定します。
             */
            String LF = "\n";
            String CR = "\r";
            String CRLF = CR + LF;
            String lineSeparatorMark = input.getLineSeparator();
            String lineSeparator = "";
            if ("LF".equals(lineSeparatorMark)) {
                lineSeparator = LF;
            } else if ("CR".equals(lineSeparatorMark)) {
                lineSeparator = CR;
            } else if ("CRLF".equals(lineSeparatorMark)) {
                lineSeparator = CRLF;
            }
            if (lineSeparator.length() != 0) {
                System.setProperty("line.separator", lineSeparator);
                if (input.getVerbose()) {
                    System.out.println("lineSeparator try to change to " + lineSeparatorMark);
                    String newProp = System.getProperty("line.separator");
                    String newMark = "other";
                    if (LF.equals(newProp)) {
                        newMark = "LF";
                    } else if (CR.equals(newProp)) {
                        newMark = "CR";
                    } else if (CRLF.equals(newProp)) {
                        newMark = "CRLF";
                    }
                    System.out.println("New System Props = " + newMark);
                }
            }

            /*
             * targetdir, targetStyleの処理。
             * 生成されたコードの保管場所を設定する。
             * targetstyle = blanco:
             *  targetdirの下に main ディレクトリを作成
             * targetstyle = maven:
             *  targetdirの下に main/java ディレクトリを作成
             * targetstyle = free:
             *  targetdirをそのまま使用してディレクトリを作成。
             *  ただしtargetdirがからの場合はデフォルト文字列(blanco)使用する。
             * by tueda, 2019/08/30
             */
            String strTarget = input.getTargetdir();
            String style = input.getTargetStyle();
            // ここを通ったら常にtrue
            boolean isTargetStyleAdvanced = true;

            if (style != null && BlancoRestGeneratorConstants.TARGET_STYLE_MAVEN.equals(style)) {
                strTarget = strTarget + "/" + BlancoRestGeneratorConstants.TARGET_DIR_SUFFIX_MAVEN;
            } else if (style == null ||
                    !BlancoRestGeneratorConstants.TARGET_STYLE_FREE.equals(style)) {
                strTarget = strTarget + "/" + BlancoRestGeneratorConstants.TARGET_DIR_SUFFIX_BLANCO;
            }
            /* style が free だったらtargetdirをそのまま使う */
            if (input.getVerbose()) {
                System.out.println("/* tueda */ TARGETDIR = " + strTarget);
            }

            /*
             * 共通の設定
             */
            BlancoRestGeneratorUtil.encoding = input.getEncoding();
            BlancoRestGeneratorUtil.isVerbose = input.getVerbose();
            BlancoRestGeneratorUtil.packageSuffix = input.getPackageSuffix();
            BlancoRestGeneratorUtil.overridePackage = input.getOverridePackage();
            BlancoRestGeneratorUtil.overrideLocation = input.getOverrideLocation();
            BlancoRestGeneratorUtil.voPackageSuffix = input.getVoPackageSuffix();
            BlancoRestGeneratorUtil.voOverridePackage = input.getVoOverridePackage();
            BlancoRestGeneratorUtil.ignoreDefault = input.getIgnoreDefault();
            BlancoRestGeneratorUtil.ignoreAnnotation = input.getIgnoreAnnotation();
            BlancoRestGeneratorUtil.ignoreImport = input.getIgnoreImport();
            BlancoRestGeneratorUtil.telegramPackage = input.getTelegrampackage();
            BlancoRestGeneratorUtil.processBaseClass = input.getProcessBaseClass();
            BlancoRestGeneratorUtil.defaultExceptionClass = input.getDefaultExceptionClass();
            BlancoRestGeneratorUtil.serverType = input.getServerType();
            BlancoRestGeneratorUtil.createServiceMethod = !input.getClient();
            BlancoRestGeneratorUtil.telegramsOnly = input.getTelegramsOnly();

            /*
             * validator を作る時に使うために，
             * ValueObject で既に定義されている（はずの）オブジェクトを取得しておく
             */
            BlancoRestGeneratorUtil.processValueObjects(input);

            // テンポラリディレクトリを作成。
            new File(input.getTmpdir()
                    + BlancoRestGeneratorConstants.TARGET_SUBDIRECTORY)
                    .mkdirs();

            // 指定されたメタディレクトリを処理します。
            new BlancoRestGeneratorMeta2Xml()
                    .processDirectory(fileMetadir, input.getTmpdir()
                            + BlancoRestGeneratorConstants.TARGET_SUBDIRECTORY);

            // XML化された中間ファイルからソースコードを生成
            final File[] fileMeta2 = new File(input.getTmpdir()
                    + BlancoRestGeneratorConstants.TARGET_SUBDIRECTORY)
                    .listFiles();
            for (int index = 0; index < fileMeta2.length; index++) {
                if (fileMeta2[index].getName().endsWith(".xml") == false) {
                    continue;
                }

                final BlancoRestGeneratorXml2SourceFile xml2source = new BlancoRestGeneratorXml2SourceFile();
                xml2source.setEncoding(input.getEncoding());
                xml2source.setSheetLang(new BlancoCgSupportedLang().convertToInt(input.getSheetType()));
                xml2source.setTargetStyleAdvanced(isTargetStyleAdvanced);
                xml2source.setVerbose(input.getVerbose());
                xml2source.process(fileMeta2[index], true, new File(strTarget));
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.toString());
        } catch (TransformerException ex) {
            throw new IllegalArgumentException(ex.toString());
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            throw ex;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean progress(final String argProgressMessage) {
        System.out.println(argProgressMessage);
        return false;
    }
}
