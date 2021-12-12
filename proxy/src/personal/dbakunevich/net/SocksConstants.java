package personal.dbakunevich.net;

public interface SocksConstants {
    int LISTEN_TIMEOUT = 200;
    int DEFAULT_SERVER_TIMEOUT = 200;

    int DEFAULT_BUF_SIZE = 4096;
    int DEFAULT_PROXY_TIMEOUT = 10;

    byte SOCKS5_Version = 0x05;

    byte SC_CONNECT = 0x01;
    byte SC_BIND = 0x02;
    byte SC_UDP = 0x03;
}