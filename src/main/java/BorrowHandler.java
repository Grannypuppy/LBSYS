import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import queries.ApiResult;
import queries.BorrowHistories;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BorrowHandler implements HttpHandler {
    private final LibraryManagementSystem library;
    private final Gson gson;

    public BorrowHandler(LibraryManagementSystem library) {
        this.library = library;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 设置CORS头
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        String method = exchange.getRequestMethod();
        
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if ("GET".equals(method)) {
                handleGet(exchange);
            } else {
                sendErrorResponse(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, "Internal server error: " + e.getMessage(), 500);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();
        Map<String, String> params = parseQuery(query);

        if ("records".equals(params.get("type")) && params.containsKey("cardId")) {
            try {
                int cardId = Integer.parseInt(params.get("cardId"));
                ApiResult result = library.showBorrowHistory(cardId);
                
                if (result.ok) {
                    BorrowHistories histories = (BorrowHistories) result.payload;
                    List<BorrowRecord> records = histories.getItems().stream()
                            .map(this::convertToRecord)
                            .collect(Collectors.toList());
                    
                    JsonObject response = new JsonObject();
                    response.add("records", gson.toJsonTree(records));
                    sendJsonResponse(exchange, response.toString(), 200);
                } else {
                    sendErrorResponse(exchange, result.message, 400);
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, "Invalid cardId format", 400);
            }
        } else {
            sendErrorResponse(exchange, "Invalid request parameters", 400);
        }
    }

    private BorrowRecord convertToRecord(BorrowHistories.Item item) {
        BorrowRecord record = new BorrowRecord();
        record.cardID = item.getCardId();
        record.bookID = item.getBookId();
        record.borrowTime = item.getBorrowTime();
        record.returnTime = item.getReturnTime();
        return record;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    try {
                        String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.toString());
                        String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.toString());
                        params.put(key, value);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return params;
    }

    private void sendJsonResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("error", message);
        sendJsonResponse(exchange, response.toString(), statusCode);
    }

    // 内部类用于JSON响应
    private static class BorrowRecord {
        public int cardID;
        public int bookID;
        public long borrowTime;
        public long returnTime;
    }
}
