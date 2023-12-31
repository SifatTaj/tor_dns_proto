import org.apache.thrift.TException;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

public class SuperNodeHandler implements SuperNodeService.Iface {
    private Properties prop;
    private ArrayList<String> nodeInfos;
    private Integer dhtSize;
    private String joinInProcess;
    private Integer mbits;
    private Integer nextNodeIndex;

    SuperNodeHandler(Properties properties) {
        prop = properties;
        nodeInfos = new ArrayList<>();
        dhtSize = Integer.valueOf(prop.getProperty("nodes.count"));
        mbits = Integer.valueOf(prop.getProperty("nodes.mbits"));
        nextNodeIndex = 0;
        joinInProcess = "";
    }

    @Override
    public boolean ping() throws TException {
        System.out.println("PING!");
        return false;
    }

    @Override
    public synchronized JoinResponse Join(String address, int port) throws TException {
        if (!joinInProcess.equals("")) {
            // if another join is in process
            System.out.printf("Rejected JOIN from %s:%d\n", address, port);
            return new JoinResponse(-1, "NACK");
        }
        joinInProcess = address + ":" + port;
        System.out.printf("Accepted JOIN from %s\n", joinInProcess);
        Integer nodeId = (int) (nextNodeIndex * Math.pow(2, mbits) / dhtSize);
        nextNodeIndex += 1;
        String newNodeInfo = address + "," + port + "," + nodeId;

        if(nodeInfos.isEmpty()){
            // return the same nodeInfo if its the first one
            nodeInfos.add(newNodeInfo);
            return new JoinResponse(nodeId, newNodeInfo);
        } else {
            // get a random node from the existing DHT
            String neighborNodeInfo = GetRandomDHTNode();
            nodeInfos.add(newNodeInfo);
            return new JoinResponse(nodeId, neighborNodeInfo);
        }
    }

    @Override
    public synchronized void PostJoin(String ip, int port) throws TException {
        if (joinInProcess.equals(ip + ":" + port)) {
            // notify join complete
            System.out.printf("JOIN of Node %s Completed Successfully!\n", joinInProcess);
            joinInProcess = "";
        }
    }

    @Override
    public String GetNode() throws TException {
        if (!joinInProcess.equals("") || nodeInfos.size() < dhtSize) {
            // DHT isn't complete yet.
            return "NACK";
        }
        return GetRandomDHTNode();
    }

    private String GetRandomDHTNode() {
        // return a random node from DHT
        Random rand = new Random();
        int nodeIndex = rand.nextInt(nodeInfos.size());
        return nodeInfos.get(nodeIndex);
    }
}
