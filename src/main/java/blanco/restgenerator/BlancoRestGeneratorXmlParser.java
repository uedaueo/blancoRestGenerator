package blanco.restgenerator;

import blanco.commons.util.BlancoNameUtil;
import blanco.commons.util.BlancoStringUtil;
import blanco.restgenerator.resourcebundle.BlancoRestGeneratorResourceBundle;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegram;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegramField;
import blanco.restgenerator.valueobject.BlancoRestGeneratorTelegramProcess;
import blanco.xml.bind.BlancoXmlBindingUtil;
import blanco.xml.bind.BlancoXmlUnmarshaller;
import blanco.xml.bind.valueobject.BlancoXmlDocument;
import blanco.xml.bind.valueobject.BlancoXmlElement;

import java.io.File;
import java.util.*;

public class BlancoRestGeneratorXmlParser {

    /**
     * このプロダクトのリソースバンドルへのアクセスオブジェクト。
     */
    private final BlancoRestGeneratorResourceBundle fBundle = new BlancoRestGeneratorResourceBundle();


    private boolean fVerbose = false;
    public void setVerbose(boolean argVerbose) {
        this.fVerbose = argVerbose;
    }
    public boolean isVerbose() {
        return fVerbose;
    }

    /**
     * 中間XMLファイルのXMLドキュメントをパースして、バリューオブジェクト情報の配列を取得します。
     *
     * @param argMetaXmlSourceFile
     *            中間XMLファイル。
     * @return パースの結果得られたバリューオブジェクト情報の配列。
     */
    public BlancoRestGeneratorTelegramProcess[] parse(
            final File argMetaXmlSourceFile) {
        final BlancoXmlDocument documentMeta = new BlancoXmlUnmarshaller()
                .unmarshal(argMetaXmlSourceFile);
        if (documentMeta == null) {
            System.out.println("Fail to unmarshal XML.");
            return null;
        }

        // ルートエレメントを取得します。
        final BlancoXmlElement elementRoot = BlancoXmlBindingUtil
                .getDocumentElement(documentMeta);
        if (elementRoot == null) {
            // ルートエレメントが無い場合には処理中断します。
            if (this.isVerbose()) {
                System.out.println("praser !!! NO ROOT ELEMENT !!!");
            }
            return null;
        }

        if (this.isVerbose()) {
            System.out.println("[" + argMetaXmlSourceFile.getName() + "の処理を開始します]");
        }

        // まず電文の一覧を取得します。
        Map<String, BlancoRestGeneratorTelegram> telegramStructureMap =
                parseTelegrams(elementRoot);

        if (telegramStructureMap.isEmpty()) {
            if (this.isVerbose()) {
                System.out.println("praser !!! NO telegramStructureMap !!!");
            }
            return null;
        } else {
            if (this.isVerbose()) {
                System.out.println("parser !!! got telegrams = " + telegramStructureMap.size());
            }
        }

        // 次に電文処理を取得します。
        return parseTelegramProcess(elementRoot,telegramStructureMap);
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、電文名で検索可能な電文情報の一覧を作成します。
     *
     * @param argElementRoot
     * @return
     */
    public Map<String, BlancoRestGeneratorTelegram> parseTelegrams(final BlancoXmlElement argElementRoot) {

        Map <String, BlancoRestGeneratorTelegram> telegramStructureMap = new HashMap<>();

        // sheet(Excelシート)のリストを取得します。
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(argElementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        for (int index = 0; index < sizeListSheet; index++) {
            // おのおののシートを処理します。
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // シートから詳細な情報を取得します。
            final BlancoRestGeneratorTelegram telegramStructure = parseTelegramSheet(elementSheet);

            // 電文情報を電文IDをキーにMapに格納する
            if (telegramStructure != null) {
                telegramStructureMap.put(telegramStructure.getName(), telegramStructure);
            }
        }
        /**
         *  InputとOutputが対になっているかのチェックは
         *  TelegramProcessStructureへの格納時にを行う
         */
        return telegramStructureMap;
    }


    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、電文情報を取得します。
     * @param argElementSheet
     * @return
     */
    private BlancoRestGeneratorTelegram parseTelegramSheet(final BlancoXmlElement argElementSheet) {

        final BlancoRestGeneratorTelegram telegramStructure = new BlancoRestGeneratorTelegram();

        // 共通情報を取得します。
        final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                .getElement(argElementSheet, fBundle
                        .getMeta2xmlTelegramCommon());
        if (elementCommon == null) {
            // commonが無い場合には、このシートの処理をスキップします。
            // System.out.println("BlancoRestXmlSourceFile#process !!! NO COMMON !!!");
            return telegramStructure;
        }

        final String name = BlancoXmlBindingUtil.getTextContent(
                elementCommon, "name");
        if (BlancoStringUtil.null2Blank(name).trim().length() == 0) {
            // nameが空の場合には処理をスキップします。
            // System.out.println("BlancoRestXmlSourceFile#process !!! NO NAME !!!");
            return telegramStructure;
        }

        final String httpMethod = BlancoXmlBindingUtil.getTextContent(
                elementCommon, "telegramMethod");
        if (BlancoStringUtil.null2Blank(httpMethod).trim().length() == 0) {
            // httpMethodが空の場合には処理をスキップします。
            // System.out.println("BlancoRestXmlSourceFile#process !!! NO NAME !!!");
            return telegramStructure;
        }

        if (this.isVerbose()) {
            System.out.println("BlancoRestGeneratorXmlParser#parseTelegramSheet name = " + name);
        }

        // はじめにPackage上書き系オプションを設定します。
        telegramStructure.setPackageSuffix(BlancoRestGeneratorUtil.packageSuffix);
        telegramStructure.setOverridePackage(BlancoRestGeneratorUtil.overridePackage);
        // telegram には location はありません。

        // 電文定義・共通
        this.parseTelegramCommon(elementCommon, telegramStructure);

        // 電文定義・継承
        this.parseTelegramExtends(telegramStructure);

        // 電文定義・実装
        final List<BlancoXmlElement> interfaceList = BlancoXmlBindingUtil
                .getElementsByTagName(argElementSheet,
                        fBundle.getMeta2xmlTelegramImplements());
        if (interfaceList != null && interfaceList.size() != 0) {
            final BlancoXmlElement elementInterfaceRoot = interfaceList.get(0);
            this.parseTelegramImplements(elementInterfaceRoot, telegramStructure);
        }

        /*
         * TelegramDefinition import:
         * prefer definition for java or java only if ignoreImport is true.
         */
        List<BlancoXmlElement> importList = BlancoXmlBindingUtil
                .getElementsByTagName(argElementSheet, "blancotelegramJava-import");
        if (!BlancoRestGeneratorUtil.ignoreImport && (importList == null || importList.size() == 0)) {
            importList = BlancoXmlBindingUtil
                    .getElementsByTagName(argElementSheet, fBundle.getMeta2xmlTelegramImport());
        }
        if (importList != null && importList.size() != 0) {
            final BlancoXmlElement elementImportRoot = importList.get(0);
            this.parseTelegramImport(elementImportRoot, telegramStructure);
        }

        // 一覧情報を取得します。
        final List<BlancoXmlElement> listList = BlancoXmlBindingUtil
                .getElementsByTagName(argElementSheet, fBundle.getMeta2xmlTeregramList());
        if (listList != null && listList.size() != 0) {
            final BlancoXmlElement elementListRoot = listList.get(0);
            this.parseTelegramFields(elementListRoot, telegramStructure);
        }

        return telegramStructure;
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、「電文定義・共通」を取得します。
     *
     * @param argElementCommon
     * @param argTelegramStructure
     */
    private void parseTelegramCommon(final BlancoXmlElement argElementCommon, final BlancoRestGeneratorTelegram argTelegramStructure) {

        // 電文ID
        argTelegramStructure.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));
        // パッケージ
        argTelegramStructure.setPackage(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "package"));
        // 説明
        argTelegramStructure.setDescription(BlancoXmlBindingUtil.getTextContent(argElementCommon, "description"));
        // 電文種類 Input/Output
        argTelegramStructure.setTelegramType(BlancoXmlBindingUtil.getTextContent(argElementCommon, "type"));
        // HTTP メソッド
        argTelegramStructure.setTelegramMethod(BlancoXmlBindingUtil.getTextContent(argElementCommon, "telegramMethod"));

        /* クラスの annotation に対応 */
        String classAnnotation = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "annotationJava");
        if (BlancoStringUtil.null2Blank(classAnnotation).length() == 0 && !BlancoRestGeneratorUtil.ignoreAnnotation) {
            classAnnotation = BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "annotation");
        }
        if (BlancoStringUtil.null2Blank(classAnnotation).length() > 0) {
            argTelegramStructure.setAnnotationList(createAnnotaionList(classAnnotation));
        }
        /* micronaut では必ず @Introspected をつける */
        if ("micronaut".equalsIgnoreCase(BlancoRestGeneratorUtil.serverType)) {
            argTelegramStructure.getAnnotationList().add("Introspected");
            argTelegramStructure.getImportList().add("io.micronaut.core.annotation.Introspected");
        }

        /*
         * 電文クラスでは常に true
         */
        argTelegramStructure.setCreateImportList(true);

        /*
         * 電文クラスは常に
         *  フィールド名の変形
         * を行う
         */
        argTelegramStructure.setAdjustFieldName(true);
    }

    /**
     * blancoRestGenerator では、電文は常に
     * Api[Get|Post|Put|Delete]Telegram <- ApiTelegram
     * を継承します。
     *
     * @param argTelegramStructure
     */
    private void parseTelegramExtends(
            final BlancoRestGeneratorTelegram argTelegramStructure) {

        // method の NULL check は common で済み
        String method = argTelegramStructure.getTelegramMethod().toUpperCase();
        boolean isRequest = "Input".equals(argTelegramStructure.getTelegramType());
        String superClassId = "";
        if (BlancoRestGeneratorConstants.HTTP_METHOD_GET.endsWith(method)) {
            superClassId = isRequest ?
                    BlancoRestGeneratorConstants.DEFAULT_API_GET_REQUESTID :
                    BlancoRestGeneratorConstants.DEFAULT_API_GET_RESPONSEID;
        } else if (BlancoRestGeneratorConstants.HTTP_METHOD_POST.endsWith(method)) {
            superClassId = isRequest ?
                    BlancoRestGeneratorConstants.DEFAULT_API_POST_REQUESTID :
                    BlancoRestGeneratorConstants.DEFAULT_API_POST_RESPONSEID;
        } else if (BlancoRestGeneratorConstants.HTTP_METHOD_PUT.endsWith(method)) {
            superClassId = isRequest ?
                    BlancoRestGeneratorConstants.DEFAULT_API_PUT_REQUESTID :
                    BlancoRestGeneratorConstants.DEFAULT_API_PUT_RESPONSEID;
        } else if (BlancoRestGeneratorConstants.HTTP_METHOD_DELETE.endsWith(method)) {
            superClassId = isRequest ?
                    BlancoRestGeneratorConstants.DEFAULT_API_DELETE_REQUESTID :
                    BlancoRestGeneratorConstants.DEFAULT_API_DELETE_RESPONSEID;
        } else {
            throw new IllegalArgumentException("!!! NO SUCH METHOD !!! " + method);
        }
        /*
         * このクラスのパッケージ名を探す
         */
        String packageName = null;
        if (BlancoRestGeneratorUtil.telegramPackage != null && BlancoRestGeneratorUtil.telegramPackage.length() > 0) {
            packageName = BlancoRestGeneratorUtil.telegramPackage;
        } else {
            packageName = BlancoRestGeneratorUtil.searchPackageBySimpleName(superClassId);
        }
        String superClassIdCanon = superClassId;
        if (packageName != null) {
            superClassIdCanon = packageName + "." + superClassId;
        }
        if (isVerbose()) {
            System.out.println("Extends : " + superClassIdCanon);
        }
        argTelegramStructure.setExtends(superClassIdCanon);
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、「電文定義・実装」を取得します。
     *  @param argElementInterfaceRoot
     * @param argTelegramStructure
     */
    private void parseTelegramImplements(
            final BlancoXmlElement argElementInterfaceRoot,
            final BlancoRestGeneratorTelegram argTelegramStructure) {

        final List<BlancoXmlElement> listInterfaceChildNodes = BlancoXmlBindingUtil
                .getElementsByTagName(argElementInterfaceRoot, "interface");
        for (int index = 0;
             listInterfaceChildNodes != null &&
                     index < listInterfaceChildNodes.size();
             index++) {
            final BlancoXmlElement elementList = listInterfaceChildNodes
                    .get(index);

            final String interfaceName = BlancoXmlBindingUtil
                    .getTextContent(elementList, "name");
            if (interfaceName == null || interfaceName.trim().length() == 0) {
                continue;
            }
            /*
             * import 情報の作成
             */
            String fqInterface = interfaceName;
            if (argTelegramStructure.getCreateImportList()) {
                String packageName = BlancoRestGeneratorUtil.getPackageName(interfaceName);
                String className = BlancoRestGeneratorUtil.getSimpleClassName(interfaceName);
                if (packageName.length() == 0) {
                    /*
                     * このクラスのパッケージ名を探す
                     */
                    packageName = BlancoRestGeneratorUtil.searchPackageBySimpleName(className);
                    if (packageName != null & packageName.length() > 0) {
                        fqInterface = packageName + "." + className;
                    }
                }
                argTelegramStructure.getImplementsList().add(fqInterface);
            }
        }
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、「電文定義・インポート」を取得します。
     * インポートクラス名はCanonicalで記述されている前提です。
     * @param argElementListRoot
     * @param argTelegramStructure
     */
    private void parseTelegramImport(
            final BlancoXmlElement argElementListRoot,
            final BlancoRestGeneratorTelegram argTelegramStructure) {
        final List<BlancoXmlElement> listImportChildNodes = BlancoXmlBindingUtil
                .getElementsByTagName(argElementListRoot, "import");
        for (int index = 0;
             listImportChildNodes != null &&
                     index < listImportChildNodes.size();
             index++) {
            final BlancoXmlElement elementList = listImportChildNodes
                    .get(index);

            final String importName = BlancoXmlBindingUtil
                    .getTextContent(elementList, "name");
            if (importName == null || importName.trim().length() == 0) {
                continue;
            }
            argTelegramStructure.getImportList().add(
                    BlancoXmlBindingUtil
                            .getTextContent(elementList, "name"));
        }
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、「電文定義・一覧」を取得します。
     *  @param argElementListRoot
     * @param argTelegramStructure
     */
    private void parseTelegramFields(
            final BlancoXmlElement argElementListRoot,
            final BlancoRestGeneratorTelegram argTelegramStructure) {

        final List<BlancoXmlElement> listChildNodes = BlancoXmlBindingUtil
                .getElementsByTagName(argElementListRoot, "field");

        for (int index = 0; index < listChildNodes.size(); index++) {
            final BlancoXmlElement elementList = listChildNodes.get(index);
            final BlancoRestGeneratorTelegramField fieldStructure = new BlancoRestGeneratorTelegramField();

            fieldStructure.setNo(BlancoXmlBindingUtil.getTextContent(
                    elementList, "no"));
            fieldStructure.setName(BlancoXmlBindingUtil.getTextContent(
                    elementList, "fieldName"));

//            System.out.println("*** field name = " + fieldStructure.getName());

            if (fieldStructure.getName() == null
                    || fieldStructure.getName().trim().length() == 0) {
//                System.out.println("*** NO NAME SKIP!!! ");
                continue;
            }

            /*
             * 型の取得。Java 型が指定されている場合は Java 型を優先。
             * デフォルト型指定にはphp風な型が定義されている前提。
             * ここで Java 風の型名に変えておく
             */
            String phpType = BlancoXmlBindingUtil.getTextContent(elementList, "fieldTypeJava");
            if (BlancoStringUtil.null2Blank(phpType).length() == 0) {
                phpType = BlancoXmlBindingUtil.getTextContent(elementList, "fieldType");
                if (BlancoStringUtil.null2Blank(phpType).length() == 0) {
                    // 型は必須
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileErr005(
                            argTelegramStructure.getName(),
                            fieldStructure.getName()
                    ));
                }
            }
            String javaType = BlancoRestGeneratorUtil.parsePhpTypes(phpType, false);
            fieldStructure.setFieldType(javaType);

            /* Generic に対応 */
            String phpGeneric = BlancoXmlBindingUtil.getTextContent(elementList, "fieldGenericJava");
            if (BlancoStringUtil.null2Blank(phpGeneric).length() == 0) {
                phpGeneric = BlancoXmlBindingUtil.getTextContent(elementList, "fieldGeneric");
            }
            if (BlancoStringUtil.null2Blank(phpGeneric).length() != 0) {
                String kotlinGeneric = BlancoRestGeneratorUtil.parsePhpTypes(phpGeneric, true);
                fieldStructure.setFieldGeneric(kotlinGeneric);
            }

            /* annnotation に対応 */
            String fieldAnnotation = BlancoXmlBindingUtil.getTextContent(elementList, "fieldAnnotationJava");
            if (BlancoStringUtil.null2Blank(fieldAnnotation).length() == 0 && !BlancoRestGeneratorUtil.ignoreAnnotation) {
                fieldAnnotation = BlancoXmlBindingUtil.getTextContent(elementList, "fieldAnnotation");
            }
            if (BlancoStringUtil.null2Blank(fieldAnnotation).length() != 0) {
                fieldStructure.setFieldAnnotationList(createAnnotaionList(fieldAnnotation));
            }

            // required に対応 (NotNullアノテーションの付与）
            fieldStructure.setFieldRequired("true".equals(BlancoXmlBindingUtil
                    .getTextContent(elementList, "fieldRequired")));
            if (fieldStructure.getFieldRequired()) {
                fieldStructure.getFieldAnnotationList().add("NotNull");
                argTelegramStructure.getImportList().add("javax.validation.constraints.NotNull");
            }

            // 説明
            fieldStructure.setDescription(BlancoXmlBindingUtil
                    .getTextContent(elementList, "fieldDescription"));
            final String[] lines = BlancoNameUtil.splitString(
                    fieldStructure.getDescription(), '\n');
            for (int indexLine = 0; indexLine < lines.length; indexLine++) {
                if (indexLine == 0) {
                    fieldStructure.setDescription(lines[indexLine]);
                } else {
                    // 複数行の description については、これを分割して格納します。
                    // ２行目からは、適切に文字参照エンコーディングが実施されているものと仮定します。
                    fieldStructure.getDescriptionList().add(
                            lines[indexLine]);
                }
            }

            // デフォルト
            String fieldDefault = BlancoXmlBindingUtil.getTextContent(
                    elementList, "defaultJava");
            if (BlancoStringUtil.null2Blank(fieldDefault).length() == 0 && !BlancoRestGeneratorUtil.ignoreDefault) {
                fieldDefault = BlancoXmlBindingUtil.getTextContent(
                        elementList, "default");
            }
            fieldStructure.setDefault(fieldDefault);

            // 長さ
            String strMinLength = BlancoXmlBindingUtil
                    .getTextContent(elementList, "minLength");
            if (strMinLength != null) {
                try {
                    int minLength = Integer.parseInt(strMinLength);
                    fieldStructure.setMinLength(minLength);

                } catch (NumberFormatException e) {
                    System.out.println(fBundle.getXml2sourceFileErr008(argTelegramStructure.getName(), fieldStructure.getName()));
                }
            }
            String strMaxLength = BlancoXmlBindingUtil
                    .getTextContent(elementList, "maxLength");
            if (strMaxLength != null) {
                try {
                    int maxLength = Integer.parseInt(strMaxLength);
                    fieldStructure.setMaxLength(maxLength);
                } catch (NumberFormatException e) {

                }
            }

            // 最大最小
            fieldStructure.setMinInclusive(BlancoXmlBindingUtil
                    .getTextContent(elementList, "minInclusive"));
            fieldStructure.setMaxInclusive(BlancoXmlBindingUtil
                    .getTextContent(elementList, "maxInclusive"));
            // 正規表現
            fieldStructure.setPattern(BlancoXmlBindingUtil.getTextContent(
                    elementList, "pattern"));

            argTelegramStructure.getListField().add(fieldStructure);
        }
    }

    /**
     *
     *
     * @param argHeaderElementList
     * @param argImportHeaderList
     * @return
     */
    private List<String> parseHeaderList(final List<BlancoXmlElement> argHeaderElementList, final Map<String, List<String>> argImportHeaderList) {
        if (this.isVerbose()) {
            System.out.println("BlancoRestGeneratorXmlParser#parseHeaderList: Start parseHeaderList.");
        }

        List<String> headerList = new ArrayList<>();

        /*
         * header の一覧作成
         * まず、定義書に書かれたものをそのまま出力します。
         */
        if (argHeaderElementList != null && argHeaderElementList.size() > 0) {
            final BlancoXmlElement elementHeaderRoot = argHeaderElementList.get(0);
            final List<BlancoXmlElement> listHeaderChildNodes = BlancoXmlBindingUtil
                    .getElementsByTagName(elementHeaderRoot, "header");
            for (int index = 0; index < listHeaderChildNodes.size(); index++) {
                final BlancoXmlElement elementList = listHeaderChildNodes
                        .get(index);

                final String headerName = BlancoXmlBindingUtil
                        .getTextContent(elementList, "name");
                if (this.isVerbose()) {
                    System.out.println("BlancoRestGeneratorXmlParser#parseHeaderList header = " + headerName);
                }
                if (headerName == null || headerName.trim().length() == 0) {
                    continue;
                }
                headerList.add(
                        BlancoXmlBindingUtil
                                .getTextContent(elementList, "name"));
            }
        }

        /*
         * 次に、自動生成されたものを出力します。
         * 現在の方式だと、以下の前提が必要。
         *  * 1ファイルに1クラスの定義
         *  * 定義シートでは Java/kotlin 式の package 表記でディレクトリを表現
         * TODO: 定義シート上にファイルの配置ディレクトリを定義できるようにすべし？
         */
        if (argImportHeaderList != null && argImportHeaderList.size() > 0) {
            Set<String> fromList = argImportHeaderList.keySet();
            for (String strFrom : fromList) {
                StringBuffer sb = new StringBuffer();
                sb.append("import { ");
                List<String> classNameList = argImportHeaderList.get(strFrom);
                int count = 0;
                for (String className : classNameList) {
                    if (count > 0) {
                        sb.append(", ");
                    }
                    sb.append(className);
                    count++;
                }
                if (count > 0) {
                    sb.append(" } from \"" + strFrom + "\"");
                    if (this.isVerbose()) {
                        System.out.println("BlancoRestGeneratorXmlParser#parseHeaderList import = " + sb.toString());
                    }
                    headerList.add(sb.toString());
                }
            }
        }

        return headerList;
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、バリューオブジェクト情報の配列を取得します。
     *
     * @param argElementRoot
     *            中間XMLファイルのXMLドキュメント。
     * @return パースの結果得られたバリューオブジェクト情報の配列。
     */
    public BlancoRestGeneratorTelegramProcess[] parseTelegramProcess(
            final BlancoXmlElement argElementRoot,
            final Map <String, BlancoRestGeneratorTelegram> argTelegramStructureMap) {

        final List<BlancoRestGeneratorTelegramProcess> processStructures = new ArrayList<>();

        // sheet(Excelシート)のリストを取得します。
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(argElementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        if (this.isVerbose()) {
            System.out.println("Sheet Size Is " + sizeListSheet);
        }
        for (int index = 0; index < sizeListSheet; index++) {
            // おのおののシートを処理します。
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // シートから詳細な情報を取得します。
            final BlancoRestGeneratorTelegramProcess structure =
                    parseProcessSheet(elementSheet, argTelegramStructureMap);

            if (structure != null) {
                processStructures.add(structure);
            }
        }

        final BlancoRestGeneratorTelegramProcess[] result = new BlancoRestGeneratorTelegramProcess[processStructures
                .size()];
        processStructures.toArray(result);
        return result;
    }

    /**
     *
     * @param argElementSheet
     * @param argTelegramStructureMap
     * @return
     */
    private BlancoRestGeneratorTelegramProcess parseProcessSheet(
            final BlancoXmlElement argElementSheet,
            final Map <String, BlancoRestGeneratorTelegram> argTelegramStructureMap) {
        BlancoRestGeneratorTelegramProcess processStructure = new BlancoRestGeneratorTelegramProcess();

        // 共通情報を取得します。
        final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                .getElement(argElementSheet, fBundle
                        .getMeta2xmlProcessCommon());
        if (elementCommon == null) {
            // commonが無い場合には、このシートの処理をスキップします。
//             System.out.println("BlancoRestXmlSourceFile#processTelegramProcess !!! NO COMMON !!!");
            return null;
        }

        final String name = BlancoXmlBindingUtil.getTextContent(
                elementCommon, "name");

        if (BlancoStringUtil.null2Blank(name).length() == 0) {
            // nameが空の場合には処理をスキップします。
             System.out.println("BlancoRestXmlSourceFile#processTelegramProcess !!! NO NAME !!!");
            return null;
        }

        if (argTelegramStructureMap == null || argTelegramStructureMap.isEmpty()) {
            System.out.println("parseProcessSheet !!! NO TelegramStructureMap for " + name + " !!!");
            return null;
        }

        if (this.isVerbose()) {
            System.out.println("BlancoRestGeneratorXmlParser#parseProcessSheet name = " + name);
        }

        // パッケージ上書き系の設定をします。
        processStructure.setPackageSuffix(BlancoRestGeneratorUtil.packageSuffix);
        processStructure.setOverridePackage(BlancoRestGeneratorUtil.overridePackage);
        processStructure.setOverrideLocation(BlancoRestGeneratorUtil.overrideLocation);

        // 電文処理定義・共通
        parseProcessCommon(elementCommon, processStructure);

        /*
         * 電文処理定義・継承
         * 電文処理は常にApiBaseを継承する。
         * processBaseClassオプションで指定が無い場合は
         * blanco.restgenerator.common.ApiBase を継承する。
         *
         */
        if (!"micronaut".equalsIgnoreCase(BlancoRestGeneratorUtil.serverType)) {
            parseProcessExtends(processStructure);
        }

        // 電文処理定義・実装（通常はしない）
        final List<BlancoXmlElement> interfaceList = BlancoXmlBindingUtil
                .getElementsByTagName(argElementSheet, fBundle.getMeta2xmlProcessImplements());
        if (interfaceList != null && interfaceList.size() != 0) {
            final BlancoXmlElement elementInterfaceRoot = interfaceList.get(0);
            parseProcessImplements(elementInterfaceRoot, processStructure);
        }

        // 電文処理定義・インポート
        final List<BlancoXmlElement> importList = BlancoXmlBindingUtil
                .getElementsByTagName(argElementSheet, fBundle.getMeta2xmlProcessImport());
        if (importList != null && importList.size() != 0) {
            final BlancoXmlElement elementInterfaceRoot = importList.get(0);
            parseProcessImport(elementInterfaceRoot, processStructure);
        }

        /*
         * 電文書IDから電文IDを決定し、定義されているもののみをprocessStructureに設定します。
         * 電文IDは以下のルールで決定されます。
         * <電文処理ID> + <Method> + <Request|Response>
         */
        if (!this.linkTelegramToProcess(processStructure.getName(), argTelegramStructureMap, processStructure)) {
            /* 電文が未定義またはInとOutが揃っていない */
            System.out.println("!!! Invalid Telegram !!! for " + processStructure.getName());
            return null;
        }

        return processStructure;
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、「電文定義・共通」を取得します。
     *
     * @param argElementCommon
     * @param argProcessStructure
     */
    private void parseProcessCommon(final BlancoXmlElement argElementCommon, final BlancoRestGeneratorTelegramProcess argProcessStructure) {


        // 電文処理ID
        argProcessStructure.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));
        // 説明
        argProcessStructure.setDescription(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "description"));
        if (BlancoStringUtil.null2Blank(argProcessStructure.getDescription())
                .length() > 0) {
            final String[] lines = BlancoNameUtil.splitString(argProcessStructure
                    .getDescription(), '\n');
            for (int index = 0; index < lines.length; index++) {
                if (index == 0) {
                    argProcessStructure.setDescription(lines[index]);
                }
            }
        }

        // WebサービスID
        argProcessStructure.setServiceId(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "webServiceId"));

        // ロケーション
        argProcessStructure.setLocation(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "location"));

        // パッケージ
        argProcessStructure.setPackage(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "package"));

        // 配置ディレクトリ
        argProcessStructure.setBasedir(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "basedir"));

        // 実装ディレクトリ
        String impleDir = BlancoXmlBindingUtil.getTextContent(argElementCommon, "impledirJava");
        if (BlancoStringUtil.null2Blank(impleDir).length() == 0) {
            impleDir = BlancoXmlBindingUtil.getTextContent(argElementCommon, "impledirKt");
        }
        argProcessStructure.setImpleDirKt(impleDir);

        // アノテーション
        String classAnnotation = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "annotationJava");
        if (BlancoStringUtil.null2Blank(classAnnotation).length() == 0 && !BlancoRestGeneratorUtil.ignoreAnnotation) {
            classAnnotation = BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "annotation");
        }
        if (BlancoStringUtil.null2Blank(classAnnotation).length() > 0) {
            argProcessStructure.setAnnotationList(createAnnotaionList(classAnnotation));
        }

        // リクエストヘッダクラス情報
        String requestHeaderClass = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "requestHeaderClassJava");
        if (BlancoStringUtil.null2Blank(requestHeaderClass).length() == 0) {
            requestHeaderClass = BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "requestHeaderClass");
        }
        argProcessStructure.setRequestHeaderClass(requestHeaderClass);
        // レスポンスヘッダクラス情報
        String responseHeaderClass = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "responseHeaderClassJava");
        if (BlancoStringUtil.null2Blank(responseHeaderClass).length() == 0) {
            responseHeaderClass = BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "responseHeaderClass");
        }
        argProcessStructure.setResponseHeaderClass(responseHeaderClass);

        // メタIDリスト
        String metaIds = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "metaIdList");
        if (BlancoStringUtil.null2Blank(metaIds).length() > 0) {
            String[] metaIdArray = metaIds.split(",");
            List<String> metaIdList = new ArrayList<>(Arrays.asList(metaIdArray));
            for (String metaId : metaIdList) {
                argProcessStructure.getMetaIdList().add(metaId.trim());
            }
        }

        // 認証が不要なAPI
        argProcessStructure.setCreateImportList("true"
                .equals(BlancoXmlBindingUtil.getTextContent(argElementCommon,
                        "noAuthentication")));

        // import 文の自動生成
        argProcessStructure.setCreateImportList("true"
                .equals(BlancoXmlBindingUtil.getTextContent(argElementCommon,
                        "createImportList")));
    }

    /**
     * 電文処理は常にApiBaseを継承する。
     * processBaseClassオプションで指定が無い場合は
     * blanco.restgenerator.common.ApiBase を継承する。
     * @param argProcessStructure
     */
    private void parseProcessExtends(
            final BlancoRestGeneratorTelegramProcess argProcessStructure) {

        String className = BlancoRestGeneratorUtil.processBaseClass;
        if (className == null) {
            className = BlancoRestGeneratorConstants.BASE_CLASS;
        }
        if (className != null) {
            String classNameCanon = className;
            String classNameSimple = BlancoRestGeneratorUtil.getSimpleClassName(classNameCanon);
            String packageName = BlancoRestGeneratorUtil.getPackageName(classNameCanon);
            if (BlancoStringUtil.null2Blank(packageName).length() == 0) {
                /*
                 * このクラスのパッケージ名を探す
                 */
                packageName = BlancoRestGeneratorUtil.searchPackageBySimpleName(classNameSimple);
            }
            if (BlancoStringUtil.null2Blank(packageName).length() > 0) {
                classNameCanon = packageName + "." + classNameSimple;
            }
            if (isVerbose()) {
                System.out.println("Extends : " + classNameCanon);
            }
            argProcessStructure.setExtends(classNameCanon);
        } else {
            System.out.println("/* Extends Skip */ className is not specified!!!");
        }
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、「電文定義・実装」を取得します。
     * @param argElementInterfaceRoot
     * @param argProcessStructure
     */
    private void parseProcessImplements(
            final BlancoXmlElement argElementInterfaceRoot,
            final BlancoRestGeneratorTelegramProcess argProcessStructure) {

        final List<BlancoXmlElement> listInterfaceChildNodes = BlancoXmlBindingUtil
                .getElementsByTagName(argElementInterfaceRoot, "interface");
        for (int index = 0;
             listInterfaceChildNodes != null &&
                     index < listInterfaceChildNodes.size();
             index++) {
            final BlancoXmlElement elementList = listInterfaceChildNodes
                    .get(index);

            final String interfaceName = BlancoXmlBindingUtil
                    .getTextContent(elementList, "name");
            if (interfaceName == null || interfaceName.trim().length() == 0) {
                continue;
            }

            /*
             * import 情報の作成
             */
            String fqInterface = interfaceName;
            if (argProcessStructure.getCreateImportList()) {
                String packageName = BlancoRestGeneratorUtil.getPackageName(interfaceName);
                String className = BlancoRestGeneratorUtil.getSimpleClassName(interfaceName);
                if (packageName.length() == 0) {
                    /*
                     * このクラスのパッケージ名を探す
                     */
                    packageName = BlancoRestGeneratorUtil.searchPackageBySimpleName(className);
                    if (packageName != null & packageName.length() > 0) {
                        fqInterface = packageName + "." + className;
                    }
                }
            }
            argProcessStructure.getImplementsList().add(fqInterface);
        }
    }

    /**
     * 中間XMLファイル形式のXMLドキュメントをパースして、「電文処理定義・インポート」を取得します。
     * インポートクラス名はCanonicalで記述されている前提です。
     * @param argElementListRoot
     * @param argProcessStructure
     */
    private void parseProcessImport(
            final BlancoXmlElement argElementListRoot,
            final BlancoRestGeneratorTelegramProcess argProcessStructure) {
        final List<BlancoXmlElement> listImportChildNodes = BlancoXmlBindingUtil
                .getElementsByTagName(argElementListRoot, "import");
        for (int index = 0;
             listImportChildNodes != null &&
                     index < listImportChildNodes.size();
             index++) {
            final BlancoXmlElement elementList = listImportChildNodes
                    .get(index);

            final String importName = BlancoXmlBindingUtil
                    .getTextContent(elementList, "name");
            if (importName == null || importName.trim().length() == 0) {
                continue;
            }
            argProcessStructure.getImportList().add(
                    BlancoXmlBindingUtil
                            .getTextContent(elementList, "name"));
        }
    }

    /**
     * 電文処理IDから電文IDを決定し、定義されているもののみをprocessStructureに設定します。
     * 電文IDは以下のルールで決定されます。
     * <電文処理ID> + <Method> + <Request|Response>
     *
     * @param argProcessId
     * @param argTelegramStructureMap
     * @param argProcessStructure
     * @return
     */
    private boolean linkTelegramToProcess(
            final String argProcessId,
            final Map<String, BlancoRestGeneratorTelegram> argTelegramStructureMap,
            final BlancoRestGeneratorTelegramProcess argProcessStructure
    ) {
        boolean found = false;

        Map<String, String> httpMethods = new HashMap<>();
        httpMethods.put(BlancoRestGeneratorConstants.HTTP_METHOD_GET, "Get");
        httpMethods.put(BlancoRestGeneratorConstants.HTTP_METHOD_POST, "Post");
        httpMethods.put(BlancoRestGeneratorConstants.HTTP_METHOD_PUT, "Put");
        httpMethods.put(BlancoRestGeneratorConstants.HTTP_METHOD_DELETE, "Delete");
        Map<String, String> telegramKind = new HashMap<>();
        telegramKind.put(BlancoRestGeneratorConstants.TELEGRAM_INPUT, "Request");
        telegramKind.put(BlancoRestGeneratorConstants.TELEGRAM_OUTPUT, "Response");

        Set<String> methodKeys = httpMethods.keySet();
        for (String methodKey : methodKeys) {
            String method = httpMethods.get(methodKey);
            Set<String> kindKeys = telegramKind.keySet();
            HashMap<String, BlancoRestGeneratorTelegram> telegrams = new HashMap<>();
            for (String kindKey : kindKeys) {
                String kind = telegramKind.get(kindKey);
                String telegramId = argProcessId + method + kind;

                BlancoRestGeneratorTelegram telegramStructure =
                        argTelegramStructureMap.get(telegramId);
                if (telegramStructure != null) {
                    telegrams.put(kindKey, telegramStructure);
                }
            }

            if (argProcessStructure.getCreateImportList() && BlancoRestGeneratorUtil.createServiceMethod) {
                /*
                 * デフォルト電文クラスのimport情報を生成する
                 */
                // obsolete?
//                // 要求
//                String defaultTelegramId = BlancoRestGeneratorUtil.getDefaultRequestTelegramId(method);
//                String defaultTelegramPackage = BlancoRestGeneratorUtil.searchPackageBySimpleName(defaultTelegramId);
//                // 応答
//                defaultTelegramId = BlancoRestGeneratorUtil.getDefaultResponseTelegramId(method);
//                defaultTelegramPackage = BlancoRestGeneratorUtil.searchPackageBySimpleName(defaultTelegramId);
            }

            if (telegrams.size() == 0) {
                continue;
            }
            if (telegrams.size() != 2) {
                /* In と Out が揃っていない */
                return false;
            }
            argProcessStructure.getListTelegrams().put(methodKey, telegrams);
            found = true;
        }

        return found;
    }

    /**
     * Annotationリストを生成します。
     * @param annotations
     * @return
     */
    private List<String> createAnnotaionList(String annotations) {
        List<String> annotationList = new ArrayList<>();
        final String[] lines = BlancoNameUtil.splitString(annotations, '\n');
        StringBuffer sb = new StringBuffer();
        for (String line : lines) {
            if (line.startsWith("@")) {
                if (sb.length() > 0) {
                    annotationList.add(sb.toString());
                    sb = new StringBuffer();
                }
                line = line.substring(1);
            }
            sb.append(line + System.getProperty("line.separator", "\n"));
        }
        if (sb.length() > 0) {
            annotationList.add(sb.toString());
        }
        if (this.isVerbose()) {
            for (String ann : annotationList) {
                System.out.println("Ann: " + ann);
            }
        }
        return annotationList;
    }
}
