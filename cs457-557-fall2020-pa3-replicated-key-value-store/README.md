# Replicated Key-Value Store with Configurable Consistency

In this assignment, we have implemented a distributed key-value store. For communication among different entities in the system, we have used the Apache Thrift framework. The client sends an RPC call to the server, giving details of the operation to be performed, and the server returns the result.

## Key-Value Store

Each replica server will be a key-value store. Keys are unsigned integers between 0 and 255. Values are strings.
Each replica server should support the following key-value operations:
• get key – given a key, return its corresponding value
• put key value – if the key does not already exist, create a new key-value pair; otherwise, update the key to the new value

## Replicated Key-Value Store

This distributed key-value store will include four replicas. Each replica server is pre-configured with information about all other replicas. Keys are assigned to replica servers using a partitioner similar to the ```ByteOrderedPartitioner``` in Cassandra. Each replica server is expected to be assigned equal portions of the key space. The replication factor will be 3 – every key-value pair should be stored on three out of four replicas. Three replicas are selected as follows: the first replica is determined by the partitioner, and the second and third replicas are determined by going clockwise on the partitioner ring.
Every client request (get or put) is handled by a coordinator. A client can select any replica server as the coordinator. Therefore, any replica can be a coordinator.

## Consistency level

Similar to Cassandra, consistency level is configured by the client. When issuing a request, put or get, the client explicitly specifies the desired consistency level: ONE or QUORUM

## Hinted handoff

During write, the coordinator tries to write to all replicas. As long as enough replicas have succeeded, ONE or QUORUM, it will respond successful to the client. However, if not all replicas succeeded, e.g., two have succeeded but one replica server has failed, the coordinator would store a “hint” locally. If at a later time the failed server has recovered, it will send a message to all replicas. This will allow other replica servers that have stored “hints” for it to know it has recovered and send over the stored hints.

## Client

A client issues a stream of get and put requests to the key-value store. Once started, the client acts as a console, allowing users to issue a stream of requests. The client selects one arbitrary replica server as the coordinator for all its requests. That is, all requests from a single client are handled by the same coordinator. Multiple clients can also be run with different coordinator selected by client.



## Setting up the Environment

Add the thrift compiler path to your PATH environment variable:
```
export PATH=$PATH:/home/cs557-inst/local/bin
```


## Generating stub code, compilation, and execution

Inside assignment directory, you'll see file:
* keyValue.thrift: Contains the thrift definitions of interfaces, services and custom datatypes/structs for our application. This is the Interface Definition File.

Now, there is a folder named as java/src. To generate the client stub and server stub code run the following command:


```
thrift -r --gen java keyValue.thrift
```

Now, this will generate a folder called "gen-java" in your current directory.
This folder contains the generated client-stub code, server-stub code.

Now we'll compile and run the client and servers.

___
###
1. Generate the classes by typing "make" command. Makefile is already present in the repo.
```
$ make
```

2. Run the server:
```
$ ./server.sh

```

3. Run the client by providing it the IP and port number to act as a coordinator:
```
$ ./client.sh 128.226.114.201 9090
```

## Tasks both group members worked

Ketan: Requirement gathering from assignment description, coding server and client module, documenting the code, creating test cases and testing.

Chaitanya: Requirement gathering from assignment description, coding the RPC module (Node.java), creating IDL file, testing.

## Description

We have completed this assignment using Java and Apache Thrift tool. We have tested this assignment on remote01.cs.binghamton.edu machine.
