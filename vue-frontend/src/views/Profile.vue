<template>
  <div class="profile-studio-page">
    <header class="studio-topbar">
      <div class="title-group">
        <h1>Personal Studio</h1>
        <p>维护你的求职档案，简历生成会自动引用这里的数据</p>
      </div>
      <div class="toolbar-actions">
        <div class="completion-chip" :class="completionClass">完成度 {{ completionRate }}% · {{ completionLevelText }}</div>
        <el-button class="reload-btn" @click="loadProfile" :loading="reloading" :disabled="saving || reloading">
          {{ reloading ? '加载中...' : '重新加载' }}
        </el-button>
        <el-button type="primary" @click="saveProfile" :loading="saving">保存信息</el-button>
      </div>
    </header>

    <section class="studio-grid">
      <aside class="nav-pane">
        <div class="pane-card">
          <h3>信息导航</h3>
          <ul class="section-nav">
            <li v-for="item in sectionMeta" :key="item.key">
              <button class="section-nav-btn" :class="{ active: activeSection === item.key }" @click="scrollToSection(item.key)">
                {{ item.label }}
              </button>
            </li>
          </ul>
        </div>

        <div class="pane-card">
          <h3>填写提醒</h3>
          <ul class="tips-list">
            <li>手机号和邮箱建议至少填一项</li>
            <li>技能请用逗号分隔，便于 AI 提取</li>
            <li>期望薪资建议填写区间更准确</li>
          </ul>
          <ul v-if="missingHints.length" class="missing-list">
            <li v-for="item in missingHints" :key="item">待完善：{{ item }}</li>
          </ul>
        </div>
      </aside>

      <main ref="editorPaneRef" class="editor-pane">
        <section :ref="setSectionRef('basic')" class="editor-section">
          <div class="section-head">
            <h3>基本资料</h3>
          </div>
          <el-form label-width="92px">
            <el-form-item label="姓名">
              <el-input v-model="profile.name" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profile.gender">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="出生日期">
              <el-date-picker v-model="profile.birthDate" type="date" placeholder="选择出生日期" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profile.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profile.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="现居地址">
              <el-input v-model="profile.address" placeholder="请输入现居地址" />
            </el-form-item>
          </el-form>
        </section>

        <section :ref="setSectionRef('job')" class="editor-section">
          <div class="section-head">
            <h3>求职意向</h3>
          </div>
          <el-form label-width="92px">
            <el-form-item label="求职状态">
              <el-select v-model="profile.jobStatus" placeholder="请选择求职状态">
                <el-option label="在职-考虑机会" value="在职" />
                <el-option label="离职-随时到岗" value="离职" />
                <el-option label="应届生" value="应届生" />
              </el-select>
            </el-form-item>
            <el-form-item label="期望职位">
              <el-input v-model="profile.jobTitle" placeholder="如：Java开发工程师" />
            </el-form-item>
            <el-form-item label="期望城市">
              <el-input v-model="profile.jobCity" placeholder="如：北京、上海" />
            </el-form-item>
            <el-form-item label="期望薪资">
              <div class="salary-range">
                <el-input-number v-model="profile.salaryMin" :min="1" :max="200" placeholder="最低" />
                <span class="salary-separator">-</span>
                <el-input-number v-model="profile.salaryMax" :min="1" :max="200" placeholder="最高" />
                <span class="salary-unit">K</span>
              </div>
            </el-form-item>
          </el-form>
        </section>

        <section :ref="setSectionRef('education')" class="editor-section">
          <div class="section-head">
            <h3>教育背景</h3>
          </div>
          <el-form label-width="92px">
            <el-form-item label="毕业院校">
              <el-input v-model="profile.school" placeholder="请输入毕业院校" />
            </el-form-item>
            <el-form-item label="专业">
              <el-input v-model="profile.major" placeholder="请输入专业" />
            </el-form-item>
            <el-form-item label="学历">
              <el-select v-model="profile.degree" placeholder="请选择学历">
                <el-option label="大专" value="大专" />
                <el-option label="本科" value="本科" />
                <el-option label="硕士" value="硕士" />
                <el-option label="博士" value="博士" />
              </el-select>
            </el-form-item>
            <el-form-item label="毕业时间">
              <el-date-picker v-model="profile.graduationDate" type="date" placeholder="选择毕业时间" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-form>
        </section>

        <section :ref="setSectionRef('skills')" class="editor-section">
          <div class="section-head">
            <h3>技能与自我介绍</h3>
          </div>
          <el-form label-width="92px">
            <el-form-item label="技能标签">
              <el-input
                v-model="profile.skills"
                type="textarea"
                :rows="4"
                placeholder="请输入技能，用逗号分隔，如：Java, Spring Boot, Vue"
              />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input
                v-model="profile.selfIntroduction"
                type="textarea"
                :rows="6"
                placeholder="介绍你的核心经验、项目亮点和个人优势"
              />
            </el-form-item>
          </el-form>
        </section>
      </main>

      <aside class="preview-pane">
        <div class="preview-toolbar">
          <span>实时摘要</span>
          <span>{{ completionRate }}% · {{ completionLevelText }}</span>
        </div>
        <div class="preview-scroll">
          <article class="profile-card">
            <h2>{{ profile.name || '未填写姓名' }}</h2>
            <p>{{ profile.jobTitle || '未填写期望职位' }}</p>
            <div class="meta-line">{{ profile.phone || '未填写手机号' }} · {{ profile.email || '未填写邮箱' }}</div>

            <section class="info-group">
              <h4>求职意向</h4>
              <p>状态：{{ profile.jobStatus || '未填写' }}</p>
              <p>城市：{{ profile.jobCity || '未填写' }}</p>
              <p>薪资：{{ salaryDisplay }}</p>
            </section>

            <section class="info-group">
              <h4>教育背景</h4>
              <p>{{ profile.degree || '未填写学历' }} · {{ profile.school || '未填写院校' }}</p>
              <p>{{ profile.major || '未填写专业' }}</p>
            </section>

            <section class="info-group">
              <h4>技能标签</h4>
              <p class="skills-preview">{{ profile.skills || '未填写技能' }}</p>
            </section>

            <section class="info-group">
              <h4>自我介绍</h4>
              <p>{{ profile.selfIntroduction || '未填写' }}</p>
            </section>
          </article>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';

const API_BASE_URL = 'http://localhost:9090';

const saving = ref(false);
const reloading = ref(false);
const activeSection = ref('basic');
const editorPaneRef = ref(null);
const sectionElements = ref({});

const sectionMeta = [
  { key: 'basic', label: '基本资料' },
  { key: 'job', label: '求职意向' },
  { key: 'education', label: '教育背景' },
  { key: 'skills', label: '技能与自我介绍' }
];

const profile = ref({
  name: '',
  gender: '',
  birthDate: null,
  phone: '',
  email: '',
  address: '',
  jobStatus: '',
  jobTitle: '',
  jobCity: '',
  salaryMin: null,
  salaryMax: null,
  school: '',
  major: '',
  degree: '',
  graduationDate: null,
  skills: '',
  selfIntroduction: ''
});

const createProfileFromApiData = (data) => ({
  name: data.name || '',
  gender: data.gender || '',
  birthDate: data.birthDate || null,
  phone: data.phone || '',
  email: data.email || '',
  address: data.address || '',
  jobStatus: data.jobStatus || '',
  jobTitle: data.jobTitle || '',
  jobCity: data.jobCity || '',
  salaryMin: data.salaryMin || null,
  salaryMax: data.salaryMax || null,
  school: data.school || '',
  major: data.major || '',
  degree: data.degree || '',
  graduationDate: data.graduationDate || null,
  skills: data.skills || '',
  selfIntroduction: data.selfIntroduction || ''
});

const fetchJson = async (url, options) => {
  const response = await fetch(url, options);
  return response.json();
};

const salaryDisplay = computed(() => {
  if (profile.value.salaryMin && profile.value.salaryMax) {
    return `${profile.value.salaryMin}K - ${profile.value.salaryMax}K`;
  }
  if (profile.value.salaryMin) {
    return `${profile.value.salaryMin}K以上`;
  }
  if (profile.value.salaryMax) {
    return `${profile.value.salaryMax}K以内`;
  }
  return '未填写';
});

const hasText = (value) => value !== null && value !== undefined && String(value).trim() !== '';

const completionRules = [
  { label: '姓名', weight: 15, done: () => hasText(profile.value.name) },
  { label: '联系方式（手机或邮箱）', weight: 15, done: () => hasText(profile.value.phone) || hasText(profile.value.email) },
  { label: '求职状态', weight: 8, done: () => hasText(profile.value.jobStatus) },
  { label: '期望职位', weight: 12, done: () => hasText(profile.value.jobTitle) },
  { label: '期望城市', weight: 8, done: () => hasText(profile.value.jobCity) },
  {
    label: '期望薪资',
    weight: 8,
    done: () => Boolean(profile.value.salaryMin || profile.value.salaryMax)
  },
  { label: '毕业院校', weight: 8, done: () => hasText(profile.value.school) },
  { label: '专业', weight: 8, done: () => hasText(profile.value.major) },
  { label: '学历', weight: 6, done: () => hasText(profile.value.degree) },
  { label: '技能标签', weight: 6, done: () => hasText(profile.value.skills) },
  { label: '个人简介', weight: 6, done: () => hasText(profile.value.selfIntroduction) }
];

const completionRateRaw = computed(() => {
  const total = completionRules.reduce((sum, item) => sum + item.weight, 0);
  const scored = completionRules.reduce((sum, item) => sum + (item.done() ? item.weight : 0), 0);
  return Math.round((scored / total) * 100);
});

const completionRate = computed(() => {
  return completionRateRaw.value;
});

const completionLevelText = computed(() => {
  if (completionRate.value >= 85) return '优秀';
  if (completionRate.value >= 65) return '良好';
  if (completionRate.value >= 45) return '一般';
  return '待完善';
});

const completionClass = computed(() => {
  if (completionRate.value >= 85) return 'is-excellent';
  if (completionRate.value >= 65) return 'is-good';
  if (completionRate.value >= 45) return 'is-fair';
  return 'is-low';
});

const missingHints = computed(() => completionRules.filter((item) => !item.done()).slice(0, 4).map((item) => item.label));

const setSectionRef = (sectionKey) => (el) => {
  if (el) {
    sectionElements.value[sectionKey] = el;
  }
};

const scrollToSection = (sectionKey) => {
  activeSection.value = sectionKey;
  const target = sectionElements.value[sectionKey];
  if (!target) return;

  const editorPane = editorPaneRef.value;
  if (editorPane) {
    const relativeTop = target.offsetTop - editorPane.offsetTop - 10;
    editorPane.scrollTo({ top: Math.max(0, relativeTop), behavior: 'smooth' });
    return;
  }

  target.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const loadProfile = async () => {
  if (reloading.value) {
    return;
  }

  const userId = localStorage.getItem('userId');
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }

  try {
    reloading.value = true;
    const result = await fetchJson(`${API_BASE_URL}/api/profile/${userId}`);
    if (result.success && result.data) {
      profile.value = createProfileFromApiData(result.data);
    }
  } catch (error) {
    ElMessage.error('加载个人信息失败');
    console.error('加载个人信息失败:', error);
  } finally {
    reloading.value = false;
  }
};

const saveProfile = async () => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }

  if (profile.value.salaryMin && profile.value.salaryMax && profile.value.salaryMin > profile.value.salaryMax) {
    ElMessage.warning('期望薪资范围不正确，请检查');
    return;
  }

  try {
    saving.value = true;

    const result = await fetchJson(`${API_BASE_URL}/api/profile/save`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId,
        ...profile.value
      })
    });

    if (result.success) {
      ElMessage.success('保存成功');
    } else {
      throw new Error(result.error || '保存失败');
    }
  } catch (error) {
    ElMessage.error(error.message || '保存失败');
    console.error('保存个人信息失败:', error);
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  loadProfile();
});
</script>

<style scoped>
.profile-studio-page {
  min-height: calc(100vh - 56px);
  background:
    radial-gradient(circle at 15% 12%, rgba(254, 240, 138, 0.25), transparent 26%),
    radial-gradient(circle at 80% 10%, rgba(153, 246, 228, 0.22), transparent 28%),
    #f4f8f5;
  color: #1f2937;
  font-family: 'Source Han Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.studio-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 22px;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
  position: sticky;
  top: 0;
  z-index: 12;
}

.title-group h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.title-group p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #475569;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

:deep(.toolbar-actions .el-button + .el-button) {
  margin-left: 0;
}

.completion-chip {
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.completion-chip.is-excellent {
  border-color: #86efac;
  background: #dcfce7;
  color: #166534;
}

.completion-chip.is-good {
  border-color: #99f6e4;
  background: #ccfbf1;
  color: #0f766e;
}

.completion-chip.is-fair {
  border-color: #fde68a;
  background: #fef9c3;
  color: #a16207;
}

.completion-chip.is-low {
  border-color: #fecaca;
  background: #fee2e2;
  color: #b91c1c;
}

:deep(.toolbar-actions .el-button) {
  border-radius: 999px;
  font-weight: 600;
  border-color: #d4dde8;
  transition: all 0.2s ease;
}

:deep(.toolbar-actions .el-button--default) {
  background: #ffffff;
  color: #445264;
}

:deep(.toolbar-actions .reload-btn) {
  border-color: #b9cad8;
  background: #f8fbff;
  color: #34506a;
}

:deep(.toolbar-actions .reload-btn:hover) {
  border-color: #8fb2cf;
  background: #edf5ff;
}

:deep(.toolbar-actions .el-button--primary) {
  background: #2f8f86;
  border-color: #2f8f86;
}

:deep(.toolbar-actions .el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
}

:deep(.toolbar-actions .el-button:active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.12);
}

.studio-grid {
  display: grid;
  grid-template-columns: 240px minmax(520px, 1fr) 380px;
  gap: 16px;
  padding: 16px;
  align-items: start;
}

.nav-pane {
  position: sticky;
  top: 76px;
  display: grid;
  gap: 12px;
}

.pane-card,
.editor-section,
.preview-pane {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.pane-card {
  padding: 14px;
}

.pane-card h3 {
  margin: 0 0 12px;
  font-size: 14px;
}

.section-nav {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 8px;
}

.section-nav-btn {
  width: 100%;
  border: 1px solid #d6dee8;
  background: #fff;
  border-radius: 10px;
  text-align: left;
  padding: 8px 10px;
  cursor: pointer;
  color: #334155;
}

.section-nav-btn.active,
.section-nav-btn:hover {
  border-color: #0f766e;
  color: #0f766e;
  background: #f0fdfa;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.7;
  font-size: 13px;
}

.missing-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #0f766e;
  line-height: 1.6;
  font-size: 12px;
}

.editor-pane {
  display: grid;
  gap: 12px;
  max-height: calc(100vh - 92px);
  overflow-y: auto;
  padding-right: 4px;
}

.editor-pane::-webkit-scrollbar {
  width: 8px;
}

.editor-pane::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

.editor-section {
  padding: 16px;
  scroll-margin-top: 90px;
}

:deep(.el-input__wrapper) {
  background-color: #ffffff;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0f766e inset;
}

:deep(.el-input__inner:-webkit-autofill),
:deep(.el-input__inner:-webkit-autofill:hover),
:deep(.el-input__inner:-webkit-autofill:focus) {
  -webkit-text-fill-color: #1f2937;
  -webkit-box-shadow: 0 0 0 1000px #ffffff inset;
  box-shadow: 0 0 0 1000px #ffffff inset;
  transition: background-color 9999s ease-out 0s;
}

.section-head {
  margin-bottom: 8px;
}

.section-head h3 {
  margin: 0;
  font-size: 16px;
}

.salary-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.salary-separator,
.salary-unit {
  color: #64748b;
  font-size: 13px;
}

.preview-pane {
  position: sticky;
  top: 76px;
  height: calc(100vh - 92px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
  font-size: 13px;
  color: #475569;
}

.preview-scroll {
  flex: 1;
  overflow: auto;
  padding: 14px;
  background: #edf2f7;
}

.profile-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #dbe3ee;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  padding: 16px;
}

.profile-card h2 {
  margin: 0;
  font-size: 24px;
}

.profile-card > p {
  margin: 6px 0 0;
  color: #475569;
}

.meta-line {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.info-group {
  margin-top: 14px;
}

.info-group h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #0f766e;
}

.info-group p {
  margin: 4px 0;
  font-size: 13px;
  line-height: 1.6;
}

.skills-preview {
  word-break: break-word;
}

@media (max-width: 1360px) {
  .studio-grid {
    grid-template-columns: 220px 1fr;
  }

  .preview-pane {
    grid-column: 1 / -1;
    position: relative;
    top: 0;
    height: auto;
  }

  .editor-pane {
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }
}

@media (max-width: 900px) {
  .studio-topbar {
    position: static;
  }

  .studio-grid {
    grid-template-columns: 1fr;
  }

  .nav-pane,
  .preview-pane {
    position: static;
    top: 0;
  }
}
</style>
