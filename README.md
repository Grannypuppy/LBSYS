# 图书管理系统实验报告

**数据库系统 (2025 年 春夏学期)**  
**浙江大学**  
**完成日期：2025 年 5 月 10 日**  
**教师：** 苗晓晔

---

## 目录
**1. 简介**
    1.1 实验目标  
    1.2 实验环境  
**2. 系统设计及实现**  
    2.1 图书管理系统 E-R 图  
    2.2 主要接口设计思路  
    2.3 数据库连接与事务管理  
    2.4 关键实现细节  
    2.5 前端系统设计与实现
**3. 问题与解决方法**  
**4. 思考题**  
**5. 总结**  

---

## 第一章 简介

### 1.1 实验目标

设计并实现一个精简的图书管理程序，要求具有以下核心功能：
- 图书管理：入库、查询、修改、删除、库存管理
- 借书证管理：注册、注销、查询
- 借阅管理：借书、还书、借阅历史查询
- 数据库事务管理和并发控制

### 1.2 实验环境

**本地开发环境：**
- 操作系统：Windows 11  
- IDE：IntelliJ IDEA 2025.1.2  
- Java 版本：JDK 1.8.0  
- 构建工具：Apache Maven 3.6.3+  
- 数据库：MySQL (支持 MySQL Connector/J 8.0.31)  
- 其他依赖：Lombok 1.18.24, SnakeYAML 1.33, Gson 2.10.1, JUnit 4.13.2  

---

## 第二章 系统设计及实现

### 2.1 图书管理系统 E-R 图
数据库设计如下：  
- **book**: `book_id`, `category`, `title`, `press`, `publish_year`, `author`, `price`, `stock`  
- **card**: `card_id`, `name`, `department`, `type`  
- **borrow**: `card_id`, `book_id`, `borrow_time`, `return_time`  

**ER图:**
![alt text](image.png)

### 2.2 主要接口设计思路

基于 `LibraryManagementSystem` 接口，系统实现了以下核心功能模块：

**图书管理接口：**
- `storeBook(Book book)` - 单本图书入库
- `storeBook(List<Book> books)` - 批量图书入库
- `incBookStock(int bookId, int deltaStock)` - 库存增减
- `removeBook(int bookId)` - 图书删除
- `modifyBookInfo(Book book)` - 图书信息修改
- `queryBook(BookQueryConditions conditions)` - 图书查询

**借阅管理接口：**
- `borrowBook(Borrow borrow)` - 借书操作
- `returnBook(Borrow borrow)` - 还书操作
- `showBorrowHistory(int cardId)` - 借阅历史查询

**借书证管理接口：**
- `registerCard(Card card)` - 借书证注册
- `removeCard(int cardId)` - 借书证注销
- `showCards()` - 借书证列表查询

### 2.3 数据库连接与事务管理

主要需要实现`LibraryManagementSystemImpl.java`中的函数，具体要求已在接口定义文件中给出。实现每个函数的思路结构基本相同：

+ 阅读接口要求，构造 SQL 语句
+ 使用 JDBC 创建相应的 PreparedStatement
+ 从函数调用参数中获取参数，设置 PreparedStatement 的参数
+ 执行 SQL 语句
+ 根据 SQL 语句的执行结果，返回相应的 ApiResult

**具体步骤：**
1. 使用 JDBC 创建数据库连接
2. 所有操作都在事务中执行
3. 操作成功时调用 `commit()`
4. 操作失败时调用 `rollback()`
5. 使用 `PreparedStatement` 防止 SQL 注入

**示例代码：事务管理**
```java
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
            return new ApiResult(false, "Stock will be negative");
        }
        stmt = conn.prepareStatement("UPDATE book SET stock = stock + ? WHERE book_id = ?");
        stmt.setInt(1, deltaStock);
        stmt.setInt(2, bookId);
        stmt.executeUpdate();
        commit(conn);
    } catch (Exception e) {
        rollback(conn);
        return new ApiResult(false, e.getMessage());
    }
    return new ApiResult(true, null);
}
```

### 2.4 关键实现细节

**批量入库实现：**
使用 `PreparedStatement.addBatch()` 和 `executeBatch()` 提高性能，确保原子性。

**借书逻辑：**
1. 检查图书是否存在且有库存
2. 检查用户是否已借阅该书且未归还
3. 减少库存，记录借阅信息

**查询优化：**
使用数据库层面的条件过滤，按 `book_id` 升序排序，避免在应用层处理大量数据。

### 2.5 前端系统设计与实现

**技术栈选择：**
- **前端框架：** Vue.js 3.x - 采用组合式 API，提供响应式数据绑定和组件化开发
- **UI 组件库：** Element Plus - 提供丰富的企业级 UI 组件，包含表格、对话框、表单等
- **图标库：** @element-plus/icons-vue - 统一的图标风格和交互体验
- **HTTP 客户端：** Axios - 处理前后端数据交互，支持请求拦截和响应处理
- **路由管理：** Vue Router 4.x - 实现单页应用的路由控制

**系统架构设计：**
1. **主界面布局 (App.vue)：** 
   - 顶部标题栏：显示系统名称和项目信息
   - 左侧导航菜单：提供三大功能模块的快速切换
   - 主内容区域：动态加载对应功能组件

2. **三大功能模块：**
   - **图书管理 (Book.vue)：** 卡片式功能入口，包含借书、还书、查询、入库、删除、修改等8大功能
   - **借书证管理 (Card.vue)：** 卡片展示所有借书证信息，支持增删改查和实时搜索
   - **借阅记录查询 (Borrow.vue)：** 表格形式展示借阅历史，支持条件筛选和排序

**前后端交互机制：**
1. **RESTful API 设计：** 
   - GET `/book?type=records` - 图书查询，支持多条件过滤和排序
   - POST `/book` - 图书管理操作（借书、还书、入库、删除、修改等）
   - GET `/card?type=records` - 借书证列表查询
   - POST `/card` - 借书证管理操作（注册、修改、删除）
   - GET `/borrow?type=records&cardId=xxx` - 借阅记录查询

2. **数据格式统一：** 使用 JSON 格式进行数据交换，响应格式包含 `success`/`error` 字段和业务数据

3. **错误处理机制：** 
   - 前端使用 Element Plus 的 Message 组件显示操作结果
   - 后端错误信息统一返回并在前端友好展示

**关键实现细节：**

**图书管理模块：**
- 采用卡片式布局，每个功能独立成卡片，提供直观的操作入口
- 查询功能支持多条件组合（类别、书名、出版社、年份、作者、价格）
- 批量入库使用动态表格，支持行级编辑和动态增删
- 日期处理统一格式化为 `YYYYMMDDHHmmss` 字符串格式

**借书证管理模块：**
- 实时搜索功能，根据姓名动态过滤显示卡片
- 卡片展示借书证详细信息，提供就地编辑和删除操作
- 新建借书证使用对话框形式，支持类型选择（教师/学生）

**借阅记录模块：**
- 表格展示借阅历史，支持按时间默认排序
- 时间格式友好化显示（如：2025年03月04日 09:26:13）
- 未归还图书显示为"未归还"状态

**前端优化策略：**
1. **响应式设计：** 使用 CSS 媒体查询适配不同屏幕尺寸
2. **组件复用：** Element Plus 组件统一样式和交互逻辑
3. **状态管理：** Vue 3 响应式系统自动更新视图
4. **用户体验：** 操作反馈及时，加载状态清晰，错误提示友好
5. **数据验证：** 前端表单验证确保数据完整性，按钮状态动态控制

**代码组织结构：**
```
src/
├── router/index.js          # 路由配置
├── main.js                  # 应用入口
├── App.vue                  # 主布局组件
├── components/
│   ├── Book.vue            # 图书管理组件
│   ├── Card.vue            # 借书证管理组件
│   └── Borrow.vue          # 借阅记录组件
└── assets/                 # 样式和静态资源
```

该前端系统通过模块化设计和统一的交互规范，为图书管理系统提供了直观易用的用户界面，确保了良好的用户体验和系统可维护性。

在 `/front-end/src/router/index.js` 文件中，定义了前端应用的路由配置。Vue Router 是 Vue.js 官方的路由管理工具，用于实现单页面应用（SPA）中的页面跳转和导航。通过 createRouter 和 createWebHistory 方法创建了一个路由实例，其中 createWebHistory 使用了 HTML5 的 History API 来实现无刷新页面跳转。

路由配置中定义了三个主要路径：/book、/card 和 /borrow，分别对应图书管理、借书证管理和借书记录查询三个功能模块。默认情况下，访问根路径 / 时会重定向到 /book，即图书管理页面。每个路径都映射到一个 Vue 组件，分别是 Book.vue、Card.vue 和 Borrow.vue，这些组件负责渲染对应的页面内容并处理相关逻辑。

在 `/front-end/src/App.vue` 文件中，定义了整个应用的入口页面布局。页面采用了 Element Plus 提供的容器组件（el-container、el-header、el-aside 和 el-main）来构建整体结构。页面左侧是一个侧边栏（el-aside），使用 Element Plus 的菜单组件（el-menu）实现了导航功能。菜单中包含了三个选项：图书管理、借书证管理和借书记录查询，每个选项都绑定了一个路由路径，点击后会跳转到对应的页面。侧边栏的样式和交互行为通过 el-menu 的属性和事件进行配置，例如高亮当前选中的菜单项、设置菜单的背景颜色和文字颜色等。页面的主内容区（el-main）通过 <RouterView> 组件动态渲染当前路由对应的页面内容。为了支持长内容的滚动，主内容区使用了 Element Plus 的滚动条组件（el-scrollbar），确保页面内容在超出可视区域时能够平滑滚动。

具体的功能实现位于 `/front-end/src/components` 目录下的三个 Vue 组件文件中：Book.vue、Borrow.vue 和 Card.vue。每个组件对应一个功能模块，分别负责处理图书管理、借书记录查询和借书证管理的相关逻辑。这些组件通过 Vue 的组合式 API 编写，利用 ref、reactive、computed 等特性管理组件的状态和逻辑。此外，组件中还会通过 Axios 或其他 HTTP 客户端与后端 REST API 进行交互，获取数据并更新页面内容。

#### Book.vue

Book.vue 是图书管理系统的核心组件之一，负责实现图书管理的各项功能。该组件通过 Vue 3 的组合式 API 编写，结合 Element Plus 的 UI 组件库，提供了丰富的用户交互界面和功能。
![alt text](image-1.png)

##### 1. 页面布局与样式

- 页面主体部分采用弹性布局（flex），将功能卡片以网格形式排列。每个功能卡片都是一个 el-button 组件，点击后会触发相应的对话框或操作。
- 功能卡片的样式通过 box-shadow 实现了立体效果，并使用了 Element Plus 的图标组件（如 Notebook、DocumentChecked 等）来增强视觉效果。

##### 2. 功能卡片

- **借书功能**：点击"我要借书"卡片后，会弹出一个对话框，用户需要输入书号、卡号和借书时间。借书时间通过 el-date-picker 组件选择，支持日期和时间的选择。点击"确定"按钮后，会通过 Axios 向后端发送借书请求。

- **还书功能**：点击"我要还书"卡片后，会弹出一个对话框，用户需要输入书号、卡号和还书时间。还书时间同样通过 el-date-picker 组件选择。点击"确定"按钮后，会通过 Axios 向后端发送还书请求。

- **查询图书**：点击"查询图书"卡片后，会弹出一个对话框，用户可以根据类别、书名、出版社、出版年份、作者、价格等条件进行查询。查询结果会以表格形式展示在页面下方，支持按不同字段排序。

- **图书入库**：点击"图书入库"卡片后，会弹出一个对话框，用户可以输入图书的类别、书名、出版社、出版年份、作者、价格和库存信息。点击"确定"按钮后，会通过 Axios 向后端发送入库请求。

- **图书批量入库**：点击"图书批量入库"卡片后，会弹出一个对话框，用户可以通过表格形式批量录入多本图书的信息。每行代表一本图书，支持动态增加和删除行。点击"确定"按钮后，会通过 Axios 向后端发送批量入库请求。

- **删除图书**：点击"删除图书"卡片后，会弹出一个对话框，用户需要输入要删除的图书的书号。点击"确定"按钮后，会通过 Axios 向后端发送删除请求。

- **修改库存**：点击"图书库存修改"卡片后，会弹出一个对话框，用户需要输入书号和库存变化量。点击"确定"按钮后，会通过 Axios 向后端发送库存修改请求。

- **修改图书信息**：点击"图书信息修改"卡片后，会弹出一个对话框，用户可以修改图书的类别、书名、出版社、出版年份、作者和价格等信息。点击"确定"按钮后，会通过 Axios 向后端发送信息修改请求。

##### 3. 对话框与表单

- 每个功能卡片点击后都会弹出一个对话框（el-dialog），对话框中包含相应的表单（el-input、el-date-picker 等），用户需要填写必要的信息。
- 表单的输入项通过 v-model 与 Vue 的数据属性进行双向绑定，确保用户输入的数据能够实时更新到组件的状态中。
- 对话框的"确定"按钮会根据表单的填写情况动态禁用或启用，确保用户必须填写所有必填项后才能提交请求。

##### 4. 数据交互

- 所有与后端的数据交互都通过 Axios 实现。Axios 是一个基于 Promise 的 HTTP 客户端，支持发送 GET、POST 等请求，并处理响应数据。
- 请求的 URL 根据功能不同而变化，例如借书功能的 URL 为 `/book?action=borrow`，还书功能的 URL 为 `/book?action=return`，查询功能的 URL 为 `/book?type=records` 等。
- 请求的参数通过 JavaScript 对象的形式传递给后端，后端返回的响应数据会通过 ElMessage 组件显示给用户，例如成功提示或错误信息。

##### 5. 查询结果展示

- 查询图书功能的结果会以表格形式展示在页面下方。表格使用 el-table 组件实现，支持按不同字段排序（如类别、书名、出版社等）。
- 表格的高度固定为 500px，超出部分可以通过滚动条查看。表格的列宽根据内容自动调整，确保数据能够完整显示。

##### 6. 状态管理

- 组件的状态通过 Vue 的 data 函数进行管理，包括对话框的可见性、表单的输入值、查询结果等。
- 每个功能的状态都独立管理，例如借书功能的 newBorrowVisible 控制借书对话框的显示与隐藏，newBorrowInfo 存储用户输入的借书信息。

##### 7. 工具方法

- `formatDateToString` 方法用于将日期对象转换为后端所需的字符串格式（如 YYYYMMDDHHmmss），确保日期数据能够正确传递。
- `deleteRow` 方法用于删除批量入库表格中的某一行数据。
- `onAddItem` 方法用于在批量入库表格中新增一行数据。

##### 8. 样式与交互

- 组件的样式通过 `<style scoped>` 定义，确保样式只作用于当前组件，避免与其他组件冲突。
- 功能卡片的交互通过 @click 事件绑定，点击后会触发相应的对话框或操作。
- 对话框的关闭操作通过 @click 事件绑定到"取消"按钮，点击后会隐藏对话框并清空表单数据。

##### 9. 错误处理

- 所有 Axios 请求都通过 .then 和 .catch 处理响应和错误。如果后端返回错误信息，会通过 ElMessage.error 显示给用户；如果操作成功，会通过 ElMessage.success 显示成功提示。

##### 10. 代码结构

- 组件的代码结构分为模板部分（`<template>`）、脚本部分（`<script>`）和样式部分（`<style>`）。
- 模板部分定义了页面的布局和交互元素，脚本部分实现了业务逻辑和数据交互，样式部分定义了页面的视觉效果。

通过以上设计，Book.vue 实现了一个交互友好的图书管理界面，能够满足用户对图书管理的各项需求。

#### Borrow.vue

与 Book.vue 结构相似，Borrow.vue 是图书管理系统中用于查询借书记录的组件，提供了简洁且功能完善的借书记录查询界面。

##### 1. 页面布局与样式

- 标题右侧提供了一个搜索框（el-input），用户可以在查询结果中进一步筛选借书记录。搜索框支持按图书 ID、借出时间或归还时间进行筛选。
- 页面主体部分包含一个查询框和一个结果表格。查询框用于输入借书证 ID，点击"查询"按钮后会显示对应的借书记录。

##### 2. 查询功能

- 用户在查询框中输入借书证 ID 后，点击"查询"按钮会触发 QueryBorrows 方法。
- QueryBorrows 方法通过 Axios 向 `/borrow?type=records&cardId=<借书证 ID>` 发送 GET 请求，获取该借书证的所有借书记录。
- 如果后端返回错误信息，会通过 ElMessage.error 显示给用户；如果查询成功，借书记录会以表格形式展示在页面下方。

##### 3. 结果表格

- 查询结果通过 el-table 组件展示,表格包含四列:借书证ID、图书ID、借出时间和归还时间。 
- 表格支持按图书ID、借出时间和归还时间进行排序,默认按借出时间升序排列。 
- 表格的高度固定为600px,超出部分可以通过滚动条查看。表格的列宽根据内容自动调整,确保数据能够完整显示。 

#### Card.vue 
Card.vue 是图书管理系统中用于管理借书证的组件,界面示例如下: 

[Image: Card management interface screenshot] 

##### 1. 页面布局与样式 

- 标题右侧提供了一个搜索框(el-input),用户可以根据借书证号、姓名或部门进行筛选。
- 页面主体部分采用弹性布局(flex),将借书证卡片以网格形式排列。每个卡片展示了借书证的基本信息,并提供了修改和删除操作按钮。 

##### 2. 借书证卡片 

- 每个借书证卡片展示了以下信息: 
  - 借书证号: card.cardId 
  - 姓名: card.name 
  - 部门: card.department 
  - 类型:通过reflection 方法将类型代码(T或S)转换为可读的“教师”或“学生”。 
- 每个卡片底部提供了两个操作按钮: 
  - 修改:点击后弹出修改对话框,允许用户修改借书证的姓名、部门和类型。 
  - 删除:点击后弹出删除确认对话框,用户确认后删除该借书证。 

##### 3. 新建借书证 

- 页面右下角提供了一个“新建借书证”按钮,点击后会弹出一个对话框。 
- 对话框中包含三个输入项: 
  - 姓名:必填项,用户输入借书证持有人的姓名。 
  - 部门:必填项,用户输入借书证持有人所属的部门。 
  - 类型:下拉选择框,用户可以选择“教师”或“学生”。 
- 点击“确定”按钮后,会通过Axios 向后端发送新建借书证的请求。 

##### 4. 修改借书证 

- 点击某个借书证卡片的“修改”按钮后,会弹出一个修改对话框。 
- 对话框中预填充了当前借书证的信息,用户可以修改姓名、部门和类型。 
- 点击“确定”按钮后,会通过Axios 向后端发送修改借书证的请求。 

##### 5. 删除借书证 

- 点击某个借书证卡片的“删除”按钮后,会弹出一个删除确认对话框。 
- 用户确认后,会通过Axios 向后端发送删除借书证的请求。 

##### 6. 数据交互 
- 所有与后端的数据交互都通过Axios实现。Axios是一个基于Promise 的HTTP客户端,支持发送GET、POST等请求,并处理响应数据。 
- 请求的URL 根据功能不同而变化,例如: 
  - 查询借书证:/card?type=records 
  - 新建借书证:/card?action=register 
  - 修改借书证:/card?action=modify 
  - 删除借书证:/card?action=remove 
- 请求的参数通过JavaScript 对象的形式传递给后端,后端返回的响应数据会通过 ElMessage 组件显示给用户,例如成功提示或错误信息。 

##### 7. 状态管理
  - 组件的状态通过Vue 的data 函数进行管理,包括借书证列表(cards)、搜索内容(toSearch)、新建借书证信息(newCardInfo)、待修改借书证信息(toModifyInfo)等。 
  - 每个功能的状态都独立管理,例如新建借书证对话框的可见性通过 newCardVisible控制,修改借书证对话框的可见性通过 modifyCardVisible 控制。 

##### 8.工具方法 

- reflection 方法用于将借书证类型代码(T或s)转换为可读的“教师”或“学生”。 
- QueryCards 方法用于查询所有借书证,并将结果存储在cards 列表中。 
- ConfirmNewCard、ConfirmModifyCard 和 ConfirmRemoveCard 方法分别用于处理新建、修改和删除借书证的逻辑。

--- 
## 第三章 问题与解决方法

### 3.1 并发测试问题
**问题：** 多线程并发操作时出现数据不一致  
**解决方案：** 设置适当的事务隔离级别，使用数据库锁机制

### 3.2 批量操作的原子性
**问题：** 批量入库时部分成功部分失败  
**解决方案：** 使用 `PreparedStatement.executeBatch()` 配合事务回滚机制

### 3.3 SQL 注入防护
**问题：** 动态 SQL 拼接存在安全风险  
**解决方案：** 全面使用 `PreparedStatement` 参数绑定

### 3.4 数据库连接管理
**问题：** 连接池管理和连接超时  
**解决方案：** 配置合适的连接参数，及时释放资源

---

## 第四章 思考题

### 绘制该图书管理系统的E-R图 
如图所示: 

![alt text](image.png)

### 描述 SQL注入攻击的原理(并简要举例)。在图书管理系统中,哪些模块可能会遭受SQL注入攻击?如何解决? 

#### SQL注入攻击的原理 
SQL注入攻击是一种常见的网络安全攻击方式,攻击者通过在输入字段中插入恶意的SQL 代码,试图篡改数据库查询语句,从而获取、篡改或删除数据库中的数据。例如,在登录系统中,假设登录验证的SQL语句是: 

```SQL
SELECT * FROM users WHERE username = '$username' AND password = '$password'; [cite: 76]
```

如果用户输入的用户名是 admin' OR 1=1--,密码是任意值,那么拼接后的SQL语句就会变成:

```SQL
SELECT * FROM users WHERE username = 'admin' OR 1=1 -- AND password = '任意值'; [cite: 77, 78]
```
由于OR 1=1 始终为真,--是SQL的注释符号,后面的语句被注释掉,因此这条SQL语句会返回所有用户的信息,攻击者可以借此绕过登录验证。

#### 易受攻击的板块

- **用户登录模块**:如果用户名和密码的输入没有经过严格的过滤和验证,攻击者可能会通过注入恶意 SQL 代码来获取管理员或其他用户的登录信息。
- **图书查询模块**:用户在查询图书时,可能会输入恶意的SQL代码来篡改查询语句,从而获取不该获取的图书信息或篡改图书库存等数据。
- **借书和还书模块**:在借书和还书操作中,如果对用户输入的图书ID、借书证ID等信息没有进行严格的验证和过滤,攻击者可能会通过注入恶意SQL 代码来篡改借书记录或还书记录,从而导致图书库存不一致或借书记录混乱。

#### 解决方案

1. 使用 PreparedStatement(项目中使用的)
    
    PreparedStatement 是Java中的一种预编译SQL语句的技术,它可以有效防止SQL注入攻击。通过使用PreparedStatement,可以将SQL语句中的参数与SQL语句本身分离,从而避免了恶意SQL代码的注入。例如:

```Java
PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?"); [cite: 78]
stmt.setString(1, username); [cite: 79]
stmt.setString(2, password); [cite: 79]
ResultSet rs = stmt.executeQuery(); [cite: 79]
```

在这种情况下,即使用户输入了恶意的 代码,PreparedStatement 也会将其视为普通的字符串参数,而不会将其作为SQL语句的一部分来执行,从而有效防止了SQL注入攻击。 

2. 使用ORM 框架 
    
    ORM (Object-Relational Mapping)框架可以将数据库中的表映射为对象,通过对象的操作来实现对数据库的操作,从而避免了直接编写SQL语句。ORM框架通常会自动处理SQL语句的拼接和参数绑定,从而有效防止SQL注入攻击。 

### 在InnoDB的默认隔离级别(RR, Repeated Read)下,当出现并发访问时,如何保证借书结果的正确性?

#### InnoDB RR
InnoDB 的默认隔离级别是RR(Repeated Read),在这种隔离级别下,一个事务在读取数据时,会看到该事务开始时数据库中数据的一致性视图。这意味着在同一个事务中,即使其他事务对数据进行了修改,该事务仍然会看到事务开始时的数据状态。这种隔离级别可以有效防止不可重复读(Non-Repeatable Read)问题,即在同一个事务中,对同一行数据的多次读取可能会得到不同的结果。

#### 对应策略
1. 使用 synchronized 锁

    在借书操作中,可以使用synchronized 锁来保证同一本书在同一时刻只能被一个用户借阅。例如:

```Java
synchronized (lock) {
    // 检查库存
    int stock = getStock(bookId); [cite: 80]
    if (stock <= 0) {
        return new ApiResult(false, "图书库存不足!"); [cite: 81]
    }
    // 更新库存
    updateStock(bookId, stock - 1); [cite: 81]
    // 插入借书记录
    insertBorrowRecord(borrow); [cite: 81]
} [cite: 80]
```
在这种情况下,通过synchronized锁,可以确保同一本书在同一时刻只能被一个用户借阅,从而避免了并发借书导致的库存不足和数据不一致问题。 

2. 使用数据库事务 
    在借书操作中,可以使用数据库事务来保证操作的原子性。例如: 

```Java
try {
    conn.setAutoCommit(false); [cite: 82]
    //检查库存
    int stock = getStock(bookId); [cite: 82]
    if (stock <= 0) {
        conn.rollback(); [cite: 82]
        return new ApiResult(false, "图书库存不足!"); [cite: 82]
    }
    // 更新库存
    updateStock(bookId, stock - 1); [cite: 82]
    //插入借书记录 [cite: 83]
    insertBorrowRecord(borrow); [cite: 83]
    conn.commit(); [cite: 83]
} catch (Exception e) {
    conn.rollback(); [cite: 83]
    return new ApiResult(false, "Database Error:" + e.getMessage()); [cite: 83]
} [cite: 83]
```

在这种情况下,通过数据库事务,可以确保借书操作的原子性。如果在借书过程中出现任何错误,事务会回滚,从而保证了借书记录和图书库存之间的一致性。

---

## 第五章 总结

本次实验通过实现图书管理系统，深入学习了：
1. **数据库设计：** E-R 图设计和关系型数据库建模
2. **JDBC 编程：** 连接管理、事务控制、SQL 操作
3. **并发控制：** 事务隔离级别和数据一致性保证
4. **系统架构：** 分层设计和接口抽象
5. **测试驱动：** 单元测试和系统测试方法

这次实验是我第一次接触 web 前后端开发，也算是小小当了一次全栈工程师。看到自己
写的网页最后成功跑起来，心里很有成就感。虽然实验过程中遇到了很多问题，一开始
连项目结构都搞不清楚，对前后端 api 更是一无所知，但在完成项目的过程中也不断探
索，学到了很多有用的技术：前端的 HTML, CSS, JavaScript, Vue，后端的 Java, JDBC，非常有收获。
个人感觉，最有挑战的是前后端 api 的部分。使用 GET，POST 的过程中需要传递 JSON字符串，因此前端传给后端的、后端处理方法的对应变量名必须一致，否则 console 就会输出报错。
在这个项目之前，虽然我也写过不少代码，但往往都是一个或者几个文件，没有很复杂
的结构，也没有涉及计算机网络相关的知识，都是在本地运行。通过这次实验，我极大
提高了自己的项目开发能力，也基本掌握了 web 开发的有关内容，之后如果使用 docker部署，就能成为一个真正可以运行的网页了。希望之后可以继续精进 web 开发的相关知识，把前端写的更美观，前后端 api 指定的更加优雅！
---
