<template>
  <div class="header-container">
    <!-- 左侧 Logo 和欢迎语 -->
    <div class="header-left">
      <div class="logo-area">
        <div class="welcome-logo-balls" aria-hidden="true">
          <span class="welcome-ball welcome-ball-left"></span>
          <span class="welcome-ball welcome-ball-right"></span>
        </div>
        <span class="logo-text">简优</span>
      </div>
      <span class="welcome-text">欢迎，{{ username || '用户' }}</span>
    </div>

    <!-- 中间导航 -->
    <nav class="header-nav">
      <button class="nav-btn nav-btn-assistant" @click="goAssistant">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="9"></circle>
          <path d="M8 12h8"></path>
          <path d="M12 8v8"></path>
        </svg>
        简优助手
      </button>
      <button class="nav-btn nav-btn-info" @click="goToProfile">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>
        个人信息
      </button>
      <button class="nav-btn" @click="showMyResume">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="16" y1="13" x2="8" y2="13"></line>
          <line x1="16" y1="17" x2="8" y2="17"></line>
        </svg>
        我的简历
      </button>
      <button class="nav-btn nav-btn-primary" @click="openResumeModal">
        <svg class="ai-logo-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
          <rect x="4" y="4" width="16" height="16" rx="3"></rect>
          <path d="M9 15l2-6 2 6"></path>
          <line x1="8" y1="13" x2="12" y2="13"></line>
          <line x1="15" y1="9" x2="15" y2="15"></line>
          <line x1="17" y1="9" x2="17" y2="15"></line>
        </svg>
        生成简历
      </button>
      <button v-if="isAdmin" class="nav-btn nav-btn-success" @click="clickJobPool">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
          <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
        </svg>
        职位库管理
      </button>
      <button v-if="!isAdmin" class="nav-btn nav-btn-success" @click="clickRecommend">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        推荐职位
      </button>
    </nav>

    <!-- 右侧退出按钮 -->
    <div class="header-right">
      <button class="logout-btn" @click="openLogoutConfirm">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
          <polyline points="16 17 21 12 16 7"></polyline>
          <line x1="21" y1="12" x2="9" y2="12"></line>
        </svg>
        退出
      </button>
    </div>

    <el-dialog
      v-model="showLogoutConfirm"
      width="430px"
      append-to-body
      class="logout-confirm-dialog"
      :show-close="false"
      center
    >
      <div class="logout-confirm-content">
        <div class="logout-confirm-icon" aria-hidden="true">!</div>
        <h3 class="logout-confirm-title">确认退出当前账号？</h3>
        <p class="logout-confirm-desc">退出后将清空本地登录状态，并返回登录页面。</p>
      </div>
      <template #footer>
        <div class="logout-confirm-actions">
          <button class="logout-cancel-btn" @click="showLogoutConfirm = false">取消</button>
          <button class="logout-submit-btn" @click="handleLogout">确认退出</button>
        </div>
      </template>
    </el-dialog>

    <!-- 简历生成弹窗 -->
    <el-dialog
      v-model="showResumeModal"
      title="AI生成简历"
      width="440px"
      :close-on-click-modal="false"
      append-to-body
      modal-class="resume-ai-dialog-mask"
      center
    >
      <div class="modal-content">
        <div class="version-tabs">
          <button 
            :class="['tab-btn', resumeVersion === '应届生版' ? 'active' : '']" 
            @click="resumeVersion = '应届生版'"
          >应届生版</button>
          <button 
            :class="['tab-btn', resumeVersion === '标准版' ? 'active' : '']" 
            @click="resumeVersion = '标准版'"
          >标准版</button>
        </div>
        
        <div v-if="resumeVersion === '应届生版'" class="form-fields">
          <div class="field-group">
            <label>我的专业</label>
            <input v-model="form.major" maxlength="20" placeholder="请输入专业" />
          </div>
          <div class="field-group">
            <label>期望职位</label>
            <input v-model="form.position" maxlength="20" placeholder="请输入职位" />
          </div>
          <div class="field-group">
            <label>补充信息</label>
            <textarea v-model="form.extra" maxlength="40" placeholder="补充内容（选填）"></textarea>
          </div>
        </div>
        
        <div v-if="resumeVersion === '标准版'" class="form-fields">
          <div class="field-group">
            <label>工作经历</label>
            <input v-model="form.experience" maxlength="20" placeholder="请输入经历" />
          </div>
          <div class="field-group">
            <label>期望职位</label>
            <input v-model="form.position" maxlength="20" placeholder="请输入职位" />
          </div>
          <div class="field-group">
            <label>补充信息</label>
            <textarea v-model="form.extra" maxlength="40" placeholder="补充内容（选填）"></textarea>
          </div>
        </div>
      </div>
      <template #footer>
        <button 
          class="generate-btn" 
          @click="generateResume" 
          :disabled="!isFormValid || generating"
        >
          {{ generating ? '正在生成...' : '生成简历' }}
        </button>
      </template>
    </el-dialog>

    <!-- 生成状态弹窗 -->
    <el-dialog
      v-model="showGeneratingDialog"
      :show-close="false"
      width="320px"
      append-to-body
      modal-class="resume-ai-dialog-mask"
      center
    >
      <div class="generating-status">
        <div class="spinner"></div>
        <p class="status-text">{{ statusText }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script>
const API_BASE_URL = 'http://localhost:9090';

export default {
  name: 'r-header',
  data() {
    return {
      username: null,
      isAdmin: false, // 默认不显示心理分析按钮
      // ▼▼▼ 新增以下三个关键变量 ▼▼▼
      showResumeModal: false,  // 控制弹窗显示
      resumeVersion: '应届生版', // 默认选中的简历版本
      form: {                  // 表单数据模型
        major: '',
        position: '',
        extra: '',
        experience: ''
      },
      generating: false,
      showGeneratingDialog: false,
      statusText: '',
      // 个人信息缓存（用于联动）
      profileData: null,
      showLogoutConfirm: false
    };
  },
  computed: {
    isFormValid() {
      if (this.resumeVersion === '应届生版') {
        return this.form.major && this.form.position;
      }
      return this.form.experience && this.form.position;
    }
  },
  created() {
    this.username = localStorage.getItem('username');
    // 根据 userType 判断是否为管理员 (1=管理员)
    const userType = localStorage.getItem('userType');
    if (userType === '1') {
      this.isAdmin = true;
    }
  },
  methods: {
    buildResumeRequestPayload(userId, username) {
      return {
        userId,
        username,
        resumeVersion: this.resumeVersion,
        formData: this.form,
        profileData: this.profileData
      };
    },
    applyProfilePrefill(profileData) {
      this.profileData = profileData;
      if (profileData.major) {
        this.form.major = profileData.major;
      }
      if (profileData.jobTitle) {
        this.form.position = profileData.jobTitle;
      }
      if (profileData.jobStatus === '应届生') {
        this.resumeVersion = '应届生版';
      } else if (profileData.jobStatus === '在职' || profileData.jobStatus === '离职') {
        this.resumeVersion = '标准版';
      }
    },
    clickJobPool() {
      this.$router.push('/jobPool');
    },
    clickRecommend() {
      this.$router.push('/recommend');
    },
    openLogoutConfirm() {
      this.showLogoutConfirm = true;
    },
    handleLogout() {
      this.showLogoutConfirm = false;
      localStorage.clear();
      this.$router.push('/login');
    },
    showMyResume() {
      // 清除本地存储的简历数据，以便加载最新的数据库数据
      localStorage.removeItem('resumeData');
      this.$router.push('/resume'); 
    },
    goAssistant() {
      this.$router.push({
        path: '/chatView',
        query: {
          newChat: '1',
          ts: String(Date.now())
        }
      });
    },
    goToProfile() {
      this.$router.push('/profile');
    },
    // 打开AI生成简历弹窗（从个人信息预填充）
    openResumeModal() {
      // 先打开弹窗，避免接口慢时用户误以为按钮无效。
      this.showResumeModal = true;
      this.loadProfileForResume();
    },
    // 加载个人信息用于预填充简历表单
    async loadProfileForResume() {
      const userId = localStorage.getItem('userId');
      if (!userId) return;

      try {
        const response = await fetch(`${API_BASE_URL}/api/profile/${userId}`);
        const result = await response.json();

        if (result.success && result.data) {
          this.applyProfilePrefill(result.data);
        }
      } catch (error) {
        console.error('加载个人信息失败:', error);
      }
    },
    // 生成简历逻辑（你可以替换为调用后端接口）
    async generateResume() {
      if (!this.isFormValid) {
        this.$message.warning('请填写必要信息');
        return;
      }

      this.generating = true;
      this.showGeneratingDialog = true;
      this.statusText = '正在生成简历...';

      try {
        const userId = localStorage.getItem('userId');
        const username = localStorage.getItem('username');
        
        const response = await fetch(`${API_BASE_URL}/ai/chat`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(this.buildResumeRequestPayload(userId, username))
        });

        if (!response.ok) {
          throw new Error('网络请求失败');
        }

        const result = await response.json();
        
        if (!result.success) {
          throw new Error(result.error || '生成失败');
        }

        // 存储简历数据
        localStorage.setItem('resumeData', JSON.stringify(result.content));
        window.dispatchEvent(new CustomEvent('resume-data-updated'));
        
        // 显示成功状态
        this.statusText = '简历生成成功！';
        
        // 延迟关闭对话框并跳转
        setTimeout(() => {
          this.showResumeModal = false;
          this.showGeneratingDialog = false;
          this.$router.push('/resume');
        }, 1000);

      } catch (error) {
        console.error('生成简历失败:', error);
        this.statusText = '生成失败: ' + error.message;
        this.$message.error(error.message);
      } finally {
        this.generating = false;
      }
    }
  },
};
</script>

<style scoped>
/* 头部容器 */
.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(6px);
  font-family: 'Source Han Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* 左侧区域 */
.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.welcome-logo-balls {
  position: relative;
  width: 30px;
  height: 20px;
  flex: 0 0 auto;
}

.welcome-ball {
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
}

.welcome-ball-left {
  left: 0;
  background: #10b981;
}

.welcome-ball-right {
  right: 0;
  background: #3b82f6;
  opacity: 0.85;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.welcome-text {
  font-size: 14px;
  color: #4b5563;
}

/* 中间导航 */
.header-nav {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #3f4b5a;
  background: #ffffff;
  border: 1px solid #d4dde8;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.nav-btn svg {
  transition: transform 0.2s ease, color 0.2s ease;
}

.nav-btn:hover {
  color: #0b6b64;
  background: #f5fbfa;
  border-color: #8ccfc8;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.nav-btn:hover svg {
  transform: scale(1.08);
}

.nav-btn:active {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.1);
}

.nav-btn svg {
  flex-shrink: 0;
}

.nav-btn-primary {
  color: #0b6b64;
  background: #e8f3f1;
  border-color: #9ec9c3;
  box-shadow: 0 1px 3px rgba(15, 118, 110, 0.1);
}

.nav-btn-primary:hover {
  background: #deeeeb;
  box-shadow: 0 2px 8px rgba(15, 118, 110, 0.14);
}

.ai-logo-icon {
  flex-shrink: 0;
}

.nav-btn-success {
  color: #1d6f52;
  background: #e4f6ea;
  border-color: #98d7b2;
}

.nav-btn-success:hover {
  background: #d6efdf;
}

.nav-btn-info {
  color: #1f6c8f;
  background: #eef7fb;
  border-color: #b8d7e7;
}

.nav-btn-info:hover {
  background: #e4f1f8;
}

.nav-btn-assistant {
  color: #0f766e;
  background: #e8f6f3;
  border-color: #9ed0c9;
}

.nav-btn-assistant:hover {
  background: #dbefe9;
}

/* 右侧区域 */
.header-right {
  display: flex;
  align-items: center;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #9a3f3f;
  background: #fff5f3;
  border: 1px solid #e8b6ad;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn svg {
  transition: transform 0.2s ease;
}

.logout-btn:hover {
  background: #fdebea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(154, 63, 63, 0.16);
}

.logout-btn:hover svg {
  transform: translateX(1px);
}

:deep(.logout-confirm-dialog .el-dialog) {
  border-radius: 14px;
  border: 1px solid #f2d2ce;
  box-shadow: 0 16px 36px rgba(146, 42, 42, 0.16);
}

.logout-confirm-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 8px 6px 2px;
}

.logout-confirm-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #fff3f1;
  border: 1px solid #f2c3bc;
  color: #b54545;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logout-confirm-title {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.logout-confirm-desc {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  text-align: center;
}

.logout-confirm-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.logout-cancel-btn,
.logout-submit-btn {
  min-width: 108px;
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-cancel-btn {
  background: #ffffff;
  border-color: #d9e2ec;
  color: #475569;
}

.logout-cancel-btn:hover {
  background: #f8fafc;
  border-color: #bcc9d8;
}

.logout-submit-btn {
  background: #c14f4f;
  color: #fff;
}

.logout-submit-btn:hover {
  background: #ab4545;
  box-shadow: 0 6px 16px rgba(171, 69, 69, 0.24);
}

/* 弹窗内容 */
.modal-content {
  padding: 0 8px;
}

.version-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.tab-btn {
  flex: 1;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #5b6678;
  background: #f6fbfa;
  border: 1px solid #dbe8e3;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  color: #0f766e;
  background: #e8f3f1;
  border-color: #5fb8af;
  box-shadow: none;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 12px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-group label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.field-group input,
.field-group textarea {
  padding: 12px 14px;
  font-size: 14px;
  color: #1f2937;
  background: #ffffff;
  border: 1px solid #d7e3ef;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.field-group input:focus,
.field-group textarea:focus {
  border-color: #2c8d86;
  background: #fbfffe;
  box-shadow: 0 0 0 3px rgba(44, 141, 134, 0.14);
}

.field-group textarea {
  min-height: 80px;
  resize: vertical;
}

.generate-btn {
  width: 100%;
  padding: 14px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: #0f766e;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.generate-btn:hover:not(:disabled) {
  background: #0d9488;
}

.generate-btn:disabled {
  background: #9fbdb9;
  color: #eef5f4;
  cursor: not-allowed;
}

/* 生成状态 */
.generating-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 32px 20px;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 3px solid #e5e7eb;
  border-top-color: #0f766e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.status-text {
  margin: 0;
  font-size: 15px;
  color: #64748b;
  text-align: center;
}

/* 响应式 */
@media (max-width: 900px) {
  .header-container {
    padding: 0 16px;
  }
  
  .header-nav {
    gap: 8px;
  }
  
  .nav-btn {
    padding: 8px 12px;
    font-size: 13px;
  }
  
  .welcome-text {
    display: none;
  }
}
</style>
