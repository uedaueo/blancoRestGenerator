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
     * An access object to the resource bundle for this product.
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
     * Parses an XML document in an intermediate XML file to get an array of value object information.
     *
     * @param argMetaXmlSourceFile
     *            An intermediate XML file.
     * @return An array of value object information obtained as a result of parsing.
     */
    public BlancoRestGeneratorTelegramProcess[] parse(
            final File argMetaXmlSourceFile) {
        final BlancoXmlDocument documentMeta = new BlancoXmlUnmarshaller()
                .unmarshal(argMetaXmlSourceFile);
        if (documentMeta == null) {
            System.out.println("Fail to unmarshal XML.");
            return null;
        }

        // Gets the root element.
        final BlancoXmlElement elementRoot = BlancoXmlBindingUtil
                .getDocumentElement(documentMeta);
        if (elementRoot == null) {
            // The process is aborted if there is no root element.
            if (this.isVerbose()) {
                System.out.println("praser !!! NO ROOT ELEMENT !!!");
            }
            return null;
        }

        if (this.isVerbose()) {
            System.out.println("[Starts the process of " + argMetaXmlSourceFile.getName() + "]");
        }

        // First, it gets the list of telegrams.
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

        // Next, it gets the telegram processing.
        return parseTelegramProcess(elementRoot,telegramStructureMap);
    }

    /**
     * Parses an XML document in the form of an  intermediate XML file 
     * to create a list of telegram information that can be searched by telegram name.
     *
     * @param argElementRoot
     * @return
     */
    public Map<String, BlancoRestGeneratorTelegram> parseTelegrams(final BlancoXmlElement argElementRoot) {

        Map <String, BlancoRestGeneratorTelegram> telegramStructureMap = new HashMap<>();

        // Gets a list of sheets (Excel sheets).
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(argElementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        for (int index = 0; index < sizeListSheet; index++) {
            // Processes each sheet individually.
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // Gets detailed information from the sheet.
            final BlancoRestGeneratorTelegram telegramStructure = parseTelegramSheet(elementSheet);

            // Stores the telegram information in a map with the telegram ID as the key.
            if (telegramStructure != null) {
                telegramStructureMap.put(telegramStructure.getName(), telegramStructure);
            }
        }
        /**
         *  Checking if Input and Output are paired is done when storing in the TelegramProcessStructure.
         */
        return telegramStructureMap;
    }


    /**
     * Parses an XML document in the form of an  intermediate XML file to get telegram information.
     * @param argElementSheet
     * @return
     */
    private BlancoRestGeneratorTelegram parseTelegramSheet(final BlancoXmlElement argElementSheet) {

        final BlancoRestGeneratorTelegram telegramStructure = new BlancoRestGeneratorTelegram();

        // Gets the common information.
        final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                .getElement(argElementSheet, fBundle
                        .getMeta2xmlTelegramCommon());
        if (elementCommon == null) {
            // If there is no common, skips the processing of this sheet.
            // System.out.println("BlancoRestXmlSourceFile#process !!! NO COMMON !!!");
            return telegramStructure;
        }

        final String name = BlancoXmlBindingUtil.getTextContent(
                elementCommon, "name");
        if (BlancoStringUtil.null2Blank(name).trim().length() == 0) {
            // If name is empty, skips the process.
            // System.out.println("BlancoRestXmlSourceFile#process !!! NO NAME !!!");
            return telegramStructure;
        }

        final String httpMethod = BlancoXmlBindingUtil.getTextContent(
                elementCommon, "telegramMethod");
        if (BlancoStringUtil.null2Blank(httpMethod).trim().length() == 0) {
            // If httpMethod is empty, skips the process.
            // System.out.println("BlancoRestXmlSourceFile#process !!! NO NAME !!!");
            return telegramStructure;
        }

        if (this.isVerbose()) {
            System.out.println("BlancoRestGeneratorXmlParser#parseTelegramSheet name = " + name);
        }

        // First, it sets the Package overwrite options.
        telegramStructure.setPackageSuffix(BlancoRestGeneratorUtil.packageSuffix);
        telegramStructure.setOverridePackage(BlancoRestGeneratorUtil.overridePackage);
        // There is no location in telegram.

        // TelegramDefinition common
        this.parseTelegramCommon(elementCommon, telegramStructure);

        // TelegramDefinition inheritance
        this.parseTelegramExtends(telegramStructure);

        // TelegramDefinition implementation
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

        // Gets the list information.
        final List<BlancoXmlElement> listList = BlancoXmlBindingUtil
                .getElementsByTagName(argElementSheet, fBundle.getMeta2xmlTeregramList());
        if (listList != null && listList.size() != 0) {
            final BlancoXmlElement elementListRoot = listList.get(0);
            this.parseTelegramFields(elementListRoot, telegramStructure);
        }

        return telegramStructure;
    }

    /**
     * Parses an XML document in the form of an  intermediate XML file to get "TelegramDefinition common".
     *
     * @param argElementCommon
     * @param argTelegramStructure
     */
    private void parseTelegramCommon(final BlancoXmlElement argElementCommon, final BlancoRestGeneratorTelegram argTelegramStructure) {

        // Telegram ID
        argTelegramStructure.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));
        // Package
        argTelegramStructure.setPackage(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "package"));
        // Description
        argTelegramStructure.setDescription(BlancoXmlBindingUtil.getTextContent(argElementCommon, "description"));
        // Telegram type (Input/Output)
        argTelegramStructure.setTelegramType(BlancoXmlBindingUtil.getTextContent(argElementCommon, "type"));
        // HTTP method
        argTelegramStructure.setTelegramMethod(BlancoXmlBindingUtil.getTextContent(argElementCommon, "telegramMethod"));

        /* Supports for class annotation. */
        String classAnnotation = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "annotationJava");
        if (BlancoStringUtil.null2Blank(classAnnotation).length() == 0 && !BlancoRestGeneratorUtil.ignoreAnnotation) {
            classAnnotation = BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "annotation");
        }
        if (BlancoStringUtil.null2Blank(classAnnotation).length() > 0) {
            argTelegramStructure.setAnnotationList(createAnnotaionList(classAnnotation));
        }
        /* Always adds @Introspected in micronaut. */
        if ("micronaut".equalsIgnoreCase(BlancoRestGeneratorUtil.serverType)) {
            argTelegramStructure.getAnnotationList().add("Introspected");
            argTelegramStructure.getImportList().add("io.micronaut.core.annotation.Introspected");
        }

        /*
         * Always true for the telegram class.
         */
        argTelegramStructure.setCreateImportList(true);

        /*
         * The telegram class alwats transforms the field name.
         */
        argTelegramStructure.setAdjustFieldName(true);
    }

    /**
     * In blancoRestGenerator, a telegram always inherits from Api[Get|Post|Put|Delete]Telegram <- ApiTelegram.
     *
     * @param argTelegramStructure
     */
    private void parseTelegramExtends(
            final BlancoRestGeneratorTelegram argTelegramStructure) {

        // Null in a method is already checked in common.
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
         * Finds the package name for this class.
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
     * Parses an XML document in the form of an  intermediate XML file to get "TelegramDefinition implementation".
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
             * Creates import information.
             */
            String fqInterface = interfaceName;
            if (argTelegramStructure.getCreateImportList()) {
                String packageName = BlancoRestGeneratorUtil.getPackageName(interfaceName);
                String className = BlancoRestGeneratorUtil.getSimpleClassName(interfaceName);
                if (packageName.length() == 0) {
                    /*
                     * Finds the package name for this class.
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
     * Parses an XML document in the form of an  intermediate XML file to get "TelegramDefinition import".
     * Assumes that the import class name is written in Canonical.
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
     * Parses an XML document in the form of an  intermediate XML file to get "TelegramDefinition list".
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
             * Gets the type. If the Java type is specified, that is the priority.
             * The default type specification assumes that a php-like type has been defined.
             * Changes the type name to Java-style here.
             */
            String phpType = BlancoXmlBindingUtil.getTextContent(elementList, "fieldTypeJava");
            if (BlancoStringUtil.null2Blank(phpType).length() == 0) {
                phpType = BlancoXmlBindingUtil.getTextContent(elementList, "fieldType");
                if (BlancoStringUtil.null2Blank(phpType).length() == 0) {
                    // Type is required.
                    throw new IllegalArgumentException(fBundle.getXml2sourceFileErr005(
                            argTelegramStructure.getName(),
                            fieldStructure.getName()
                    ));
                }
            }
            String javaType = BlancoRestGeneratorUtil.parsePhpTypes(phpType, false);
            fieldStructure.setFieldType(javaType);

            /* Supports for Generic. */
            String phpGeneric = BlancoXmlBindingUtil.getTextContent(elementList, "fieldGenericJava");
            if (BlancoStringUtil.null2Blank(phpGeneric).length() == 0) {
                phpGeneric = BlancoXmlBindingUtil.getTextContent(elementList, "fieldGeneric");
            }
            if (BlancoStringUtil.null2Blank(phpGeneric).length() != 0) {
                String kotlinGeneric = BlancoRestGeneratorUtil.parsePhpTypes(phpGeneric, true);
                fieldStructure.setFieldGeneric(kotlinGeneric);
            }

            /* Supports for annnotation. */
            String fieldAnnotation = BlancoXmlBindingUtil.getTextContent(elementList, "fieldAnnotationJava");
            if (BlancoStringUtil.null2Blank(fieldAnnotation).length() == 0 && !BlancoRestGeneratorUtil.ignoreAnnotation) {
                fieldAnnotation = BlancoXmlBindingUtil.getTextContent(elementList, "fieldAnnotation");
            }
            if (BlancoStringUtil.null2Blank(fieldAnnotation).length() != 0) {
                fieldStructure.setFieldAnnotationList(createAnnotaionList(fieldAnnotation));
            }

            // Supports for required (giving NotNull annotation）
            fieldStructure.setFieldRequired("true".equals(BlancoXmlBindingUtil
                    .getTextContent(elementList, "fieldRequired")));
            if (fieldStructure.getFieldRequired()) {
                fieldStructure.getFieldAnnotationList().add("NotNull");
                argTelegramStructure.getImportList().add("javax.validation.constraints.NotNull");
            }

            // Description
            fieldStructure.setDescription(BlancoXmlBindingUtil
                    .getTextContent(elementList, "fieldDescription"));
            final String[] lines = BlancoNameUtil.splitString(
                    fieldStructure.getDescription(), '\n');
            for (int indexLine = 0; indexLine < lines.length; indexLine++) {
                if (indexLine == 0) {
                    fieldStructure.setDescription(lines[indexLine]);
                } else {
                    // For a multi-line description, it will be split and stored.
                    // From the second line, assumes that character reference encoding has been properly implemented.                    
                    fieldStructure.getDescriptionList().add(
                            lines[indexLine]);
                }
            }

            // Default
            String fieldDefault = BlancoXmlBindingUtil.getTextContent(
                    elementList, "defaultJava");
            if (BlancoStringUtil.null2Blank(fieldDefault).length() == 0 && !BlancoRestGeneratorUtil.ignoreDefault) {
                fieldDefault = BlancoXmlBindingUtil.getTextContent(
                        elementList, "default");
            }
            fieldStructure.setDefault(fieldDefault);

            // Length
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

            // Maximum and minimum
            fieldStructure.setMinInclusive(BlancoXmlBindingUtil
                    .getTextContent(elementList, "minInclusive"));
            fieldStructure.setMaxInclusive(BlancoXmlBindingUtil
                    .getTextContent(elementList, "maxInclusive"));
            // Regular expression
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
         * Creates a list of header
         * First, outputs what is written in the definition as it is.
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
         * Next, outputs the auto-generated one.
         * The current method requires the following assumptions.
         *  * One class definition per file
         *  * Represents directories with Java/kotlin style package notation in the definition sheet
         * TODO: Should it be possible to define the directory where the files are located on the definition sheet?
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
     * Parses an XML document in the form of an  intermediate XML file to get an array of value object information.
     *
     * @param argElementRoot
     *            An intermediate XML file.
     * @return An array of value object information obtained as a result of parsing.
     */
    public BlancoRestGeneratorTelegramProcess[] parseTelegramProcess(
            final BlancoXmlElement argElementRoot,
            final Map <String, BlancoRestGeneratorTelegram> argTelegramStructureMap) {

        final List<BlancoRestGeneratorTelegramProcess> processStructures = new ArrayList<>();

        // Gets a list of sheets (Excel sheets).
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(argElementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        if (this.isVerbose()) {
            System.out.println("Sheet Size Is " + sizeListSheet);
        }
        for (int index = 0; index < sizeListSheet; index++) {
            // Processes each sheet individually.
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // Gets detailed information from the sheet.
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

        // Gets the common information.
        final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                .getElement(argElementSheet, fBundle
                        .getMeta2xmlProcessCommon());
        if (elementCommon == null) {
            // If there is no common, skips the processing of this sheet.
//             System.out.println("BlancoRestXmlSourceFile#processTelegramProcess !!! NO COMMON !!!");
            return null;
        }

        final String name = BlancoXmlBindingUtil.getTextContent(
                elementCommon, "name");

        if (BlancoStringUtil.null2Blank(name).length() == 0) {
            // If name is empty, skips the process.
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

        // Sets the Package overwrite options.
        processStructure.setPackageSuffix(BlancoRestGeneratorUtil.packageSuffix);
        processStructure.setOverridePackage(BlancoRestGeneratorUtil.overridePackage);
        processStructure.setOverrideLocation(BlancoRestGeneratorUtil.overrideLocation);

        // TelegramProcessDefinition common
        parseProcessCommon(elementCommon, processStructure);

        /*
         * TelegramProcessDefinition inheritance
         * Telegram processing always inherits from ApiBase.
         * If not specified in the processBaseClass option, it inherits from blanco.restgenerator.common.ApiBase.
         *
         */
        if (!"micronaut".equalsIgnoreCase(BlancoRestGeneratorUtil.serverType)) {
            parseProcessExtends(processStructure);
        }

        // TelegramProcessDefinition implementation (usually not done).
        final List<BlancoXmlElement> interfaceList = BlancoXmlBindingUtil
                .getElementsByTagName(argElementSheet, fBundle.getMeta2xmlProcessImplements());
        if (interfaceList != null && interfaceList.size() != 0) {
            final BlancoXmlElement elementInterfaceRoot = interfaceList.get(0);
            parseProcessImplements(elementInterfaceRoot, processStructure);
        }

        // TelegramProcessDefinition import
        final List<BlancoXmlElement> importList = BlancoXmlBindingUtil
                .getElementsByTagName(argElementSheet, fBundle.getMeta2xmlProcessImport());
        if (importList != null && importList.size() != 0) {
            final BlancoXmlElement elementInterfaceRoot = importList.get(0);
            parseProcessImport(elementInterfaceRoot, processStructure);
        }

        /*
         * Determines the telegram ID from telegram process ID, and sets only the defined one to processStructure.
         * The telegram ID is determined by the following rule.
         * <Telegram process ID> + <Method> + <Request|Response>
         */
        if (!this.linkTelegramToProcess(processStructure.getName(), argTelegramStructureMap, processStructure)) {
            /* The telegram is undefined or In and Out are not aligned. */
            System.out.println("!!! Invalid Telegram !!! for " + processStructure.getName());
            return null;
        }

        return processStructure;
    }

    /**
     * Parses an XML document in the form of an  intermediate XML file to get "TelegramDefinition common".
     *
     * @param argElementCommon
     * @param argProcessStructure
     */
    private void parseProcessCommon(final BlancoXmlElement argElementCommon, final BlancoRestGeneratorTelegramProcess argProcessStructure) {


        // Telegram process ID
        argProcessStructure.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));
        // Description
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

        // Web service ID
        argProcessStructure.setServiceId(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "webServiceId"));

        // Location
        argProcessStructure.setLocation(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "location"));

        // Package
        argProcessStructure.setPackage(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "package"));

        // Base directory
        argProcessStructure.setBasedir(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "basedir"));

        // Implementation directory
        String impleDir = BlancoXmlBindingUtil.getTextContent(argElementCommon, "impledirJava");
        if (BlancoStringUtil.null2Blank(impleDir).length() == 0) {
            impleDir = BlancoXmlBindingUtil.getTextContent(argElementCommon, "impledirKt");
        }
        argProcessStructure.setImpleDirKt(impleDir);

        // Annotation
        String classAnnotation = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "annotationJava");
        if (BlancoStringUtil.null2Blank(classAnnotation).length() == 0 && !BlancoRestGeneratorUtil.ignoreAnnotation) {
            classAnnotation = BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "annotation");
        }
        if (BlancoStringUtil.null2Blank(classAnnotation).length() > 0) {
            argProcessStructure.setAnnotationList(createAnnotaionList(classAnnotation));
        }

        // Request header class information
        String requestHeaderClass = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "requestHeaderClassJava");
        if (BlancoStringUtil.null2Blank(requestHeaderClass).length() == 0) {
            requestHeaderClass = BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "requestHeaderClass");
        }
        argProcessStructure.setRequestHeaderClass(requestHeaderClass);
        // Response header class information
        String responseHeaderClass = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "responseHeaderClassJava");
        if (BlancoStringUtil.null2Blank(responseHeaderClass).length() == 0) {
            responseHeaderClass = BlancoXmlBindingUtil.getTextContent(
                    argElementCommon, "responseHeaderClass");
        }
        argProcessStructure.setResponseHeaderClass(responseHeaderClass);

        // Meta ID list
        String metaIds = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "metaIdList");
        if (BlancoStringUtil.null2Blank(metaIds).length() > 0) {
            String[] metaIdArray = metaIds.split(",");
            List<String> metaIdList = new ArrayList<>(Arrays.asList(metaIdArray));
            for (String metaId : metaIdList) {
                argProcessStructure.getMetaIdList().add(metaId.trim());
            }
        }

        // API that do not require authentication
        argProcessStructure.setCreateImportList("true"
                .equals(BlancoXmlBindingUtil.getTextContent(argElementCommon,
                        "noAuthentication")));

        // Auto-generation of import statements
        argProcessStructure.setCreateImportList("true"
                .equals(BlancoXmlBindingUtil.getTextContent(argElementCommon,
                        "createImportList")));
    }

    /**
     * Telegram processing always inherits from ApiBase.
     * If not specified in the processBaseClass option, it inherits from blanco.restgenerator.common.ApiBase.
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
                 * Finds the package name for this class.
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
     * Parses an XML document in the form of an  intermediate XML file to get "TelegramDefinition implementation".
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
             * Creates import information
             */
            String fqInterface = interfaceName;
            if (argProcessStructure.getCreateImportList()) {
                String packageName = BlancoRestGeneratorUtil.getPackageName(interfaceName);
                String className = BlancoRestGeneratorUtil.getSimpleClassName(interfaceName);
                if (packageName.length() == 0) {
                    /*
                     * Finds the package name for this class.
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
     * Parses an XML document in the form of an  intermediate XML file to get "TelegramDefinition import".
     * Assumes that the import class name is written in Canonical.
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
     * Determines the telegram ID from telegram process ID, and sets only the defined one to processStructure.
     * The telegram ID is determined by the following rule.
     * <Telegram process ID> + <Method> + <Request|Response>
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
                 * Creates import information for the default telegram class.
                 */
                // obsolete?
//                // Request
//                String defaultTelegramId = BlancoRestGeneratorUtil.getDefaultRequestTelegramId(method);
//                String defaultTelegramPackage = BlancoRestGeneratorUtil.searchPackageBySimpleName(defaultTelegramId);
//                // Response
//                defaultTelegramId = BlancoRestGeneratorUtil.getDefaultResponseTelegramId(method);
//                defaultTelegramPackage = BlancoRestGeneratorUtil.searchPackageBySimpleName(defaultTelegramId);
            }

            if (telegrams.size() == 0) {
                continue;
            }
            if (telegrams.size() != 2) {
                /* In and Out are not aligned. */
                return false;
            }
            argProcessStructure.getListTelegrams().put(methodKey, telegrams);
            found = true;
        }

        return found;
    }

    /**
     * Creates an Annotation list.
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
