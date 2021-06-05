exception SystemException {
  1: optional string message
}

struct KeyValuePair{
	1: optional i32 key;
	2: optional string value;
	3: optional string timestamp;
}

service KeyValueStore{
  string get(1: i32 key, 2: i32 consistencyLevel)
    throws (1: SystemException systemException),
  
  bool put(1: i32 key, 2: string value, 3: i32 consistencyLevel)
    throws (1: SystemException systemException),	

  KeyValuePair getDataFromKey(1: i32 key)
   throws (1: SystemException systemException),
	
  bool putInDataStore(1: KeyValuePair keyValuePair)
   throws (1: SystemException systemException),

  list<KeyValuePair> giveMissingUpdates(1: i32 port)
   throws (1: SystemException systemException)
}