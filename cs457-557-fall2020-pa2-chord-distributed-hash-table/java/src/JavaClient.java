import org.apache.thrift.TException;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import java.io.File;

import java.security.MessageDigest;

public class JavaClient {
  public static void main(String [] args) {

    if (args.length != 3) {
      System.out.println("Please enter 'simple' or 'secure' ip/host port");
      System.exit(0);
    }

    try {
      TTransport transport;
      if (args[0].contains("simple")) {
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
      FileStore.Client client = new FileStore.Client(protocol);

       String fileName = "128.226.114.202:9092";
       /*File file = new File(fileName);
       RFile rFile = new RFile();
       RFileMetadata rFileMetadata = new RFileMetadata();
       rFileMetadata.setFilename(file.getName());
	   rFileMetadata.setVersion(0);
		rFile.setMeta(rFileMetadata);
		rFile.setContent("some content");
      
      client.writeFile(rFile);*/
      client.readFile(fileName);
		//System.out.println("file written successfully.");
      transport.close();
    } catch (TException x) {
      x.printStackTrace();
    } 
  }

	public static String getSha256(String key) {
		try{
		    MessageDigest digest = MessageDigest.getInstance("SHA-256");
		    byte[] hash = digest.digest(key.getBytes("UTF-8"));
		    StringBuffer hexString = new StringBuffer();

		    for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if(hex.length() == 1) hexString.append('0');
				hexString.append(hex);
		    }

		    return hexString.toString();
		} catch(Exception ex){
		   throw new RuntimeException(ex);
		}
	}
}
