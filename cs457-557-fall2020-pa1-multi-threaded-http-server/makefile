all: run

run: httpProcessor
	java Server

httpProcessor: HttpProcessor.java Server.java
	javac *.java

clean:
	rm *.class