import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import java.util.List;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.FileWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedWriter;

public class DHTHandler implements FileStore.Iface {
	List<NodeID> nodeList = new ArrayList<>();
	public int port;
	public String ip;
	String key;
	public NodeID currentNode;
	public HashMap<String, HashMap<String, String>> nodeMetaData;

	public DHTHandler(String ip, int port) {
		this.ip = ip;
		this.port = port;
		key = ip + ":" + port;
		currentNode = new NodeID(getSha256(key), ip, port);
		nodeMetaData = new HashMap<String, HashMap<String, String>>();
	}

	@Override
	public void writeFile(RFile rFile) throws SystemException, org.apache.thrift.TException {
		String fileName = rFile.getMeta().getFilename();
		NodeID predNode = findPred(fileName);
		String keyOfFile = getSha256(fileName);
		if(!((predNode.getId().compareTo(keyOfFile)<1) && (currentNode.getId().compareTo(keyOfFile)>=1))) {
			SystemException ex = new SystemException();
			ex.setMessage("This server doesn't own this file.");
			throw ex;
		}
		try {
			File file = new File(rFile.getMeta().getFilename());
			if(!nodeMetaData.containsKey(file.getName())) {
				HashMap<String, String> hashmap = new HashMap<String, String>();
				hashmap.put("version", "0");
				hashmap.put("filename", rFile.getMeta().getFilename());
				String hashValue = getSha256(rFile.getMeta().getFilename());
				hashmap.put("content_hash", hashValue);
				nodeMetaData.put(file.getName(), hashmap);
			} else {
				HashMap<String, String> hashmap = nodeMetaData.get(file.getName());
				Integer version = Integer.parseInt(hashmap.get("version")) + 1;
				hashmap.put("version", version.toString());
			}
		} catch(NullPointerException npe) {
			System.err.println("No metadata found for the file.");
		}
	}

	@Override
	public RFile readFile(java.lang.String filename) throws SystemException, org.apache.thrift.TException {
		String key = getSha256(filename);
		NodeID node = findSucc(key);
		if(!(node.equals(currentNode))) {
			SystemException ex = new SystemException();
			ex.setMessage("This server doesn't own this file.");
			throw ex;
		}
		
		RFile rFile = null;
		try {
			File file = new File(filename);
			if(!(nodeMetaData.containsKey(file.getName()))) {
				SystemException ex = new SystemException();
				ex.setMessage("The file not found on this server.");
				throw ex;
			}
			
			HashMap<String, String> hashmap = nodeMetaData.get(file.getName());
			RFileMetadata rFileMetadata = new RFileMetadata();
			
			rFileMetadata.setFilename(file.getName());
			rFileMetadata.setFilenameIsSet(true);
			rFileMetadata.setVersion(Integer.parseInt(hashmap.get("version")));
			rFileMetadata.setVersionIsSet(true);
			rFile = new RFile();
			rFile.setMeta(rFileMetadata);
			rFile.setMetaIsSet(true);
			byte[] fileContent = Files.readAllBytes(Paths.get(filename));
			String s = new String(fileContent);
			rFile.setContent(s);
			rFile.setContentIsSet(true);
		} catch(IOException ioe) {
			System.err.println("IO error. Cannot access the file.");
		}
		
		return rFile;
	}

	@Override
	public void setFingertable(java.util.List<NodeID> node_list) throws org.apache.thrift.TException {
		this.nodeList = node_list;
	}

	@Override
	public NodeID findSucc(java.lang.String key) throws SystemException, org.apache.thrift.TException {
		NodeID node = findPred(key);
		FileStore.Client client = null;
		TTransport transport = null;
		try {
			transport = new TSocket(node.getIp(), node.getPort());
			transport.open();
			TProtocol protocol = new  TBinaryProtocol(transport);
			client = new FileStore.Client(protocol);
		} catch(Exception e) {
			e.printStackTrace();
		}
	
		NodeID nodeID = client.getNodeSucc();
		transport.close();
		return nodeID;
	}

	@Override
	public NodeID findPred(java.lang.String key) throws SystemException, org.apache.thrift.TException {
		boolean isInRange = false;
		if(nodeList == null) {
			SystemException ex = new SystemException();
			ex.setMessage("finger table not set. Run init first.");
			throw ex;
		}
		
		if((key.compareTo(currentNode.getId()) > 0) && (key.compareTo(nodeList.get(0).getId()) < 0)) {
			isInRange = true;
		} else if(currentNode.getId().compareTo(nodeList.get(0).getId()) > 0) {
			if((key.compareTo(currentNode.getId()) < 0) && (key.compareTo(nodeList.get(0).getId()) < 0)) {
				isInRange = true;
			}
		}

		while(!isInRange) {
			for(int i = (nodeList.size()-1); i>=0; i--) {
				if((nodeList.get(i).getId().compareTo(currentNode.getId()) > 0) && (nodeList.get(i).getId().compareTo(key) < 0)) {
					isInRange = true;
				} else if(currentNode.getId().compareTo(key) > 0) {
					if((nodeList.get(i).getId().compareTo(currentNode.getId()) < 0) && (nodeList.get(i).getId().compareTo(key) < 0)) {
						TTransport transport = null;
						try {
							transport = new TSocket(nodeList.get(i).getIp(), nodeList.get(i).getPort());
							transport.open();
							TProtocol protocol = new  TBinaryProtocol(transport);
      							FileStore.Client client = new FileStore.Client(protocol);

      							NodeID nodeID = client.findPred(key);
      							transport.close();
      							return nodeID;
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			break;
		}
		
		return currentNode;
	}

	@Override
	public NodeID getNodeSucc() throws SystemException, org.apache.thrift.TException {
		if(nodeList == null) {
			SystemException ex = new SystemException();
			ex.setMessage("finger table not set. Run init first.");
			throw ex;
		}

		return nodeList.get(0);
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

