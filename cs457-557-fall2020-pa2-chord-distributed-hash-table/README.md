# Chord Distributed Hash Table

In this assignment, we will implement the basic functions of the Chord distributed hash table (DHT).


## Setting up the Environment

Add the thrift compiler path to your PATH environment variable:
```
export PATH=$PATH:/home/cs557-inst/local/bin
```


## Generating stub code, compilation and execution

Inside assignment directory, you'll see file:
* chord.thrift : Contains the thrift definitions of interfaces, services and custom datatypes/structs for our application. This is the Interface Definition File.

Now, there is a folder named as java/src. To generate the client stub and server stub code run the following command:


```
cd java/src
thrift -r --gen java chord.thrift
```

Now, this will generate a folder called "gen-java" in your current directory.
This folder contains the generated client-stub code, server-stub code.

Now we'll compile and run the client and servers.

___
###
1. Generate the classes by typing "make" command. Makefile is already present in the repo.
```
$ make
rm -rf bin/
mkdir bin
mkdir bin/client_classes
mkdir bin/server_classes
javac -classpath /home/cs557-inst/local/lib/libthrift-0.13.0.jar:/home/cs557-inst/local/lib/slf4j-api-1.7.30.jar:/home/cs557-inst/loca/lib/slf4j-log4j12-1.7.12.jar:/home/cs557-inst/local/lib/javax.annotation-api-1.3.2.jar -d bin/client_classes/ java/src/JavaClient.java java/src/DHTHandler.java gen-java/*
javac -classpath /home/cs557-inst/local/lib/libthrift-0.13.0.jar:/home/cs557-inst/local/lib/slf4j-api-1.7.30.jar:/home/cs557-inst/loca/lib/slf4j-log4j12-1.7.12.jar:/home/cs557-inst/local/lib/javax.annotation-api-1.3.2.jar -d bin/server_classes/ java/src/JavaServer.java java/src/DHTHandler.java gen-java/*
Note: java/src/JavaServer.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
$
```

2. Run the server by providing it the port number to listen to (please use a different port number):
```
$ ./server.sh 9090
Starting the simple server...


$ ./server.sh 9091
Starting the simple server...

```

3. Run the client by providing it the IP and port number to connect to:
```
$ ./client.sh 128.226.114.202 9090
```
___

## Description:

For this assignment, I have used Java as a programming language. All the six basic functions provided in the handler file are implemented according to requirements.

Note to TA: As per the discussion with professor, I am submitting this assignment late. Please consider this assignment for grading.

