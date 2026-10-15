package com.revolic.tscprint.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
public class CoreTest {
 @Test public void blackWhiteMsbAndPadding() {
  assertArrayEquals(new byte[]{0x55},TsplEncoder.packRow(new int[]{0xff000000,0xffffffff,0xff000000,0xffffffff,0xff000000,0xffffffff,0xff000000,0xffffffff}));
  assertArrayEquals(new byte[]{0,0x7f},TsplEncoder.packRow(new int[]{0xff000000,0xff000000,0xff000000,0xff000000,0xff000000,0xff000000,0xff000000,0xff000000,0xff000000}));
 }
 @Test public void transparencyCompositesOnWhite() { assertArrayEquals(new byte[]{(byte)0xdf},TsplEncoder.packRow(new int[]{0x00000000,0x7f000000,0x80000000})); }
 @Test public void thresholdAndColor() { assertArrayEquals(new byte[]{0x57},TsplEncoder.packRow(new int[]{0xff7f7f7f,0xff808080,0xffff0000,0xff00ff00,0xff0000ff})); }
 @Test public void headerPreservesRegistrationAndCountsBinaryExactly() throws Exception {
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  TsplEncoder.writeLabel(out,(y,row)->java.util.Arrays.fill(row,0xffffffff),new Cancellation());
  byte[] b=out.toByteArray();byte[] head="SIZE 102 mm,76 mm\r\nDIRECTION 1,0\r\nCLS\r\nBITMAP 0,0,102,607,1,".getBytes(StandardCharsets.US_ASCII);
  assertArrayEquals(head,java.util.Arrays.copyOf(b,head.length));assertEquals(head.length+102*607+13,b.length);
  assertEquals("\r\nPRINT 1,1\r\n",new String(b,b.length-13,13,StandardCharsets.US_ASCII));
  for(int i=head.length;i<head.length+102*607;i++)assertEquals(255,b[i]&255);
 }
 @Test public void cancellationStopsBeforeHeader() throws Exception { Cancellation c=new Cancellation();c.cancel();ByteArrayOutputStream out=new ByteArrayOutputStream();assertThrows(InterruptedIOException.class,()->TsplEncoder.writeLabel(out,(y,r)->{},c));assertEquals(0,out.size()); }
 @Test public void fitPreservesAspectAndCenters() { PrintLayout p=PrintLayout.fit(288,432,false);assertEquals(607f/432,p.scale,0.00001);assertEquals(0,p.offsetY,0.001);assertTrue(p.offsetX>200);assertFalse(p.rotate); }
 @Test public void portraitLabelRotatesToPhysicalStock() { PrintLayout p=PrintLayout.fit(216,289,true);assertTrue(p.rotate);assertTrue(p.scale*289<=815.01);assertTrue(p.scale*216<=607.01); }
 @Test public void invalidGeometryRejected() { assertThrows(IllegalArgumentException.class,()->PrintLayout.fit(0,76,false)); }
 @Test public void selectsAbsolutePageIndexesOnce() { assertArrayEquals(new int[]{1,2,4},PageSelection.select(5,new int[][]{{1,2},{2,2},{4,4}}));assertArrayEquals(new int[]{0,1,2},PageSelection.select(3,null)); }
 @Test public void invalidOrOutsideRangesFailBeforePrinting() { assertThrows(IllegalArgumentException.class,()->PageSelection.select(3,new int[][]{{4,5}}));assertThrows(IllegalArgumentException.class,()->PageSelection.select(3,new int[][]{{2,1}}));assertThrows(IllegalArgumentException.class,()->PageSelection.select(101,null)); }
 @Test public void limitsRejectExcessCopiesAndLabels() { assertEquals(6,PageSelection.totalLabels(3,2));assertThrows(IllegalArgumentException.class,()->PageSelection.totalLabels(100,100));assertThrows(IllegalArgumentException.class,()->PageSelection.totalLabels(1,101)); }
 @Test public void ipv4CannotInjectCommandsOrPorts() {assertEquals("192.168.70.37",PrinterAddress.validate("192.168.70.37"));for(String x:new String[]{"0.0.0.0","224.0.0.1","255.255.255.255","192.168.70.37:9100","127.0.0.1\r\nPRINT 1,1","999.1.1.1"})assertThrows(IllegalArgumentException.class,()->PrinterAddress.validate(x));}
 @Test public void transportSendsExactDocumentInCopyOrder() throws Exception {
  Path file=Files.createTempFile("tsc-test",".tspl");Files.write(file,new byte[]{1,2,3});
  ExecutorService e=Executors.newSingleThreadExecutor();
  try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())) {
   Future<byte[]> got=e.submit(()->{try(Socket s=server.accept()){return s.getInputStream().readAllBytes();}});
   JobTransport.send(file.toFile(),2,"127.0.0.1",server.getLocalPort(),new Cancellation(),2000,2000);
   assertArrayEquals(new byte[]{1,2,3,1,2,3},got.get(3,TimeUnit.SECONDS));
  } finally { e.shutdownNow();Files.delete(file); }
 }
 @Test public void cancelledTransportDoesNotConnect() throws Exception {
  Cancellation c=new Cancellation();c.cancel();Path f=Files.createTempFile("tsc-test",".tspl");Files.write(f,new byte[]{1});
  try(ServerSocket s=new ServerSocket(0)){s.setSoTimeout(200);assertThrows(InterruptedIOException.class,()->JobTransport.send(f.toFile(),1,"127.0.0.1",s.getLocalPort(),c,500,500));assertThrows(SocketTimeoutException.class,s::accept);}finally{Files.delete(f);}
 }

 @Test public void allPagesSentinelIsRelativeToPrunedPdf() { assertArrayEquals(new int[]{0},PageSelection.select(1,new int[][]{{0,Integer.MAX_VALUE}})); }
 @Test public void stalledWriteHasDeadline() throws Exception {
  Path f=Files.createTempFile("tsc-stall",".tspl");Files.write(f,new byte[16*1024*1024]);
  ExecutorService pool=Executors.newSingleThreadExecutor();
  try(ServerSocket server=new ServerSocket()) {
   server.setReceiveBufferSize(1024);server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(),0));
   Future<Socket> incoming=pool.submit(server::accept);
   assertThrows(SocketTimeoutException.class,()->JobTransport.send(f.toFile(),1,"127.0.0.1",server.getLocalPort(),new Cancellation(),2000,250));
   incoming.get(2,TimeUnit.SECONDS).close();
  } finally {pool.shutdownNow();Files.delete(f);}
 }
 @Test public void cancellationClosesBlockedWriter() throws Exception {
  Path f=Files.createTempFile("tsc-cancel",".tspl");Files.write(f,new byte[16*1024*1024]);
  ExecutorService pool=Executors.newSingleThreadExecutor();Cancellation cancel=new Cancellation();
  try(ServerSocket server=new ServerSocket()) {
   server.setReceiveBufferSize(1024);server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(),0));
   Future<?> sending=pool.submit(()->{try {JobTransport.send(f.toFile(),1,"127.0.0.1",server.getLocalPort(),cancel,2000,15000);fail("Expected cancellation");}catch(IOException expected){}});
   try(Socket accepted=server.accept()) {cancel.cancel();sending.get(2,TimeUnit.SECONDS);}
  } finally {pool.shutdownNow();Files.delete(f);}
 }
}
