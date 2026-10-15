package com.revolic.tscprint.core;

import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class UpdateIntegrityTest {
    @Test public void versionsCompareNumericallyAndRejectNonStableTags() {
        assertTrue(ReleaseVersion.parse("v1.3.10").compareTo(ReleaseVersion.parse("1.3.9"))>0);
        assertEquals(0,ReleaseVersion.parse("v1.3.3").compareTo(ReleaseVersion.parse("1.3.3")));
        for(String value:new String[]{"1.3","1.3.4-beta","v01.3.4","1.3.4/../../x","999999999999.1.0"})
            assertThrows(IllegalArgumentException.class,()->ReleaseVersion.parse(value));
    }
    @Test public void checksumIsBoundToTheExactFilenameAndCannotBeAmbiguous() {
        String hash="a".repeat(64),name="TSC-Direct-Print-1.3.4.apk";
        assertEquals(hash,UpdateIntegrity.checksum(hash+"  "+name+"\n",name));
        assertEquals(hash,UpdateIntegrity.checksum(hash+" *"+name+"\r\n",name));
        assertThrows(IllegalArgumentException.class,()->UpdateIntegrity.checksum(hash+"  other.apk\n",name));
        assertThrows(IllegalArgumentException.class,()->UpdateIntegrity.checksum(hash+"  "+name+"\n"+hash+"  "+name,name));
    }
    @Test public void verificationRejectsCorruptionTruncationAndOversizedStreams() throws Exception {
        byte[] bytes="abc".getBytes(StandardCharsets.UTF_8);
        String expected="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        assertEquals(expected,UpdateIntegrity.copy(new ByteArrayInputStream(bytes),output,3,3,new Cancellation(),n->{}));
        assertArrayEquals(bytes,output.toByteArray());
        assertThrows(IOException.class,()->UpdateIntegrity.copy(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),4,10,new Cancellation(),n->{}));
        assertThrows(IOException.class,()->UpdateIntegrity.copy(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),2,2,new Cancellation(),n->{}));
        Cancellation cancelled=new Cancellation();cancelled.cancel();
        assertThrows(IOException.class,()->UpdateIntegrity.copy(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),3,3,cancelled,n->{}));
    }
}
