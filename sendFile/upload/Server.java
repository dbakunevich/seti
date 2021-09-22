package personal.bakunevich.server;

import java.io.*;
import java.net.ServerSocket;
import java.util.Scanner;

public class Server {
    static String UPLOAD_DIRECTORY = "/home/dbakunevich/IdeaProjects/seti/sendFile/upload/";

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(44444);
        } catch (IOException e) {
            System.err.println("Can't setup server on this port number.");
            System.exit(1);
        }

        Thread live = new Thread(() -> {
            Scanner waitExit = new Scanner(System.in);
            String exit = waitExit.next();
            while (exit == null)
                exit = waitExit.next();
            Thread.currentThread().interrupt();
        });
        live.start();
        int counterThreads = 0;
        int lengthMyThreadArr = 2;
        MyThread[] threads = new MyThread[lengthMyThreadArr];

        threads[counterThreads] = new MyThread(String.valueOf(counterThreads), serverSocket);
        threads[counterThreads].start();
        while (live.isAlive()) {
            if (threads[counterThreads].isInterrupted()){
                threads[counterThreads] = new MyThread(String.valueOf(counterThreads), serverSocket);
                threads[counterThreads].start();
            }
            if (threads[counterThreads].isRun()){
                if (counterThreads == lengthMyThreadArr - 1)
                    counterThreads = -1;
                ++counterThreads;
                if (threads[counterThreads] != null && threads[counterThreads].isRun())
                    continue;
                threads[counterThreads] = new MyThread(String.valueOf(counterThreads), serverSocket);
                threads[counterThreads].start();
            }
        }
    }
}