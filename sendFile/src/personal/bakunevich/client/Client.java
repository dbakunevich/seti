package personal.bakunevich.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    static String HOST = "192.168.1.130";
//  static String FILE_NAME = "3.mp4";
    static String FILE_NAME = "/home/dbakunevich/IdeaProjects/seti/sendFile/file";
    static String PATH_TO_STORAGE = "/home/dbakunevich/IdeaProjects/seti/sendFile/downloads/";
    static String COMMAND = "send";
    public static void main(String[] args) throws IOException {
        Socket socket;

        socket = new Socket(HOST, 44444);

        File file = new File(FILE_NAME);
        byte[] bytes;
        OutputStream out = socket.getOutputStream();
        InputStream in = null;
        out.write(COMMAND.getBytes(StandardCharsets.UTF_8));
        bytes = FILE_NAME.split("/")[FILE_NAME.split("/").length - 1].getBytes(StandardCharsets.UTF_8);
        out.write(bytes);
        out.write(" ".getBytes(StandardCharsets.UTF_8));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int count;
        bytes = new byte[16 * 1024];
        if (COMMAND.equals("send")) {
            in = new FileInputStream(file);
            while ((count = in.read(bytes)) > 0) {
                out.write(bytes, 0, count);
            }
            out.close();
            in.close();
            socket.close();
        }
        else if (COMMAND.equals("receive")){
            in = socket.getInputStream();
            String inputFile = FILE_NAME.split("/")[FILE_NAME.split("/").length - 1];
            OutputStream receiveFile = new FileOutputStream(PATH_TO_STORAGE + inputFile);
            while (true) {
                assert in != null;
                if (!((count = in.read(bytes)) > 0)) break;
                receiveFile.write(bytes, 0, count);
            }
            receiveFile.close();
        }
        assert in != null;
        out.close();
        in.close();
        socket.close();
    }
}
