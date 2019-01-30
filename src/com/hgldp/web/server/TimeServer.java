package com.hgldp.web.server;

/*
 * @program:NIO
 * @description:
 * @author:hgl
 * @crate:2019-01-28 09:57
 **/
public class TimeServer {

    public static void main(String[] args) {
        int port = 8080;
        if(args != null && args.length > 0){
            try {
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){

            }
        }

        MultiplexerTimeServer timeServer = new MultiplexerTimeServer(port);
        new Thread(timeServer,"NIO-MUltiplexerTimeServer-001").start();
    }
}
