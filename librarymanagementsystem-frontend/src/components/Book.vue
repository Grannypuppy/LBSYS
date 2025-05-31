<!-- TODO: YOUR CODE HERE -->
<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%;">
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold;">图书管理
            <el-input v-model="searchKeyword" :prefix-icon="Search"
                style="width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;"
                clearable placeholder="搜索图书..." />
        </div>

        <!-- 查询筛选区域 -->
        <el-card style="margin: 20px 40px;">
            <template #header>
                <span style="font-weight: bold;">图书查询</span>
                <el-button style="float: right;" type="primary" @click="QueryBooks">查询</el-button>
                <el-button style="float: right; margin-right: 10px;" @click="resetQuery">重置</el-button>
            </template>
            
            <el-row :gutter="20">
                <el-col :span="6">
                    <el-form-item label="类别">
                        <el-input v-model="queryConditions.category" placeholder="请输入类别" clearable />
                    </el-form-item>
                </el-col>
                <el-col :span="6">
                    <el-form-item label="书名">
                        <el-input v-model="queryConditions.title" placeholder="请输入书名" clearable />
                    </el-form-item>
                </el-col>
                <el-col :span="6">
                    <el-form-item label="出版社">
                        <el-input v-model="queryConditions.press" placeholder="请输入出版社" clearable />
                    </el-form-item>
                </el-col>
                <el-col :span="6">
                    <el-form-item label="作者">
                        <el-input v-model="queryConditions.author" placeholder="请输入作者" clearable />
                    </el-form-item>
                </el-col>
            </el-row>
            
            <el-row :gutter="20">
                <el-col :span="6">
                    <el-form-item label="出版年份">
                        <el-input v-model.number="queryConditions.minPublishYear" placeholder="最小年份" type="number" />
                    </el-form-item>
                </el-col>
                <el-col :span="6">
                    <el-form-item label="至">
                        <el-input v-model.number="queryConditions.maxPublishYear" placeholder="最大年份" type="number" />
                    </el-form-item>
                </el-col>
                <el-col :span="6">
                    <el-form-item label="价格范围">
                        <el-input v-model.number="queryConditions.minPrice" placeholder="最低价格" type="number" />
                    </el-form-item>
                </el-col>
                <el-col :span="6">
                    <el-form-item label="至">
                        <el-input v-model.number="queryConditions.maxPrice" placeholder="最高价格" type="number" />
                    </el-form-item>
                </el-col>
            </el-row>
            
            <el-row :gutter="20">
                <el-col :span="6">
                    <el-form-item label="排序字段">
                        <el-select v-model="queryConditions.sortBy" placeholder="选择排序字段">
                            <el-option label="图书ID" value="book_id" />
                            <el-option label="类别" value="category" />
                            <el-option label="书名" value="title" />
                            <el-option label="出版社" value="press" />
                            <el-option label="出版年份" value="publish_year" />
                            <el-option label="作者" value="author" />
                            <el-option label="价格" value="price" />
                            <el-option label="库存" value="stock" />
                        </el-select>
                    </el-form-item>
                </el-col>
                <el-col :span="6">
                    <el-form-item label="排序方式">
                        <el-select v-model="queryConditions.sortOrder" placeholder="选择排序方式">
                            <el-option label="升序" value="asc" />
                            <el-option label="降序" value="desc" />
                        </el-select>
                    </el-form-item>
                </el-col>
            </el-row>
        </el-card>

        <!-- 操作按钮区域 -->
        <div style="margin: 20px 40px;">
            <el-button type="primary" @click="showAddDialog">添加图书</el-button>
            <el-button type="success" @click="showBatchAddDialog">批量添加</el-button>
            <el-button type="warning" @click="showBorrowDialog">借书</el-button>
            <el-button type="info" @click="showReturnDialog">还书</el-button>
        </div>

        <!-- 图书列表表格 -->
        <el-table :data="filteredBooks" style="width: 95%; margin: 20px 40px;" stripe border>
            <el-table-column prop="bookId" label="图书ID" width="80" sortable />
            <el-table-column prop="category" label="类别" width="120" />
            <el-table-column prop="title" label="书名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="press" label="出版社" width="150" show-overflow-tooltip />
            <el-table-column prop="publishYear" label="出版年份" width="100" sortable />
            <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
            <el-table-column prop="price" label="价格" width="100" sortable>
                <template #default="scope">
                    ¥{{ scope.row.price.toFixed(2) }}
                </template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="80" sortable />
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                    <el-button size="small" @click="editBook(scope.row)">编辑</el-button>
                    <el-button size="small" @click="showStockDialog(scope.row)">库存</el-button>
                    <el-button size="small" type="danger" @click="deleteBook(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 添加图书对话框 -->
        <el-dialog v-model="addDialogVisible" title="添加图书" width="50%">
            <el-form :model="newBook" label-width="100px">
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="类别" required>
                            <el-input v-model="newBook.category" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="书名" required>
                            <el-input v-model="newBook.title" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="出版社" required>
                            <el-input v-model="newBook.press" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="出版年份" required>
                            <el-input v-model.number="newBook.publishYear" type="number" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="作者" required>
                            <el-input v-model="newBook.author" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="价格" required>
                            <el-input v-model.number="newBook.price" type="number" step="0.01" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="库存" required>
                    <el-input v-model.number="newBook.stock" type="number" style="width: 200px;" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="addDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmAddBook">确定</el-button>
            </template>
        </el-dialog>

        <!-- 批量添加图书对话框 -->
        <el-dialog v-model="batchAddDialogVisible" title="批量添加图书" width="60%">
            <div style="margin-bottom: 20px;">
                <el-button type="primary" @click="addNewBookToBatch">添加图书</el-button>
                <el-button type="success" @click="confirmBatchAdd" :disabled="batchBooks.length === 0">确认批量添加</el-button>
            </div>
            <el-table :data="batchBooks" style="width: 100%;">
                <el-table-column label="类别">
                    <template #default="scope">
                        <el-input v-model="scope.row.category" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="书名">
                    <template #default="scope">
                        <el-input v-model="scope.row.title" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="出版社">
                    <template #default="scope">
                        <el-input v-model="scope.row.press" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="出版年份">
                    <template #default="scope">
                        <el-input v-model.number="scope.row.publishYear" type="number" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="作者">
                    <template #default="scope">
                        <el-input v-model="scope.row.author" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="价格">
                    <template #default="scope">
                        <el-input v-model.number="scope.row.price" type="number" step="0.01" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="库存">
                    <template #default="scope">
                        <el-input v-model.number="scope.row.stock" type="number" size="small" />
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                    <template #default="scope">
                        <el-button size="small" type="danger" @click="removeBatchBook(scope.$index)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <template #footer>
                <el-button @click="batchAddDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>

        <!-- 编辑图书对话框 -->
        <el-dialog v-model="editDialogVisible" title="编辑图书" width="50%">
            <el-form :model="editingBook" label-width="100px">
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="类别">
                            <el-input v-model="editingBook.category" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="书名">
                            <el-input v-model="editingBook.title" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="出版社">
                            <el-input v-model="editingBook.press" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="出版年份">
                            <el-input v-model.number="editingBook.publishYear" type="number" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="作者">
                            <el-input v-model="editingBook.author" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="价格">
                            <el-input v-model.number="editingBook.price" type="number" step="0.01" />
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="editDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmEditBook">确定</el-button>
            </template>
        </el-dialog>

        <!-- 库存管理对话框 -->
        <el-dialog v-model="stockDialogVisible" title="库存管理" width="30%">
            <div style="text-align: center;">
                <p><strong>{{ currentBook.title }}</strong></p>
                <p>当前库存：{{ currentBook.stock }}</p>
                <el-form :model="stockForm" label-width="80px">
                    <el-form-item label="变更数量">
                        <el-input-number v-model="stockForm.amount" :min="-currentBook.stock" />
                    </el-form-item>
                </el-form>
                <p>变更后库存：{{ currentBook.stock + stockForm.amount }}</p>
            </div>
            <template #footer>
                <el-button @click="stockDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmStockChange">确定</el-button>
            </template>
        </el-dialog>

        <!-- 借书对话框 -->
        <el-dialog v-model="borrowDialogVisible" title="借书" width="30%">
            <el-form :model="borrowForm" label-width="100px">
                <el-form-item label="借书证ID" required>
                    <el-input v-model.number="borrowForm.cardId" type="number" />
                </el-form-item>
                <el-form-item label="图书ID" required>
                    <el-input v-model.number="borrowForm.bookId" type="number" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="borrowDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmBorrow">确定</el-button>
            </template>
        </el-dialog>

        <!-- 还书对话框 -->
        <el-dialog v-model="returnDialogVisible" title="还书" width="30%">
            <el-form :model="returnForm" label-width="100px">
                <el-form-item label="借书证ID" required>
                    <el-input v-model.number="returnForm.cardId" type="number" />
                </el-form-item>
                <el-form-item label="图书ID" required>
                    <el-input v-model.number="returnForm.bookId" type="number" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="returnDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmReturn">确定</el-button>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

export default {
    data() {
        return {
            Search,
            searchKeyword: '',
            books: [],
            queryConditions: {
                category: '',
                title: '',
                press: '',
                author: '',
                minPublishYear: null,
                maxPublishYear: null,
                minPrice: null,
                maxPrice: null,
                sortBy: '',
                sortOrder: ''
            },
            
            // 对话框状态
            addDialogVisible: false,
            batchAddDialogVisible: false,
            editDialogVisible: false,
            stockDialogVisible: false,
            borrowDialogVisible: false,
            returnDialogVisible: false,
            
            // 表单数据
            newBook: {
                category: '',
                title: '',
                press: '',
                publishYear: null,
                author: '',
                price: null,
                stock: null
            },
            batchBooks: [],
            editingBook: {},
            currentBook: {},
            stockForm: {
                amount: 0
            },
            borrowForm: {
                cardId: null,
                bookId: null
            },
            returnForm: {
                cardId: null,
                bookId: null
            }
        }
    },
    computed: {
        filteredBooks() {
            if (!this.searchKeyword) {
                return this.books;
            }
            return this.books.filter(book => {
                return Object.values(book).some(value => 
                    value.toString().toLowerCase().includes(this.searchKeyword.toLowerCase())
                );
            });
        }
    },
    methods: {
        async QueryBooks() {
            try {
                const params = { type: 'records' };
                
                // 添加查询条件
                if (this.queryConditions.category) params.category = this.queryConditions.category;
                if (this.queryConditions.title) params.title = this.queryConditions.title;
                if (this.queryConditions.press) params.press = this.queryConditions.press;
                if (this.queryConditions.author) params.author = this.queryConditions.author;
                if (this.queryConditions.minPublishYear) params['min-publish-year'] = this.queryConditions.minPublishYear;
                if (this.queryConditions.maxPublishYear) params['max-publish-year'] = this.queryConditions.maxPublishYear;
                if (this.queryConditions.minPrice) params.minprice = this.queryConditions.minPrice;
                if (this.queryConditions.maxPrice) params.maxprice = this.queryConditions.maxPrice;
                if (this.queryConditions.sortBy) params.sortby = this.queryConditions.sortBy;
                if (this.queryConditions.sortOrder) params.sortorder = this.queryConditions.sortOrder;
                
                const response = await axios.get('http://localhost:8081/book', { params });
                this.books = response.data.records || [];
                ElMessage.success(`查询到 ${this.books.length} 本图书`);
            } catch (error) {
                console.error('查询图书失败:', error);
                ElMessage.error('查询图书失败');
            }
        },
        
        resetQuery() {
            this.queryConditions = {
                category: '',
                title: '',
                press: '',
                author: '',
                minPublishYear: null,
                maxPublishYear: null,
                minPrice: null,
                maxPrice: null,
                sortBy: '',
                sortOrder: ''
            };
            this.QueryBooks();
        },
        
        showAddDialog() {
            this.newBook = {
                category: '',
                title: '',
                press: '',
                publishYear: null,
                author: '',
                price: null,
                stock: null
            };
            this.addDialogVisible = true;
        },
        
        async confirmAddBook() {
            try {
                await axios.post('http://localhost:8081/book', {
                    action: 'store',
                    book: {
                        bookId: '0',
                        category: this.newBook.category,
                        title: this.newBook.title,
                        press: this.newBook.press,
                        publishYear: this.newBook.publishYear.toString(),
                        author: this.newBook.author,
                        price: this.newBook.price.toString(),
                        stock: this.newBook.stock.toString()
                    }
                });
                ElMessage.success('图书添加成功');
                this.addDialogVisible = false;
                this.QueryBooks();
            } catch (error) {
                console.error('添加图书失败:', error);
                if (error.response && error.response.data && error.response.data.error) {
                    ElMessage.error(error.response.data.error);
                } else {
                    ElMessage.error('添加图书失败');
                }
            }
        },
        
        showBatchAddDialog() {
            this.batchBooks = [];
            this.batchAddDialogVisible = true;
        },
        
        addNewBookToBatch() {
            this.batchBooks.push({
                category: '',
                title: '',
                press: '',
                publishYear: null,
                author: '',
                price: null,
                stock: null
            });
        },
        
        removeBatchBook(index) {
            this.batchBooks.splice(index, 1);
        },
        
        async confirmBatchAdd() {
            try {
                const books = this.batchBooks.map(book => ({
                    bookId: '0',
                    category: book.category,
                    title: book.title,
                    press: book.press,
                    publishYear: book.publishYear.toString(),
                    author: book.author,
                    price: book.price.toString(),
                    stock: book.stock.toString()
                }));
                
                await axios.post('http://localhost:8081/book', {
                    action: 'storemulti',
                    books: books
                });
                ElMessage.success('批量添加成功');
                this.batchAddDialogVisible = false;
                this.QueryBooks();
            } catch (error) {
                console.error('批量添加失败:', error);
                if (error.response && error.response.data && error.response.data.error) {
                    ElMessage.error(error.response.data.error);
                } else {
                    ElMessage.error('批量添加失败');
                }
            }
        },
        
        editBook(book) {
            this.editingBook = { ...book };
            this.editDialogVisible = true;
        },
        
        async confirmEditBook() {
            try {
                await axios.post('http://localhost:8081/book', {
                    action: 'modify',
                    book: {
                        bookId: this.editingBook.bookId.toString(),
                        category: this.editingBook.category,
                        title: this.editingBook.title,
                        press: this.editingBook.press,
                        publishYear: this.editingBook.publishYear.toString(),
                        author: this.editingBook.author,
                        price: this.editingBook.price.toString()
                    }
                });
                ElMessage.success('图书信息修改成功');
                this.editDialogVisible = false;
                this.QueryBooks();
            } catch (error) {
                console.error('修改图书失败:', error);
                if (error.response && error.response.data && error.response.data.error) {
                    ElMessage.error(error.response.data.error);
                } else {
                    ElMessage.error('修改图书失败');
                }
            }
        },
        
        async deleteBook(book) {
            try {
                await ElMessageBox.confirm(`确定删除图书《${book.title}》吗？`, '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                });
                
                await axios.post('http://localhost:8081/book', {
                    action: 'remove',
                    bookID: book.bookId
                });
                ElMessage.success('图书删除成功');
                this.QueryBooks();
            } catch (error) {
                if (error !== 'cancel') {
                    console.error('删除图书失败:', error);
                    if (error.response && error.response.data && error.response.data.error) {
                        ElMessage.error(error.response.data.error);
                    } else {
                        ElMessage.error('删除图书失败');
                    }
                }
            }
        },
        
        showStockDialog(book) {
            this.currentBook = { ...book };
            this.stockForm.amount = 0;
            this.stockDialogVisible = true;
        },
        
        async confirmStockChange() {
            try {
                await axios.post('http://localhost:8081/book', {
                    action: 'incstock',
                    bookID: this.currentBook.bookId,
                    amount: this.stockForm.amount
                });
                ElMessage.success('库存修改成功');
                this.stockDialogVisible = false;
                this.QueryBooks();
            } catch (error) {
                console.error('修改库存失败:', error);
                if (error.response && error.response.data && error.response.data.error) {
                    ElMessage.error(error.response.data.error);
                } else {
                    ElMessage.error('修改库存失败');
                }
            }
        },
        
        showBorrowDialog() {
            this.borrowForm = { cardId: null, bookId: null };
            this.borrowDialogVisible = true;
        },
        
        async confirmBorrow() {
            try {
                const now = new Date();
                const timeStr = now.getFullYear().toString() +
                               (now.getMonth() + 1).toString().padStart(2, '0') +
                               now.getDate().toString().padStart(2, '0') +
                               now.getHours().toString().padStart(2, '0') +
                               now.getMinutes().toString().padStart(2, '0') +
                               now.getSeconds().toString().padStart(2, '0');
                
                await axios.post('http://localhost:8081/book', {
                    action: 'borrow',
                    borrow: {
                        cardId: this.borrowForm.cardId.toString(),
                        bookId: this.borrowForm.bookId.toString(),
                        borrowTime: timeStr,
                        returnTime: '0'
                    }
                });
                ElMessage.success('借书成功');
                this.borrowDialogVisible = false;
                this.QueryBooks();
            } catch (error) {
                console.error('借书失败:', error);
                if (error.response && error.response.data && error.response.data.error) {
                    ElMessage.error(error.response.data.error);
                } else {
                    ElMessage.error('借书失败');
                }
            }
        },
        
        showReturnDialog() {
            this.returnForm = { cardId: null, bookId: null };
            this.returnDialogVisible = true;
        },
        
        async confirmReturn() {
            try {
                const now = new Date();
                const timeStr = now.getFullYear().toString() +
                               (now.getMonth() + 1).toString().padStart(2, '0') +
                               now.getDate().toString().padStart(2, '0') +
                               now.getHours().toString().padStart(2, '0') +
                               now.getMinutes().toString().padStart(2, '0') +
                               now.getSeconds().toString().padStart(2, '0');
                
                await axios.post('http://localhost:8081/book', {
                    action: 'return',
                    borrow: {
                        cardId: this.returnForm.cardId.toString(),
                        bookId: this.returnForm.bookId.toString(),
                        borrowTime: '0',
                        returnTime: timeStr
                    }
                });
                ElMessage.success('还书成功');
                this.returnDialogVisible = false;
                this.QueryBooks();
            } catch (error) {
                console.error('还书失败:', error);
                if (error.response && error.response.data && error.response.data.error) {
                    ElMessage.error(error.response.data.error);
                } else {
                    ElMessage.error('还书失败');
                }
            }
        }
    },
    
    mounted() {
        this.QueryBooks();
    }
}
</script>

<style scoped>
.el-form-item {
    margin-bottom: 10px;
}
</style>