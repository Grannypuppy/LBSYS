import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entities.Card;
import jsonHandlers.CardJson;
import jsonHandlers.CardIDJson;
import queries.ApiResult;
import queries.CardList;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CardHandler implements HttpHandler {
    private final LibraryManagementSystem library;
    private final Gson gson;

    public CardHandler(LibraryManagementSystem library) {
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
            } else if ("POST".equals(method)) {
                handlePost(exchange);
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

        if ("records".equals(params.get("type"))) {
            ApiResult result = library.showCards();
            if (result.ok) {
                CardList cardList = (CardList) result.payload;
                JsonObject response = new JsonObject();
                response.add("records", gson.toJsonTree(cardList.getCards()));
                sendJsonResponse(exchange, response.toString(), 200);
            } else {
                sendErrorResponse(exchange, result.message, 400);
            }
        } else {
            sendErrorResponse(exchange, "Invalid request", 400);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        
        try {
            JsonObject jsonObject = gson.fromJson(requestBody, JsonObject.class);
            String action = jsonObject.get("action").getAsString();

            switch (action) {
                case "register":
                    handleRegister(exchange, requestBody);
                    break;
                case "modify":
                    handleModify(exchange, requestBody);
                    break;
                case "remove":
                    handleRemove(exchange, requestBody);
                    break;
                default:
                    sendErrorResponse(exchange, "Unknown action: " + action, 400);
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, "Invalid JSON format: " + e.getMessage(), 400);
        }
    }

    private void handleRegister(HttpExchange exchange, String requestBody) throws IOException {
        CardJson cardJson = gson.fromJson(requestBody, CardJson.class);
        
        Card card = new Card();
        card.setName(cardJson.getName());
        card.setDepartment(cardJson.getDepartment());
        card.setType(Card.CardType.values(cardJson.getType()));

        ApiResult result = library.registerCard(card);
        if (result.ok) {
            sendSuccessResponse(exchange, "注册借书证成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private void handleModify(HttpExchange exchange, String requestBody) throws IOException {
        CardJson cardJson = gson.fromJson(requestBody, CardJson.class);
        
        Card card = new Card();
        card.setCardId(cardJson.getCardId());
        card.setName(cardJson.getName());
        card.setDepartment(cardJson.getDepartment());
        card.setType(Card.CardType.values(cardJson.getType()));

        ApiResult result = library.modifyCardInfo(card);
        if (result.ok) {
            sendSuccessResponse(exchange, "修改借书证信息成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private void handleRemove(HttpExchange exchange, String requestBody) throws IOException {
        CardIDJson cardIDJson = gson.fromJson(requestBody, CardIDJson.class);
        
        ApiResult result = library.removeCard(cardIDJson.getCardID());
        if (result.ok) {
            sendSuccessResponse(exchange, "删除借书证成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
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

    private String readRequestBody(HttpExchange exchange) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private void sendJsonResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendSuccessResponse(HttpExchange exchange, String message) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("success", message);
        sendJsonResponse(exchange, response.toString(), 200);
    }

    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        JsonObject response = new JsonObject();
        response.addProperty("error", message);
        sendJsonResponse(exchange, response.toString(), statusCode);
    }
}
