package com.revolic.tscprint.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PaperSizeTest {
 @Test public void presetsAndDecimalSizesHaveStableGeometry() {
  assertEquals(815,PaperSize.LABEL.widthDots());assertEquals(607,PaperSize.LABEL.heightDots());
  assertEquals(1183,PaperSize.A5.widthDots());assertEquals(1678,PaperSize.A5.heightDots());
  PaperSize custom=PaperSize.parse("100.5","150.2");assertEquals("100.5",custom.widthMm());
  assertEquals(custom,PaperSize.fromId(custom.id()));assertEquals(PaperSize.LABEL,PaperSize.fromId("TSC_102x76"));
 }
 @Test public void invalidSizesCannotAllocateOrInjectCommands() {
  for(String[] pair:new String[][]{{"0","76"},{"169","210"},{"148","501"},{"NaN","76"},{"1e2","76"},{"100.55","76"},{"102\r\nPRINT 1,1","76"}})
   assertThrows(IllegalArgumentException.class,()->PaperSize.parse(pair[0],pair[1]));
  assertThrows(IllegalArgumentException.class,()->PaperSize.fromId("ISO_A5"));
 }
 @Test public void a5HeaderPayloadAndPaddingFollowSelectedSize() throws Exception {
  PaperSize paper=PaperSize.A5;ByteArrayOutputStream out=new ByteArrayOutputStream();
  TsplEncoder.writeLabel(out,paper,(y,row)->Arrays.fill(row,0xff000000),new Cancellation());
  byte[] head="SIZE 148 mm,210 mm\r\nDIRECTION 1,0\r\nCLS\r\nBITMAP 0,0,148,1678,1,".getBytes(StandardCharsets.US_ASCII);
  byte[] data=out.toByteArray();assertArrayEquals(head,Arrays.copyOf(data,head.length));
  assertEquals(head.length+148*1678+13,data.length);
  for(int y=0;y<paper.heightDots();y++)assertEquals(1,data[head.length+y*148+147]&255);
 }
 @Test public void portraitStockDoesNotBecomeWidePhysicalPaper() {
  PrintLayout p=PrintLayout.fit(148,210,PaperSize.A5,false);
  assertFalse(p.rotate);assertTrue(p.scale*148<=1183.01);assertTrue(p.scale*210<=1678.01);
  PrintLayout landscape=PrintLayout.fit(210,148,PaperSize.A5,true);
  assertTrue(landscape.rotate);assertTrue(landscape.scale*210<=1678.01);
 }
 @Test public void decimalHeaderKeepsMillimetresAndWhitePadding() throws Exception {
  PaperSize p=PaperSize.parse("100.5","150.2");ByteArrayOutputStream out=new ByteArrayOutputStream();
  TsplEncoder.writeLabel(out,p,(y,row)->Arrays.fill(row,0xffffffff),new Cancellation());
  assertTrue(out.toString(StandardCharsets.ISO_8859_1).startsWith("SIZE 100.5 mm,150.2 mm\r\n"));
 }
}
