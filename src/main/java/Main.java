import utils.ConnectConfig;
import utils.DatabaseConnector;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {
        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            DatabaseConnector connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            /* do somethings */
            LibraryManagementSystem library = new LibraryManagementSystemImpl(connector);
            log.info("Successfully connected database.");

            // 创建HTTP服务器，监听指定端口
            // 修改为8081端口以匹配API手册
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

            // 添加handler，这里就绑定到/card路由
            server.createContext("/card", new CardHandler(library));
            server.createContext("/borrow", new BorrowHandler(library));
            server.createContext("/book", new BookHandler(library));

            //启动服务器
            server.start();

            // 标识一下，这样才知道我的后端启动了（确信
            System.out.println("Server is listening on port 8081");

            // release database connection handler
            // add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (connector.release()) {
                    log.info("Success to release connection.");
                } else {
                    log.warning("Failed to release connection.");
                }
            }));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
