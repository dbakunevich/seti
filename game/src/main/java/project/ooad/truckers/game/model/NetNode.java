package project.ooad.truckers.game.model;

import project.ooad.truckers.game.model.SnakesProto.GameMessage;
import project.ooad.truckers.game.model.SnakesProto.GameMessage.AckMsg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;


public class NetNode {
    GameModel gameModel;

    NetNode() throws IOException {
        gameModel = GameModel.getInstance();
    }

    void sendConfirmation(InetAddress inetAddress, int port, DatagramPacket receivedPacket) throws IOException {
        GameMessage gameMessage = GameMessage.parseFrom(Arrays.copyOf(receivedPacket.getData(), receivedPacket.getLength()));

        byte[] buf = createAckMsg(gameMessage.getMsgSeq()).toByteArray();

        gameModel.getDatagramSocket().send(new DatagramPacket(buf, buf.length, inetAddress, port));
    }

    private GameMessage createAckMsg(long msgSeq) {
        GameMessage.Builder gameMessage = GameMessage.newBuilder();

        gameMessage.setAck(AckMsg.newBuilder().build());

        gameMessage.setMsgSeq(msgSeq);

        return gameMessage.build();
    }

}
