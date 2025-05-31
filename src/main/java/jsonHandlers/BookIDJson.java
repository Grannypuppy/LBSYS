package jsonHandlers;

public class BookIDJson {
    private String action;
    private int bookID;

    public BookIDJson() {}

    public BookIDJson(String action, int bookID) {
        this.action = action;
        this.bookID = bookID;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getBookID() {
        return bookID;
    }

    public void setBookID(int bookID) {
        this.bookID = bookID;
    }
}
