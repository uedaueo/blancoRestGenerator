package blanco.restgenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import blanco.cg.BlancoCgSupportedLang;
import blanco.commons.util.BlancoStringUtil;
import blanco.restgenerator.resourcebundle.BlancoRestGeneratorResourceBundle;
import blanco.restgenerator.task.valueobject.BlancoRestGeneratorProcessInput;
import blanco.valueobject.BlancoValueObjectXmlParser;
import blanco.valueobject.valueobject.BlancoValueObjectClassStructure;

/**
 * Gets the list of Object created by BlancoValueObject from XML and keeps it.
 *
 * Created by tueda on 15/07/05.
 */
public class BlancoRestGeneratorUtil {
    /**
     * An access object to the resource bundle for ValueObject.
     */
    private final static BlancoRestGeneratorResourceBundle fBundle = new BlancoRestGeneratorResourceBundle();

    public static Map<String, Integer> mapCommons = new HashMap<String, Integer>() {
        {put(fBundle.getMeta2xmlElementCommon(), BlancoCgSupportedLang.JAVA);}
        {put(fBundle.getMeta2xmlElementCommonCs(), BlancoCgSupportedLang.CS);}
        {put(fBundle.getMeta2xmlElementCommonJs(), BlancoCgSupportedLang.JS);}
        {put(fBundle.getMeta2xmlElementCommonVb(), BlancoCgSupportedLang.VB);}
        {put(fBundle.getMeta2xmlElementCommonPhp(), BlancoCgSupportedLang.PHP);}
        {put(fBundle.getMeta2xmlElementCommonRuby(), BlancoCgSupportedLang.RUBY);}
        {put(fBundle.getMeta2xmlElementCommonPython(), BlancoCgSupportedLang.PYTHON);}
        {put(fBundle.getMeta2xmlElementCommonPython(), BlancoCgSupportedLang.KOTLIN);}
        {put(fBundle.getMeta2xmlElementCommonPython(), BlancoCgSupportedLang.TS);}
    };

    /**
     * Character encoding of auto-generated source files.
     */
    public static String encoding = "UTF-8";
    public static boolean isVerbose = false;

    public static HashMap<String, BlancoValueObjectClassStructure> objects = new HashMap<>();

    public static String packageSuffix = null;
    public static String overridePackage = null;
    public static String overrideLocation = null;
    public static String voPackageSuffix = null;
    public static String voOverridePackage = null;
    public static boolean ignoreDefault = false;
    public static boolean ignoreAnnotation = false;
    public static boolean ignoreImport = false;
    public static String telegramPackage = null;
    public static String processBaseClass = null;
    public static String defaultExceptionClass = null;
    public static boolean createServiceMethod = false;
    public static String serverType = "tomcat";
    public static boolean telegramsOnly = false;

    static public void processValueObjects(final BlancoRestGeneratorProcessInput input) throws IOException {
        if (isVerbose) {
            System.out.println("BlancoRestGeneratorUtil#processValueObjects : processValueObjects start !");
        }

        /* tmpdir is unique. */
        String baseTmpdir = input.getTmpdir();
        /* searchTmpdir is comma separated. */
        String tmpTmpdirs = input.getSearchTmpdir();
        List<String> searchTmpdirList = null;
        if (tmpTmpdirs != null && !tmpTmpdirs.equals(baseTmpdir)) {
            String[] searchTmpdirs = tmpTmpdirs.split(",");
            searchTmpdirList = new ArrayList<>(Arrays.asList(searchTmpdirs));
        }
        if (searchTmpdirList == null) {
            searchTmpdirList = new ArrayList<>();
        }
        searchTmpdirList.add(baseTmpdir);

        for (String tmpdir : searchTmpdirList) {
            searchTmpdir(tmpdir.trim());
        }
    }

    static private void searchTmpdir(String tmpdir) {

        // Reads information from XML-ized intermediate files.
        final File[] fileMeta3 = new File(tmpdir
                + BlancoRestGeneratorConstants.OBJECT_SUBDIRECTORY)
                .listFiles();

        if (fileMeta3 == null) {
            System.out.println("!!! NO FILES in " + tmpdir
                    + BlancoRestGeneratorConstants.OBJECT_SUBDIRECTORY);
            throw new IllegalArgumentException("!!! NO FILES in " + tmpdir
                    + BlancoRestGeneratorConstants.OBJECT_SUBDIRECTORY);
        }

        for (int index = 0; index < fileMeta3.length; index++) {
            if (fileMeta3[index].getName().endsWith(".xml") == false) {
                continue;
            }

            BlancoValueObjectXmlParser parser = new BlancoValueObjectXmlParser();
//            parser.setVerbose(this.isVerbose());
            /*
             * First, it searches all the sheets and make a list of class and package names.
             * This is because in the php-style definitions, the package name is not specified when specifying class.
             *
             */
            final BlancoValueObjectClassStructure[] structures = parser.parse(fileMeta3[index]);

            if (structures != null ) {
                for (int index2 = 0; index2 < structures.length; index2++) {
                    BlancoValueObjectClassStructure structure = structures[index2];
                    if (structure != null) {
                        if (isVerbose) {
                            System.out.println("processValueObjects: " + structure.getName());
                        }
                        objects.put(structure.getName(), structure);
                    } else {
                        System.out.println("processValueObjects: a structure is NULL!!!");
                    }
                }
            } else {
                System.out.println("processValueObjects: structures are NULL!!!");
            }
        }
    }


    /**
     * Make canonical classname into Simple.
     *
     * @param argClassNameCanon
     * @return simpleName
     */
    static public String getSimpleClassName(final String argClassNameCanon) {
        if (argClassNameCanon == null) {
            return "";
        }

        String simpleName = "";
        final int findLastDot = argClassNameCanon.lastIndexOf('.');
        if (findLastDot == -1) {
            simpleName = argClassNameCanon;
        } else if (findLastDot != argClassNameCanon.length() - 1) {
            simpleName = argClassNameCanon.substring(findLastDot + 1);
        }
        return simpleName;
    }

    /**
     * Make canonical classname into packageName
     *
     * @param argClassNameCanon
     * @return
     */
    static public String getPackageName(final String argClassNameCanon) {
        if (argClassNameCanon == null) {
            return "";
        }

        String simpleName = "";
        final int findLastDot = argClassNameCanon.lastIndexOf('.');
        if (findLastDot > 0) {
            simpleName = argClassNameCanon.substring(0, findLastDot);
        }
        return simpleName;
    }


    public static String parsePhpTypes(String phpType, boolean isGeneric) {
        String javaType = phpType;
        if (BlancoStringUtil.null2Blank(phpType).length() != 0) {
            if ("boolean".equalsIgnoreCase(phpType)) {
                javaType = "java.lang.Boolean";
            } else
            if ("integer".equalsIgnoreCase(phpType)) {
                // Converts integer type to 64 bit.
                javaType = "java.lang.Long";
            } else
            if ("double".equalsIgnoreCase(phpType)) {
                javaType = "java.lang.Double";
            } else
            if ("float".equalsIgnoreCase(phpType)) {
                javaType = "java.lang.Double";
            } else
            if ("string".equalsIgnoreCase(phpType)) {
                javaType = "java.lang.String";
            } else
            if ("datetime".equalsIgnoreCase(phpType)) {
                javaType = "java.util.Date";
            } else
            if ("array".equalsIgnoreCase(phpType)) {
                if (isGeneric) {
                    throw new IllegalArgumentException("Cannot use array for Generics.");
                } else {
                    javaType = "java.util.ArrayList";
                }
            } else
            if ("object".equalsIgnoreCase(phpType)) {
                javaType = "java.lang.Object";
            } else
            if ("ArrayList".equals(phpType)) { // Replaces with CanonicalName only if there is an exact match.
                javaType = "java.util.ArrayList";
            } else
            if ("List".equals(phpType)) { // Replaces with CanonicalName only if there is an exact match.
                javaType = "java.util.List";
            } else
            if ("Map".equals(phpType)) { // Replaces with CanonicalName only if there is an exact match.
                javaType = "java.util.Map";
            } else
            if ("HashMap".equals(phpType)) { // Replaces with CanonicalName only if there is an exact match.
                javaType = "java.util.HashMap";
            } else {
                /* Searches for a package with this name. */
                String packageName = BlancoRestGeneratorUtil.searchPackageBySimpleName(phpType);
                if (packageName != null) {
                    javaType = packageName + "." + phpType;
                }

                /* Others are written as is. */
                if (isVerbose) {
                    System.out.println("/* tueda */ Unknown php type: " + javaType);
                }
            }
        }
        return javaType;
    }

    static public String searchPackageBySimpleName(String simpleName) {
        // Replaces the package name if the replace package name option is specified.
        // If there is Suffix, that is the priority.
        String packageName = null;
        BlancoValueObjectClassStructure voStructure = objects.get(simpleName);
        if (voStructure != null) {
            packageName = voStructure.getPackage();
            if (BlancoRestGeneratorUtil.voPackageSuffix != null && BlancoRestGeneratorUtil.voPackageSuffix.length() > 0) {
                packageName = packageName + "." + BlancoRestGeneratorUtil.voPackageSuffix;
            } else if (BlancoRestGeneratorUtil.voOverridePackage != null && BlancoRestGeneratorUtil.voOverridePackage.length() > 0) {
                packageName = BlancoRestGeneratorUtil.voOverridePackage;
            }
        }
        return packageName;
    }

    public static String getDefaultExceptionId() {
        String exceptionName = BlancoRestGeneratorConstants.DEFAULT_EXCEPTION;
        if (BlancoStringUtil.null2Blank(BlancoRestGeneratorUtil.defaultExceptionClass).length() > 0) {
            exceptionName = BlancoRestGeneratorUtil.defaultExceptionClass;
        }
        return exceptionName;
    }
}
