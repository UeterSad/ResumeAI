<template>
  <div class="history-container">
    <div class="header">
      <el-button type="primary" @click="goBackToChat">返回</el-button>
      <h2>历史对话记录</h2>
    </div>
    
    <el-card class="history-card">
      <el-table 
        :data="historyRows" 
        stripe 
        style="width: 100%"
        v-loading="loading"
        element-loading-text="加载中..."
      >
        <el-table-column type="index" :index="indexMethod" label="序号" width="80" align="center" />
        <el-table-column prop="question" label="问题" min-width="200">
          <template #default="scope">
            <div class="question-cell">
              {{ scope.row.question }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="回答" min-width="300">
          <template #default="scope">
            <div class="result-cell">
              {{ scope.row.result }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="180" align="center">
          <template #default="scope">
            <span>{{ formatDate(scope.row.time) }}</span>
          </template>
        </el-table-column>
      </el-table>
  
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>
  
<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import axios from 'axios';
import dayjs from 'dayjs';

const router = useRouter();
const historyRows = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);

const toArray = (value) => (Array.isArray(value) ? value : []);

const toSafeNumber = (value, fallback = 0) => {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : fallback;
};

const extractHistoryPayload = (raw) => {
  const queue = [raw];
  const visited = new Set();
  let fallbackTotal;

  while (queue.length) {
    const node = queue.shift();
    if (!node || typeof node !== 'object' || visited.has(node)) continue;
    visited.add(node);

    const records = toArray(node.records).length ? node.records : toArray(node.list);
    const hasTotal = node.total !== undefined || node.count !== undefined;

    if (hasTotal && fallbackTotal === undefined) {
      fallbackTotal = node.total ?? node.count;
    }

    if (records.length) {
      return {
        records: toArray(records),
        total: node.total ?? node.count
      };
    }

    ['data', 'result', 'page', 'payload', 'rows'].forEach((key) => {
      if (node[key] && typeof node[key] === 'object') {
        queue.push(node[key]);
      }
    });
  }

  return { records: [], total: fallbackTotal };
};

const goBackToChat = () => {
  router.push('/chatView');
};

// 分页序号换算。
const indexMethod = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1;
};

const loadHistoryRecords = async () => {
  const userId = localStorage.getItem("userId");
  if (!userId) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  loading.value = true;
  try {
    const response = await axios.get("/api/history/getHistory", {
      params: {
        userId,
        pageNum: currentPage.value,
        pageSize: pageSize.value,
      },
    });

    const payload = extractHistoryPayload(response?.data ?? {});
    const records = toArray(payload.records);

    const headerTotal = response?.headers?.['x-total-count'];
    const resolvedTotal = payload.total ?? headerTotal ?? records.length;
    const numericTotal = toSafeNumber(resolvedTotal, records.length);
    const effectiveTotal = records.length > 0 && numericTotal <= 0 ? records.length : numericTotal;

    historyRows.value = records;
    total.value = effectiveTotal;
  } catch (error) {
    console.error("获取历史记录失败:", error);
    ElMessage.error('获取历史记录失败: ' + (error.response?.data?.message || error.message));
  } finally {
    loading.value = false;
  }
};

const handleCurrentChange = (page) => {
  currentPage.value = page;
  loadHistoryRecords();
};

const handleSizeChange = (size) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadHistoryRecords();
};

// 格式化日期
const formatDate = (time) => {
  if (!time) return '';
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
};

onMounted(() => {
  loadHistoryRecords();
});
</script>
  
<style scoped>
/* 页面容器 */
.history-container {
  --theme-main: #0f766e;
  --theme-main-soft: #ccfbf1;
  --theme-main-line: #9dd9cf;
  --theme-text: #1f2937;
  --theme-sub: #52606d;
  padding: 32px 24px;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 56px);
  background:
    radial-gradient(circle at 10% 8%, rgba(209, 250, 229, 0.38), transparent 28%),
    radial-gradient(circle at 84% 12%, rgba(204, 251, 241, 0.32), transparent 30%),
    #f4f8f5;
  font-family: 'PingFang SC', 'Noto Sans SC', -apple-system, sans-serif;
}

/* 头部 */
.header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--theme-text);
}

/* 卡片 */
.history-card {
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.94);
}

/* 单元格 */
.question-cell,
.result-cell {
  max-height: 100px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 8px 0;
  font-size: 14px;
  color: var(--theme-text);
  line-height: 1.5;
}

/* 分页 */
.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 14px 12px 6px;
  border-top: 1px solid #dbe8e3;
  background: linear-gradient(180deg, rgba(240, 253, 250, 0.55), rgba(255, 255, 255, 0));
}

/* Element Plus 覆盖 */
:deep(.el-card__body) {
  padding: 24px;
}

:deep(.el-table) {
  margin-bottom: 0;
}

:deep(.el-table th.el-table__cell) {
  background: #ecfdf8;
  color: var(--theme-main);
  font-weight: 600;
}

:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.el-table__row:hover > td) {
  background-color: #f0fdfa !important;
}

:deep(.el-button--primary) {
  background: var(--theme-main);
  border-color: var(--theme-main);
}

:deep(.el-button--primary:hover) {
  background: #0d9488;
  border-color: #0d9488;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: var(--theme-main);
  color: #fff;
}

:deep(.el-pagination .el-pager li:hover) {
  color: var(--theme-text);
  background: #eef6f3;
}

:deep(.el-pagination) {
  padding: 8px 10px;
  border-radius: 12px;
  background: #f8fcfb;
  border: 1px solid #dbe8e3;
}

:deep(.el-pagination .el-pager li) {
  border-radius: 8px;
  min-width: 28px;
  height: 28px;
  line-height: 28px;
  transition: all 0.2s ease;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  border-radius: 8px;
  color: var(--theme-sub);
  transition: color 0.2s ease, background-color 0.2s ease;
}

:deep(.el-pagination .el-select .el-input__wrapper),
:deep(.el-pagination .el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-pagination button:hover) {
  color: var(--theme-text);
  background: #eef6f3;
}

:deep(.el-pagination .el-pagination__total) {
  margin-right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #edf7f3;
  border: 1px solid #d4e8e1;
  color: var(--theme-sub);
  font-weight: 500;
}

:deep(.el-pagination .el-pagination__sizes),
:deep(.el-pagination .el-pagination__jump) {
  color: var(--theme-sub);
}

:deep(.el-pagination .el-pagination__jump .el-input__inner) {
  text-align: center;
}

:deep(.el-table__body tr.current-row > td.el-table__cell) {
  background: #e6fffa;
}

:deep(.el-table) {
  --el-table-border-color: #d5e5df;
  --el-table-header-border-color: #d5e5df;
  --el-table-row-hover-bg-color: #f0fdfa;
}

@media (max-width: 900px) {
  .history-container {
    padding: 20px 14px;
  }

  .pagination-container {
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  :deep(.el-pagination) {
    flex-wrap: nowrap;
  }
}
</style>