package org.openmrs.maven.plugins.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerHelper {
    private Wizard wizard;
    private final Logger log = LoggerFactory.getLogger(ServerHelper.class);

    private static final int MAX_USHORT_VALUE = 65535;

    public ServerHelper(Wizard wizard) {
        this.wizard = wizard;
    }

    public boolean isPortInUse(int port) {
        String message = String.format("\nChecking if port %s is in use... ", port);
        wizard.showMessageNoEOL(message);
        return checkPortUsage(port);
    }

    public boolean checkPortUsage(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            this.closeTestingServerSocket(serverSocket);
        } catch (IOException e) {
            wizard.showMessageNoEOL("[in use]\n");
            return true;
        }
        wizard.showMessageNoEOL("[free]\n");
        return false;
    }

    private void closeTestingServerSocket(ServerSocket serverSocket) throws IOException {
        serverSocket.close();

        while (!serverSocket.isClosed()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.debug("Failed to close test socket", e);
            }
        }
    }

    public int findFreePort(int port) {
        while(isPortInUse(port)) {
            port++;
        }
        return port;
    }

    public boolean isPort(int port) {
        if (port <= ServerHelper.MAX_USHORT_VALUE && port > 0) {
            return true;
        }
        return false;
    }
}
