import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try {
            PreparedStatement stmt  = conn.prepareStatement("SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?");
            stmt.setString(1, book.getCategory());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getPress());
            stmt.setInt(4, book.getPublishYear());
            stmt.setString(5, book.getAuthor());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // rollback(conn);
                return new ApiResult(false, "Book already exists");
            } //check if already have book existed
            //insert book into database
            stmt = conn.prepareStatement("INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, book.getCategory());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getPress());
            stmt.setInt(4, book.getPublishYear());
            stmt.setString(5, book.getAuthor());
            stmt.setDouble(6, book.getPrice());
            stmt.setInt(7, book.getStock());
            stmt.executeUpdate();
            /* 从数据库中获取自动生成的主键 */
            ResultSet index_rs = stmt.getGeneratedKeys();
            //如果有数据库自动生成的主键
            if (index_rs.next()) {
                book.setBookId(index_rs.getInt(1)); //就获取一下存入book
            }
            else{
                rollback(conn);
                return new ApiResult(false, "Failed to update book id");
            }
            commit(conn);
        } catch (SQLException e) {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "book stored successfully");
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT stock FROM book WHERE book_id = ?");
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "Book not found");
            }
            int stock = rs.getInt("stock");
            if (stock + deltaStock < 0) {
                return new ApiResult(false, "Not enough stock");
            }
            //update the stock
            stmt = conn.prepareStatement("UPDATE book SET stock = ? WHERE book_id = ?");
            stmt.setInt(1, stock + deltaStock);
            stmt.setInt(2, bookId);
            stmt.executeUpdate();
            commit(conn);
        } catch (SQLException e) {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "stock updated successfully");
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            // 检查列表内是否有重复书籍
            for (int i = 0; i < books.size(); i++)
            {
                Book book1 = books.get(i);
                for (int j = i + 1; j < books.size(); j++)
                {
                    Book book2 = books.get(j);
                    if (book1.getCategory().equals(book2.getCategory()) &&
                            book1.getTitle().equals(book2.getTitle()) &&
                            book1.getPress().equals(book2.getPress()) &&
                            book1.getPublishYear() == book2.getPublishYear() &&
                            book1.getAuthor().equals(book2.getAuthor()))
                    {

                        return new ApiResult(false, "Duplicate books in the list");
                    }
                }
            }
            PreparedStatement stmt  = conn.prepareStatement("SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?");
            for(Book book : books) {
                stmt.setString(1, book.getCategory());
                stmt.setString(2, book.getTitle());
                stmt.setString(3, book.getPress());
                stmt.setInt(4, book.getPublishYear());
                stmt.setString(5, book.getAuthor());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    //throw new RuntimeException("Book already exists: " + rs.getInt("book_id") + rs.getString("category")+ rs.getString("title")+ rs.getString("press")+ rs.getInt("publish_year")+ rs.getString("author")+ rs.getDouble("price")+ rs.getInt("stock"));
                    return new ApiResult(false, "Book already exists");
                } //check if already have book existed
            }
            stmt = conn.prepareStatement("INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            for (Book book : books) {
                stmt.setString(1, book.getCategory());
                stmt.setString(2, book.getTitle());
                stmt.setString(3, book.getPress());
                stmt.setInt(4, book.getPublishYear());
                stmt.setString(5, book.getAuthor());
                stmt.setDouble(6, book.getPrice());
                stmt.setInt(7, book.getStock());
                stmt.addBatch();
            }
            stmt.executeBatch();
            /* 从数据库中获取自动生成的主键 */
            ResultSet index_rs = stmt.getGeneratedKeys();
            //如果有数据库自动生成的主键
            for (Book book : books) {
                if (index_rs.next()) {
                    book.setBookId(index_rs.getInt(1)); //就获取一下
                }
                else {
                    rollback(conn);
                    return new ApiResult(false, "Failed to update book id");
                }
            }
            commit(conn);
            return new ApiResult(true, "Bulk Store Success");
        }
        catch (SQLException e) {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult removeBook(int bookId) {
        //先查询库中是否有这本书，无则直接返回false
        //再查询是否有借阅记录，有则返回false
        //没有则删除
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);
            // 查询图书是否存在
            PreparedStatement checkBookstmt = conn.prepareStatement("SELECT * FROM book WHERE book_id = ?");
            checkBookstmt.setInt(1, bookId);
            ResultSet checkBookRes = checkBookstmt.executeQuery();
            if (!checkBookRes.next()) {
                return new ApiResult(false, "not exist such book_id");
            }

            // 检查是否有未归还的借阅
            PreparedStatement checkBorstmt = conn.prepareStatement("SELECT * FROM borrow WHERE book_id = ? AND (return_time<=borrow_time)");
            checkBorstmt.setInt(1, bookId);
            ResultSet checkBorRes = checkBorstmt.executeQuery();
            if (checkBorRes.next()) {
                return new ApiResult(false, "have not-returned book");
            }

            // 删除图书
            PreparedStatement delstmt = conn.prepareStatement("DELETE FROM book WHERE book_id = ?");
            delstmt.setInt(1, bookId);
            int effected_row = delstmt.executeUpdate();
            if (effected_row == 1) {
                commit(conn);
                return new ApiResult(true, "remove success");
            }
            else{
                rollback(conn);
                return new ApiResult(false, "fail: effect_row != 1");
            }
        }
        catch (Exception e) {
            rollback(conn); // 使用辅助方法
            e.printStackTrace();
            return new ApiResult(false, "raise exception, fail to remove book");
        }
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try{
            //先查询库中是否有这本书，无则直接返回false
            PreparedStatement existstmt = conn.prepareStatement("SELECT * FROM book WHERE book_id = ?");
            existstmt.setInt(1, book.getBookId());
            ResultSet existrs = existstmt.executeQuery();
            if(!existrs.next()){
                return new ApiResult(false, "not exist such book_id");
            }
            //再进行修改
            String upSql = "UPDATE book SET ";
            // 这里需要注意的是，book.getBookId()不能被修改
            int flag = 0;
            if(book.getCategory() != null){
                upSql += "category = '" + book.getCategory() + "'";
                flag ++;
            }
            if(book.getTitle() != null){
                if(flag != 0){
                    upSql += ", ";
                }
                upSql += "title = '" + book.getTitle() + "'";
                flag ++;
            }
            if(book.getPress() != null){
                if(flag != 0){
                    upSql += ", ";
                }
                upSql += "press = '" + book.getPress() + "'";
                flag ++;
            }
            if(book.getPublishYear() >= 0){
                if(flag != 0){
                    upSql += ", ";
                }
                upSql += "publish_year = " + book.getPublishYear();
                flag ++;
            }
            if(book.getAuthor() != null){
                if(flag != 0){
                    upSql += ", ";
                }
                upSql += "author = '" + book.getAuthor() + "'";
                flag ++;
            }
            if(book.getPrice() >= 0){
                if(flag != 0){
                    upSql += ", ";
                }
                upSql += "price = " + book.getPrice();
                flag ++;
            }
            //注意这里的stock不能被修改
            if(flag == 0)
            {
                return new ApiResult(false, "empty require: no update needed");
            }
            upSql += " WHERE book_id = ?";
            PreparedStatement upstmt = conn.prepareStatement(upSql);
            upstmt.setInt(1, book.getBookId());
            int effected_row = upstmt.executeUpdate();
            if(effected_row == 1)
            {
                commit(conn);
                return new ApiResult(true, "update success");
            }
            else
            {
                rollback(conn);
                return new ApiResult(false, "fail: effect_row != 1");
            }
        }
        catch(Exception e)
        {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        try{
            StringBuilder querySql = new StringBuilder();
            querySql.append("SELECT * FROM book WHERE 1=1");
            if(conditions.getCategory() != null)
            {
                querySql.append(" AND category = '").append(conditions.getCategory()).append("'");
            }
            if(conditions.getTitle() != null)
            {
                querySql.append(" AND title LIKE '%").append(conditions.getTitle()).append("%'");
            }
            if(conditions.getPress() != null)
            {
                querySql.append(" AND press LIKE '%").append(conditions.getPress()).append("%'");
            }
            if(conditions.getMinPublishYear() != null)
            {
                querySql.append(" AND publish_year >= ").append(conditions.getMinPublishYear());
            }
            if(conditions.getMaxPublishYear() != null)
            {
                querySql.append(" AND publish_year <= ").append(conditions.getMaxPublishYear());
            }
            if(conditions.getAuthor() != null)
            {
                querySql.append(" AND author LIKE '%").append(conditions.getAuthor()).append("%'");
            }
            if(conditions.getMinPrice() != null)
            {
                querySql.append(" AND price >= ").append(conditions.getMinPrice());
            }
            if(conditions.getMaxPrice() != null)
            {
                querySql.append(" AND price <= ").append(conditions.getMaxPrice());
            }
            boolean flag = false;
            if(conditions.getSortBy() != null)
            {
                flag = true;
                querySql.append(" ORDER BY ").append(conditions.getSortBy());
                if(conditions.getSortOrder() != null)
                {
                    querySql.append(" ").append(conditions.getSortOrder());
                }
            }
            if(!flag)
            {
                querySql.append(" ORDER BY book_id ASC");
            }
            else
            {
                querySql.append(", book_id ASC");
            }
            PreparedStatement stmt = conn.prepareStatement(querySql.toString());
            ResultSet rs = stmt.executeQuery();
            List<Book> books = new ArrayList<>();
            while(rs.next())
            {
                Book abook = new Book();
                abook.setBookId(rs.getInt("book_id"));
                abook.setCategory(rs.getString("category"));
                abook.setTitle(rs.getString("title"));
                abook.setPress(rs.getString("press"));
                abook.setPublishYear(rs.getInt("publish_year"));
                abook.setAuthor(rs.getString("author"));
                abook.setPrice(rs.getDouble("price"));
                abook.setStock(rs.getInt("stock"));
                books.add(abook);
            }
            //获取完这样一个books的结果列表
            BookQueryResults bookQueryResults = new BookQueryResults(books);
            return new ApiResult(true, bookQueryResults);
        }
        catch(Exception e)
        {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage()+"query book fail");
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        //先查询库中是否有这本书以及库存是否够，无则直接返回false
        //再查询是否有借阅记录，有则返回false
        try{
            //检查库存
            PreparedStatement stockstmt = conn.prepareStatement("SELECT * FROM book WHERE book_id = ? AND stock > 0");
            stockstmt.setInt(1, borrow.getBookId());
            ResultSet stockrs = stockstmt.executeQuery();
            if(!stockrs.next())
                return new ApiResult(false, "not exist such book_id or stock not enough");
            //检查card_id合法性
            //检查card_id合法性
            PreparedStatement cardStmt = conn.prepareStatement("SELECT * FROM card WHERE card_id = ?");
            cardStmt.setInt(1, borrow.getCardId());
            ResultSet cardrs = cardStmt.executeQuery();
            if(!cardrs.next())
                return new ApiResult(false, "Card does not exist");
            //检查过往借阅记录
            PreparedStatement checkstmt = conn.prepareStatement("SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time < borrow_time");
            checkstmt.setInt(1, borrow.getCardId());
            checkstmt.setInt(2, borrow.getBookId());
            ResultSet checkrs = checkstmt.executeQuery();
            if(checkrs.next())
                return new ApiResult(false, "have already borrowed such book");
            //现在已经检查了book_id的正确性和库存，检查了card_id合法性，检查了过往借阅记录
            //可以直接插入借阅记录了:更新库存+插入记录
            PreparedStatement stkupstmt = conn.prepareStatement("UPDATE book SET stock = stock - 1 WHERE book_id = ?");
            stkupstmt.setInt(1, borrow.getBookId());
            int effected_row = stkupstmt.executeUpdate();
            if(effected_row!=1)
            {
                rollback(conn);
                return new ApiResult(false,"stock update fail: effected_row!=1");
            }
            PreparedStatement borrowstmt = conn.prepareStatement("INSERT INTO borrow(card_id, book_id, borrow_time) VALUES(?, ?, ?)");
            borrowstmt.setInt(1, borrow.getCardId());
            borrowstmt.setInt(2, borrow.getBookId());
            borrowstmt.setLong(3,borrow.getBorrowTime());
            effected_row = borrowstmt.executeUpdate();
            if(effected_row !=1)
            {
                rollback(conn);
                return new ApiResult(false,"borrow insert failed");
            }
            return new ApiResult(true,"borrow succeed");
        }
        catch(Exception e)
        {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try{
            PreparedStatement returncheckstmt = conn.prepareStatement("SELECT * from borrow WHERE card_id = ? AND book_id = ? AND (return_time <= borrow_time || return_time = 0) AND borrow_time < ?");
            returncheckstmt.setInt(1,borrow.getCardId());
            returncheckstmt.setInt(2,borrow.getBookId());
            returncheckstmt.setLong(3,borrow.getReturnTime());
            ResultSet returnrs = returncheckstmt.executeQuery();
            if(!returnrs.next())
                return new ApiResult(false,"no such borrow record");
            PreparedStatement stkupstmt = conn.prepareStatement("UPDATE book SET stock = stock+1 WHERE book_id = ?");
            stkupstmt.setInt(1, borrow.getBookId());
            int effected_row = stkupstmt.executeUpdate();
            if(effected_row!=1) {
                rollback(conn);
                return new ApiResult(false, "fail to update stock: effected_row !=1");
            }
            PreparedStatement returnsetstmt = conn.prepareStatement("UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND (return_time <= borrow_time || return_time = 0)");
            returnsetstmt.setLong(1, borrow.getReturnTime());
            returnsetstmt.setInt(2,borrow.getCardId());
            returnsetstmt.setInt(3,borrow.getBookId());
            effected_row = returnsetstmt.executeUpdate();
            if(effected_row!=1)
            {
                rollback(conn);
                return new ApiResult(false,"fail to update return_time: effected_row != 1");
            }
            return new ApiResult(true,"return succeed");
        }
        catch(Exception e)
        {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        try {
            // 首先检查卡是否存在
            PreparedStatement checkcardstmt = conn.prepareStatement("SELECT * FROM card WHERE card_id = ?");
            checkcardstmt.setInt(1, cardId);
            ResultSet cardRs = checkcardstmt.executeQuery();
            if (!cardRs.next()) {
                return new ApiResult(false, "Card not found");
            }
            // 查询该卡的所有借阅历史
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM borrow join book using (book_id) WHERE card_id = ? ORDER BY borrow_time DESC, book_id ASC");
            stmt.setInt(1, cardId);
            ResultSet rs = stmt.executeQuery();
            // 这个rs返回的单行的row的结构是：(card_id, book_id, borrow_time, return_time, book_id, category, title, press, publish_year, author, price, stock)
            List<BorrowHistories.Item> borrowHistories = new ArrayList<>();
            while (rs.next()) {
                BorrowHistories.Item borrow = new BorrowHistories.Item();
                borrow.setCardId(rs.getInt("card_id"));
                borrow.setBookId(rs.getInt("book_id"));
                borrow.setBorrowTime(rs.getLong("borrow_time"));
                borrow.setReturnTime(rs.getLong("return_time"));
                borrow.setCategory(rs.getString("category"));
                borrow.setTitle(rs.getString("title"));
                borrow.setPress(rs.getString("press"));
                borrow.setPublishYear(rs.getInt("publish_year"));
                borrow.setAuthor(rs.getString("author"));
                borrow.setPrice(rs.getDouble("price"));
                borrowHistories.add(borrow);
            }
            BorrowHistories histories = new BorrowHistories(borrowHistories);
            return new ApiResult(true, histories);
        }
        catch (Exception e)
        {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }

    }

    @Override
    public ApiResult registerCard(Card card)
    {
        Connection conn = connector.getConn();
        try {
            // 检查卡是否已存在
            PreparedStatement checkstmt = conn.prepareStatement("SELECT * FROM card WHERE name = ? AND department = ? AND type = ?");
            checkstmt.setString(1, card.getName());
            checkstmt.setString(2, card.getDepartment());
            checkstmt.setString(3, card.getType().getStr());
            ResultSet rs = checkstmt.executeQuery();
            if (rs.next()) {
                return new ApiResult(false, "Card already exists");
            }

            // 插入新卡
            PreparedStatement insertstmt = conn.prepareStatement(
                "INSERT INTO card (name, department, type) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            insertstmt.setString(1, card.getName());
            insertstmt.setString(2, card.getDepartment());
            insertstmt.setString(3, card.getType().getStr());
            insertstmt.executeUpdate();

            // 获取生成的卡号
            ResultSet generatedKeys = insertstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                card.setCardId(generatedKeys.getInt(1));
            } else {
                rollback(conn);
                return new ApiResult(false, "Failed to get generated card ID");
            }
            commit(conn);
            return new ApiResult(true, "Card registered successfully");
        } catch (SQLException e) {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try {
            // 首先检查卡是否存在
            PreparedStatement checkCardStmt = conn.prepareStatement("SELECT * FROM card WHERE card_id = ?");
            checkCardStmt.setInt(1, cardId);
            ResultSet cardRs = checkCardStmt.executeQuery();
            if (!cardRs.next()) {
                return new ApiResult(false, "Card not found");
            }

            // 检查是否有未归还的书籍
            PreparedStatement checkBorrowStmt = conn.prepareStatement(
                "SELECT * FROM borrow WHERE card_id = ? AND (return_time < borrow_time OR return_time = 0)"
            );
            checkBorrowStmt.setInt(1, cardId);
            ResultSet borrowRs = checkBorrowStmt.executeQuery();
            if (borrowRs.next()) {
                return new ApiResult(false, "Card has unreturned books");
            }

            // 删除卡
            PreparedStatement deleteCardStmt = conn.prepareStatement("DELETE FROM card WHERE card_id = ?");
            deleteCardStmt.setInt(1, cardId);
            int affectedRows = deleteCardStmt.executeUpdate();

            if (affectedRows == 1) {
                commit(conn);
                return new ApiResult(true, "Card removed successfully");
            } else {
                rollback(conn);
                return new ApiResult(false, "Failed to remove card");
            }
        } catch (SQLException e) {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM card ORDER BY card_id ASC");
            ResultSet rs = stmt.executeQuery();

            List<Card> cards = new ArrayList<>();
            while (rs.next()) {
                Card card = new Card();
                card.setCardId(rs.getInt("card_id"));
                card.setName(rs.getString("name"));
                card.setDepartment(rs.getString("department"));
                card.setType(Card.CardType.values(rs.getString("type")));
                cards.add(card);
            }
            CardList cardList = new CardList(cards);
            return new ApiResult(true, cardList);
        } catch (SQLException e) {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /// /////////////////////////////////////有问题，前端突然多出来一个组件，不知道要不要实现，先写一个接口吧
    @Override
    public ApiResult modifyCardInfo(Card card) {
        Connection conn = connector.getConn();

        // check if card exists
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM card WHERE card_id =?");
            stmt.setInt(1, card.getCardId());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "借书证不存在!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResult(false, "Database Error:" + e.getMessage());
        }

        // update card info
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE card SET name =?, department =?, type =? WHERE card_id =?");
            stmt.setString(1, card.getName());
            stmt.setString(2, card.getDepartment());
            stmt.setString(3, card.getType().getStr());
            stmt.setInt(4, card.getCardId());
            stmt.executeUpdate();
            commit(conn);
            return new ApiResult(true, "Successfully modified card info!");
        } catch (Exception e) {
            rollback(conn);
            e.printStackTrace();
            return new ApiResult(false, "Database Error:" + e.getMessage());
        }
    }
}
