package io.quarkus.vertx.http.deployment.devmode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

public class RemoteDebugSocketProvider {

    private static final Logger log = Logger.getLogger(RemoteDebugSocketProvider.class);

    public static void run(String host, int port) {
        try {
            ServerSocket s = new ServerSocket(5005);
            try {
                //todo: lifecycle management of connections
                Socket connection = s.accept();

                //TODO: this is hugely hacky
                Socket httpUpgradeConnection = new Socket(host, port);
                httpUpgradeConnection.getOutputStream()
                        .write("GET /remote-debug HTTP/1.1\r\nConnection: upgrade\r\nUpgrade: quarkus-remote-debug\r\n\r\n"
                                .getBytes(StandardCharsets.UTF_8));
                String line = readHttpLine(httpUpgradeConnection.getInputStream());
                String[] parts = line.split(" ");
                if (!parts[1].equals("101")) {
                    try {
                        connection.close();
                    } finally {
                        httpUpgradeConnection.close();
                    }
                    throw new IOException("Invalid HTTP response: " + line);
                }
                while (!line.isEmpty()) {
                    //TODO: actually validate that this is a correct upgrade response
                    line = readHttpLine(httpUpgradeConnection.getInputStream());
                }
                //ok, we have an upgraded connection
                new Thread(new Pump(connection.getInputStream(), httpUpgradeConnection.getOutputStream()),
                        "Debug pump to remote").start();
                new Thread(new Pump(httpUpgradeConnection.getInputStream(), connection.getOutputStream()),
                        "Debug pump from remote").start();

            } catch (Exception e) {
                log.error("Remote debug failed: ", e);
            }

        } catch (IOException e) {
            log.error("Remote debug failed: ", e);
        }
    }

    static String readHttpLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        boolean crRead = false;
        for (;;) {
            int r = in.read();
            if (crRead) {
                if (r != '\n') {
                    throw new IOException("Invalid HTTP response");
                }
                return buf.toString(StandardCharsets.UTF_8);
            }
            if (r == '\r') {
                crRead = true;
            } else {
                buf.write(r);
            }
        }
    }

    static class Pump implements Runnable {
        final InputStream inputStream;
        final OutputStream outputStream;

        Pump(InputStream inputStream, OutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            System.out.println("PUMP STARTING");
            try (inputStream; outputStream) {
                byte[] data = new byte[1024];
                int r;
                while ((r = inputStream.read(data)) > 0) {
                    System.out.println("PUMP: " + r);
                    outputStream.write(data, 0, r);
                }
            } catch (IOException e) {
                log.error("Exception pumping debug data", e);
            } finally {
                System.out.println("PUMP DONE");
            }

        }
    }

}
