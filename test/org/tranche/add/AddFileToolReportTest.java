/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tranche.add;

import java.io.*;
import org.tranche.exceptions.TodoException;
import org.tranche.hash.BigHash;
import org.tranche.hash.span.HashSpan;
import org.tranche.util.DevUtil;
import org.tranche.util.TempFileUtil;
import org.tranche.util.TestNetwork;
import org.tranche.util.TestServerConfiguration;
import org.tranche.util.TestUtil;
import org.tranche.util.TrancheTestCase;

/**
 *
 * @author James "Augie" Hill - augman85@gmail.com
 */
public class AddFileToolReportTest extends TrancheTestCase {


    public void testFileSerialization() throws Exception {
        TestUtil.printTitle("AddFileToolReportTest: File Serialization Test()");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        //create temp file
        File upload = TempFileUtil.createTempFileWithName("name.file");
        DevUtil.createTestFile(upload, 125);


        try {
            testNetwork.start();

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(upload);
//            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

            // upload
            AddFileToolReport uploadReport = aft.execute();
            assertFalse("Invalid Upload!", uploadReport.isFailed());

            //store original variables
            long  _timestampStart = uploadReport.getTimestampStart();
            long _timestampEnd = uploadReport.getTimestampEnd();
            BigHash  _hash = uploadReport.getHash();    
            long _bytesUploaded = uploadReport.getBytesUploaded();
            long _filesUploaded = uploadReport.getFilesUploaded();
            long _originalBytesUploaded = uploadReport.getOriginalBytesUploaded();
            long _originalFileCount = uploadReport.getOriginalFileCount();
            boolean _isEncrypted = uploadReport.isEncrypted();
            boolean _isShowMetaDataIfEncrypted = uploadReport.isShowMetaDataIfEncrypted();
            String _title = uploadReport.getTitle();
            String _description = uploadReport.getDescription();

            ByteArrayOutputStream o = new ByteArrayOutputStream();
            uploadReport.serialize(o);

            ByteArrayInputStream i = new ByteArrayInputStream(o.toByteArray());
            AddFileToolReport uploadReportDeserialized = new AddFileToolReport(i);

            //check deserialized variables
           assertEquals( _timestampStart, uploadReportDeserialized.getTimestampStart());
           assertEquals(_timestampEnd, uploadReportDeserialized.getTimestampEnd());
           assertEquals(_hash.toString(), uploadReportDeserialized.getHash().toString());
           assertEquals(_bytesUploaded, uploadReportDeserialized.getBytesUploaded());
           assertEquals(_filesUploaded, uploadReportDeserialized.getFilesUploaded());
           assertEquals(_originalBytesUploaded, uploadReportDeserialized.getOriginalBytesUploaded());
           assertEquals(_originalFileCount, uploadReportDeserialized.getOriginalFileCount());
           assertEquals(_isEncrypted, uploadReportDeserialized.isEncrypted());
           assertEquals(_isShowMetaDataIfEncrypted, uploadReportDeserialized.isShowMetaDataIfEncrypted());
           assertEquals(_title,uploadReportDeserialized.getTitle());
           assertEquals(_description.toString(),uploadReportDeserialized.getDescription().toString());
            
            }
            catch (Exception e){}
        }


     public void testDirectorySerialization() throws Exception {
        TestUtil.printTitle("AddFileToolReportTest: Directory Serialization Test()");

        final String HOST1 = "aardvark.org";
        final String HOST2 = "bryan.com";
        final String HOST3 = "cherry.gov";

        TestNetwork testNetwork = new TestNetwork();
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST1, 1500, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST2, 1501, "127.0.0.1", true, true, false, HashSpan.FULL_SET));
        testNetwork.addTestServerConfiguration(TestServerConfiguration.generateForDataServer(443, HOST3, 1502, "127.0.0.1", true, true, false, HashSpan.FULL_SET));

        //create test direct
        File testDir = DevUtil.createTestProject(3, 1024 * 512, 1024 * 1024);


        try {
            testNetwork.start();

            // set up add file tool
            AddFileTool aft = new AddFileTool();
            aft.addServerToUse(HOST1);
            aft.addServerToUse(HOST2);
            aft.addServerToUse(HOST3);
            aft.setUseUnspecifiedServers(false);
            aft.setUserCertificate(DevUtil.getDevAuthority());
            aft.setUserPrivateKey(DevUtil.getDevPrivateKey());
            aft.setFile(testDir);
//            aft.addListener(new CommandLineAddFileToolListener(aft, System.out));

            // upload
            AddFileToolReport uploadReport = aft.execute();
            assertFalse("Invalid Upload!", uploadReport.isFailed());

            //store original variables
            long  _timestampStart = uploadReport.getTimestampStart();
            long _timestampEnd = uploadReport.getTimestampEnd();
            BigHash  _hash = uploadReport.getHash();
            long _bytesUploaded = uploadReport.getBytesUploaded();
            long _filesUploaded = uploadReport.getFilesUploaded();
            long _originalBytesUploaded = uploadReport.getOriginalBytesUploaded();
            long _originalFileCount = uploadReport.getOriginalFileCount();
            boolean _isEncrypted = uploadReport.isEncrypted();
            boolean _isShowMetaDataIfEncrypted = uploadReport.isShowMetaDataIfEncrypted();
            String _title = uploadReport.getTitle();
            String _description = uploadReport.getDescription();

            ByteArrayOutputStream o = new ByteArrayOutputStream();
            uploadReport.serialize(o);

            ByteArrayInputStream i = new ByteArrayInputStream(o.toByteArray());
            AddFileToolReport uploadReportDeserialized = new AddFileToolReport(i);

            //check deserialized variables
           assertEquals( _timestampStart, uploadReportDeserialized.getTimestampStart());
           assertEquals(_timestampEnd, uploadReportDeserialized.getTimestampEnd());
           assertEquals(_hash.toString(), uploadReportDeserialized.getHash().toString());
           assertEquals(_bytesUploaded, uploadReportDeserialized.getBytesUploaded());
           assertEquals(_filesUploaded, uploadReportDeserialized.getFilesUploaded());
           assertEquals(_originalBytesUploaded, uploadReportDeserialized.getOriginalBytesUploaded());
           assertEquals(_originalFileCount, uploadReportDeserialized.getOriginalFileCount());
           assertEquals(_isEncrypted, uploadReportDeserialized.isEncrypted());
           assertEquals(_isShowMetaDataIfEncrypted, uploadReportDeserialized.isShowMetaDataIfEncrypted());
           assertEquals(_title,uploadReportDeserialized.getTitle());
           assertEquals(_description.toString(),uploadReportDeserialized.getDescription().toString());

            }
            catch (Exception e){}
        }

    }

