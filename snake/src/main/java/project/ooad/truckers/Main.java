package project.ooad.truckers;

public class Main {
    public static int port;

    public static void main(String[] args) {
        if (args.length == 0)
            port = 44444;
        else
            port = Integer.parseInt(args[0]);
        MainFX.main(args);
    }
}
