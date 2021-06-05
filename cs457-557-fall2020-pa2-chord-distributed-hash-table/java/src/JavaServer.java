import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import java.net.InetAddress;

import java.util.HashMap;

public class JavaServer {
	public static DHTHandler dhtHandler;
	public static FileStore.Processor processor;
	public static int port;
	public static String ip;

	public static void main(String [] args) {
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
			port = Integer.parseInt(args[0]);
			dhtHandler = new DHTHandler(ip, port);
			processor = new FileStore.Processor(dhtHandler);
			Runnable simple = new Runnable() {
				public void run() {
					simple(processor);
				}
			};

			new Thread(simple).start();
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	public static void simple(FileStore.Processor processor) {
		try {
			TServerTransport serverTransport = new TServerSocket(port);
			TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

			// Use this for a multithreaded server
			// TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

			System.out.println("Starting the simple server...");
			server.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void secure(FileStore.Processor processor) {
		try {
			/*
			* Use TSSLTransportParameters to setup the required SSL parameters. In this example
			* we are setting the keystore and the keystore password. Other things like algorithms,
			* cipher suites, client auth etc can be set. 
			*/
			TSSLTransportParameters params = new TSSLTransportParameters();
			// The Keystore contains the private key
			params.setKeyStore("/home/cs557-inst/thrift-0.13.0/lib/java/test/.keystore", "thrift", null, null);

			/*
			* Use any of the TSSLTransportFactory to get a server transport with the appropriate
			* SSL configuration. You can use the default settings if properties are set in the command line.
			* Ex: -Djavax.net.ssl.keyStore=.keystore and -Djavax.net.ssl.keyStorePassword=thrift
			* 
			* Note: You need not explicitly call open(). The underlying server socket is bound on return
			* from the factory class. 
			*/
			TServerTransport serverTransport = TSSLTransportFactory.getServerSocket(port, 0, null, params);
			TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

			// Use this for a multi threaded server
			// TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

			System.out.println("Starting the secure server...");
			server.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
