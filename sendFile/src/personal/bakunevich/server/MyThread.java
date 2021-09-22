package personal.bakunevich.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MyThread extends Thread implements Runnable{
    static String UPLOAD_DIRECTORY = "/home/dbakunevich/IdeaProjects/seti/sendFile/upload/";
    private final ServerSocket serverSocket;
    private final String name;
    private boolean isRun = false;

    MyThread(String name, ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        this.name = name;
    }

    public boolean isRun(){
        return isRun;
    }

    public void run() {
        Socket socket = null;
        InputStream in = null;
        try {
            socket = serverSocket.accept();
        } catch (IOException e) {
            System.err.println("Can't accept client connection.");
            System.exit(1);
        }
        try {
            assert socket != null;
            in = socket.getInputStream();
        } catch (IOException e) {
            System.err.println("Can't get socket input stream.");
        }

        byte[] bytes = new byte[10];
        assert in != null;
        try {
            if (in.read(bytes) <= 0) {
                System.err.println("Can't read command.");
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Can't read command.");
            System.exit(1);
        }
        isRun = true;

        String command = new String(bytes, StandardCharsets.UTF_8).trim();

        System.out.println( "Thread: " + name +
                            "\nStatus: start\n" +
                            "Command: " + command);

        InputStream finalIn = in;
        if (command.equals("send")) {

            Thread send = new Thread(() -> {
                try {
                    send(finalIn);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error receive packet");
                    System.exit(1);
                }
            });
            send.start();
            try {
                send.join();
            } catch (InterruptedException e) {
                System.err.println("Error join thread");
            }
        }
        else if (command.equals("receive")) {
            Socket finalSocket = socket;
            Thread receive = new Thread(() -> {
                try {
                    receive(finalIn, finalSocket);
                } catch (IOException e){
                    e.printStackTrace();
                    System.err.println("Error send packet");
                    System.exit(1);
                }
            });
            receive.start();
            try {
                receive.join();
            } catch (InterruptedException e) {
                System.err.println("Error join thread");
            }
        }

        isRun = false;
        try {
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread.currentThread().interrupt();
        System.out.println( "Thread: " + name +
                "\nStatus: finish");
    }

    private byte[] readFileName(InputStream in) throws IOException {
        byte[] bytes = new byte[4 * 1024];
        if (in.read(bytes) <= 0) {
            System.err.println("Can't file name.");
            System.exit(1);
        }
        return bytes;
    }
    private void send(InputStream in) throws IOException {
        byte[] bytes = readFileName(in);
        String fileName = new String(bytes, StandardCharsets.UTF_8).trim();
        System.out.println( "Thread: " + name +
                            "\nStatus: receive\n" +
                            "File name: " + fileName);
        OutputStream out = null;
        try {
            out = new FileOutputStream(UPLOAD_DIRECTORY + fileName);
        } catch (FileNotFoundException ex) {
            System.err.println("File not found.");
            System.exit(1);
        }

        int count;
        long countBytes = 0;
        bytes = new byte[16 * 1024];
        long time = System.currentTimeMillis();
        while ((count = in.read(bytes)) > 0) {
            if (time + 3 * 1000 < System.currentTimeMillis()) {
                System.out.println("Current speed: " + countBytes / 3000 + "KB/s");
                countBytes = 0;
                time = System.currentTimeMillis();
            }
            out.write(bytes, 0, count);
            countBytes+=count;
        }
        System.out.println("Finish speed: " + (countBytes)  / (time + 3 * 1000 - System.currentTimeMillis()) + "KB/s");

        out.close();
        in.close();
    }

    private void receive(InputStream in, Socket socket) throws IOException {
        byte[] bytes = readFileName(in);

        String fileName = new String(bytes, StandardCharsets.UTF_8).trim();
        System.out.println( "Thread: " + name +
                "\nStatus: receive\n" +
                "File name: " + fileName);
        InputStream sendFile = null;
        OutputStream out = socket.getOutputStream();
        try {
            sendFile = new FileInputStream(UPLOAD_DIRECTORY + fileName);
        } catch (FileNotFoundException ex) {
            System.err.println("File not found.");
            System.exit(1);
        }
        int count;
        long countBytes = 0;
        bytes = new byte[1024];
        long time = System.currentTimeMillis();
        while ((count = sendFile.read(bytes)) != -1) {
            if (time + 3 * 1000 < System.currentTimeMillis()) {
                System.out.println("Current speed: " + countBytes / 3 + "KB/s");
                countBytes = 0;
                time = System.currentTimeMillis();
            }
            out.write(bytes, 0, count);
            countBytes+=count;
        }
        System.out.println("Finish speed: " + (countBytes * 1000)  / (time + 3 * 1000 - System.currentTimeMillis()) + "KB/s");

        sendFile.close();
        out.close();
        in.close();
    }

    public void printName() {
        System.out.println(name);
    }
}
