<template>
  <div class="chat-workbench" :class="{ 'is-dark': theme === 'dark', 'panel-collapsed': isCollapsed }">
    <aside class="history-panel" :class="{ collapsed: isCollapsed }">
      <div class="panel-head">
        <div v-if="!isCollapsed" class="panel-title">会话档案</div>
        <button class="collapse-btn" @click="toggleSidebar">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline :points="isCollapsed ? '9 18 15 12 9 6' : '15 18 9 12 15 6'" />
          </svg>
        </button>
      </div>

      <div v-if="!isCollapsed" class="panel-body">
        <div class="panel-tools">
          <button class="tool-btn" @click="loadHistoryList">刷新</button>
          <button class="tool-btn" @click="openSettingsDialog">设置</button>
        </div>

        <div v-if="historyList.length === 0" class="empty-tip">暂无历史记录</div>
        <ul v-else class="history-list">
          <li
            v-for="history in historyList"
            :key="history.id"
            class="history-item"
            :class="{ active: currentHistory && currentHistory.id === history.id }"
            @click="showHistoryDetail(history.id)"
          >
            <button class="history-delete-btn" title="删除记录" aria-label="删除会话记录" @click.stop="deleteHistory(history)">×</button>
            <p class="history-question">{{ buildHistoryTitle(history) }}</p>
            <p class="history-time">{{ formatTime(history.time) }}</p>
          </li>
        </ul>
      </div>
    </aside>

    <main class="chat-stage">
      <header class="stage-head">
        <div>
          <h2>简优 AI 对话台</h2>
          <p>支持流式回复、历史回放与简历相关问答</p>
        </div>
        <div class="head-status">
          <span class="dot" :class="socketStatus"></span>
          <span>{{ socketStatusText }}</span>
        </div>
      </header>

      <section ref="messageListRef" class="message-stream">
        <div v-if="messageList.length === 0" class="welcome-block">
          <h3>可以先从这些问题开始</h3>
          <div class="starter-grid">
            <button class="starter-btn" @click="fillPrompt('帮我优化简历')">帮我优化简历</button>
            <button class="starter-btn" @click="fillPrompt('推荐适合我的职位')">推荐适合我的职位</button>
            <button class="starter-btn" @click="fillPrompt('面试自我介绍怎么说')">面试自我介绍怎么说</button>
          </div>
        </div>

        <article v-for="(message, index) in messageList" :key="index" class="bubble-row" :class="message.sender">
          <div
            class="bubble"
            :class="{ 'ai-rich': message.sender === 'ai' }"
            v-html="message.sender === 'ai' ? formatAiMessage(message.text) : escapeHtml(message.text)"
          ></div>
        </article>
      </section>

      <footer class="composer">
        <button class="history-nav-btn" @click="clickHistory">历史页</button>
        <textarea
          v-model="inputMessage"
          placeholder="输入你的问题，回车发送..."
          :disabled="loading"
          rows="1"
          @keydown.enter.exact.prevent="sendMessage"
        ></textarea>
        <button class="send-btn" :disabled="loading || !inputMessage.trim()" @click="sendMessage">
          {{ loading ? '发送中...' : '发送' }}
        </button>
      </footer>
    </main>

    <el-dialog v-model="settingsDialogVisible" title="界面设置" width="360px" center>
      <div class="setting-item">
        <span>主题模式</span>
        <div class="theme-toggle">
          <button :class="['theme-btn', theme === 'light' ? 'active' : '']" @click="changeTheme('light')">浅色</button>
          <button :class="['theme-btn', theme === 'dark' ? 'active' : '']" @click="changeTheme('dark')">深色</button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import dayjs from 'dayjs';

const router = useRouter();
const route = useRoute();

const messageList = ref([]);
const inputMessage = ref('');
const loading = ref(false);
const isCollapsed = ref(false);
const historyList = ref([]);
const currentHistory = ref(null);
const messageListRef = ref(null);

const settingsDialogVisible = ref(false);
const theme = ref('light');

let chatSocket = null;
let reconnectTimer = null;
let reconnectCount = 0;
const maxReconnect = 3;
const socketStatus = ref('connecting');

const userId = localStorage.getItem('userId');
const username = localStorage.getItem('username');
const conversationStorageKey = `chat_conversation_id_${userId || 'guest'}`;
const createConversationId = () => {
  return window.crypto?.randomUUID?.() ?? `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
};
const conversationId = ref(
  localStorage.getItem(conversationStorageKey) || createConversationId()
);
localStorage.setItem(conversationStorageKey, conversationId.value);

const resetConversation = () => {
  conversationId.value = createConversationId();
  localStorage.setItem(conversationStorageKey, conversationId.value);
  currentHistory.value = null;
  messageList.value = [];
};

const socketStatusText = computed(() => {
  if (socketStatus.value === 'open') return '连接正常';
  if (socketStatus.value === 'closed') return '连接关闭';
  if (socketStatus.value === 'error') return '连接异常';
  return '连接中';
});

const escapeHtml = (value = '') => {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};

const formatAiMessage = (rawText = '') => {
  const text = rawText.replace(/\r\n/g, '\n');
  const blocks = [];
  let html = escapeHtml(text).replace(/```([\s\S]*?)```/g, (_, code) => {
    const key = `__CODE_BLOCK_${blocks.length}__`;
    blocks.push(`<pre><code>${code.trim()}</code></pre>`);
    return key;
  });

  html = html
    .replace(/^###\s?(.*)$/gm, '<h4>$1</h4>')
    .replace(/^##\s?(.*)$/gm, '<h3>$1</h3>')
    .replace(/^#\s?(.*)$/gm, '<h2>$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+?)`/g, '<code>$1</code>')
    .replace(/^-\s+(.*)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>\n?)+/g, (match) => `<ul>${match}</ul>`)
    .replace(/\n/g, '<br>');

  blocks.forEach((block, index) => {
    html = html.replace(`__CODE_BLOCK_${index}__`, block);
  });

  return html;
};

const normalizeEscapedText = (value = '') => {
  return String(value)
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t');
};

const parseStreamPayloads = (raw = '') => {
  const source = String(raw ?? '').replace(/\r\n/g, '\n');
  if (!source) return [];

  return source
    .split('\n\n')
    .map((eventBlock) => {
      if (!eventBlock) return '';
      const lines = eventBlock.split('\n');
      let payload = '';

      lines.forEach((line, index) => {
        if (index === 0 && line.startsWith('data:')) {
          payload = line.slice(5);
          return;
        }
        if (line.startsWith('data:')) {
          payload += `\n${line.slice(5)}`;
          return;
        }
        payload += `\n${line}`;
      });

      return payload;
    })
    .filter((payload) => payload !== '');
};

const extractStreamText = (payload = '') => {
  const chunk = String(payload ?? '');
  const normalized = chunk.trim();
  if (!normalized || normalized === '[DONE]') return '';

  try {
    const parsed = JSON.parse(normalized);
    if (typeof parsed === 'string') return normalizeEscapedText(parsed);
    if (typeof parsed.text === 'string') return normalizeEscapedText(parsed.text);
    if (typeof parsed.content === 'string') return normalizeEscapedText(parsed.content);
    if (typeof parsed.result === 'string') return normalizeEscapedText(parsed.result);

    const choiceContent = parsed.choices?.[0]?.delta?.content ?? parsed.choices?.[0]?.message?.content;
    if (typeof choiceContent === 'string') return normalizeEscapedText(choiceContent);

    return '';
  } catch {
    return normalizeEscapedText(chunk);
  }
};

const scrollToBottom = async () => {
  await nextTick();
  const el = messageListRef.value;
  if (el) {
    el.scrollTop = el.scrollHeight;
  }
};

const trimText = (text, length) => {
  if (!text) return '无内容';
  if (text.length <= length) return text;
  return `${text.slice(0, length)}...`;
};

const getLatestRoleTurn = (text = '', role = '用户') => {
  const normalized = String(text || '').replace(/\r\n/g, '\n');
  const regexp = new RegExp(`(?:^|\\n\\n)${role}[：:]([\\s\\S]*?)(?=\\n\\n${role}[：:]|$)`, 'g');
  const matches = [...normalized.matchAll(regexp)];
  if (matches.length === 0) return '';
  return matches[matches.length - 1]?.[1]?.trim() || '';
};

const buildHistoryTitle = (history) => {
  const question = String(history?.question || '');
  const result = String(history?.result || '');

  let summary =
    getLatestRoleTurn(question, '用户') ||
    getLatestRoleTurn(question, 'AI') ||
    question ||
    result ||
    '无内容';

  summary = summary
    .replace(/^(用户|AI)[：:]/, '')
    .replace(/\n+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  return trimText(summary, 28);
};

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
};

const clickHistory = () => {
  router.push('/history');
};

const fillPrompt = (text) => {
  inputMessage.value = text;
};

const buildConversationHistory = () => {
  return messageList.value
    .filter((message) => message?.text?.trim())
    .slice(-12)
    .map((message) => ({
      role: message.sender === 'ai' ? 'assistant' : 'user',
      content: message.text
    }));
};

const sendMessage = async () => {
  const question = inputMessage.value.trim();
  if (!question) return;
  const history = buildConversationHistory();

  messageList.value.push({ text: question, sender: 'user' });
  messageList.value.push({ text: '', sender: 'ai' });
  inputMessage.value = '';
  loading.value = true;
  await scrollToBottom();

  if (chatSocket && chatSocket.readyState === WebSocket.OPEN) {
    chatSocket.send(
      JSON.stringify({
        type: 'message',
        text: question,
        conversationId: conversationId.value,
        history,
        userId,
        username
      })
    );
    return;
  }

  loading.value = false;
  const latest = messageList.value[messageList.value.length - 1];
  if (latest && latest.sender === 'ai' && !latest.text) {
    messageList.value.pop();
  }
  ElMessage.error('WebSocket 连接未建立，请稍后重试');
};

const getWsUrl = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://localhost:9090/chat/socket`;
};

const setupWebSocket = () => {
  socketStatus.value = 'connecting';
  chatSocket = new WebSocket(getWsUrl());

  chatSocket.onopen = () => {
    socketStatus.value = 'open';
    reconnectCount = 0;
  };

  chatSocket.onmessage = async (event) => {
    try {
      const raw = String(event.data ?? '');
      const payloads = parseStreamPayloads(raw);
      if (payloads.length === 0) return;

      let finished = false;
      let mergedText = '';

      payloads.forEach((payload) => {
        if (payload.trim() === '[DONE]') {
          finished = true;
          return;
        }
        mergedText += extractStreamText(payload);
      });

      if (finished) {
        loading.value = false;
        await scrollToBottom();
        return;
      }

      if (!mergedText) return;

      const latest = messageList.value[messageList.value.length - 1];
      if (latest && latest.sender === 'ai') {
        latest.text += mergedText;
      }
      await scrollToBottom();
    } catch {
      loading.value = false;
      ElMessage.error('消息处理失败');
    }
  };

  chatSocket.onerror = () => {
    socketStatus.value = 'error';
  };

  chatSocket.onclose = () => {
    socketStatus.value = 'closed';
    if (reconnectCount >= maxReconnect) return;
    reconnectCount += 1;
    reconnectTimer = setTimeout(setupWebSocket, 1500 * reconnectCount);
  };
};

const formatTime = (time) => {
  if (!time) return '';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};

const parseRoleTurns = (text = '', role = '') => {
  const normalized = String(text || '').replace(/\r\n/g, '\n').trim();
  if (!normalized) return [];

  const marker = `${role}：`;
  if (!normalized.includes(marker)) {
    return [normalized];
  }

  return normalized
    .split(new RegExp(`(?:^|\\n\\n)${role}[：:]`))
    .map((item) => item.trim())
    .filter(Boolean);
};

const buildHistoryMessages = (history) => {
  const questionText = history?.question || '';
  const resultText = history?.result || '';

  const hasConversationMarkers = /(^|\n\n)用户[：:]/.test(questionText) || /(^|\n\n)AI[：:]/.test(resultText);
  if (!hasConversationMarkers) {
    return [
      { text: questionText, sender: 'user' },
      { text: resultText, sender: 'ai' }
    ].filter((item) => item.text);
  }

  const userTurns = parseRoleTurns(questionText, '用户');
  const aiTurns = parseRoleTurns(resultText, 'AI');
  const merged = [];
  const maxTurns = Math.max(userTurns.length, aiTurns.length);

  for (let index = 0; index < maxTurns; index += 1) {
    if (userTurns[index]) {
      merged.push({ text: userTurns[index], sender: 'user' });
    }
    if (aiTurns[index]) {
      merged.push({ text: aiTurns[index], sender: 'ai' });
    }
  }

  return merged;
};

const loadHistoryList = async () => {
  const uid = localStorage.getItem('userId');
  if (!uid) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  try {
    const response = await axios.get('/api/history/list', {
      params: {
        userId: uid,
        pageNum: 1,
        pageSize: 20
      }
    });

    if (response.data?.records) {
      historyList.value = response.data.records;
    }
  } catch (error) {
    ElMessage.error(`加载历史记录失败: ${error.response?.data?.message || error.message}`);
  }
};

const deleteHistory = async (history) => {
  const uid = localStorage.getItem('userId');
  if (!uid || !history?.id) {
    ElMessage.warning('缺少用户或记录信息');
    return;
  }

  try {
    await ElMessageBox.confirm('确认删除这条会话记录吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      customClass:
        theme.value === 'dark'
          ? 'history-delete-confirm history-delete-confirm-dark'
          : 'history-delete-confirm',
      confirmButtonClass: 'history-delete-confirm-btn',
      cancelButtonClass: 'history-delete-cancel-btn'
    });

    await axios.delete(`/api/history/${history.id}`, {
      params: { userId: uid }
    });

    historyList.value = historyList.value.filter((item) => item.id !== history.id);
    if (currentHistory.value?.id === history.id) {
      currentHistory.value = null;
      messageList.value = [];
    }
    ElMessage.success('已删除会话记录');
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(`删除失败: ${error.response?.data?.message || error.message}`);
  }
};

const showHistoryDetail = async (historyId) => {
  try {
    const response = await axios.get(`/api/history/detail/${historyId}`);
    const history = response.data;

    messageList.value = buildHistoryMessages(history);

    currentHistory.value = history;
    await scrollToBottom();
  } catch (error) {
    ElMessage.error(`加载对话详情失败: ${error.response?.data?.message || error.message}`);
  }
};

const openSettingsDialog = () => {
  settingsDialogVisible.value = true;
};

const changeTheme = (value) => {
  theme.value = value;
};

onMounted(async () => {
  if (route.query.newChat === '1') {
    resetConversation();
  }
  await loadHistoryList();
  setupWebSocket();
});

watch(
  () => `${route.query.newChat || ''}:${route.query.ts || ''}`,
  (newValue, oldValue) => {
    if (route.query.newChat === '1' && newValue !== oldValue) {
      resetConversation();
    }
  }
);

onBeforeUnmount(() => {
  if (chatSocket) {
    chatSocket.close();
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
  }
});
</script>

<style scoped>
.chat-workbench {
  --bg-main: #f4f8f5;
  --card: #ffffff;
  --line: #d8e2dc;
  --text: #1f2933;
  --sub: #52606d;
  --accent: #0f766e;
  --user-bubble-bg: #169184;
  --ai-bubble-bg: #f2f4f7;
  --ai-bubble-line: #d9dfe6;
  --history-accent: #10b981;
  --history-hover-bg: rgba(16, 185, 129, 0.08);
  --history-active-bg: rgba(16, 185, 129, 0.14);
  --accent-soft: #ccfbf1;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 14px;
  padding: 14px;
  height: calc(100vh - 56px);
  overflow: hidden;
  background: radial-gradient(circle at 12% 10%, #fdf6cc 0%, transparent 25%), var(--bg-main);
  color: var(--text);
  font-family: 'Source Han Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  transition: grid-template-columns 0.26s ease;
}

.chat-workbench.panel-collapsed {
  grid-template-columns: 58px 1fr;
}

.chat-workbench.is-dark {
  --bg-main: #0f1720;
  --card: #16212d;
  --line: #263649;
  --text: #dce7f2;
  --sub: #9eb0c3;
  --accent: #14b8a6;
  --user-bubble-bg: #1fb8a9;
  --ai-bubble-bg: #222f3b;
  --ai-bubble-line: #2f4152;
  --history-accent: #10b981;
  --history-hover-bg: rgba(16, 185, 129, 0.12);
  --history-active-bg: rgba(16, 185, 129, 0.2);
  --accent-soft: #153b38;
  background: #0f1720;
}

.history-panel,
.chat-stage {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
}

.history-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  transition: width 0.26s ease;
}

.history-panel.collapsed {
  width: 58px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid var(--line);
}

.panel-title {
  font-weight: 700;
  letter-spacing: 0.2px;
}

.collapse-btn {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--sub);
  border-radius: 8px;
  width: 34px;
  height: 34px;
  cursor: pointer;
}

.panel-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  height: 100%;
  min-height: 0;
}

.panel-tools {
  display: flex;
  gap: 8px;
}

.tool-btn {
  flex: 1;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--sub);
  border-radius: 8px;
  padding: 7px;
  cursor: pointer;
}

.empty-tip {
  font-size: 13px;
  color: var(--sub);
  padding: 8px;
}

.history-list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: grid;
  gap: 8px;
}

.history-item {
  position: relative;
  border: 1px solid var(--line);
  background: transparent;
  border-radius: 10px;
  padding: 10px 34px 10px 10px;
  cursor: pointer;
}

.history-delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: transparent;
  color: var(--sub);
  line-height: 20px;
  text-align: center;
  font-size: 14px;
  cursor: pointer;
  opacity: 0;
  transform: scale(0.92);
  transition: opacity 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, color 0.18s ease, background-color 0.18s ease;
}

.history-item:hover .history-delete-btn,
.history-item.active .history-delete-btn,
.history-item:focus-within .history-delete-btn {
  opacity: 1;
  transform: scale(1);
}

.history-delete-btn:hover {
  border-color: var(--history-accent);
  color: var(--history-accent);
  background: rgba(16, 185, 129, 0.08);
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.12);
}

.history-delete-btn:active {
  transform: scale(0.96);
  border-color: var(--history-accent);
  background: rgba(16, 185, 129, 0.12);
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.24), inset 0 0 0 1px rgba(16, 185, 129, 0.5);
}

.history-delete-btn:focus-visible {
  opacity: 1;
  transform: scale(1);
  outline: none;
  border-color: var(--history-accent);
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.24);
}

.history-item:hover {
  border-color: var(--history-accent);
  background: var(--history-hover-bg);
}

.history-item.active {
  border-color: var(--history-accent);
  background: var(--history-active-bg);
}

.history-question {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
}

.history-time {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--sub);
}

.chat-stage {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 0;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

.stage-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--line);
  padding: 14px 18px;
}

.stage-head h2 {
  margin: 0;
  font-size: 20px;
}

.stage-head p {
  margin: 4px 0 0;
  color: var(--sub);
  font-size: 13px;
}

.head-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--sub);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f59e0b;
}

.dot.open {
  background: #22c55e;
}

.dot.error,
.dot.closed {
  background: #ef4444;
}

.message-stream {
  padding: 18px;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.history-list,
.message-stream {
  scrollbar-width: thin;
  scrollbar-color: rgba(82, 96, 109, 0.35) transparent;
}

.history-list::-webkit-scrollbar,
.message-stream::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.history-list::-webkit-scrollbar-track,
.message-stream::-webkit-scrollbar-track {
  background: transparent;
}

.history-list::-webkit-scrollbar-thumb,
.message-stream::-webkit-scrollbar-thumb {
  background: rgba(82, 96, 109, 0.3);
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.history-list::-webkit-scrollbar-thumb:hover,
.message-stream::-webkit-scrollbar-thumb:hover {
  background: rgba(82, 96, 109, 0.48);
  background-clip: padding-box;
}

.chat-workbench.is-dark .history-list,
.chat-workbench.is-dark .message-stream {
  scrollbar-color: rgba(158, 176, 195, 0.42) transparent;
}

.chat-workbench.is-dark .history-list::-webkit-scrollbar-thumb,
.chat-workbench.is-dark .message-stream::-webkit-scrollbar-thumb {
  background: rgba(158, 176, 195, 0.36);
  background-clip: padding-box;
}

.chat-workbench.is-dark .history-list::-webkit-scrollbar-thumb:hover,
.chat-workbench.is-dark .message-stream::-webkit-scrollbar-thumb:hover {
  background: rgba(220, 231, 242, 0.52);
  background-clip: padding-box;
}

.welcome-block {
  margin: auto;
  width: min(640px, 100%);
  text-align: center;
}

.welcome-block h3 {
  margin: 0 0 12px;
}

.starter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.starter-btn {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--sub);
  border-radius: 10px;
  padding: 9px 8px;
  cursor: pointer;
}

.starter-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.bubble-row {
  display: flex;
}

.bubble-row.user {
  justify-content: flex-end;
}

.bubble {
  max-width: min(680px, 80%);
  white-space: pre-wrap;
  line-height: 1.65;
  border-radius: 14px;
  padding: 12px 14px;
  border: 1px solid var(--line);
}

.bubble.ai-rich {
  white-space: normal;
}

.bubble.ai-rich :deep(h2),
.bubble.ai-rich :deep(h3),
.bubble.ai-rich :deep(h4) {
  margin: 4px 0 8px;
  line-height: 1.4;
}

.bubble.ai-rich :deep(ul) {
  margin: 8px 0;
  padding-left: 18px;
}

.bubble.ai-rich :deep(pre) {
  margin: 10px 0;
  padding: 10px 12px;
  background: rgba(15, 23, 42, 0.06);
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow-x: auto;
}

.chat-workbench.is-dark .bubble.ai-rich :deep(pre) {
  background: rgba(241, 245, 249, 0.08);
}

.bubble.ai-rich :deep(code) {
  font-family: Consolas, 'Courier New', monospace;
}

.bubble.ai-rich :deep(strong) {
  font-weight: 700;
}

.bubble-row.ai .bubble {
  background: var(--ai-bubble-bg);
  border-color: var(--ai-bubble-line);
}

.bubble-row.user .bubble {
  background: var(--user-bubble-bg);
  color: #ffffff;
  border-color: transparent;
}

.composer {
  border-top: 1px solid var(--line);
  padding: 12px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
}

.history-nav-btn,
.send-btn {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--sub);
  border-radius: 10px;
  padding: 0 14px;
  cursor: pointer;
}

.history-nav-btn {
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.history-nav-btn:hover {
  border-color: var(--history-accent);
  color: var(--history-accent);
  background: var(--history-hover-bg);
}

.send-btn {
  border-color: var(--accent);
  color: #ffffff;
  background: var(--accent);
  font-weight: 600;
  box-shadow: 0 6px 14px rgba(15, 118, 110, 0.24);
  transform: translateY(0);
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 18px rgba(15, 118, 110, 0.28);
  filter: brightness(1.06);
}

.send-btn:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: 0 4px 10px rgba(15, 118, 110, 0.22);
  filter: brightness(0.98);
}

.send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.composer textarea {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text);
  border-radius: 10px;
  padding: 10px 12px;
  min-height: 44px;
  max-height: 140px;
  resize: vertical;
  line-height: 1.5;
  font-family: 'PingFang SC', 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0.15px;
  caret-color: var(--accent);
  outline: none;
}

.composer textarea::placeholder {
  color: #7b8b9a;
  font-weight: 400;
}

.theme-toggle {
  display: flex;
  gap: 8px;
}

.theme-btn {
  border: 1px solid #d5dde7;
  background: #fff;
  color: #51606f;
  border-radius: 8px;
  padding: 6px 10px;
  cursor: pointer;
}

.theme-btn.active {
  border-color: #0f766e;
  color: #0f766e;
}

:global(.history-delete-confirm) {
  border-radius: 14px;
  border: 1px solid #d8e7e1;
  box-shadow: 0 18px 38px rgba(15, 118, 110, 0.2);
}

:global(.history-delete-confirm .el-message-box__header) {
  border-bottom: 1px solid #e7f0ec;
  padding-bottom: 10px;
}

:global(.history-delete-confirm .el-message-box__title) {
  color: #1f2933;
  font-weight: 700;
}

:global(.history-delete-confirm .el-message-box__content) {
  color: #4d5f6d;
}

:global(.history-delete-confirm .history-delete-confirm-btn) {
  border-radius: 10px;
  border-color: #0f766e;
  background: #0f766e;
  color: #fff;
  box-shadow: 0 8px 16px rgba(15, 118, 110, 0.2);
}

:global(.history-delete-confirm .history-delete-confirm-btn:hover) {
  border-color: #0d9488;
  background: #0d9488;
}

:global(.history-delete-confirm .history-delete-cancel-btn) {
  border-radius: 10px;
  border-color: #cfdcd6;
  color: #4e6270;
  background: #ffffff;
}

:global(.history-delete-confirm .history-delete-cancel-btn:hover) {
  border-color: #9fc7bc;
  color: #0f766e;
  background: #f1faf7;
}

:global(.history-delete-confirm-dark) {
  background: #16212d;
  border-color: #2d3f51;
}

:global(.history-delete-confirm-dark .el-message-box__header) {
  border-bottom-color: #2a3b4d;
}

:global(.history-delete-confirm-dark .el-message-box__title) {
  color: #dce7f2;
}

:global(.history-delete-confirm-dark .el-message-box__content) {
  color: #aec0d2;
}

:global(.history-delete-confirm-dark .history-delete-cancel-btn) {
  background: #1b2a38;
  border-color: #355066;
  color: #b8c9d9;
}

:global(.history-delete-confirm-dark .history-delete-cancel-btn:hover) {
  background: #1e3244;
  border-color: #4c738f;
  color: #d6e5f3;
}

@media (max-width: 1000px) {
  .chat-workbench {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(160px, 32vh) 1fr;
  }

  .history-panel {
    min-height: 0;
  }

  .chat-stage {
    min-height: 0;
    height: 100%;
  }

  .starter-grid {
    grid-template-columns: 1fr;
  }
}

@media (hover: none), (pointer: coarse) {
  .history-delete-btn {
    opacity: 1;
    transform: scale(1);
    width: 24px;
    height: 24px;
    line-height: 22px;
  }
}
</style>
