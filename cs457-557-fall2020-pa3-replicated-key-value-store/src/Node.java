import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.TException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter; 
import java.io.FileNotFoundException;
import java.util.Scanner;
/**
	Node class for the individual server
*/
public class Node implements KeyValueStore.Iface{

	private String ip;

	private Integer port;
	
	private Map<Integer, Range> nodeRangeMap;

	private List<KeyValuePair> keyValueStore;

	private Map<Integer, KeyValuePair> missedDataForServer;

	private final String writeAheadLogFileName;
		
	public Node(String ip, Integer port, Map nodeRangeMap) throws SystemException, TException{
		this.ip = ip;
		this.port = port;
		this.nodeRangeMap = nodeRangeMap;
		this.keyValueStore = new ArrayList<>();
		this.writeAheadLogFileName = "/writeAheadLog_" + this.port;
		//System.out.println(nodeRangeMap.toString());
		System.out.println("this.port:"+this.port);
		this.missedDataForServer = new HashMap<>();
		this.askForMissingUpdates();
		this.restoreMemoryFromLog();
	}

	public int getPort(){
		return port;
	}

	/**
		RPC called by every server when its up, started or recovered from failure.
		This server calls this RPC to all other server so as to get the all missing
		updates if this server has missed any which he was supposed to receive.
		(Basically called when server is started)
	*/
	public void askForMissingUpdates(){
		 for (Map.Entry mapElement : this.nodeRangeMap.entrySet()) { 
            Integer portNumber = (Integer)mapElement.getKey(); 
            try{
            	if(!portNumber.equals(this.port)){
					TProtocol protocol = null; 
					TTransport transport = null;
					try{
						transport = new TSocket(this.ip, portNumber.intValue());
						transport.open();
						protocol = new TBinaryProtocol(transport);
					}catch(TException t){
						System.out.println("Could not connect the port number : "+portNumber.intValue());
						transport.close();
						continue;
					}

					KeyValueStore.Client client = new KeyValueStore.Client(protocol);
					
					// Asking client for any update to send this server
					//System.out.println("Asking server:"+portNumber);
					List<KeyValuePair> missingKeyValuePairs = client.giveMissingUpdates(this.port.intValue());
					if (missingKeyValuePairs != null && !missingKeyValuePairs.isEmpty()) {
						System.out.println("Got the missing update successfully" + missingKeyValuePairs.toString());
						keyValueStore.addAll(missingKeyValuePairs);
					}
					else{
						System.out.println("Failed while fetching the missing update");
					}

					transport.close();
            	}
            }catch(TException t){
            	t.printStackTrace();
            }
        } 
	}

	/**
		Restore the memory from the write ahead log, accessing the 
		writing the key value store to the logs from the write ahead log
	*/
	public void restoreMemoryFromLog(){
		try {
    		File fileObj = new File(System.getProperty("user.dir") + this.writeAheadLogFileName);
			fileObj.createNewFile();	// Creates file if does not exist and returns true and returns false if does not exist
			fileObj.setWritable(true, false);

      		Scanner myReader = new Scanner(fileObj);
     	 	while (myReader.hasNextLine()) {
        		String data = myReader.nextLine();
        		System.out.println(data);
        		String[] lineSplitted = data.split(" ");
        		KeyValuePair keyValuePair = new KeyValuePair();
        		keyValuePair.setKey(Integer.parseInt(lineSplitted[0]))
        			.setValue(lineSplitted[1])
        			.setTimestamp(lineSplitted[2]);

        		this.keyValueStore.add(keyValuePair);
      		}
      		System.out.println("Successfully recovered the content from write ahead" + 
      			" log\nContents of store are : " + this.keyValueStore.toString());

      		myReader.close();
    	} catch (FileNotFoundException e) {
      		System.out.println("An error occurred in reading file.");
      		e.printStackTrace();
    	} catch (IOException e) {
      		System.out.println("An error occurred in reading file.");
      		e.printStackTrace();
    	}
	}

	/**
		If some server asks for any missing updates this server sends it the 
		missing updates. This server checks for the port number of the server 
		who wants to get its updates, and checks it in its own missedDataForServer
		map and sends the update to it by invoking its PUT method.
	*/
	public List<KeyValuePair> giveMissingUpdates(int port) throws SystemException, org.apache.thrift.TException{
		List<KeyValuePair> missingKeyValuePairList = new ArrayList<>();
		if(this.missedDataForServer != null && !this.missedDataForServer.isEmpty()){
			for (Map.Entry missingElement : this.missedDataForServer.entrySet()) {
				int missingPort = (int)missingElement.getKey();
				if(port == missingPort){
					KeyValuePair missingKeyValuePair = (KeyValuePair)missingElement.getValue();
					missingKeyValuePairList.add(missingKeyValuePair);
				}
			}	
		}

		return missingKeyValuePairList;
	}

	/**
		Basic get the value for the given key with given consistency level
	*/
	public java.lang.String get(int key, int consistencyLevel) throws SystemException, org.apache.thrift.TException{
		int ownerPort;
		Map<Integer, KeyValuePair> portKeyValueMap = new HashMap<>();

		switch(consistencyLevel){
			case 1: // For consistency level ONE, read 
					final KeyValuePair keyValuePair;
					ownerPort = getOwner(key);
					
					if(ownerPort == (int)this.port){
						keyValuePair = getDataFromKey(key);
					}
					else{
						keyValuePair = getValueWithTimestampFromNode(ownerPort, key);
					}					
				
					if(keyValuePair == null){
						SystemException systemException = new SystemException();
						throw systemException.setMessage("Number of replicas found less than expected");
					}

					final Integer newPort = this.port;
					
					Runnable readRepairInBackground = new Runnable() {
	       				public void run(){
	       					try {
	       						KeyValuePair keyValuePairRR = null;

		          				portKeyValueMap.put(ownerPort, keyValuePair);

								List<Integer> listOfSuccessors = getSuccessiveNodes(ownerPort);

								// Get values and timestamps of successors' port numbers
								for(Integer successor : listOfSuccessors){
									if(!successor.equals(newPort)){
										keyValuePairRR = getValueWithTimestampFromNode(successor, key);
									}
									else{
										keyValuePairRR = getDataFromKey(key);
									}
									
									portKeyValueMap.put(successor, keyValuePairRR);
								}
								KeyValuePair keyValuePairWithLatestTime = readRepair(portKeyValueMap);

			                }catch (Exception e) {
    	        	    	    e.printStackTrace();
        		    	    }
	        			}
	      			};

	      			new Thread(readRepairInBackground).start();
					
					return keyValuePair.getValue();

			case 2: // For consistency level QUORUM, read
					int numberOfReplicasFound = 0;
					KeyValuePair keyValuePairOne = null, keyValuePairTwo = null;

					ownerPort = getOwner(key);
					
					if(ownerPort == (int)this.port){
						keyValuePairOne = getDataFromKey(key);
					}
					else{
						keyValuePairOne = getValueWithTimestampFromNode(ownerPort, key);
					}

					if(isValid(keyValuePairOne)) numberOfReplicasFound++;

      				portKeyValueMap.put(ownerPort, keyValuePairOne);

					List<Integer> listOfSuccessors = getSuccessiveNodes(ownerPort);

					// Get values and timestamps of successors
					for(Integer successor : listOfSuccessors){
						if(!successor.equals(this.port))
							keyValuePairTwo = getValueWithTimestampFromNode(successor, key);
						else
							keyValuePairTwo = getDataFromKey(key);
				
						if(isValid(keyValuePairTwo)){
							numberOfReplicasFound++;
							portKeyValueMap.put(successor, keyValuePairTwo);	
//							System.out.println("numberOfReplicasFound:"+numberOfReplicasFound);
						} 
					}

					if(numberOfReplicasFound < 2) {
						SystemException systemException = new SystemException();
						throw systemException.setMessage("Number of replicas found less than expected");
					}

					KeyValuePair keyValuePairWithLatestTime = readRepair(portKeyValueMap);

					return keyValuePairWithLatestTime.getValue();
		}
		return "";
	}


	/**
		Basic put request storing the given key value pair with the
		given consisteny level.
	*/
	public boolean put(int key, String value, int consistencyLevel) throws SystemException, org.apache.thrift.TException{
		/* If this is the coordinator node then find the owner of the key do things accordingly */
		// find the range and owner node port number 
		int ownerPort = getOwner(key);
		// successive two node entries in map would be replicated
		List<Integer> listOfSuccessors = getSuccessiveNodes(ownerPort);

		KeyValuePair keyValuePair = new KeyValuePair();
		keyValuePair.setKey(key);
		keyValuePair.setValue(value);
		keyValuePair.setTimestamp(LocalDateTime.now().toString());

		switch(consistencyLevel){
			case 1: // For ONE consistency level, write
					boolean status;
					if(ownerPort == (int)this.port){
						status = putInDataStore(keyValuePair);
					}
					else{
						status = putValueToNode(ownerPort, keyValuePair);
					}

					final Integer newPort = this.port;

					/* Should write in the background */
					for(Integer successor : listOfSuccessors){
	      				Runnable simple = new Runnable() {
	       					public void run() {
	       						boolean status = false;
	       						try{
	       							if(successor.equals(newPort)){
	       								status = putInDataStore(keyValuePair);
	       							}
									else{
										status = putValueToNode(successor, keyValuePair);
									}

	       						}catch(Exception e){
	       							e.printStackTrace();
	       						}
	        				}
	      				};
	      				new Thread(simple).start();
	      			}
	      			return status;


			case 2: // For QUORUM consistency level, write
					boolean statusOfOwner, statusOfSucc = false;

					if(ownerPort == (int)this.port){
						statusOfOwner = putInDataStore(keyValuePair);
					}
					else{
						statusOfOwner = putValueToNode(ownerPort, keyValuePair);
					}
	          		
					for(Integer successor : listOfSuccessors){
	          			if(successor == (int)this.port)
							statusOfSucc = putInDataStore(keyValuePair);
						else
							statusOfSucc = putValueToNode(successor, keyValuePair);
				
	          			if(!statusOfSucc){
	          				// Write failed to one of the repalicas
	          				// Hinted handoff
	          				System.out.println("Failed: "+successor);
	          			}	
	          			else{
	          				System.out.println("SUCCESS! wrote a key:"+key+" with value:"+ value + " on this port number:" 
									+this.port);
	          			}
	       			}
	       			
	      			if(statusOfOwner && statusOfSucc){
	      				return true;
	      			}

	      			return false;
		}	

		return false;
	}

	/**
		For non-coordinator nodes to put the data in the datastore
		and returns the status
	*/
	public boolean putInDataStore(KeyValuePair keyValuePair) throws TException{
		/* If this is not the coordinator node, set the values in the key-value store */

		KeyValuePair matchingKeyValuePair = null;

		if((boolean)this.keyValueStore.isEmpty() == false){
			for(KeyValuePair keyValuePairElement : this.keyValueStore){
				if(keyValuePairElement.getKey() == keyValuePair.getKey() 
					&& LocalDateTime.parse(keyValuePairElement.getTimestamp())
						.isBefore(LocalDateTime.parse(keyValuePair.getTimestamp()))){
					matchingKeyValuePair = keyValuePairElement;
				}
			}
			// Update if value is found
			if(matchingKeyValuePair != null){
				// Write ahead log
				writeAheadLog(keyValuePair);
				matchingKeyValuePair.setValue(keyValuePair.getValue());
				matchingKeyValuePair.setTimestamp(keyValuePair.getTimestamp());
				System.out.println("SUCCESS! updated key:"+keyValuePair.getKey()+" with value:"+ keyValuePair.getValue() 
					+ " on this port number:" +this.port);

				System.out.println("Store contents are : "+this.keyValueStore.toString());
				return true;
			}
		}

		// Write ahead log
		writeAheadLog(keyValuePair);

		// add if not found
		this.keyValueStore.add(keyValuePair);
		System.out.println("SUCCESS! wrote a key:"+keyValuePair.getKey()+" with value:"+ keyValuePair.getValue() 
			+ " on this port number:" +this.port);

		System.out.println("Store contents are : "+this.keyValueStore.toString());

		return true;
	}


	public KeyValuePair getDataFromKey(int key) throws TException{
		for(KeyValuePair keyValuePair : keyValueStore){
			if(keyValuePair.getKey() == key){
				return keyValuePair;
			}
		}	
		return new KeyValuePair();	
	}

	private KeyValuePair readRepair(Map portKeyValueMap) throws SystemException, TException{
		Set<Integer> portNumbers = portKeyValueMap.keySet();

		KeyValuePair prev = null, max = new KeyValuePair();
		Integer portWithLatest = 0;

		for(Integer portNumber : portNumbers){
			KeyValuePair keyValuePair = (KeyValuePair) portKeyValueMap.get(portNumber);
			if(prev != null && keyValuePair != null){
				LocalDateTime prevDateTime = LocalDateTime.parse(prev.getTimestamp());
				LocalDateTime thisDateTime = LocalDateTime.parse(keyValuePair.getTimestamp());
				if(prevDateTime.isAfter(thisDateTime)){
					max = prev;
					portWithLatest = portNumber;
				}
			}
			prev = keyValuePair;
		}

		if((int)portWithLatest == 0){
			portWithLatest = getOwner(prev.getKey());
			max = (KeyValuePair)portKeyValueMap.get(portWithLatest);
		}

		// Got the max 
		for(Integer portNumber : portNumbers){
			if(!portNumber.equals(portWithLatest)){
				if(portNumber.equals(this.port)){
					putInDataStore(max);
				}
				else if(!putValueToNode(portNumber, max)){
					// If fails use hinted handoff
					return null;
				}
			}
		}
		return max;
	}


	private boolean putValueToNode(int port, KeyValuePair keyValuePair) throws SystemException, TException{
		TProtocol protocol = null; 
		TTransport transport = null;
		try{
			transport = new TSocket(this.ip, port);
			transport.open();
			protocol = new TBinaryProtocol(transport);
		}catch(TException t){
			/* Exception if the server with the given port number is failed */
			this.missedDataForServer.put(port, keyValuePair);
			System.out.println("missedDataForServer:"+this.missedDataForServer.toString());
			transport.close();
			return false;
		}

		KeyValueStore.Client client = new KeyValueStore.Client(protocol);
		boolean status = client.putInDataStore(keyValuePair);

		transport.close();
		return status;
	}

	private KeyValuePair getValueWithTimestampFromNode(int port, int key) throws SystemException, TException{
		TProtocol protocol = null; 
		TTransport transport = null;
		try{
			transport = new TSocket(this.ip, port);
			transport.open();
			protocol = new TBinaryProtocol(transport);
		}catch(TException t){
			return null;
		}

		KeyValueStore.Client client = new KeyValueStore.Client(protocol);
		KeyValuePair keyValuePair = client.getDataFromKey(key);

		transport.close();
		return keyValuePair;
	}

	private int getOwner(int key){
		try{
			for(Map.Entry mapElement : this.nodeRangeMap.entrySet()){
				Range range = (Range)mapElement.getValue();
				if(key >= range.getStart() && key <= range.getEnd()){
					return (int) mapElement.getKey();
				}
			}
			throw new Exception("Specified key is out of range:"+key);	
		}catch(Exception e){
			e.printStackTrace();
		}
		return -1;
	}

	private List<Integer> getSuccessiveNodes(int ownerPort){
		Set<Integer> setPort = this.nodeRangeMap.keySet();

		List<Integer> listOfSuccessors = new ArrayList<>();

		Integer[] arr = new Integer[4];
		arr = setPort.toArray(arr);

		int counter = 2, i=0;
		final int replicas = 4;

		while(true){
			int element = (int)arr[i];
			if(element == ownerPort){
				for(int j=0; j<counter; j++){
					i++;
					i = i % replicas;
					listOfSuccessors.add(arr[i]);	
				}
				return listOfSuccessors;
			}
			i++;
		}
	}

	private boolean isValid(KeyValuePair keyValuePair){
		return keyValuePair != null && keyValuePair.getKey() != 0 && keyValuePair.getValue() != null;
	}

	// Write ahead log for writing contents to the file
	private void writeAheadLog(KeyValuePair keyValuePair){
		try {
			File fileObj = new File(System.getProperty("user.dir") + this.writeAheadLogFileName);
			fileObj.createNewFile();	// Creates file if does not exist and returns true and returns false if does not exist
			fileObj.setWritable(true, false);

      		FileWriter myWriter = new FileWriter(fileObj, true);
      		myWriter.write(keyValuePair.getKey() + " " + keyValuePair.getValue() + " " + keyValuePair.getTimestamp() + "\n");
      		myWriter.close();
      		//System.out.println("Successfully wrote to the file.");
    	} catch (IOException e) {
      		System.out.println("An error occurred in writing the file.");
      		e.printStackTrace();
    	}
	}
}