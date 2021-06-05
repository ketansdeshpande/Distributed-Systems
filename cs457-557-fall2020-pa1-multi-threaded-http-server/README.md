# CS557: Assignment 1
**Name:** Ketan Deshpande
-----------------------------------------------------------------------
This is a simple simple multi-threaded HTTP server that only accepts HTTP GET requests and returns the desired content to the client.

## How to compile and run:
1. make (This will compile the program and run the Server class file)
2. make clean (This is an optional command to clean the .class files)
3. Ctrl + C (To stop the server process)

## Implementation details:
The code divided into 2 modules, Server.java and HttpProcessor.java.
Server.java creates a server socket and listens to this socket by using accept() method. This listening will be done by socket named client. This instannce of client is sent to the HttpProcessor.java file to process the HTTP requests. HttpProcessor will set the client socket and start a thread from constructor. In run() method an instance of input stram reader will read the contents of the request sent from client. In this input line, we are checking for the requested resource. If the resource is found, the built response is sent to the output stream of the client and the contents of the resource will be copied to output stram to be served at the client side. If the resource is not found, it will send 404 Not Found error message to the client as well as it will create an exception.

## Sample input and output:
1. Run make command  
Output:  
javac *.java  
java Server  
Server started at: remote06:8080  

2. wget http://remote06.cs.binghamton.edu:8080/sample.html

Output at client side:  

--2020-10-01 15:42:26--  http://remote06.cs.binghamton.edu:8080/sample.html  
Resolving remote06.cs.binghamton.edu (remote06.cs.binghamton.edu)... 128.226.114.206  
Connecting to remote06.cs.binghamton.edu (remote06.cs.binghamton.edu)|128.226.114.206|:8080... connected.  
HTTP request sent, awaiting response... 200 OK  
Length: 107 [text/html]  
Saving to: ‘sample.html.1’  

sample.html.1                                               100%  [========================================================================================================================================>]     107  --.-KB/s    in 0s  

2020-10-01 15:42:26 (15.9 MB/s) - ‘sample.html.1’ saved [107/107]  
  
Ouput at server side:  

/sample.html|128.226.114.203|60528|1  