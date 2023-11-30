import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.io.*;
import java.util.List;
import java.util.Properties;

public class ClientTester {
    enum Option {
        setDns, getDns, descDns, exit
    }

    public static void main(String[] args) {
        try {
            // Get Configs
            Properties prop = new Properties();
            String cfgPath = args[0];
            InputStream is = new FileInputStream(cfgPath);
            prop.load(is);
            String[] nodeInfo;
            Option option = Option.setDns;
            Console console = System.console();

            if (args.length == 2) {
                // set all the entries in the file provided
                do {
                    nodeInfo = getNode(prop);
                    if (nodeInfo[0].equals("NACK")) {
                        console.printf("DHT isn't ready yet, trying again.\n");
                        Thread.sleep(Integer.valueOf(prop.getProperty("client.wait")));
                    } else {
                        bulkSet(nodeInfo, args[1]);
                    }
                } while(nodeInfo[0].equals("NACK"));
            }

            while (option != Option.exit) {
                // UI Menu Loop
                System.out.println("CHOOSE> setDns, getDns, descDns, exit\n> ");
                BufferedReader inp = new BufferedReader (new InputStreamReader(System.in));
                String strOption = inp.readLine();

                option = Option.valueOf(strOption);
                switch (option) {
                    case getDns:
                        System.out.println("Enter ODNS Address: ");
                        inp = new BufferedReader (new InputStreamReader(System.in));
                        String key = inp.readLine();
                        nodeInfo = getNode(prop);
                        if(nodeInfo[0].equals("NACK")) {
                            console.printf("DHT isn't ready yet, try again later.\n");
                        }
                        else {
                            get(nodeInfo, key);
                        }
                        break;
                    case setDns:
                        System.out.println("Enter ODNS Address, .onion Address: ");
                        inp = new BufferedReader (new InputStreamReader(System.in));
                        String[] keyValue = inp.readLine().split("\\s*,\\s*");
                        nodeInfo = getNode(prop);
                        if(nodeInfo[0].equals("NACK")) {
                            console.printf("DHT isn't ready yet, try again later.\n");
                        }
                        else {
                            set(nodeInfo, keyValue[0], keyValue[1]);
                        }
                        break;
                    case descDns:
                        nodeInfo = getNode(prop);
                        if(nodeInfo[0].equals("NACK")) {
                            System.out.println("DHT isn't ready yet, try again later.\n");
                        }
                        else {
                            desc(nodeInfo);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String[] getNode(Properties prop) {
        // create client connection
        try{
            String superNodeAddress = prop.getProperty("supernode.address");
            Integer superNodePort = Integer.valueOf(prop.getProperty("supernode.port"));
            TTransport transport = new TSocket(superNodeAddress, superNodePort);
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            SuperNodeService.Client client = new SuperNodeService.Client(protocol);
            String[] nodeInfo = client.GetNode().split("\\s*,\\s*");
            transport.close();
            System.out.println("Successfully retrieved nodeInfo!\n");
            return nodeInfo;
        } catch (TException e) {
            System.out.println("Error connecting to the SuperNode!\n");
            System.exit(-1);
            return new String[]{};
        }
    }

    // helper methods
    private static void bulkSet(String[] nodeInfo, String fileName) throws Exception {
        try(BufferedReader br = new BufferedReader(new FileReader(fileName))){
            String line;
            while ((line = br.readLine()) != null) {
                set(nodeInfo, line.split("\\s*,\\s*")[0], line.split("\\s*,\\s*")[1]);
            }
            System.out.printf("BulkSet from %s complete!\n", fileName);
        } catch (FileNotFoundException err) {
            System.out.printf("File %s doesn't exist.\n", fileName);
        }
    }

    private static void set(String[] nodeInfo, String key, String value) throws TException {
        // create client connection
        System.out.printf("Sending Set(%s, %s) to %s\n", key, value, String.join(",", nodeInfo));
        TTransport transport = new TSocket(nodeInfo[0], Integer.valueOf(nodeInfo[1]));
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);
        String trail = client.set_(key, value);
        System.out.printf("Set(%s, %s) Successful!\nTrail: %s\n", key, value, trail);
        transport.close();
    }

    private static void get(String[] nodeInfo, String key) throws TException {
        // create client connection
        System.out.printf("Sending Get(%s) to %s\n", key, String.join(",", nodeInfo));
        TTransport transport = new TSocket(nodeInfo[0], Integer.valueOf(nodeInfo[1]));
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);
        GetResponse getResponse = client.get_(key);
        if(getResponse.address == null) {
            System.out.printf("Get(%s) Failed - ODNS Address Not Found!\nTrail: %s\n", key, getResponse.trail);
        } else {
            System.out.printf("Get(%s) Successful!\nOnion Address: %s\nTrail: %s\n",
                    key, getResponse.address, getResponse.trail);
        }
        transport.close();
    }

    private static void desc(String[] nodeInfo) throws TException {
        System.out.printf("Sending Desc() to %s\n", String.join(",", nodeInfo));
        TTransport transport = new TSocket(nodeInfo[0], Integer.valueOf(nodeInfo[1]));
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);
        NodeService.Client client = new NodeService.Client(protocol);
        List<String> detailList = client.desc(Integer.valueOf(nodeInfo[2]));
        for(String nodeDetails: detailList){
            System.out.print(nodeDetails);
        }
    }
}
