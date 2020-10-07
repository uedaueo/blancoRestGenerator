package blanco.restgenerator;

import blanco.cg.BlancoCgSupportedLang;
import blanco.restgenerator.resourcebundle.BlancoRestGeneratorResourceBundle;
import blanco.restgenerator.task.valueobject.BlancoRestGeneratorProcessInput;
import blanco.valueobject.BlancoValueObjectXmlParser;
import blanco.valueobject.valueobject.BlancoValueObjectClassStructure;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * BlancoValueObject で作成されているObjectの一覧を XML から取得し，保持しておきます
 *
 * Created by tueda on 15/07/05.
 */
public class BlancoRestGeneratorObjectsInfo {
    /**
     * ValueObject 用リソースバンドルへのアクセスオブジェクト。
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
     * フィールド名やメソッド名の名前変形を行うかどうか。
     */
    private boolean fNameAdjust = true;

    /**
     * 自動生成するソースファイルの文字エンコーディング。
     */
    private String fEncoding = null;

    public void setEncoding(final String argEncoding) {
        fEncoding = argEncoding;
    }

    private boolean fVerbose = false;
    public void setVerbose(boolean argVerbose) {
        this.fVerbose = argVerbose;
    }
    public boolean isVerbose() {
        return this.fVerbose;
    }

    public static HashMap<String, BlancoValueObjectClassStructure> objects = new HashMap<>();

    public void process(final BlancoRestGeneratorProcessInput input) throws IOException {

        // XML化された中間ファイルから情報を読み込む
        final File[] fileMeta3 = new File(input.getTmpdir()
                + BlancoRestGeneratorConstants.OBJECT_SUBDIRECTORY)
                .listFiles();

        if (fileMeta3 == null) {
            System.out.println("!!! NO FILES in " + input.getTmpdir()
                    + BlancoRestGeneratorConstants.OBJECT_SUBDIRECTORY);
            throw new IllegalArgumentException("!!! NO FILES in " + input.getTmpdir()
                    + BlancoRestGeneratorConstants.OBJECT_SUBDIRECTORY);
        }

        /*
         * まず始めにすべてのシートを検索して，クラス名とpackage名のリストを作ります．
         * php形式の定義書では，クラスを指定する際にpackage名が指定されていないからです．
         */
        BlancoValueObjectXmlParser.classList =
                BlancoValueObjectXmlParser.createClassListFromSheets(fileMeta3);

        for (int index = 0; index < fileMeta3.length; index++) {
            if (fileMeta3[index].getName().endsWith(".xml") == false) {
                continue;
            }

            BlancoValueObjectXmlParser parser = new BlancoValueObjectXmlParser();
            parser.setVerbose(this.isVerbose());
            final BlancoValueObjectClassStructure[] structures = parser.parse(fileMeta3[index]);

            if (structures != null ) {
                for (int index2 = 0; index2 < structures.length; index2++) {
                    BlancoValueObjectClassStructure structure = structures[index2];
                    if (structure != null) {
                        objects.put(structure.getName(), structure);
                    } else {
                        System.out.println("/* tueda */ BlancoRestObjextsInfo a structure is NULL!!!");
                    }
                }
            } else {
                System.out.println("/* tueda */ BlancoRestObjextsInfo strstrac is NULL!!!");
            }
        }
    }
}
