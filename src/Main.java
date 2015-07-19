public class Main {

    public static void main(String[] args) {
        NioTcpServer server = new NioTcpServer( 8989);
        server.start();
    }
}
