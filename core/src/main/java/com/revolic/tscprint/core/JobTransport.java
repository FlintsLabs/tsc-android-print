package com.revolic.tscprint.core;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.TimeUnit;

/** One attempt only. A timeout/cancellation can leave a partially printed job. */
public final class JobTransport {
    private JobTransport() { }
    public static void send(File file, int copies, String host, int port, Cancellation cancel, int connectMs, int stallMs) throws IOException {
        if (copies<1 || copies>100 || file.length()<1 || file.length()>128L*1024*1024) throw new IllegalArgumentException("Invalid print payload");
        if (connectMs<1 || stallMs<1 || port<1 || port>65535) throw new IllegalArgumentException("Invalid transport settings");
        PrinterAddress.validate(host); cancel.check();
        try (SocketChannel channel=SocketChannel.open(); Selector selector=Selector.open()) {
            cancel.register(channel);
            try {
                channel.configureBlocking(false);
                channel.register(selector,SelectionKey.OP_CONNECT);
                if (!channel.connect(new InetSocketAddress(host,port))) {
                    long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(connectMs);
                    while(!channel.finishConnect()) await(selector,cancel,deadline);
                }
                channel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                ByteBuffer buffer=ByteBuffer.allocate(32768);
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(stallMs);
                for(int copy=0;copy<copies;copy++) try(FileInputStream input=new FileInputStream(file)) {
                    int n;
                    while((n=input.read(buffer.array()))!=-1) {
                        buffer.clear(); buffer.limit(n);
                        while(buffer.hasRemaining()) {
                            cancel.check(); int written=channel.write(buffer);
                            if(written>0) deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(stallMs);
                            else await(selector,cancel,deadline);
                        }
                    }
                }
                cancel.check(); channel.shutdownOutput();
            } finally { cancel.unregister(channel); }
        }
    }
    private static void await(Selector selector,Cancellation cancel,long deadline) throws IOException {
        cancel.check(); long remaining=deadline-System.nanoTime();
        if(remaining<=0) throw new SocketTimeoutException("Printer connection/write timed out; some labels may have printed");
        selector.select(Math.max(1,Math.min(100,TimeUnit.NANOSECONDS.toMillis(remaining))));
        selector.selectedKeys().clear(); cancel.check();
    }
}
