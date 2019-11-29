package main.java.brokerwork;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class Broker {
    private static int qty = 10;
    private static int cash = 100;
    private static Attachment attach;
    private static final String fixv = "8=FIX.4.2";
    public static int bs;
    public static int dstId;

    public Broker(int id, int by) {
        dstId = id;
        bs = by;
    }

    public void contact() throws Exception
    {
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        SocketAddress serverAddr = new InetSocketAddress("localhost", 5000);
        Future<Void> result = channel.connect(serverAddr);
        result.get();
        System.out.println("Connected");
        attach = new Attachment();
        attach.client = channel;
        attach.buffer = ByteBuffer.allocate(2048);
        attach.isRead = true;
        
        attach.mainThread = Thread.currentThread();

        ReadWriteHandler readWriteHandler = new ReadWriteHandler();
        channel.read(attach.buffer, attach, readWriteHandler);
        try {
            Thread.currentThread().join();
        }
        catch (InterruptedException e) {
          //  e.printStackTrace();
        }
    }
    public static String sellProduct(int dst) {
        String soh = "" + (char)1;
        String msg = " Selling Accepted: id="+attach.clientId+soh+fixv+soh+"35=D"+soh+"54=2"+soh+"38=2"+soh+"44=55"+soh+"55=WTCSOCKS"+soh;
        msg += "50="+attach.clientId+soh+"49="+attach.clientId+soh+"56="+dst+soh;

        String errMsg = "Buying Rejected, you don't have enough funds";
        if (qty > 0)
            return msg;
        else
            return errMsg;
    }

    public static String buyProduct(int dst) {
        String soh = "" + (char)1;
        String msg =  "Buying Aceepted: \n id="+attach.clientId+soh+fixv+soh+"35=D"+soh+"54=1"+soh+"38=2"+soh+"44=90"+soh+"55=WTCSHIRTS"+soh;
        msg += "50="+attach.clientId+soh+"49="+attach.clientId+soh+"56="+dst+soh;

        String errMsg = "Buying Rejected, you don't have enough funds";
        if (cash > 0)
            return msg;
        else
            return errMsg;
    }

    public static boolean proccessReply(String reply) {
        String data[] = reply.split(""+(char)1);
        String tag = "";
        String state = "";

        for (String dat : data) {
            if (dat.contains("35="))
                tag = dat.split("=")[1];
            if (dat.contains("39="))
                state = dat.split("=")[1];
        }

        if (tag.equals("8") && state.equals("8")) {
            System.out.println("\nMarket[" + dstId +"] rejected order\n");
            return false;
        }

        if (tag.equals("8") && state.equals("2")) {
            System.out.println("\nMarket[" + dstId +"] accepted order\n");
            return true;
        }
        return false;
    }

    public static void updateData(boolean state) {
        if (state == false) {
            qty -= 2;
            cash += 55;
        } else {
            qty += 2;
            cash -= 90;
        }   
    }
}