import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by weicheng on 2015/7/19.
 */
public class NioTcpServer extends Thread {
    private static final Logger log = Logger.getLogger(NioTcpServer.class.getName());
    private InetSocketAddress inetSocketAddress;
    private Handler handler;
    private Map<String, SelectionKey> keyMap;
    private Map<String, String> userRelation;
    public NioTcpServer(int port){
        this.inetSocketAddress = new InetSocketAddress(port);
        handler = new ServerHandler();
        keyMap = new HashMap<>();
        userRelation = new HashMap<>();
    }

    @Override
    public void run(){
        try {
            Selector selector = Selector.open();//��ѡ����
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();//��ͨ��
            serverSocketChannel.configureBlocking(false);//������
            serverSocketChannel.socket().bind(inetSocketAddress);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);// ��ͨ��ע��ѡ�����Ͷ�Ӧ�¼���ʶ
            log.info("Server: socket server started.");
            while(true){//��ѯ
                int nKeys = selector.select(3000);
                if(nKeys>0){
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectedKeys.iterator();
                    while(it.hasNext()) {
                        SelectionKey key = it.next();
                        if (key.isAcceptable()) {
                            log.info("Server: SelectionKey is acceptable.");
                            handler.handleAccept(key);
                        } else if (key.isReadable()) {
                            log.info("Server: SelectionKey is readable.");
                            handler.handleRead(key);
                        } else if (key.isWritable()) {
                            log.info("Server: SelectionKey is writable.");
                            handler.handleWrite(key);
                        }
                        it.remove();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * �򵥴������ӿ�
     *
     * @author shirdrn
     */
    interface Handler {
        /**
         * ����{@link SelectionKey#OP_ACCEPT}�¼�
         * @param key
         * @throws Exception
         */
        void handleAccept(SelectionKey key) throws Exception;
        /**
         * ����{@link SelectionKey#OP_READ}�¼�
         * @param key
         * @throws Exception
         */
        void handleRead(SelectionKey key) throws Exception;
        /**
         * ����{@link SelectionKey#OP_WRITE}�¼�
         * @param key
         * @throws Exception
         */
        void handleWrite(SelectionKey key) throws Exception;
    }

    class ServerHandler implements Handler{

        @Override
        public void handleAccept(SelectionKey key) throws Exception {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            log.info("Server: accept client socket " + socketChannel);
            socketChannel.configureBlocking(false);
            socketChannel.register(key.selector(), SelectionKey.OP_READ);
        }

        @Override
        public void handleRead(SelectionKey key) throws Exception {
            ByteBuffer byteBuffer = ByteBuffer.allocate(512);
            SocketChannel socketChannel = (SocketChannel)key.channel();
            try {
                while (true) {
                    int readBytes = socketChannel.read(byteBuffer);
                    if (readBytes > 0) {
                        String data = new String(byteBuffer.array(), 0, readBytes);
                        log.info("From Client: data = " + data);
                        String otherUsername = null;
                        String myUsername = null;
                        if(data.charAt(0) == '2'){
                            String[] array = data.split(" ");
                            myUsername = array[1];
                            otherUsername = array[2];
                            key.attach(new UserToUser(myUsername, otherUsername));
                            keyMap.put(myUsername,key);
                            break;
                        }
                        UserToUser user2user = (UserToUser)key.attachment();
                        if(user2user == null)
                            break;
                        if(!keyMap.containsKey(user2user.getOtherUser()))
                            break;
                        otherUsername = user2user.getOtherUser();
                        SelectionKey otherKey = keyMap.get(otherUsername);
                        SocketChannel otherChannel = (SocketChannel)otherKey.channel();
                        byteBuffer.flip();
                        otherChannel.write(byteBuffer);
                        break;
                    }
                    else if(readBytes == -1){
                        System.out.print("客户端断开："+socketChannel.getRemoteAddress());
                        UserToUser user2user = (UserToUser)key.attachment();
                        String username = user2user.getUser();
                        keyMap.remove(username);
                        socketChannel.close();
                    }
                }
            }catch (Exception e) {
                socketChannel.close();
            }
        }

        @Override
        public void handleWrite(SelectionKey key) throws Exception {
            ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
            byteBuffer.flip();
            SocketChannel socketChannel = (SocketChannel)key.channel();
            socketChannel.write(byteBuffer);
            if(byteBuffer.hasRemaining()) {
                key.interestOps(SelectionKey.OP_READ);
            }
            byteBuffer.compact();
        }
    }
}
