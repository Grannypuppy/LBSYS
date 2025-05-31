<template>
    <el-scrollbar height="100%" style="width: 100%;">

        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold;">
            借书记录查询
            <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right; ;"
                clearable />
        </div>

        <!-- 查询框 -->
        <div style="width:30%;margin:0 auto; padding-top:5vh;">

            <el-input v-model="this.toQuery" style="display:inline; " placeholder="输入借书证ID"></el-input>
            <el-button style="margin-left: 10px;" type="primary" @click="QueryBorrows">查询</el-button>

        </div>

        <!-- 结果表格 -->
        <el-table v-if="isShow" :data="fitlerTableData" height="600"
            :default-sort="{ prop: 'borrowTime', order: 'descending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="cardID" label="借书证ID" />
            <el-table-column prop="bookID" label="图书ID" sortable />
            <el-table-column prop="borrowTime" label="借出时间" sortable />
            <el-table-column prop="returnTime" label="归还时间" sortable />
        </el-table>

    </el-scrollbar>
</template>

<script>
import axios from 'axios';
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
    data() {
        return {
            isShow: false, // 结果表格展示状态
            tableData: [],
            toQuery: '', // 待查询内容(对某一借书证号进行查询)
            toSearch: '', // 待搜索内容(对查询到的结果进行搜索)
            Search
        }
    },
    computed: {
        fitlerTableData() { // 搜索规则
            return this.tableData.filter(
                (tuple) =>
                    (this.toSearch == '') || // 搜索框为空，即不搜索
                    tuple.bookID.toString().includes(this.toSearch) || // 图书号与搜索要求一致
                    tuple.borrowTime.toString().includes(this.toSearch) || // 借出时间包含搜索要求
                    tuple.returnTime.toString().includes(this.toSearch) // 归还时间包含搜索要求
            )
        }
    },
    methods: {
        async QueryBorrows() {
            if (!this.toQuery) {
                ElMessage.warning('请输入借书证ID');
                return;
            }
            
            try {
                this.tableData = [] // 清空列表
                let response = await axios.get('http://localhost:8081/borrow', { 
                    params: { 
                        type: 'records',
                        cardId: this.toQuery 
                    } 
                });
                
                let records = response.data.records || []; // 获取响应负载
                records.forEach(record => { // 对于每一个借书记录
                    this.tableData.push({
                        cardID: record.cardID,
                        bookID: record.bookID,
                        borrowTime: this.formatTime(record.borrowTime),
                        returnTime: record.returnTime === 0 ? "未归还" : this.formatTime(record.returnTime)
                    }); // 将它加入到列表项中
                });
                this.isShow = true // 显示结果列表
                
                if (records.length === 0) {
                    ElMessage.info('该借书证暂无借书记录');
                }
            } catch (error) {
                console.error('查询失败:', error);
                if (error.response && error.response.data && error.response.data.error) {
                    ElMessage.error(error.response.data.error);
                } else {
                    ElMessage.error('查询失败，请检查网络连接');
                }
                this.isShow = false;
            }
        },
        
        formatTime(timestamp) {
            if (!timestamp || timestamp === 0) return "未设置";
            const date = new Date(timestamp);
            return date.getFullYear() + '年' + 
                   String(date.getMonth() + 1).padStart(2, '0') + '月' + 
                   String(date.getDate()).padStart(2, '0') + '日 ' +
                   String(date.getHours()).padStart(2, '0') + ':' + 
                   String(date.getMinutes()).padStart(2, '0') + ':' + 
                   String(date.getSeconds()).padStart(2, '0');
        }
    }
}
</script>