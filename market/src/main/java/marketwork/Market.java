package main.java.marketwork;


import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Random;
import java.util.concurrent.Future;
public class Market
{
    private static int qty;
    private static int price;
    private static int req;
    private static int dstId;
    private static final String fixv = "8=FIX.4.2";
    private static Attachment attach;

    public Market(int _qty, int _pr)
    {
        qty = _qty;
        price = _pr;
        try {
            Random rand = new Random();
            req = rand.nextInt(3) + 1;
        }
        catch(Exception e) {
            req = 2;
        }
    }

    public void contact() throws Exception {
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        SocketAddress serverAddr = new InetSocketAddress("localhost", 5001);
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
        } catch(Exception e) {
            
        }
    }
    public static String processRequest(String res)
    {
        String data[] = res.split("" + (char)1);
        String msgType="";
        String reqType="";
        String price="";
        String quant="";
        for(String dat : data)
        {
            if (dat.contains("35="))
                msgType = dat.split("=")[1];
            else if (dat.contains("54="))
                reqType = dat.split("=")[1];
            else if (dat.contains("44="))
                price = dat.split("=")[1];
            else if (dat.contains("38="))
                quant = dat.split("=")[1];
            else if (dat.contains("id="))
                dstId = Integer.parseInt(dat.split("=")[1]);
        }
        
        return process(msgType, reqType, price, quant);
    }
    private static String process(String msgType, String reqType, String pric, String quant)
    {
        int p = Integer.parseInt(pric);
        int q = Integer.parseInt(quant);
        if (msgType.equals("D") && reqType.equals("2") && p < price && (req == 2 || req == 3))
            return getMessage(3, Integer.parseInt(quant)); //buy from broker
        else if (msgType.equals("D") && reqType.equals("1") && p >= price && qty - q >= 0 && (req == 2 || req == 3))
            return getMessage(2, Integer.parseInt(quant)); //sell to broker
        else
            return getMessage(1, Integer.parseInt(quant)); //reject broker request
    }
    private static String getMessage(int code, int quant)
    {
        String soh = "" + (char)1;
        String msg = "";
        if (code == 1)
            msg = "id="+attach.clientId+soh+fixv+soh+"35=8"+soh+"39=8"+soh+"50="+attach.clientId+soh+"49="+attach.clientId+soh+"56="+dstId+soh;
        if (code == 2)
        {
            msg = "id="+attach.clientId+soh+fixv+soh+"35=8"+soh+"39=2"+soh+"50="+attach.clientId+soh+"49="+attach.clientId+soh+"56="+dstId+soh;
            qty -= quant;
        }
        if (code == 3)
        {
            msg = "id="+attach.clientId+soh+fixv+soh+"35=8"+soh+"39=2"+soh+"50="+attach.clientId+soh+"49="+attach.clientId+soh+"56="+dstId+soh;
            qty += quant;
        }
        return msg + getCheckSum(msg);
    }
    private static String getCheckSum(String msg)
    {
        int j = 0;
        char t[];
        String soh = "" + (char)1;
        String datum[] = msg.split(soh);
        for(int k = 0; k < datum.length; k++)
        {
          t = datum[k].toCharArray();
          for(int i = 0; i < t.length; i++)
          {
            j += (int)t[i];
          }
          j += 1;
        }
        return ("10="+ (j % 256) + soh);
    }
}