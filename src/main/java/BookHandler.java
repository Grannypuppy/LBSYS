import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entities.Book;
import entities.Borrow;
import jsonHandlers.*;
import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import queries.SortOrder;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookHandler implements HttpHandler {
    private final LibraryManagementSystem library;
    private final Gson gson;

    public BookHandler(LibraryManagementSystem library) {
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
            BookQueryConditions conditions = buildQueryConditions(params);
            ApiResult result = library.queryBook(conditions);
            
            if (result.ok) {
                BookQueryResults queryResults = (BookQueryResults) result.payload;
                JsonObject response = new JsonObject();
                response.add("records", gson.toJsonTree(queryResults.getResults()));
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
                case "store":
                    handleStore(exchange, requestBody);
                    break;
                case "storemulti":
                    handleStoreMulti(exchange, requestBody);
                    break;
                case "incstock":
                    handleIncStock(exchange, requestBody);
                    break;
                case "remove":
                    handleRemove(exchange, requestBody);
                    break;
                case "modify":
                    handleModify(exchange, requestBody);
                    break;
                case "borrow":
                    handleBorrow(exchange, requestBody);
                    break;
                case "return":
                    handleReturn(exchange, requestBody);
                    break;
                default:
                    sendErrorResponse(exchange, "Unknown action: " + action, 400);
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, "Invalid JSON format: " + e.getMessage(), 400);
        }
    }

    private BookQueryConditions buildQueryConditions(Map<String, String> params) {
        BookQueryConditions conditions = new BookQueryConditions();
        
        if (params.containsKey("category")) {
            conditions.setCategory(params.get("category"));
        }
        if (params.containsKey("title")) {
            conditions.setTitle(params.get("title"));
        }
        if (params.containsKey("press")) {
            conditions.setPress(params.get("press"));
        }
        if (params.containsKey("min-publish-year")) {
            try {
                conditions.setMinPublishYear(Integer.parseInt(params.get("min-publish-year")));
            } catch (NumberFormatException e) {
                // 忽略无效的年份
            }
        }
        if (params.containsKey("max-publish-year")) {
            try {
                conditions.setMaxPublishYear(Integer.parseInt(params.get("max-publish-year")));
            } catch (NumberFormatException e) {
                // 忽略无效的年份
            }
        }
        if (params.containsKey("author")) {
            conditions.setAuthor(params.get("author"));
        }
        if (params.containsKey("minprice")) {
            try {
                conditions.setMinPrice(Double.parseDouble(params.get("minprice")));
            } catch (NumberFormatException e) {
                // 忽略无效的价格
            }
        }
        if (params.containsKey("maxprice")) {
            try {
                conditions.setMaxPrice(Double.parseDouble(params.get("maxprice")));
            } catch (NumberFormatException e) {
                // 忽略无效的价格
            }
        }
        if (params.containsKey("sortby")) {
            String sortBy = params.get("sortby");
            switch (sortBy.toLowerCase()) {
                case "book_id":
                    conditions.setSortBy(Book.SortColumn.BOOK_ID);
                    break;
                case "category":
                    conditions.setSortBy(Book.SortColumn.CATEGORY);
                    break;
                case "title":
                    conditions.setSortBy(Book.SortColumn.TITLE);
                    break;
                case "press":
                    conditions.setSortBy(Book.SortColumn.PRESS);
                    break;
                case "publish_year":
                    conditions.setSortBy(Book.SortColumn.PUBLISH_YEAR);
                    break;
                case "author":
                    conditions.setSortBy(Book.SortColumn.AUTHOR);
                    break;
                case "price":
                    conditions.setSortBy(Book.SortColumn.PRICE);
                    break;
                case "stock":
                    conditions.setSortBy(Book.SortColumn.STOCK);
                    break;
            }
        }
        if (params.containsKey("sortorder")) {
            String sortOrder = params.get("sortorder");
            if ("desc".equalsIgnoreCase(sortOrder)) {
                conditions.setSortOrder(SortOrder.DESC);
            } else {
                conditions.setSortOrder(SortOrder.ASC);
            }
        }
        
        return conditions;
    }

    private void handleStore(HttpExchange exchange, String requestBody) throws IOException {
        BookJson bookJson = gson.fromJson(requestBody, BookJson.class);
        Book book = convertToBook(bookJson.getBook());
        
        ApiResult result = library.storeBook(book);
        if (result.ok) {
            sendSuccessResponse(exchange, "图书入库成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private void handleStoreMulti(HttpExchange exchange, String requestBody) throws IOException {
        MultiBookJson multiBookJson = gson.fromJson(requestBody, MultiBookJson.class);
        List<Book> books = new ArrayList<>();
        
        for (BookJson.BookData bookData : multiBookJson.getBooks()) {
            books.add(convertToBook(bookData));
        }
        
        ApiResult result = library.storeBook(books);
        if (result.ok) {
            sendSuccessResponse(exchange, "批量图书入库成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private void handleIncStock(HttpExchange exchange, String requestBody) throws IOException {
        BookIDAmtJson bookIDAmtJson = gson.fromJson(requestBody, BookIDAmtJson.class);
        
        ApiResult result = library.incBookStock(bookIDAmtJson.getBookID(), bookIDAmtJson.getAmount());
        if (result.ok) {
            sendSuccessResponse(exchange, "库存修改成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private void handleRemove(HttpExchange exchange, String requestBody) throws IOException {
        BookIDJson bookIDJson = gson.fromJson(requestBody, BookIDJson.class);
        
        ApiResult result = library.removeBook(bookIDJson.getBookID());
        if (result.ok) {
            sendSuccessResponse(exchange, "图书删除成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private void handleModify(HttpExchange exchange, String requestBody) throws IOException {
        BookJson bookJson = gson.fromJson(requestBody, BookJson.class);
        Book book = convertToBook(bookJson.getBook());
        
        ApiResult result = library.modifyBookInfo(book);
        if (result.ok) {
            sendSuccessResponse(exchange, "图书信息修改成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private void handleBorrow(HttpExchange exchange, String requestBody) throws IOException {
        BorrowJson borrowJson = gson.fromJson(requestBody, BorrowJson.class);
        Borrow borrow = convertToBorrow(borrowJson.getBorrow());
        
        ApiResult result = library.borrowBook(borrow);
        if (result.ok) {
            sendSuccessResponse(exchange, "借书成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private void handleReturn(HttpExchange exchange, String requestBody) throws IOException {
        BorrowJson borrowJson = gson.fromJson(requestBody, BorrowJson.class);
        Borrow borrow = convertToBorrow(borrowJson.getBorrow());
        
        ApiResult result = library.returnBook(borrow);
        if (result.ok) {
            sendSuccessResponse(exchange, "还书成功");
        } else {
            sendErrorResponse(exchange, result.message, 400);
        }
    }

    private Book convertToBook(BookJson.BookData bookData) {
        Book book = new Book();
        
        if (bookData.getBookId() != null && !bookData.getBookId().equals("0")) {
            book.setBookId(Integer.parseInt(bookData.getBookId()));
        }
        if (bookData.getCategory() != null) {
            book.setCategory(bookData.getCategory());
        }
        if (bookData.getTitle() != null) {
            book.setTitle(bookData.getTitle());
        }
        if (bookData.getPress() != null) {
            book.setPress(bookData.getPress());
        }
        if (bookData.getPublishYear() != null) {
            book.setPublishYear(Integer.parseInt(bookData.getPublishYear()));
        }
        if (bookData.getAuthor() != null) {
            book.setAuthor(bookData.getAuthor());
        }
        if (bookData.getPrice() != null) {
            book.setPrice(Double.parseDouble(bookData.getPrice()));
        }
        if (bookData.getStock() != null) {
            book.setStock(Integer.parseInt(bookData.getStock()));
        }
        
        return book;
    }

    private Borrow convertToBorrow(BorrowJson.BorrowData borrowData) {
        Borrow borrow = new Borrow();
        
        borrow.setCardId(Integer.parseInt(borrowData.getCardId()));
        borrow.setBookId(Integer.parseInt(borrowData.getBookId()));
        
        if (borrowData.getBorrowTime() != null && !borrowData.getBorrowTime().equals("0")) {
            borrow.setBorrowTime(parseTimeString(borrowData.getBorrowTime()));
        }
        if (borrowData.getReturnTime() != null && !borrowData.getReturnTime().equals("0")) {
            borrow.setReturnTime(parseTimeString(borrowData.getReturnTime()));
        }
        
        return borrow;
    }

    private long parseTimeString(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            return sdf.parse(timeStr).getTime();
        } catch (ParseException e) {
            return System.currentTimeMillis();
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
