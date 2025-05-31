package jsonHandlers;

public class BookIDAmtJson {
    private String action;
    private int bookID;
    private int amount;

    public BookIDAmtJson() {}

    public BookIDAmtJson(String action, int bookID, int amount) {
        this.action = action;
        this.bookID = bookID;
        this.amount = amount;
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

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
