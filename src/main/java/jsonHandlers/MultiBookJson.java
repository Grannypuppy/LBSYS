package jsonHandlers;

import java.util.List;

public class MultiBookJson {
    private String action;
    private List<BookJson.BookData> books;

    public MultiBookJson() {}

    public MultiBookJson(String action, List<BookJson.BookData> books) {
        this.action = action;
        this.books = books;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<BookJson.BookData> getBooks() {
        return books;
    }

    public void setBooks(List<BookJson.BookData> books) {
        this.books = books;
    }
}
