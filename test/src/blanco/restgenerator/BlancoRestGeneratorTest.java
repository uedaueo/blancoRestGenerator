/*
 * blanco Framework
 * Copyright (C) 2004-2020 IGA Tosiki
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package blanco.restgenerator;

import blanco.restgenerator.task.BlancoRestGeneratorProcessImpl;
import blanco.restgenerator.task.valueobject.BlancoRestGeneratorProcessInput;
import blanco.valueobject.task.BlancoValueObjectProcessImpl;
import blanco.valueobject.task.valueobject.BlancoValueObjectProcessInput;
import org.junit.Test;

import java.io.IOException;

/**
 * Generation test for the Java.
 *
 * @author IGA Tosiki
 * @author tueda
 */
public class BlancoRestGeneratorTest {

    @Test
    public void testBlancoRestGenerator() {
        /*
         * First, creates a ValueObject.
         */
        BlancoValueObjectProcessInput inputValueObject = new BlancoValueObjectProcessInput();
        inputValueObject.setMetadir("meta/objects");
        inputValueObject.setEncoding("UTF-8");
        inputValueObject.setSheetType("php");
        inputValueObject.setTmpdir("tmpTest");
        inputValueObject.setTargetdir("sample/blanco");
        inputValueObject.setTargetStyle("maven");
        inputValueObject.setVerbose(true);

        BlancoValueObjectProcessImpl impleValueObject = new BlancoValueObjectProcessImpl();
        try {
            impleValueObject.execute(inputValueObject);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Then, generates the telegram and the telegram processing.
         */
        BlancoRestGeneratorProcessInput inputRestGenerator = new BlancoRestGeneratorProcessInput();
        inputRestGenerator.setMetadir("meta/api");
        inputRestGenerator.setEncoding("UTF-8");
        inputRestGenerator.setSheetType("php");
        inputRestGenerator.setTmpdir("tmpTest");
        inputRestGenerator.setTargetdir("sample/blanco");
        inputRestGenerator.setTargetStyle("maven");
        inputRestGenerator.setVerbose(true);
        inputRestGenerator.setIgnoreAnnotation(true);
        inputRestGenerator.setIgnoreImport(true);
//        inputRestGenerator.setTelegramsOnly(true);

        BlancoRestGeneratorProcessImpl imple = new BlancoRestGeneratorProcessImpl();
        imple.execute(inputRestGenerator);
    }

}
