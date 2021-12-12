package personal.dbakunevich.net;

public class Main {
    public static void main(String[] args) {
        int PORT = 44444;
        SocksServer server = new SocksServer();
        server.start(PORT);

    }
}
