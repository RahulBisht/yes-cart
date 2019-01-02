/*
 * Copyright 2009 Denys Pavlov, Igor Azarnyi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.yes.cart.bulkexport.xml.impl;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yes.cart.BaseCoreDBTestCase;
import org.yes.cart.bulkcommon.service.ExportService;
import org.yes.cart.bulkexport.model.ExportDescriptor;
import org.yes.cart.service.async.JobStatusListener;
import org.yes.cart.service.async.model.JobContext;
import org.yes.cart.service.async.model.JobContextKeys;
import org.yes.cart.service.async.model.impl.JobContextImpl;
import org.yes.cart.stream.xml.XStreamProvider;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * User: denispavlov
 * Date: 26/10/2018
 * Time: 08:19
 */
public class XmlFastBulkExportServiceImplTest extends BaseCoreDBTestCase {

    ExportService bulkExportService = null;
    XStreamProvider<ExportDescriptor> xml = null;

    private final Mockery mockery = new JUnit4Mockery();

    @Override
    @Before
    public void setUp()  {

        if (bulkExportService == null) {
            bulkExportService = createContext().getBean("xmlFastBulkExportService", ExportService.class);
            xml = createContext().getBean("exportXmlDescriptorXStreamProvider", XStreamProvider.class);
        }
        super.setUp();

    }


    @Override
    @After
    public void tearDown() throws Exception {
        bulkExportService = null;
        xml = null;
        super.tearDown();

    }


    private JobContext createContext(final String descriptorPath, final JobStatusListener listener, final String fileToExport) throws Exception {

        final ExportDescriptor descriptor = xml.fromXML(new FileInputStream(new File(descriptorPath)));

        return new JobContextImpl(false, listener, new HashMap<String, Object>() {{
            put(JobContextKeys.EXPORT_DESCRIPTOR, descriptor);
            put(JobContextKeys.EXPORT_DESCRIPTOR_NAME, descriptorPath);
            put(JobContextKeys.EXPORT_FILE, fileToExport);
        }});
    }


    @Test
    public void testDoExport() throws Exception {
        try {

            final JobStatusListener listener = mockery.mock(JobStatusListener.class, "listener");

            mockery.checking(new Expectations() {{
                // ONLY allow messages during import
                allowing(listener).notifyPing();
                allowing(listener).notifyPing(with(any(String.class)));
                allowing(listener).notifyMessage(with(any(String.class)));
            }});

            Set<String> importedFilesSet = new HashSet<>();

            ResultSet rs;
            String fileToExport;
            String content;
            File xml;
            long dt;

            rs = getConnection().getConnection().createStatement().executeQuery (
                    "select count(e.ETYPE_ID) as cnt from TETYPE e");
            rs.next();
            long cntEtypeCnt = rs.getLong("cnt");
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/etypes-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/etypes.xml", listener, fileToExport));
            final long etypes = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntEtypeCnt) + " e-types in " + etypes + "millis (~" + (etypes / cntEtypeCnt) + " per item)");

            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("java-type=\"java.lang.String\""));

            validateXmlFile(xml);

            rs = getConnection().getConnection().createStatement().executeQuery (
                    "select count(g.ATTRIBUTEGROUP_ID) as cnt from TATTRIBUTEGROUP g");
            rs.next();
            long cntProductAttrGroup = rs.getLong("cnt");
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/attributegroups-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/attributegroups.xml", listener, fileToExport));
            final long attrsGroups = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntProductAttrGroup) + " attribute groups in " + attrsGroups + "millis (~" + (attrsGroups / cntProductAttrGroup) + " per item)");

            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("code=\"PRODUCT\""));

            validateXmlFile(xml);


            rs = getConnection().getConnection().createStatement().executeQuery (
                    "select count(a.ATTRIBUTE_ID) as cnt from TATTRIBUTE a, TATTRIBUTEGROUP g where a.ATTRIBUTEGROUP_ID = g.ATTRIBUTEGROUP_ID and g.CODE = 'PRODUCT'");
            rs.next();
            long cntProductAttr = rs.getLong("cnt");
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/attributes-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/attributes.xml", listener, fileToExport));
            final long attrs = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntProductAttr) + " attributes in " + attrs + "millis (~" + (attrs / cntProductAttr) + " per item)");

            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("group=\"PRODUCT\" etype=\"String\" rank=\"500\">"));

            validateXmlFile(xml);

            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TPRODUCT  ");
            rs.next();
            long cntProd = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/products-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/products.xml", listener, fileToExport));
            final long prods = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntProd) + " products in " + prods + "millis (~" + (prods / cntProd) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("<product id=\""));
            assertTrue(content.contains("guid=\"SOBOT\" code=\"SOBOT\">"));
            assertTrue(content.contains("<name><![CDATA[Bender Bending Rodriguez]]></name>"));
            assertTrue(content.contains("<custom-value><![CDATA[sobot-picture.jpeg]]></custom-value>"));
            assertTrue(content.contains("<sku id=\""));
            assertTrue(content.contains("guid=\"SOBOT-BEER\" code=\"SOBOT-BEER\" product-code=\"SOBOT\" rank=\"1\""));

            validateXmlFile(xml);

            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TCATEGORY  ");
            rs.next();
            long cntCat = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/categories-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/categories.xml", listener, fileToExport));
            final long cats = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntCat) + " categories in " + cats + "millis (~" + (cats / cntCat) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("<category id=\""));
            assertTrue(content.contains(" guid=\"112\" rank=\"60\""));
            assertTrue(content.contains("<name><![CDATA[KnickKnacks]]></name>"));
            assertTrue(content.contains("<custom-value><![CDATA[10,20,50]]></custom-value>"));

            validateXmlFile(xml);


            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TCATEGORY  ");
            rs.next();
            long cntContent = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/content-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/content.xml", listener, fileToExport));
            final long cms = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntContent) + " content in " + cms + "millis (~" + (cms / cntContent) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("<content id=\""));
            assertTrue(content.contains(" guid=\"112\" rank=\"60\""));
            assertTrue(content.contains("<name><![CDATA[KnickKnacks]]></name>"));
            assertTrue(content.contains("<custom-value><![CDATA[10,20,50]]></custom-value>"));

            validateXmlFile(xml);



            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TSKUWAREHOUSE  ");
            rs.next();
            long cntSW = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/inventory-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/inventory.xml", listener, fileToExport));
            final long inv = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntSW) + " inventory in " + inv + "millis (~" + (inv / cntSW) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("<stock id=\""));
            assertTrue(content.contains(" sku=\"SOBOT-BEER\" warehouse=\"WAREHOUSE_1\">"));

            validateXmlFile(xml);



            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TSKUPRICE  ");
            rs.next();
            long cntSP = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/pricelist-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/pricelists.xml", listener, fileToExport));
            final long price = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntSP) + " prices in " + price + "millis (~" + (price / cntSP) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("sku=\"SOBOT-BEER\" shop=\"SHOIP1\" currency=\"EUR\" quantity=\"1.00\""));
            assertTrue(content.contains("<list-price>150.85</list-price>"));

            validateXmlFile(xml);


            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TTAX  ");
            rs.next();
            long cntTax = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/taxes-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/taxes.xml", listener, fileToExport));
            final long tax = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", tax) + " taxes in " + tax + "millis (~" + (tax / cntTax) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("code=\"VAT\" shop=\"SHOIP1\" currency=\"EUR\""));
            assertTrue(content.contains("<rate gross=\"false\">20.00</rate>"));

            validateXmlFile(xml);


            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TTAXCONFIG  ");
            rs.next();
            long cntTaxCfg = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/taxconfigs-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/taxconfigs.xml", listener, fileToExport));
            final long taxCfg = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntTaxCfg) + " tax configs in " + taxCfg + "millis (~" + (taxCfg / cntTaxCfg) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains(" guid=\"SHOIP1_EUR_"));
            assertTrue(content.contains("<tax-region country=\"US\" state=\"US-US\"/>"));

            validateXmlFile(xml);


            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TPRODUCTTYPE  ");
            rs.next();
            long cntPtype = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/producttypes-export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/producttypes.xml", listener, fileToExport));
            final long pTypes = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntPtype) + " product types in " + pTypes + "millis (~" + (pTypes / cntPtype) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("<name>Power supply</name>"));
            assertTrue(content.contains("<description><![CDATA[Robots]]></description>"));
            assertTrue(content.contains("<product-type-attribute-navigation type=\"R\"><![CDATA[<range-list>"));

            validateXmlFile(xml);


            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TSYSTEMATTRVALUE  ");
            rs.next();
            long cntSysPrefs = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/systempreferences_export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/systempreferences.xml", listener, fileToExport));
            final long sysPrefs = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntSysPrefs) + " system preferences in " + sysPrefs + "millis (~" + (sysPrefs / cntSysPrefs) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("<name>YesCart e-commerce system</name>"));
            assertTrue(content.contains("guid=\"1056_TSYSTEMATTRVALUE\" attribute=\"IMPORT_JOB_TIMEOUT_MS\""));

            validateXmlFile(xml);



            rs = getConnection().getConnection().createStatement().executeQuery ("select count(*) from TDATAGROUP  ");
            rs.next();
            long cntDataGrp = rs.getLong(1);
            rs.close();

            dt = System.currentTimeMillis();
            fileToExport = "target/datagroups_export-" + UUID.randomUUID().toString() + ".xml";
            bulkExportService.doExport(createContext("src/test/resources/export/xml/datagroups.xml", listener, fileToExport));
            final long dataGrps = System.currentTimeMillis() - dt;
            System.out.println(String.format("%5d", cntDataGrp) + " data groups in " + dataGrps + "millis (~" + (dataGrps / cntDataGrp) + " per item)");


            xml = new File(fileToExport);
            content = FileUtils.readFileToString(xml, "UTF-8");
            assertTrue(content.contains("data-descriptor id=\"1000\" name=\"customer/productandcategorynamesimport.xml\""));
            assertTrue(content.contains("<value><![CDATA[customer/productskuimport.xml]]></value>"));

            validateXmlFile(xml);


            mockery.assertIsSatisfied();

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    private void validateXmlFile(final File xml) throws Exception {

        final String schemaLang = "http://www.w3.org/2001/XMLSchema";

        final SchemaFactory factory = SchemaFactory.newInstance(schemaLang);

        final Schema schema = factory.newSchema(new StreamSource(new FileInputStream(new File("src/main/resources/META-INF/schema/impex.xsd"))));
        final Validator validator = schema.newValidator();

        validator.validate(new StreamSource(new FileInputStream(xml)));
        
    }

    private static class StringStartsWithMatcher extends TypeSafeMatcher<String> {
        private String prefix;

        public StringStartsWithMatcher(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean matchesSafely(String s) {
            return s.startsWith(prefix);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a string starting with ").appendValue(prefix);
        }
    }

    private static class StringContainsMatcher extends TypeSafeMatcher<String> {
        private String text;

        public StringContainsMatcher(String text) {
            this.text = text;
        }

        @Override
        public boolean matchesSafely(String s) {
            return s.contains(text);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a string containing text: ").appendValue(text);
        }
    }

    @Factory
    public static Matcher<String> aStringStartingWith(String prefix) {
        return new StringStartsWithMatcher(prefix);
    }

    @Factory
    public static Matcher<String> aStringContains(String text) {
        return new StringContainsMatcher(text);
    }

}