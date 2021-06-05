/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.net.InetSocketAddress;
import java.net.InetAddress;

public class JavaServer {

  private static int port;

  private static Node node;

  public static KeyValueStore.Processor processor;

  private static Map<Integer, Range> nodeRangeMap;

  public static void main(String [] args) {
    try {
    	File nodesObj = new File(System.getProperty("user.dir") + "/nodes.txt");
	    Scanner fileReader = new Scanner(nodesObj);
      	
      	nodeRangeMap = new HashMap<>();
        port = Integer.valueOf(args[0]);
        String ip = "";

        
        /* Following is the code for the partitioner - diving 0-255 in four divisions 
		 0-63, 64-127, 128-191, 192-255 putting in the map for every port number */
      	int start = 0;
      	final int range = 63;
      	
      	while (fileReader.hasNextLine()) {
        	String data = fileReader.nextLine();
        	String[] arr = data.split(":");
        	nodeRangeMap.put(new Integer(Integer.parseInt(arr[1])), new Range(start, start+range));
        	start = start + range + 1;
          ip = arr[0];
      	}

      	System.out.println(nodeRangeMap.toString());

      	
      	/* First run the server will inform ranges of
      		all the nodes to all the nodes. */
 	    node = new Node(ip, port, nodeRangeMap);
      	processor = new KeyValueStore.Processor(node);
	     	System.out.println(node.getPort());
      		Runnable simple = new Runnable() {
       		public void run() {
        		simple(processor);
       		}
      	};
      	new Thread(simple).start();

      	fileReader.close();
      	    
      /*
      Runnable secure = new Runnable() {
        public void run() {
          secure(processor);
        }
      };
      */

      // new Thread(secure).start();
    }catch (FileNotFoundException e) {
      	e.printStackTrace();
    }
    catch (Exception x) {
    	x.printStackTrace();
    }
  }

  public static void simple(KeyValueStore.Processor processor) {
    try {
      TServerTransport serverTransport = new TServerSocket(port);
      //TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

      // Use this for a multithreaded server
      TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

      System.out.println("Starting the simple server at port..."+port);
      server.serve();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}