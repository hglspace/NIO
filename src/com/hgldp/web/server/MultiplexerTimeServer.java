package com.hgldp.web.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/*
 * @program:NIO
 * @description:
 * @author:hgl
 * @crate:2019-01-28 10:00
 **/
public class MultiplexerTimeServer implements Runnable{

    private Selector selector;

    private ServerSocketChannel serverChannel;

    private volatile boolean stop;
    /*
    初始化多路复用器，绑定监听端口
     */
    public MultiplexerTimeServer(int port){
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port),1024);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.printf("The time server is start in port:"+port+"\n");
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop(){
        this.stop = true;
    }


    @Override
    public void run() {

        while (!stop){
            try {
                selector.select(1000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()){
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
            }catch (Throwable t){
                t.printStackTrace();
            }
        }
//        多路复用器关闭后，所有注册在上面的channel和pipe等资源都会被自动去注册关闭，所以不需要重复释放资源
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
            //处理新接入的请求消息
            if(key.isAcceptable()){
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(selector,SelectionKey.OP_READ);
            }
            if(key.isReadable()){
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBuffer);
                if(readBytes > 0){
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes,"UTF-8");
                    System.out.printf("The time server receive order:"+body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ?
                            new java.util.Date(System.currentTimeMillis()).toString() :
                            "BAD ORDER";
                    doWriter(sc,currentTime);
                }else  if(readBytes < 0){
//对短链路关闭
                    key.cancel();
                    sc.close();
                }else {
                    //读到0字节，忽略
                }
            }
        }
    }

    private void doWriter(SocketChannel channel,String response) throws IOException{
        if(response != null && response.trim().length() > 0){
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }


    }

}
