package com.hgldp.web.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/*
 * @program:NIO
 * @description:
 * @author:hgl
 * @crate:2019-01-28 11:21
 **/
public class TimeClientHandle implements Runnable{

    private String host;

    private int port;

    private Selector selector;

    private SocketChannel socketChannel;

    private  volatile boolean stop;

    public TimeClientHandle(String host, int port) {
        this.host = host == null ? "127.0.0.1" : host;
        this.port = port;
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }



    @Override
    public void run() {

        try{
            doConnect();
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }

        while (!stop){
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();

                Iterator<SelectionKey> it = selectionKeys.iterator();

                SelectionKey key = null;
                while(it.hasNext()){
                    key = it.next();
                    it.remove();
                    try{

                        handlerInput(key);
                    }catch (Exception e){
                        if(key != null){

                            key.cancel();
                            if(key.channel() != null){
                                key.channel().close();
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }

        }

//        多路复用器关闭后，所有注册在上面的Channel和pipe等资源都会被自动去注册关闭，所以不需要重复释放资源
        if(selector != null){
            try {
                selector.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }


    private void handlerInput(SelectionKey key) throws IOException{
        if(key.isValid()){
//            判断是否连接成功
            SocketChannel sc = (SocketChannel) key.channel();
            if(key.isConnectable()){
                if(sc.finishConnect()){
                    sc.register(selector,SelectionKey.OP_READ);
                    doWriter(sc);
                }else {
                    System.exit(1);//连接失败，进程退出
                }
            }



            if(key.isReadable()){
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBuffer);
                if(readBytes > 0){
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes,"UTF-8");
                    System.out.printf("Now is :"+body);
                    this.stop = true;
                }else if(readBytes < 0){
//                    对端链路关闭
                    key.cancel();
                    sc.close();
                }else {
//                    读到0字节，忽略
                }
            }
        }
    }


    private void doConnect()throws IOException{
//        如果直接连接成功，则注册到多路复用器上，发送请求消息，读应答
        if(socketChannel.connect(new InetSocketAddress(host,port))){
            socketChannel.register(selector,SelectionKey.OP_READ);
            doWriter(socketChannel);
        }else {
            socketChannel.register(selector,SelectionKey.OP_CONNECT);
        }
    }

    private void doWriter(SocketChannel sc) throws IOException{
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuffer writerBuffer = ByteBuffer.allocate(req.length);
        writerBuffer.put(req);
        writerBuffer.flip();
        sc.write(writerBuffer);
        if(!writerBuffer.hasRemaining()){
            System.out.printf("Send order 2 server succeed.");
        }
    }
}
