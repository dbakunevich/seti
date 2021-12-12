package personal.dbakunevich.net;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static personal.dbakunevich.net.Utils.getSocketInfo;

public class Socks5 {

    private static final int[] ADDR_Size = {
            -1, //'00' No such AType
            4, //'01' IP v4 - 4Bytes
            -1, //'02' No such AType
            -1, //'03' First Byte is Len
            16  //'04' IP v6 - 16bytes
    };

    private static final byte[] SRE_REFUSE = {(byte) 0x05, (byte) 0xFF};
    private static final byte[] SRE_ACCEPT = {(byte) 0x05, (byte) 0x00};
    private static final int MAX_ADDR_LEN = 255;
    private byte ADDRESS_TYPE;
    private DatagramSocket DGSocket = null;
    private DatagramPacket DGPack = null;
    private InetAddress UDP_IA = null;
    private int UDP_port = 0;


    final Proxy m_Parent;
    final byte[] DST_Port = new byte[2];
    byte[] DST_Addr;
    byte SOCKS_Version = 0;
    byte socksCommand;

    private InetAddress m_ExtLocalIP = null;
    InetAddress m_ServerIP = null;
    int m_nServerPort = 0;
    InetAddress m_ClientIP = null;
    int m_nClientPort = 0;

    Socks5(Proxy Parent) {
        m_Parent = Parent;
        DST_Addr = new byte[MAX_ADDR_LEN];

    }

    public byte getSuccessCode() {
        return 0;
    }

    public byte getFailCode() {
        return 4;
    }

    public InetAddress calcInetAddress(byte AType, byte[] addr) {
        InetAddress IA;

        switch (AType) {
            // Version IP 4
            case 0x01:
                IA = Utils.calcInetAddress(addr);
                break;
            // Version IP DOMAIN NAME
            case 0x03:
                if (addr[0] <= 0) {
                    System.err.println("SOCKS 5 - calcInetAddress() : BAD IP in command - size : " + addr[0]);
                    return null;
                }
                StringBuilder sIA = new StringBuilder();
                for (int i = 1; i <= addr[0]; i++) {
                    sIA.append((char) addr[i]);
                }
                try {
                    IA = InetAddress.getByName(sIA.toString());
                } catch (UnknownHostException e) {
                    return null;
                }
                break;
            default:
                return null;
        }
        return IA;
    }

    public String commName(byte code) {
        return switch (code) {
            case 0x01 -> "CONNECT";
            case 0x02 -> "BIND";
            case 0x03 -> "UDP Association";
            default -> "Unknown Command";
        };
    }

    public String replyName(byte code) {
        return switch (code) {
            case 0 -> "SUCCESS";
            case 1 -> "General SOCKS Server failure";
            case 2 -> "Connection not allowed by ruleset";
            case 3 -> "Network Unreachable";
            case 4 -> "HOST Unreachable";
            case 5 -> "Connection Refused";
            case 6 -> "TTL Expired";
            case 7 -> "Command not supported";
            case 8 -> "Address Type not Supported";
            case 9 -> "to 0xFF UnAssigned";
            case 90 -> "Request GRANTED";
            case 91 -> "Request REJECTED or FAILED";
            case 92 -> "Request REJECTED - SOCKS server can't connect to Identd on the client";
            case 93 -> "Request REJECTED - Client and Identd report diff user-ID";
            default -> "Unknown Command";
        };
    }

    public boolean isInvalidAddress() {
        m_ServerIP = calcInetAddress(ADDRESS_TYPE, DST_Addr);
        m_nServerPort = Utils.calcPort(DST_Port[0], DST_Port[1]);

        m_ClientIP = m_Parent.m_ClientSocket.getInetAddress();
        m_nClientPort = m_Parent.m_ClientSocket.getPort();

        return !((m_ServerIP != null) && (m_nServerPort >= 0));
    }

    public void authenticate(byte SOCKS_Ver) throws Exception {
        SOCKS_Version = SOCKS_Ver;
        if (!checkAuthentication()) {// It reads whole Cli Request
            refuseAuthentication("SOCKS 5 - Not Supported Authentication!");
            throw new Exception("SOCKS 5 - Not Supported Authentication.");
        }
        acceptAuthentication();
    }

    public void refuseAuthentication(String msg) {
        System.out.println("SOCKS 5 - Refuse Authentication: '" + msg + "'");
        m_Parent.sendToClient(SRE_REFUSE);
    }

    public void acceptAuthentication() {
        System.out.println("SOCKS 5 - Accepts Auth. method 'NO_AUTH'");
        byte[] tSRE_Accept = SRE_ACCEPT;
        tSRE_Accept[0] = SOCKS_Version;
        m_Parent.sendToClient(tSRE_Accept);
    }

    protected byte getByte() {
        try {
            return m_Parent.getByteFromClient();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean checkAuthentication() {
        final byte Methods_Num = getByte();
        final StringBuilder Methods = new StringBuilder();

        for (int i = 0; i < Methods_Num; i++) {
            Methods.append(",-").append(getByte()).append('-');
        }

        return ((Methods.indexOf("-0-") != -1) || (Methods.indexOf("-00-") != -1));
    }

    public void getClientCommand() throws Exception {
        SOCKS_Version = getByte();
        socksCommand = getByte();
        /*byte RSV =*/ getByte(); // Reserved. Must be'00'
        ADDRESS_TYPE = getByte();

        int Addr_Len = ADDR_Size[ADDRESS_TYPE];
        DST_Addr[0] = getByte();
        if (ADDRESS_TYPE == 0x03) {
            Addr_Len = DST_Addr[0] + 1;
        }

        for (int i = 1; i < Addr_Len; i++) {
            DST_Addr[i] = getByte();
        }
        DST_Port[0] = getByte();
        DST_Port[1] = getByte();

        if (SOCKS_Version != SocksConstants.SOCKS5_Version) {
            System.out.println("SOCKS 5 - Incorrect SOCKS Version of Command: " +
                    SOCKS_Version);
            refuseCommand((byte) 0xFF);
            throw new Exception("Incorrect SOCKS Version of Command: " +
                    SOCKS_Version);
        }

        if ((socksCommand < SocksConstants.SC_CONNECT) || (socksCommand > SocksConstants.SC_UDP)) {
            System.err.println("SOCKS 5 - GetClientCommand() - Unsupported Command : \"" + commName(socksCommand) + "\"");
            refuseCommand((byte) 0x07);
            throw new Exception("SOCKS 5 - Unsupported Command: \"" + socksCommand + "\"");
        }

        if (ADDRESS_TYPE == 0x04) {
            System.err.println("SOCKS 5 - GetClientCommand() - Unsupported Address Type - IP v6");
            refuseCommand((byte) 0x08);
            throw new Exception("Unsupported Address Type - IP v6");
        }

        if ((ADDRESS_TYPE >= 0x04) || (ADDRESS_TYPE <= 0)) {
            System.err.println("SOCKS 5 - GetClientCommand() - Unsupported Address Type: " + ADDRESS_TYPE);
            refuseCommand((byte) 0x08);
            throw new Exception("SOCKS 5 - Unsupported Address Type: " + ADDRESS_TYPE);
        }

        if (isInvalidAddress()) {  // Gets the IP Address
            refuseCommand((byte) 0x04); // Host Not Exists...
            throw new Exception("SOCKS 5 - Unknown Host/IP address '" + m_ServerIP.toString() + "'");
        }

        System.out.println("SOCKS 5 - Accepted SOCKS5 Command: \"" + commName(socksCommand) + "\"");
    }

    public void replyCommand(byte replyCode) {
        System.out.println("SOCKS 5 - Reply to Client \"" + replyName(replyCode) + "\"");

        final int pt;

        byte[] REPLY = new byte[10];
        byte[] IP = new byte[4];

        if (m_Parent.m_ServerSocket != null) {
            pt = m_Parent.m_ServerSocket.getLocalPort();
        } else {
            IP[0] = 0;
            IP[1] = 0;
            IP[2] = 0;
            IP[3] = 0;
            pt = 0;
        }

        formGenericReply(replyCode, pt, REPLY, IP);

        m_Parent.sendToClient(REPLY);// BND.PORT
    }

    protected void refuseCommand(byte errorCode) {
        System.out.println("Socks 4 - Refuse Command: \"" + replyName(errorCode) + "\"");
        replyCommand(errorCode);
    }

    public void connect() throws Exception {
        System.out.println("Connecting...");
        //	Connect to the Remote Host
        try {
            m_Parent.connectToServer(m_ServerIP.getHostAddress(), m_nServerPort);
        } catch (IOException e) {
            refuseCommand(getFailCode()); // Connection Refused
            throw new Exception("Socks 4 - Can't connect to " +
                    getSocketInfo(m_Parent.m_ServerSocket));
        }

        System.out.println("Connected to " + getSocketInfo(m_Parent.m_ServerSocket));
        replyCommand(getSuccessCode());
    }

    public void bindReply(byte replyCode, InetAddress IA, int PT) {
        byte[] IP = {0, 0, 0, 0};

        System.out.println("BIND Reply to Client \"" + replyName(replyCode) + "\"");

        byte[] REPLY = new byte[10];
        if (IA != null) IP = IA.getAddress();

        formGenericReply((byte) ((int) replyCode - 90), PT, REPLY, IP);

        if (m_Parent.isActive()) {
            m_Parent.sendToClient(REPLY);
        } else {
            System.out.println("BIND - Closed Client Connection");
        }
    }

    public void udpReply(byte replyCode, InetAddress IA, int pt) {
        System.out.println("Reply to Client \"" + replyName(replyCode) + "\"");

        if (m_Parent.m_ClientSocket == null) {
            System.out.println("Error in UDP_Reply() - Client socket is NULL");
        }
        byte[] IP = IA.getAddress();

        byte[] REPLY = new byte[10];

        formGenericReply(replyCode, pt, REPLY, IP);

        m_Parent.sendToClient(REPLY);// BND.PORT
    }

    private void formGenericReply(byte replyCode, int pt, byte[] REPLY, byte[] IP) {
        REPLY[0] = SocksConstants.SOCKS5_Version;
        REPLY[1] = replyCode;
        REPLY[2] = 0x00;        // Reserved	'00'
        REPLY[3] = 0x01;        // DOMAIN NAME Address Type IP v4
        REPLY[4] = IP[0];
        REPLY[5] = IP[1];
        REPLY[6] = IP[2];
        REPLY[7] = IP[3];
        REPLY[8] = (byte) ((pt & 0xFF00) >> 8);// Port High
        REPLY[9] = (byte) (pt & 0x00FF);      // Port Low
    }

    public void udp() throws IOException {
        //	Connect to the Remote Host
        try {
            DGSocket = new DatagramSocket();
            initUdpInOut();
        } catch (IOException e) {
            refuseCommand((byte) 0x05); // Connection Refused
            throw new IOException("Connection Refused - FAILED TO INITIALIZE UDP Association.");
        }

        InetAddress MyIP = m_Parent.m_ClientSocket.getLocalAddress();
        int MyPort = DGSocket.getLocalPort();

        //	Return response to the Client
        // Code '00' - Connection Succeeded,
        // IP/Port where Server will listen
        udpReply((byte) 0, MyIP, MyPort);

        System.out.println("UDP Listen at: <" + MyIP.toString() + ":" + MyPort + ">");

        while (m_Parent.checkClientData() >= 0) {
            processUdp();
            Thread.yield();
        }
        System.out.println("UDP - Closed TCP Master of UDP Association");
    }

    private void initUdpInOut() throws IOException {
        DGSocket.setSoTimeout(SocksConstants.DEFAULT_PROXY_TIMEOUT);
        m_Parent.m_Buffer = new byte[SocksConstants.DEFAULT_BUF_SIZE];
        DGPack = new DatagramPacket(m_Parent.m_Buffer, SocksConstants.DEFAULT_BUF_SIZE);
    }

    private byte[] addDgpHead(byte[] buffer) {
        byte[] IABuf = DGPack.getAddress().getAddress();
        int DGport = DGPack.getPort();
        int HeaderLen = 6 + IABuf.length;
        int DataLen = DGPack.getLength();
        int NewPackLen = HeaderLen + DataLen;

        byte[] UB = new byte[NewPackLen];

        UB[0] = (byte) 0x00;    // Reserved 0x00
        UB[1] = (byte) 0x00;    // Reserved 0x00
        UB[2] = (byte) 0x00;    // FRAG '00' - Standalone DataGram
        UB[3] = (byte) 0x01;    // Address Type -->'01'-IP v4
        System.arraycopy(IABuf, 0, UB, 4, IABuf.length);
        UB[4 + IABuf.length] = (byte) ((DGport >> 8) & 0xFF);
        UB[5 + IABuf.length] = (byte) ((DGport) & 0xFF);
        System.arraycopy(buffer, 0, UB, 6 + IABuf.length, DataLen);
        System.arraycopy(UB, 0, buffer, 0, NewPackLen);
        return UB;
    }

    private byte[] clearDgpHead(byte[] buffer) {
        final int IAlen;
        //int	bl	= Buffer.length;
        int p = 4;    // First byte of IP Address

        byte AType = buffer[3];    // IP Address Type
        switch (AType) {
            case 0x01 -> IAlen = 4;
            case 0x03 -> IAlen = buffer[p] + 1;
            // One for Size Byte
            default -> {
                System.out.println("Error in ClearDGPhead() - Invalid Destination IP Addres type " + AType);
                return null;
            }
        }

        byte[] IABuf = new byte[IAlen];
        System.arraycopy(buffer, p, IABuf, 0, IAlen);
        p += IAlen;

        UDP_IA = calcInetAddress(AType, IABuf);
        UDP_port = Utils.calcPort(buffer[p++], buffer[p++]);

        if (UDP_IA == null) {
            System.out.println("Error in ClearDGPHead() - Invalid UDP dest IP address: NULL");
            return null;
        }

        int DataLen = DGPack.getLength();
        DataLen -= p; // <p> is length of UDP Header

        byte[] UB = new byte[DataLen];
        System.arraycopy(buffer, p, UB, 0, DataLen);
        System.arraycopy(UB, 0, buffer, 0, DataLen);

        return UB;
    }

    protected void udpSend(DatagramPacket DGP) {
        if (DGP != null) {
            String LogString = DGP.getAddress() + ":" +
                    DGP.getPort() + "> : " +
                    DGP.getLength() + " bytes";
            try {
                DGSocket.send(DGP);
            } catch (IOException e) {
                System.out.println("Error in ProcessUDPClient() - Failed to Send DGP to " + LogString);
            }
        }
    }

    public void processUdp() {
        // Trying to Receive DataGram
        try {
            DGSocket.receive(DGPack);
        } catch (InterruptedIOException e) {
            return;    // Time Out
        } catch (IOException e) {
            System.out.println("Error in ProcessUDP() - " + e.toString());
            return;
        }

        if (m_ClientIP.equals(DGPack.getAddress())) {
            processUdpClient();
        } else {
            processUdpRemote();
        }

        try {
            initUdpInOut();    // Clean DGPack & Buffer
        } catch (IOException e) {
            System.out.println("IOError in Init_UDP_IO() - " + e.toString());
            m_Parent.close();
        }
    }

    private void processUdpClient() {
        m_nClientPort = DGPack.getPort();

        // Also calculates UDP_IA & UDP_port ...
        byte[] Buf = clearDgpHead(DGPack.getData());
        if (Buf == null) return;

        if (Buf.length <= 0) return;

        if (UDP_IA == null) {
            System.out.println("Error in ProcessUDPClient() - Invalid Destination IP - NULL");
            return;
        }
        if (UDP_port == 0) {
            System.out.println("Error in ProcessUDPClient() - Invalid Destination Port - 0");
            return;
        }

        if (m_ServerIP != UDP_IA || m_nServerPort != UDP_port) {
            m_ServerIP = UDP_IA;
            m_nServerPort = UDP_port;
        }

        System.out.println("Datagram : " + Buf.length + " bytes : " + getSocketInfo(DGPack) +
                " >> <" + Utils.iP2Str(m_ServerIP) + ":" + m_nServerPort + ">");

        DatagramPacket DGPSend = new DatagramPacket(Buf, Buf.length,
                UDP_IA, UDP_port);

        udpSend(DGPSend);
    }

    public void processUdpRemote() {
        System.out.printf("Datagram : %d bytes : <%s:%d> << %s%n",
                DGPack.getLength(), Utils.iP2Str(m_ClientIP), m_nClientPort, getSocketInfo(DGPack));

        // This Method must be CALL only from <ProcessUDP()>
        // ProcessUDP() Reads a Datagram packet <DGPack>

        InetAddress DGP_IP = DGPack.getAddress();
        int DGP_Port = DGPack.getPort();

        final byte[] Buf = addDgpHead(m_Parent.m_Buffer);

        // SendTo Client
        DatagramPacket DGPSend = new DatagramPacket(Buf, Buf.length,
                m_ClientIP, m_nClientPort);
        udpSend(DGPSend);

        if (DGP_IP != UDP_IA || DGP_Port != UDP_port) {
            m_ServerIP = DGP_IP;
            m_nServerPort = DGP_Port;
        }
    }

    public InetAddress resolveExternalLocalIP() {
        InetAddress IP = null;

        if (m_ExtLocalIP != null) {
            Socket sct;
            try {
                sct = new Socket(m_ExtLocalIP, m_Parent.getPort());
                IP = sct.getLocalAddress();
                sct.close();
                return m_ExtLocalIP;
            } catch (IOException e) {
                System.out.println("WARNING !!! THE LOCAL IP ADDRESS WAS CHANGED !");
            }
        }

        final String[] hosts = {"www.wikipedia.org", "www.google.com", "www.microsoft.com", "www.amazon.com", "www.zombo.com", "www.ebay.com"};

        final List<Exception> bindExceptions = new ArrayList<>();
        for (String host : hosts) {
            try (Socket sct = new Socket(InetAddress.getByName(host), 80)) {
                IP = sct.getLocalAddress();
                break;
            } catch (Exception e) {
                bindExceptions.add(e);
            }
        }

        if (IP == null) {
            System.err.println("Error in BIND() - BIND reip Failed on all common hosts to determine external IP's");
            for (Exception bindException : bindExceptions) {
                System.err.println(bindException.getMessage());
            }
        }

        m_ExtLocalIP = IP;
        return requireNonNull(IP);
    }

    public void bind() throws IOException {
        int MyPort = 0;

        System.out.println("Binding...");
        // Resolve External IP
        InetAddress MyIP = resolveExternalLocalIP();

        System.out.println("Local IP : " + MyIP.toString());

        ServerSocket ssock = new ServerSocket(0);
        try {
            ssock.setSoTimeout(SocksConstants.DEFAULT_PROXY_TIMEOUT);
            MyPort = ssock.getLocalPort();
        } catch (IOException e) {  // MyIP == null
            System.err.println("Error in BIND() - Can't BIND at any Port");
            bindReply((byte) 92, MyIP, MyPort);
            ssock.close();
            return;
        }

        System.out.println("BIND at : <" + MyIP.toString() + ":" + MyPort + ">");
        bindReply((byte) 90, MyIP, MyPort);

        Socket socket = null;

        while (socket == null) {
            if (m_Parent.checkClientData() >= 0) {
                System.err.println("BIND - Client connection closed");
                ssock.close();
                return;
            }

            try {
                socket = ssock.accept();
                socket.setSoTimeout(SocksConstants.DEFAULT_PROXY_TIMEOUT);
            } catch (InterruptedIOException e) {
                // ignore
            }
            Thread.yield();
        }

        m_ServerIP = socket.getInetAddress();
        m_nServerPort = socket.getPort();

        bindReply((byte) 90, socket.getInetAddress(), socket.getPort());

        m_Parent.m_ServerSocket = socket;
        m_Parent.prepareServer();

        System.out.println("BIND Connection from " + getSocketInfo(m_Parent.m_ServerSocket));
        ssock.close();
    }
}
