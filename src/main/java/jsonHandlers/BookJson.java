package jsonHandlers;

public class BookJson {
    private String action;
    private BookData book;

    public BookJson() {}

    public BookJson(String action, BookData book) {
        this.action = action;
        this.book = book;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BookData getBook() {
        return book;
    }

    public void setBook(BookData book) {
        this.book = book;
    }

    public static class BookData {
        private String bookId;
        private String category;
        private String title;
        private String press;
        private String publishYear;
        private String author;
        private String price;
        private String stock;

        public BookData() {}

        public String getBookId() {
            return bookId;
        }

        public void setBookId(String bookId) {
            this.bookId = bookId;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getPress() {
            return press;
        }

        public void setPress(String press) {
            this.press = press;
        }

        public String getPublishYear() {
            return publishYear;
        }

        public void setPublishYear(String publishYear) {
            this.publishYear = publishYear;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getStock() {
            return stock;
        }

        public void setStock(String stock) {
            this.stock = stock;
        }
    }
}
