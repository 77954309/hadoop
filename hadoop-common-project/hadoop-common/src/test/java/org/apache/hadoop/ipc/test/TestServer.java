package org.apache.hadoop.ipc.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RpcEngine;
import org.apache.hadoop.ipc.TestRPC;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @Classname TestServer
 * @Description TODO
 * @Date 2020/6/28 20:50
 * @Created by limeng
 */
public class TestServer {

    private static final String ADDRESS = "0.0.0.0";

    public static final Log LOG =
            LogFactory.getLog(TestRPC.class);

    private static Configuration conf;

    @Before
    public void setupConf() {
        conf = new Configuration();
        conf.setClass(TestClientProtocol.class.getName(),
                TestClientProtocolImpl.class, TestClientProtocol.class);
        UserGroupInformation.setConfiguration(conf);
    }

   @Test
   public void testServer() throws IOException {
       RPC.Server server = new RPC.Builder(conf).setProtocol(TestClientProtocol.class)
               .setInstance(new TestClientProtocolImpl()).setBindAddress(ADDRESS).setPort(99999)
               .setNumHandlers(5).build();


       server.start();
   }



   @Test
   public void testClient() throws IOException {

       TestClientProtocol proxy = RPC.getProxy(TestClientProtocol.class, TestClientProtocol.versionID, new InetSocketAddress(ADDRESS, 99999), conf);

       int result = proxy.add(5, 6);
       String test = proxy.echo("test");

       System.out.println(result);

       System.out.println(test);
   }
}
