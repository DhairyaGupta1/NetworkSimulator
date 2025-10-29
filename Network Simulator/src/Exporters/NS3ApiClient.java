package Exporters;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import AI.GeminiPacketGenerator.NetworkPacket;

public class NS3ApiClient {
    private static final String API_URL = "https://api.ns3.azaken.com/simulate";

    public static class SimulationResult {
        public File resultsZip;
        public List<File> extractedFiles;
        public String traceLogs;
        public File namFile;
        public boolean success;
        public String errorMessage;
        public List<NetworkPacket> generatedPackets;
    }

    public static SimulationResult runSimulation(File tclFile) throws IOException {
        SimulationResult result = new SimulationResult();
        result.extractedFiles = new ArrayList<>();

        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)) {

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(tclFile.getName()).append("\"\r\n");
                writer.append("Content-Type: application/octet-stream\r\n\r\n");
                writer.flush();

                Files.copy(tclFile.toPath(), out);
                out.flush();

                writer.append("\r\n");
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                File tempZip = File.createTempFile("ns3_results_", ".zip");
                try (InputStream in = conn.getInputStream();
                        FileOutputStream fos = new FileOutputStream(tempZip)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                result.resultsZip = tempZip;

                File extractDir = Files.createTempDirectory("ns3_extracted_").toFile();
                unzip(tempZip, extractDir);

                File[] files = extractDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        result.extractedFiles.add(f);

                        if (f.getName().endsWith(".tr") || f.getName().endsWith(".log") ||
                                f.getName().endsWith(".out")) {
                            if (result.traceLogs == null) {
                                result.traceLogs = readFile(f);
                            } else {
                                result.traceLogs += "\n\n=== " + f.getName() + " ===\n\n" + readFile(f);
                            }
                        }

                        if (f.getName().endsWith(".nam")) {
                            result.namFile = f;
                        }
                    }
                }

                result.success = true;

            } else {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()));
                StringBuilder error = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
                result.success = false;
                result.errorMessage = "API Error (" + responseCode + "): " + error.toString();
            }

            conn.disconnect();

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = "Connection error: " + e.getMessage();
            e.printStackTrace();
        }

        return result;
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
