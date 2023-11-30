# Onion DNS Implmented in ChorDHT
###### ODNS naming service for onion addresses using DHT based on the Chord Protocol.

## Required libraries:
1. Apache Thrift `>= 0.9.3`
2. Javax Annotation API `>= 1.3.2`
3. SLF4J `>= 1.3.76`

## Instructions
1. Configure IP addresses and port numbers in `chrodht.cfg`.
2. Update `chrodht.cfg` file path in `SuperNode.java`, `NodeN.java`, and `DNSServer.java`.
2. Update `nodeNumber` with its corresponding index in `NodeN.java`. (N=1, 2, 3)
3. Run `SuperNode.java`.
4. Run `Node0.java`, `Node1.java`,..., `NodeN.java` in different computers.
5. Run `DNSServer.java` with an argument that contains the path to `services.csv`.
6. You may run `ClientTester.java` to test different functions.