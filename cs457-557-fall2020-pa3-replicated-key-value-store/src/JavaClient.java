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

// Generated code
import org.apache.thrift.TException;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import java.util.Scanner; 

public class JavaClient {
  public static void main(String [] args) {

    if (args.length != 3) {
      System.out.println("Please enter 'simple' or 'secure' ip/host port");
      System.exit(0);
    }

    try {
      TTransport transport;
      if (args[0].contains("simple")) {
        System.out.println("simple");
        transport = new TSocket(args[1], Integer.valueOf(args[2]));
        transport.open();
      }
      else {
        /*
         * Similar to the server, you can use the parameters to setup client parameters or
         * use the default settings. On the client side, you will need a TrustStore which
         * contains the trusted certificate along with the public key. 
         * For this example it's a self-signed cert. 
         */
        TSSLTransportParameters params = new TSSLTransportParameters();
        params.setTrustStore("/home/cs557-inst/thrift-0.13.0/lib/java/test/.truststore", "thrift", "SunX509", "JKS");
        /*
         * Get a client transport instead of a server transport. The connection is opened on
         * invocation of the factory method, no need to specifically call open()
         */
        transport = TSSLTransportFactory.getClientSocket(args[1], Integer.valueOf(args[2]), 0, params);
      }

      TProtocol protocol = new  TBinaryProtocol(transport);
      KeyValueStore.Client client = new KeyValueStore.Client(protocol);

      System.out.println("Got client connected to :"+args[2]);

      perform(client);

      transport.close();
    } catch (TException x) {
      x.printStackTrace();
    } 
  }

  private static void perform(KeyValueStore.Client client) throws TException
  {
    int methodNumber;
    
    while(true){
        System.out.println("\nEnter number of method you want:\n1)GET\n2)PUT\n3)EXIT");
        Scanner scanner = new Scanner(System.in);
        methodNumber = Integer.parseInt(scanner.nextLine());

        if(methodNumber == 3){
          return;
        }

        System.out.println("Enter the number of consistency level:\n1)ONE\n2)QUORUM");
        int consistencyNumber = Integer.parseInt(scanner.nextLine());

        System.out.println("Enter any key between 0-255");
        int key = Integer.parseInt(scanner.nextLine());

        String value = "";
        if(methodNumber == 2){
          System.out.println("Enter value for key: " + key);
          value = scanner.nextLine();
        }

        switch(methodNumber){
          case 1: // GET method 
                  String returnedValue = client.get(key, consistencyNumber);
                  System.out.println("Value: "+returnedValue);
                  break;

          case 2: // PUT method
                  boolean returnedStatus = client.put(key, value, consistencyNumber);
                  System.out.println("Status: "+returnedStatus);
                  break;
        }
    }
  }
}
