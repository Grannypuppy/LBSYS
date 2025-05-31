package jsonHandlers;

public class BorrowJson {
    private String action;
    private BorrowData borrow;

    public BorrowJson() {}

    public BorrowJson(String action, BorrowData borrow) {
        this.action = action;
        this.borrow = borrow;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BorrowData getBorrow() {
        return borrow;
    }

    public void setBorrow(BorrowData borrow) {
        this.borrow = borrow;
    }

    public static class BorrowData {
        private String cardId;
        private String bookId;
        private String borrowTime;
        private String returnTime;

        public BorrowData() {}

        public String getCardId() {
            return cardId;
        }

        public void setCardId(String cardId) {
            this.cardId = cardId;
        }

        public String getBookId() {
            return bookId;
        }

        public void setBookId(String bookId) {
            this.bookId = bookId;
        }

        public String getBorrowTime() {
            return borrowTime;
        }

        public void setBorrowTime(String borrowTime) {
            this.borrowTime = borrowTime;
        }

        public String getReturnTime() {
            return returnTime;
        }

        public void setReturnTime(String returnTime) {
            this.returnTime = returnTime;
        }
    }
}
