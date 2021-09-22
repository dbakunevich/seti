package personal.dbakunevich.findCopies;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final char PORT = 44444;
    private static final String MESSAGE = "hello";
    private static final long MULTICAST_DELAY = 1000;
    private static final long ALIVE_TIMEOUT = 5 * 1000;
    private static volatile long NEXT_SEND_TIME = 0;

    private static void multicastReceive(MulticastSocket multicastSocket, DatagramPacket receivePacket) throws IOException {
        while (!Thread.currentThread().isInterrupted()) {

            multicastSocket.receive(receivePacket);

            MyHashMap.put(receivePacket.getAddress(), System.currentTimeMillis() + ALIVE_TIMEOUT);
            MyHashMap.printMap();
            System.out.println("Packet address: " + receivePacket.getAddress() +
                    "\nGet message: " + new String(receivePacket.getData(), StandardCharsets.UTF_8));
        }
    }

    private static void socketSend(DatagramSocket datagramSocket, DatagramPacket sendPacket) throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            if (NEXT_SEND_TIME < System.currentTimeMillis()) {
                NEXT_SEND_TIME = System.currentTimeMillis() + MULTICAST_DELAY;
                datagramSocket.send(sendPacket);
            }
        }
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Usage: IPv4 or IPv6 address");
            System.exit(1);
        }

        InetAddress address = null;

        try {
            address = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.err.printf("Error address format: %s", args[0]);
            System.exit(1);
        }

        MulticastSocket multicastSocket = new MulticastSocket(PORT);
        DatagramSocket datagramSocket = new DatagramSocket();
        DatagramPacket receivePacket = new DatagramPacket(new byte[MESSAGE.length()],
                                                            MESSAGE.length());
        DatagramPacket sendPacket = new DatagramPacket(MESSAGE.getBytes(StandardCharsets.UTF_8),
                                                        MESSAGE.length(), address, PORT);

        multicastSocket.joinGroup(address);


        Thread receive = new Thread(() -> {
            try {
                multicastReceive(multicastSocket, receivePacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error receive packet");
                System.exit(1);
            }
        });

        Thread send = new Thread(() -> {
            try {
                socketSend(datagramSocket, sendPacket);
            } catch (IOException e) {
                System.err.println("Error send packet");
                System.exit(1);
            }
        });

        Thread filter = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                MyHashMap.delete();
            }
        });


        send.start();
        receive.start();
        filter.start();
    }
}