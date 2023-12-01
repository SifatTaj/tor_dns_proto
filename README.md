# Onion DNS Implmented in ChorDHT
###### ODNS naming service for onion addresses using DHT based on the Chord Protocol.

## Required libraries:
1. Apache Thrift `>= 0.9.3`
2. Javax Annotation API `>= 1.3.2`
3. SLF4J `>= 1.3.76`

## Instructions
1. Configure IP addresses and port numbers in `chrodht.cfg`.
3. Run `SuperNode.java` with an argument that contains the path to `chrodht.cfg`.
4. Run `Node.java` in different computers with two arguments: `args[0]: path to chrodht.cfg`, `args[1]: index ID`.
5. Run `DNSServer.java` two arguments: `args[0]: path to chrodht.cfg`, `args[1]: path to services.csv`.
6. You may run `ClientTester.java` to test different functions.