package com.suecagame.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Uma ligação de um cliente: lê linhas do socket numa thread própria
 * e entrega-as ao servidor; envia mensagens de volta ao cliente.
 */
class ClientHandler implements Runnable {

    private final Socket socket;
    private final SuecaServer server;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile int seat = -1;

    ClientHandler(Socket socket, SuecaServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
    }

    int getSeat() {
        return seat;
    }

    void setSeat(int seat) {
        this.seat = seat;
    }

    void send(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                server.handleMessage(this, line);
            }
        } catch (IOException e) {
            // ligação perdida — tratado no finally
        } finally {
            server.onClientDisconnected(this);
            close();
        }
    }

    void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // já fechado
        }
    }
}
