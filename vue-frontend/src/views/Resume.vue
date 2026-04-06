<template>
  <div class="resume-builder-page">
    <div v-if="loading" class="loading-overlay">
      <div class="loading-text">{{ loadingText || '处理中...' }}</div>
    </div>

    <header class="topbar">
      <div class="title-group">
        <div class="title-row">
          <h1>Resume Studio</h1>
          <span class="title-tag">Builder</span>
        </div>
        <p>实时编辑、模板切换、AI 优化与导出一体化</p>
      </div>
      <div class="top-actions">
        <el-button type="warning" @click="showOptimizeAllDialog" :loading="optimizingAll">
          <el-icon><MagicStick /></el-icon>
          AI优化整份简历
        </el-button>
        <el-button type="success" @click="saveResumeChanges" :loading="updating">更新简历</el-button>
        <el-button
          class="import-btn"
          @click="openImportDialog"
          :loading="importingPdf"
          :disabled="loading || updating || optimizingAll || exporting || importingPdf"
        >
          {{ importingPdf ? '导入中...' : '导入简历' }}
        </el-button>
        <el-button type="primary" @click="openExportDialog">导出简历</el-button>
      </div>
    </header>

    <section class="builder-grid">
      <aside class="nav-pane">
        <div class="pane-card">
          <button class="pane-header" @click="toggleLeftPanel('nav')">
            <h3>章节导航</h3>
            <span class="pane-toggle">{{ isLeftPanelOpen('nav') ? '−' : '+' }}</span>
          </button>
          <div v-show="isLeftPanelOpen('nav')" class="pane-body">
            <ul class="section-nav">
              <li v-for="item in sectionMetaList" :key="item.key">
                <button
                  class="section-nav-btn"
                  :class="{ active: activeSection === item.key }"
                  @click="scrollToSection(item.key)"
                >
                  {{ item.label }}
                </button>
              </li>
            </ul>
          </div>
        </div>

        <div class="pane-card">
          <button class="pane-header" @click="toggleLeftPanel('display')">
            <h3>显示设置</h3>
            <span class="pane-toggle">{{ isLeftPanelOpen('display') ? '−' : '+' }}</span>
          </button>
          <div v-show="isLeftPanelOpen('display')" class="pane-body">
            <el-checkbox-group v-model="selectedModules" class="module-switches">
              <el-checkbox v-for="item in sectionMetaList" :key="item.label" :label="item.label" />
            </el-checkbox-group>
            <div class="zoom-control">
              <span>预览缩放</span>
              <el-slider v-model="previewZoom" :min="70" :max="120" :step="5" />
            </div>
          </div>
        </div>

        <div class="pane-card">
          <button class="pane-header" @click="toggleLeftPanel('template')">
            <h3>模板与顺序</h3>
            <span class="pane-toggle">{{ isLeftPanelOpen('template') ? '−' : '+' }}</span>
          </button>
          <div v-show="isLeftPanelOpen('template')" class="pane-body">
            <div class="template-switch">
              <button class="template-btn" :class="{ active: activeTemplate === 'classic' }" @click="activeTemplate = 'classic'">Classic</button>
              <button class="template-btn" :class="{ active: activeTemplate === 'modern' }" @click="activeTemplate = 'modern'">Modern</button>
              <button class="template-btn" :class="{ active: activeTemplate === 'ats' }" @click="activeTemplate = 'ats'">ATS</button>
            </div>
            <h3 class="order-title">章节顺序</h3>
            <ul class="order-list">
              <li
                v-for="(item, index) in sectionMetaList"
                :key="item.key"
                class="order-item"
                :class="{ dragging: dragIndex === index }"
                draggable="true"
                @dragstart="onDragStart(index)"
                @dragover.prevent
                @drop="onDrop(index)"
              >
                <span>{{ item.label }}</span>
                <div class="order-actions">
                  <button class="order-btn" :disabled="index === 0" @click="moveSection(index, -1)">↑</button>
                  <button class="order-btn" :disabled="index === sectionMetaList.length - 1" @click="moveSection(index, 1)">↓</button>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </aside>

      <main ref="editorPaneRef" class="editor-pane">
        <section :ref="setSectionRef('basic')" class="editor-section">
          <div class="section-head">
            <h3>基本信息</h3>
          </div>
          <el-form label-width="90px">
            <el-form-item label="姓名"><el-input v-model="resume.name" placeholder="请输入姓名" /></el-form-item>
            <el-form-item label="电话"><el-input v-model="resume.phone" placeholder="请输入电话" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="resume.email" placeholder="请输入邮箱" /></el-form-item>
            <el-form-item label="头像">
              <div class="avatar-uploader" @click="triggerAvatarSelect">
                <img v-if="resume.avatar" :src="resume.avatar" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </div>
              <input
                ref="avatarInputRef"
                class="avatar-file-input"
                type="file"
                accept="image/png,image/jpeg,image/jpg,image/webp"
                @change="handleAvatarInputChange"
              />
            </el-form-item>
          </el-form>
        </section>

        <section :ref="setSectionRef('intent')" class="editor-section">
          <div class="section-head">
            <h3>求职意向</h3>
          </div>
          <el-form label-width="90px">
            <el-form-item label="状态">
              <el-select v-model="resume.jobStatus" placeholder="请选择当前状态">
                <el-option label="在职" value="在职" />
                <el-option label="离职" value="离职" />
                <el-option label="应届生" value="应届生" />
              </el-select>
            </el-form-item>
            <el-form-item label="职位"><el-input v-model="resume.jobTitle" /></el-form-item>
            <el-form-item label="薪资"><el-input v-model="resume.salaryExpectation" /></el-form-item>
          </el-form>
        </section>

        <section :ref="setSectionRef('education')" class="editor-section">
          <div class="section-head">
            <h3>教育经历</h3>
          </div>
          <div v-for="(edu, index) in resume.educationList" :key="`edu-${index}`" class="experience-card">
            <div class="experience-card-head">
              <span>教育经历 {{ index + 1 }}</span>
              <el-button v-if="resume.educationList.length > 1" size="small" type="danger" plain @click="removeEducationItem(index)">删除</el-button>
            </div>
            <el-form label-width="90px">
              <el-form-item label="学校"><el-input v-model="edu.school" /></el-form-item>
              <el-form-item label="专业"><el-input v-model="edu.major" /></el-form-item>
              <el-form-item label="学历">
                <el-select v-model="edu.degree" placeholder="请选择学历">
                  <el-option label="本科" value="本科" />
                  <el-option label="硕士" value="硕士" />
                  <el-option label="博士" value="博士" />
                </el-select>
              </el-form-item>
              <el-form-item label="在读时间">
                <el-date-picker v-model="edu.studyPeriod" type="daterange" start-placeholder="开始" end-placeholder="结束" />
              </el-form-item>
            </el-form>
          </div>
          <div class="experience-add-row">
            <el-button class="add-experience-btn" size="small" type="success" plain @click="addEducationItem">+</el-button>
          </div>
        </section>

        <section :ref="setSectionRef('profession')" class="editor-section">
          <div class="section-head">
            <h3>专业技能</h3>
            <el-button size="small" type="primary" plain @click="handleOptimizeProfession" :loading="optimizingModule === 'profession'">
              <el-icon><MagicStick /></el-icon>
              AI润色
            </el-button>
          </div>
          <el-form label-width="90px">
            <el-form-item label="技能描述">
              <el-input v-model="resume.profession.skill" type="textarea" :rows="4" />
            </el-form-item>
          </el-form>
        </section>

        <section :ref="setSectionRef('work')" class="editor-section">
          <div class="section-head">
            <h3>工作经历</h3>
            <el-button size="small" type="primary" plain @click="handleOptimizeWork" :loading="optimizingModule === 'work'">
              <el-icon><MagicStick /></el-icon>
              AI润色
            </el-button>
          </div>
          <div v-for="(work, index) in resume.workList" :key="`work-${index}`" class="experience-card">
            <div class="experience-card-head">
              <span>工作经历 {{ index + 1 }}</span>
              <el-button v-if="resume.workList.length > 1" size="small" type="danger" plain @click="removeWorkItem(index)">删除</el-button>
            </div>
            <el-form label-width="90px">
              <el-form-item label="公司"><el-input v-model="work.company" /></el-form-item>
              <el-form-item label="部门"><el-input v-model="work.department" /></el-form-item>
              <el-form-item label="职位"><el-input v-model="work.position" /></el-form-item>
              <el-form-item label="在职时间">
                <el-date-picker v-model="work.period" type="daterange" start-placeholder="开始" end-placeholder="结束" />
              </el-form-item>
              <el-form-item label="工作内容">
                <el-input v-model="work.details" type="textarea" :rows="4" />
              </el-form-item>
            </el-form>
          </div>
          <div class="experience-add-row">
            <el-button class="add-experience-btn" size="small" type="success" plain @click="addWorkItem">+</el-button>
          </div>
        </section>

        <section :ref="setSectionRef('project')" class="editor-section">
          <div class="section-head">
            <h3>项目经历</h3>
            <el-button size="small" type="primary" plain @click="handleOptimizeProject" :loading="optimizingModule === 'project'">
              <el-icon><MagicStick /></el-icon>
              AI润色
            </el-button>
          </div>
          <div v-for="(project, index) in resume.projectList" :key="`project-${index}`" class="experience-card">
            <div class="experience-card-head">
              <span>项目经历 {{ index + 1 }}</span>
              <el-button v-if="resume.projectList.length > 1" size="small" type="danger" plain @click="removeProjectItem(index)">删除</el-button>
            </div>
            <el-form label-width="90px">
              <el-form-item label="项目名"><el-input v-model="project.name" /></el-form-item>
              <el-form-item label="项目时间">
                <el-date-picker v-model="project.period" type="daterange" start-placeholder="开始" end-placeholder="结束" />
              </el-form-item>
              <el-form-item label="项目描述">
                <el-input v-model="project.details" type="textarea" :rows="4" />
              </el-form-item>
            </el-form>
          </div>
          <div class="experience-add-row">
            <el-button class="add-experience-btn" size="small" type="success" plain @click="addProjectItem">+</el-button>
          </div>
        </section>

        <section :ref="setSectionRef('award')" class="editor-section">
          <div class="section-head">
            <h3>荣誉奖项</h3>
            <el-button size="small" type="primary" plain @click="handleOptimizeAward" :loading="optimizingModule === 'award'">
              <el-icon><MagicStick /></el-icon>
              AI润色
            </el-button>
          </div>
          <el-form label-width="90px">
            <el-form-item label="奖项内容">
              <el-input v-model="resume.award.details" type="textarea" :rows="3" />
            </el-form-item>
          </el-form>
        </section>
      </main>

      <aside class="preview-pane">
        <div class="preview-toolbar">
          <span>实时预览</span>
          <span>{{ previewZoom }}%</span>
        </div>
        <div class="preview-scroll">
          <article class="resume-paper" :class="`template-${activeTemplate}`" :style="previewScaleStyle">
            <header class="paper-header">
              <div>
                <h2>{{ resume.name || '你的名字' }}</h2>
                <p>{{ resume.jobTitle || '目标岗位' }}</p>
                <p>{{ headerIntentText }}</p>
                <p>{{ resume.phone || '电话' }} · {{ resume.email || '邮箱' }}</p>
              </div>
              <div v-if="resume.avatar" class="paper-avatar" :style="paperAvatarStyle"></div>
            </header>

            <section v-for="sectionLabel in orderedPreviewSections" :key="sectionLabel" class="paper-section">
              <h4>{{ sectionLabel }}</h4>
              <template v-if="sectionLabel === '教育经历'">
                <div v-for="(edu, index) in resume.educationList" :key="`preview-edu-${index}`" class="paper-entry">
                  <div class="paper-item-row">
                    <p class="paper-item-main">{{ `${edu.school || '未填写学校'} · ${edu.major || '未填写专业'}` }}</p>
                    <p class="paper-item-period">{{ `${formatDate(edu.studyPeriod?.[0])} - ${formatDate(edu.studyPeriod?.[1])}` }}</p>
                  </div>
                  <p>{{ edu.degree || '未填写学历' }}</p>
                </div>
              </template>
              <template v-else-if="sectionLabel === '工作经历'">
                <div v-for="(work, index) in resume.workList" :key="`preview-work-${index}`" class="paper-entry">
                  <div class="paper-item-row">
                    <p class="paper-item-main">{{ `${work.company || '未填写公司'} / ${work.position || '未填写职位'}` }}</p>
                    <p class="paper-item-period">{{ `${formatDate(work.period?.[0])} - ${formatDate(work.period?.[1])}` }}</p>
                  </div>
                  <p v-for="(line, idx) in splitMultilineText(work.details, '未填写工作内容')" :key="`work-detail-${index}-${idx}`">{{ line }}</p>
                </div>
              </template>
              <template v-else-if="sectionLabel === '项目经历'">
                <div v-for="(project, index) in resume.projectList" :key="`preview-project-${index}`" class="paper-entry">
                  <div class="paper-item-row">
                    <p class="paper-item-main">{{ project.name || '未填写项目名称' }}</p>
                    <p class="paper-item-period">{{ `${formatDate(project.period?.[0])} - ${formatDate(project.period?.[1])}` }}</p>
                  </div>
                  <p v-for="(line, idx) in splitMultilineText(project.details, '未填写项目描述')" :key="`project-detail-${index}-${idx}`">{{ line }}</p>
                </div>
              </template>
              <template v-else>
                <p v-for="(line, idx) in getSectionLines(sectionLabel)" :key="`${sectionLabel}-${idx}`">{{ line }}</p>
              </template>
            </section>
          </article>
        </div>
      </aside>
    </section>

    <el-dialog v-model="showImportModal" title="导入 PDF 简历" width="560px" append-to-body>
      <div class="import-panel">
        <p class="import-tip">支持点击上传或拖拽 PDF 文件。可重复上传，识别到的字段会实时更新到当前表单。</p>
        <input
          ref="pdfInputRef"
          type="file"
          accept="application/pdf"
          class="hidden-file-input"
          @change="onPdfFileChange"
        />
        <div
          class="drop-zone"
          :class="{ active: dropActive, loading: importingPdf }"
          @click="triggerPdfSelect"
          @dragenter.prevent="onDragEnter"
          @dragover.prevent="onDragOver"
          @dragleave.prevent="onDragLeave"
          @drop.prevent="onDropPdf"
        >
          <div class="drop-title">{{ importingPdf ? '正在解析 PDF...' : '点击或拖拽 PDF 到这里' }}</div>
          <div class="drop-subtitle">{{ importedFileName || '仅支持 .pdf 格式' }}</div>
        </div>

        <div v-if="importMatches.length" class="import-result">
          <h4>已识别字段</h4>
          <div class="match-list">
            <div v-for="item in importMatches" :key="item.label" class="match-item">
              <span class="match-label">{{ item.label }}</span>
              <span class="match-value">{{ item.value }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="showExportDialog" title="导出简历" width="460px" append-to-body>
      <div class="export-picker">
        <button
          type="button"
          class="export-option"
          :class="{ active: exportFormat === 'pdf' }"
          @click="exportFormat = 'pdf'"
        >
          <div class="option-badge option-badge-pdf">PDF</div>
          <div class="option-text">
            <div class="option-title">导出 PDF</div>
            <div class="option-desc">版式稳定，适合投递与打印</div>
          </div>
        </button>
        <button
          type="button"
          class="export-option"
          :class="{ active: exportFormat === 'word' }"
          @click="exportFormat = 'word'"
        >
          <div class="option-badge option-badge-word">DOCX</div>
          <div class="option-text">
            <div class="option-title">导出 Word</div>
            <div class="option-desc">继续修改内容，便于二次编辑</div>
          </div>
        </button>
        <button
          type="button"
          class="export-option"
          :class="{ active: exportFormat === 'json' }"
          @click="exportFormat = 'json'"
        >
          <div class="option-badge option-badge-json">JSON</div>
          <div class="option-text">
            <div class="option-title">导出 JSON</div>
            <div class="option-desc">数据备份与迁移，便于系统间传输</div>
          </div>
        </button>
      </div>
      <template #footer>
        <el-button @click="showExportDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmExport" :loading="exporting">确认导出</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showOptimizeModal" title="AI优化整份简历" width="640px" append-to-body class="optimize-dialog">
      <div class="optimize-form-wrap">
        <p class="optimize-tip">根据目标岗位与优化偏好，AI 将统一润色整份简历内容，风格与当前编辑主题保持一致。</p>

        <el-form label-width="92px" class="optimize-form">
          <el-form-item label="目标岗位">
            <el-input
              v-model="optimizeForm.targetPosition"
              placeholder="例如：Java后端开发工程师"
              clearable
            />
          </el-form-item>

          <el-form-item label="表达风格">
            <el-radio-group v-model="optimizeForm.tone" class="tone-group">
              <el-radio-button label="balanced">专业均衡</el-radio-button>
              <el-radio-button label="result">结果导向</el-radio-button>
              <el-radio-button label="technical">技术深度</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="重点模块">
            <el-checkbox-group v-model="optimizeForm.focusModules" class="focus-grid">
              <el-checkbox v-for="item in optimizeModuleOptions" :key="item" :label="item" />
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="优化强度">
            <div class="strength-row">
              <el-slider v-model="optimizeForm.rewriteStrength" :min="1" :max="3" :step="1" :show-tooltip="false" />
              <span class="strength-tag">{{ strengthLabel }}</span>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showOptimizeModal = false">取消</el-button>
        <el-button type="warning" @click="confirmOptimizeAll" :loading="optimizingAll">开始优化</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showOptimizeResultModal" title="优化完成" width="700px" append-to-body class="optimize-result-dialog">
      <div class="optimize-result-wrap">
        <p class="result-tip">AI 已生成优化建议，请确认要应用到简历中的模块。</p>

        <el-form label-width="92px" class="optimize-result-form">
          <el-form-item label="应用模块">
            <el-checkbox-group v-model="optimizeResultSelection" class="focus-grid">
              <el-checkbox v-for="item in optimizeResultCandidates" :key="item" :label="item" />
            </el-checkbox-group>
          </el-form-item>
        </el-form>

        <div class="result-preview-list" v-if="optimizeResultRows.length">
          <div v-for="row in optimizeResultRows" :key="row.label" class="result-preview-item">
            <div class="preview-head">
              <span class="preview-label">{{ row.label }}</span>
              <span class="preview-tag">{{ row.tag }}</span>
            </div>
            <p class="preview-text">{{ row.preview }}</p>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="cancelOptimizeResult">放弃</el-button>
        <el-button type="primary" @click="applyOptimizeResult">应用优化内容</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import html2pdf from 'html2pdf.js';
import {
  AlignmentType,
  BorderStyle,
  Document,
  HeadingLevel,
  ImageRun,
  Packer,
  Paragraph,
  Table,
  TableCell,
  TableRow,
  TextRun,
  VerticalAlign,
  WidthType
} from 'docx';
import { saveAs } from 'file-saver';
import { Plus, MagicStick } from '@element-plus/icons-vue';
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as pdfjsLib from 'pdfjs-dist';
import pdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url';

const API_BASE_URL = 'http://localhost:9090';
const RESUME_EDITOR_PREF_KEY = 'resumeEditorPreferences';
const RESUME_DATA_EVENT = 'resume-data-updated';

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker;

const sectionMetaList = ref([
  { key: 'basic', label: '基本信息' },
  { key: 'intent', label: '求职意向' },
  { key: 'education', label: '教育经历' },
  { key: 'profession', label: '专业技能' },
  { key: 'work', label: '工作经历' },
  { key: 'project', label: '项目经历' },
  { key: 'award', label: '荣誉奖项' }
]);

const selectedModules = ref(sectionMetaList.value.map((item) => item.label));
const activeSection = ref('basic');
const previewZoom = ref(100);
const activeTemplate = ref('classic');
const dragIndex = ref(-1);
const activeLeftPanel = ref('nav');
const editorPaneRef = ref(null);
const sectionElements = ref({});

const isLeftPanelOpen = (panel) => activeLeftPanel.value === panel;

const toggleLeftPanel = (panel) => {
  activeLeftPanel.value = activeLeftPanel.value === panel ? '' : panel;
};

const previewScaleStyle = computed(() => ({
  transform: `scale(${previewZoom.value / 100})`,
  transformOrigin: 'top center'
}));

const paperAvatarStyle = computed(() => {
  if (!resume.value.avatar) {
    return {};
  }
  return {
    backgroundImage: `url(${resume.value.avatar})`
  };
});

const headerIntentText = computed(() => {
  const parts = [
    resume.value.jobStatus && `状态：${resume.value.jobStatus}`,
    resume.value.jobTitle && `意向：${resume.value.jobTitle}`,
    resume.value.salaryExpectation && `薪资：${resume.value.salaryExpectation}`
  ].filter(Boolean);

  return parts.length ? parts.join('  |  ') : '求职意向待补充';
});

const orderedSelectedModules = computed(() =>
  sectionMetaList.value
    .map((item) => item.label)
    .filter((label) => selectedModules.value.includes(label))
);

const orderedPreviewSections = computed(() =>
  orderedSelectedModules.value.filter((label) => label !== '基本信息' && label !== '求职意向')
);

const moveSection = (index, direction) => {
  const targetIndex = index + direction;
  if (targetIndex < 0 || targetIndex >= sectionMetaList.value.length) {
    return;
  }
  const copied = [...sectionMetaList.value];
  const current = copied[index];
  copied[index] = copied[targetIndex];
  copied[targetIndex] = current;
  sectionMetaList.value = copied;
};

const onDragStart = (index) => {
  dragIndex.value = index;
};

const onDrop = (dropIndex) => {
  const from = dragIndex.value;
  dragIndex.value = -1;
  if (from < 0 || from === dropIndex) {
    return;
  }

  const copied = [...sectionMetaList.value];
  const [moved] = copied.splice(from, 1);
  copied.splice(dropIndex, 0, moved);
  sectionMetaList.value = copied;
};

const createEducationItem = () => ({ school: '', major: '', degree: '', studyPeriod: [] });
const createWorkItem = () => ({ company: '', department: '', position: '', period: [], details: '' });
const createProjectItem = () => ({ name: '', period: [], details: '' });

const normalizeEducationList = (education) => {
  if (Array.isArray(education) && education.length) return education;
  if (education && typeof education === 'object') return [education];
  return [createEducationItem()];
};

const normalizeWorkList = (work) => {
  if (Array.isArray(work) && work.length) return work;
  if (work && typeof work === 'object') return [work];
  return [createWorkItem()];
};

const normalizeProjectList = (project) => {
  if (Array.isArray(project) && project.length) return project;
  if (project && typeof project === 'object') return [project];
  return [createProjectItem()];
};

const resume = ref({
  name: '',
  phone: '',
  email: '',
  avatar: '',
  jobStatus: '',
  jobTitle: '',
  salaryExpectation: '',
  educationList: [createEducationItem()],
  profession: { skill: '' },
  workList: [createWorkItem()],
  projectList: [createProjectItem()],
  award: { details: '' }
});

const addEducationItem = () => {
  resume.value.educationList.push(createEducationItem());
};

const removeEducationItem = (index) => {
  if (resume.value.educationList.length <= 1) return;
  resume.value.educationList.splice(index, 1);
};

const addWorkItem = () => {
  resume.value.workList.push(createWorkItem());
};

const removeWorkItem = (index) => {
  if (resume.value.workList.length <= 1) return;
  resume.value.workList.splice(index, 1);
};

const addProjectItem = () => {
  resume.value.projectList.push(createProjectItem());
};

const removeProjectItem = (index) => {
  if (resume.value.projectList.length <= 1) return;
  resume.value.projectList.splice(index, 1);
};

const loading = ref(false);
const loadingText = ref('');
const updating = ref(false);
const optimizingAll = ref(false);
const showOptimizeModal = ref(false);
const optimizeModuleOptions = ['求职意向', '教育经历', '专业技能', '工作经历', '项目经历', '荣誉奖项'];
const optimizeForm = ref({
  targetPosition: '',
  tone: 'balanced',
  focusModules: [...optimizeModuleOptions],
  rewriteStrength: 2
});
const showOptimizeResultModal = ref(false);
const pendingOptimizedResume = ref(null);
const optimizeResultSelection = ref([]);
const optimizeResultCandidates = ref([]);
const optimizeResultRows = ref([]);
const optimizingModule = ref('');
const showExportDialog = ref(false);
const exportFormat = ref('pdf');
const exporting = ref(false);
const showImportModal = ref(false);
const importingPdf = ref(false);
const dropActive = ref(false);
const importedFileName = ref('');
const importMatches = ref([]);
const pdfInputRef = ref(null);

const strengthLabel = computed(() => {
  if (optimizeForm.value.rewriteStrength === 1) return '轻润色';
  if (optimizeForm.value.rewriteStrength === 3) return '深优化';
  return '标准';
});

const summarizePreview = (value, maxLen = 90) => {
  const text = cleanValue(String(value || '')).replace(/\s+/g, ' ');
  if (!text) return '无变化';
  if (text.length <= maxLen) return text;
  return `${text.slice(0, maxLen)}...`;
};

const collectOptimizeRows = (optimizedData, selectedModules) => {
  const rows = [];
  const includes = (name) => selectedModules.includes(name);
  const firstEdu = Array.isArray(optimizedData?.education) ? optimizedData.education[0] : optimizedData?.education;
  const firstWork = Array.isArray(optimizedData?.work) ? optimizedData.work[0] : optimizedData?.work;
  const firstProject = Array.isArray(optimizedData?.project) ? optimizedData.project[0] : optimizedData?.project;

  if (includes('求职意向')) {
    const intentText = [optimizedData?.jobStatus, optimizedData?.jobTitle, optimizedData?.salaryExpectation].filter(Boolean).join(' | ');
    if (intentText) {
      rows.push({ label: '求职意向', tag: '结构优化', preview: summarizePreview(intentText) });
    }
  }

  if (includes('教育经历') && firstEdu) {
    const eduText = [firstEdu.school, firstEdu.major, firstEdu.degree].filter(Boolean).join(' | ');
    if (eduText) {
      rows.push({ label: '教育经历', tag: '信息规整', preview: summarizePreview(eduText) });
    }
  }

  if (includes('专业技能') && optimizedData?.profession?.skill) {
    rows.push({ label: '专业技能', tag: '表达增强', preview: summarizePreview(optimizedData.profession.skill) });
  }

  if (includes('工作经历') && firstWork) {
    rows.push({ label: '工作经历', tag: '成果化描述', preview: summarizePreview(firstWork.details || firstWork.position || firstWork.company) });
  }

  if (includes('项目经历') && firstProject) {
    rows.push({ label: '项目经历', tag: '亮点提炼', preview: summarizePreview(firstProject.details || firstProject.name) });
  }

  if (includes('荣誉奖项') && optimizedData?.award?.details) {
    rows.push({ label: '荣誉奖项', tag: '措辞润色', preview: summarizePreview(optimizedData.award.details) });
  }

  return rows;
};

const loadEditorPreferences = () => {
  try {
    const raw = localStorage.getItem(RESUME_EDITOR_PREF_KEY);
    if (!raw) return;
    const pref = JSON.parse(raw);

    if (Array.isArray(pref.sectionOrder) && pref.sectionOrder.length === sectionMetaList.value.length) {
      const map = new Map(sectionMetaList.value.map((item) => [item.key, item]));
      const sorted = pref.sectionOrder.map((key) => map.get(key)).filter(Boolean);
      if (sorted.length === sectionMetaList.value.length) {
        sectionMetaList.value = sorted;
      }
    }

    if (Array.isArray(pref.selectedModules) && pref.selectedModules.length > 0) {
      selectedModules.value = pref.selectedModules;
    }

    if (typeof pref.previewZoom === 'number' && pref.previewZoom >= 70 && pref.previewZoom <= 120) {
      previewZoom.value = pref.previewZoom;
    }

    if (typeof pref.activeTemplate === 'string' && ['classic', 'modern', 'ats'].includes(pref.activeTemplate)) {
      activeTemplate.value = pref.activeTemplate;
    }
  } catch {
    // ignore invalid local settings
  }
};

const saveEditorPreferences = () => {
  const payload = {
    sectionOrder: sectionMetaList.value.map((item) => item.key),
    selectedModules: selectedModules.value,
    previewZoom: previewZoom.value,
    activeTemplate: activeTemplate.value
  };
  localStorage.setItem(RESUME_EDITOR_PREF_KEY, JSON.stringify(payload));
};

const setSectionRef = (sectionKey) => (el) => {
  if (el) {
    sectionElements.value[sectionKey] = el;
  }
};

const avatarInputRef = ref(null);

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

  const mainScroller = document.querySelector('.app-main');
  if (mainScroller) {
    const targetRect = target.getBoundingClientRect();
    const scrollerRect = mainScroller.getBoundingClientRect();
    const nextTop = mainScroller.scrollTop + (targetRect.top - scrollerRect.top) - 84;
    mainScroller.scrollTo({ top: Math.max(0, nextTop), behavior: 'smooth' });
    return;
  }

  target.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const postJson = async (url, payload) => {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  return response.json();
};

const buildResumePayload = () => ({
  name: resume.value.name,
  phone: resume.value.phone,
  email: resume.value.email,
  avatar: resume.value.avatar,
  jobStatus: resume.value.jobStatus,
  jobTitle: resume.value.jobTitle,
  salaryExpectation: resume.value.salaryExpectation,
  education: resume.value.educationList,
  profession: resume.value.profession,
  work: resume.value.workList,
  project: resume.value.projectList,
  award: resume.value.award
});

const handleAvatarUpload = (file) => {
  if (!file) return false;
  const isImage = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'].includes(file.type);
  if (!isImage) {
    ElMessage.warning('仅支持 JPG、PNG、WEBP 图片');
    return false;
  }

  const maxSize = 2 * 1024 * 1024;
  if (file.size > maxSize) {
    ElMessage.warning('头像大小不能超过 2MB');
    return false;
  }

  const reader = new FileReader();
  reader.onload = (e) => {
    resume.value.avatar = e.target.result;
  };
  reader.readAsDataURL(file);
  return false;
};

const triggerAvatarSelect = () => {
  avatarInputRef.value?.click();
};

const handleAvatarInputChange = (event) => {
  const file = event?.target?.files?.[0];
  handleAvatarUpload(file);
  if (event?.target) {
    event.target.value = '';
  }
};

const formatDate = (date) => {
  if (!date) return '未知日期';
  const d = new Date(date);
  return Number.isNaN(d.getTime()) ? '未知日期' : d.toISOString().split('T')[0];
};

const splitMultilineText = (value, emptyFallback = '未填写') => {
  const normalized = String(value || '').replace(/\r\n/g, '\n').trim();
  if (!normalized) return [emptyFallback];
  return normalized
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
};

const getResumeData = (module) => {
  const primaryEducation = resume.value.educationList[0] || createEducationItem();
  const primaryWork = resume.value.workList[0] || createWorkItem();
  const primaryProject = resume.value.projectList[0] || createProjectItem();

  switch (module) {
    case '基本信息':
      return `姓名：${resume.value.name || '未填写'}\n电话：${resume.value.phone || '未填写'}\n邮箱：${resume.value.email || '未填写'}`;
    case '求职意向':
      return `求职状态：${resume.value.jobStatus || '未填写'}\n期望职位：${resume.value.jobTitle || '未填写'}\n期望薪资：${resume.value.salaryExpectation || '未填写'}`;
    case '教育经历':
      return `学校：${primaryEducation.school || '未填写'}\n专业：${primaryEducation.major || '未填写'}\n学历：${primaryEducation.degree || '未填写'}`;
    case '专业技能':
      return `技能：${resume.value.profession.skill || '未填写'}`;
    case '工作经历':
      return `公司：${primaryWork.company || '未填写'}\n职位：${primaryWork.position || '未填写'}\n内容：${primaryWork.details || '未填写'}`;
    case '项目经历':
      return `项目：${primaryProject.name || '未填写'}\n描述：${primaryProject.details || '未填写'}`;
    case '荣誉奖项':
      return `奖项：${resume.value.award.details || '未填写'}`;
    default:
      return '暂无数据';
  }
};

const getSectionLines = (module) => {
  switch (module) {
    case '求职意向':
      return [
        `状态：${resume.value.jobStatus || '未填写'}`,
        `职位：${resume.value.jobTitle || '未填写'}`,
        `薪资：${resume.value.salaryExpectation || '未填写'}`
      ];
    case '教育经历':
      return resume.value.educationList.flatMap((edu) => [
        `${edu.school || '未填写学校'} · ${edu.major || '未填写专业'}`,
        `${edu.degree || '未填写学历'}`,
        `${formatDate(edu.studyPeriod?.[0])} - ${formatDate(edu.studyPeriod?.[1])}`
      ]);
    case '专业技能':
      return splitMultilineText(resume.value.profession.skill, '未填写');
    case '工作经历':
      return resume.value.workList.flatMap((work) => [
        `${work.company || '未填写公司'} / ${work.position || '未填写职位'}`,
        `${formatDate(work.period?.[0])} - ${formatDate(work.period?.[1])}`,
        ...splitMultilineText(work.details, '未填写工作内容')
      ]);
    case '项目经历':
      return resume.value.projectList.flatMap((project) => [
        `${project.name || '未填写项目名称'}`,
        `${formatDate(project.period?.[0])} - ${formatDate(project.period?.[1])}`,
        ...splitMultilineText(project.details, '未填写项目描述')
      ]);
    case '荣誉奖项':
      return splitMultilineText(resume.value.award.details, '未填写');
    default:
      return [];
  }
};

const getWordSectionLines = (module) => {
  if (module === '基本信息') {
    return [
      `姓名：${resume.value.name || '未填写'}`,
      `电话：${resume.value.phone || '未填写'}`,
      `邮箱：${resume.value.email || '未填写'}`
    ];
  }
  return getSectionLines(module);
};

const dataUrlToUint8Array = (dataUrl) => {
  if (!dataUrl || typeof dataUrl !== 'string' || !dataUrl.startsWith('data:')) {
    return null;
  }
  const commaIndex = dataUrl.indexOf(',');
  if (commaIndex <= 0) {
    return null;
  }
  const base64 = dataUrl.slice(commaIndex + 1);
  try {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i += 1) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  } catch {
    return null;
  }
};

const buildWordAvatarBlock = () => {
  const avatarBytes = dataUrlToUint8Array(resume.value.avatar);
  if (!avatarBytes) {
    return null;
  }

  return new Table({
    width: { size: 1500, type: WidthType.DXA },
    rows: [
      new TableRow({
        children: [
          new TableCell({
            width: { size: 1500, type: WidthType.DXA },
            verticalAlign: VerticalAlign.CENTER,
            margins: { top: 80, bottom: 80, left: 80, right: 80 },
            borders: {
              top: { color: 'CBD5E1', size: 8, style: BorderStyle.SINGLE },
              bottom: { color: 'CBD5E1', size: 8, style: BorderStyle.SINGLE },
              left: { color: 'CBD5E1', size: 8, style: BorderStyle.SINGLE },
              right: { color: 'CBD5E1', size: 8, style: BorderStyle.SINGLE }
            },
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { after: 0 },
                children: [
                  new ImageRun({
                    data: avatarBytes,
                    transformation: { width: 88, height: 88 }
                  })
                ]
              })
            ]
          })
        ]
      })
    ]
  });
};

const buildNoBorder = () => ({
  top: { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' },
  bottom: { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' },
  left: { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' },
  right: { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' }
});

const createWordSectionBlock = (title, lines, options = {}) => {
  const titleSize = options.titleSize || 24;
  const contentSize = options.contentSize || 22;
  const contentPrefix = options.contentPrefix || '';

  const paragraphs = [
    new Paragraph({
      heading: HeadingLevel.HEADING_2,
      spacing: { before: 180, after: 90 },
      border: {
        bottom: { color: 'D1D5DB', size: 6, space: 1, style: BorderStyle.SINGLE }
      },
      children: [new TextRun({ text: title, bold: true, size: titleSize, color: '111827' })]
    })
  ];

  lines
    .flatMap((line) => splitMultilineText(line, ''))
    .map((line) => String(line || '').trim())
    .filter(Boolean)
    .forEach((line) => {
      paragraphs.push(
        new Paragraph({
          spacing: { after: 90 },
          children: [new TextRun({ text: `${contentPrefix}${line}`, size: contentSize, color: '1F2937' })]
        })
      );
    });

  return paragraphs;
};

const buildPdfExportNode = (sourceElement) => {
  const host = document.createElement('div');
  host.style.position = 'fixed';
  host.style.left = '-10000px';
  host.style.top = '0';
  host.style.zIndex = '-1';
  host.style.pointerEvents = 'none';
  host.style.background = '#ffffff';

  const cloned = sourceElement.cloneNode(true);
  cloned.classList.add('pdf-export-mode');
  cloned.style.transform = 'none';
  cloned.style.transformOrigin = 'top left';
  cloned.style.boxSizing = 'border-box';
  cloned.style.width = '194mm';
  cloned.style.maxWidth = '194mm';
  cloned.style.minHeight = '281mm';
  cloned.style.margin = '0';

  host.appendChild(cloned);
  document.body.appendChild(host);
  return { host, cloned };
};

const exportResume = async (format) => {
  const element = document.querySelector('.resume-paper');
  if (!element) {
    ElMessage.warning('预览区域未就绪');
    return;
  }

  const templateSuffix = activeTemplate.value === 'classic' ? 'classic' : activeTemplate.value;

  if (format === 'pdf') {
    const { host, cloned } = buildPdfExportNode(element);
    const exportWidth = Math.ceil(cloned.getBoundingClientRect().width);
    try {
      await html2pdf()
        .set({
          margin: [8, 8, 8, 8],
          filename: `resume-${templateSuffix}.pdf`,
          image: { type: 'jpeg', quality: 0.98 },
          pagebreak: { mode: ['css', 'legacy'] },
          html2canvas: {
            scale: 2,
            useCORS: true,
            backgroundColor: '#ffffff',
            width: exportWidth,
            windowWidth: exportWidth,
            onclone: (clonedDoc) => {
              clonedDoc.querySelectorAll('.paper-avatar').forEach((avatarEl) => {
                avatarEl.style.width = '84px';
                avatarEl.style.height = '84px';
                avatarEl.style.minWidth = '84px';
                avatarEl.style.minHeight = '84px';
                avatarEl.style.maxWidth = '84px';
                avatarEl.style.maxHeight = '84px';
                avatarEl.style.flex = '0 0 84px';
                avatarEl.style.aspectRatio = '1 / 1';
                avatarEl.style.display = 'block';
                avatarEl.style.backgroundSize = 'cover';
                avatarEl.style.backgroundPosition = 'center';
                avatarEl.style.backgroundRepeat = 'no-repeat';
              });
            }
          },
          jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' }
        })
        .from(cloned)
        .save();
    } finally {
      document.body.removeChild(host);
    }
    return;
  }

  if (format === 'word') {
    const sectionTitleTransform = activeTemplate.value === 'ats' ? (text) => text.toUpperCase() : (text) => text;
    const contentPrefix = activeTemplate.value === 'modern' ? '• ' : '';
    const moduleBlocks = orderedSelectedModules.value
      .filter((module) => module !== '基本信息' && module !== '求职意向')
      .map((module) => {
        const lines = getWordSectionLines(module);
        if (!lines.length) return [];
        return createWordSectionBlock(sectionTitleTransform(module), lines, {
          titleSize: activeTemplate.value === 'ats' ? 22 : 24,
          contentSize: 22,
          contentPrefix
        });
      })
      .flat();

    const basicLines = getWordSectionLines('基本信息');

    const avatarBlock = buildWordAvatarBlock();
    const headerInfoCell = new TableCell({
      width: { size: avatarBlock ? 80 : 100, type: WidthType.PERCENTAGE },
      verticalAlign: VerticalAlign.TOP,
      borders: buildNoBorder(),
      children: [
        new Paragraph({
          heading: HeadingLevel.TITLE,
          spacing: { after: 120 },
          children: [new TextRun({ text: `${resume.value.name || '候选人'} 简历`, bold: true, size: 34 })]
        }),
        new Paragraph({
          spacing: { after: 80 },
          children: [
            new TextRun({
              text: `${resume.value.jobTitle || '目标岗位'}`,
              bold: true,
              color: '0F766E',
              size: 24
            })
          ]
        }),
        new Paragraph({
          spacing: { after: 80 },
          children: [
            new TextRun({
              text: headerIntentText.value,
              size: 21,
              color: '334155'
            })
          ]
        }),
        new Paragraph({
          spacing: { after: 120 },
          children: [
            new TextRun({
              text: `${resume.value.phone || '电话未填写'}  |  ${resume.value.email || '邮箱未填写'}`,
              size: 21,
              color: '4B5563'
            })
          ]
        })
      ]
    });

    const headerCells = avatarBlock
      ? [
          new TableCell({
            width: { size: 20, type: WidthType.PERCENTAGE },
            verticalAlign: VerticalAlign.TOP,
            children: [avatarBlock]
          }),
          headerInfoCell
        ]
      : [headerInfoCell];

    const headerTable = new Table({
      width: { size: 100, type: WidthType.PERCENTAGE },
      columnWidths: avatarBlock ? [1900, 7600] : [9500],
      borders: buildNoBorder(),
      rows: [
        new TableRow({
          children: headerCells
        })
      ]
    });

    const basicInfoBlock = createWordSectionBlock(sectionTitleTransform('基本信息'), basicLines, {
      titleSize: activeTemplate.value === 'ats' ? 22 : 24,
      contentSize: 22,
      contentPrefix
    });

    const children = [headerTable, ...basicInfoBlock, ...moduleBlocks];

    const doc = new Document({
      sections: [
        {
          properties: {},
          children
        }
      ]
    });

    const blob = await Packer.toBlob(doc);
    saveAs(blob, `resume-${templateSuffix}.docx`);
    ElMessage.success('已导出 Word 文件');
  }
};

const exportResumeData = () => {
  const content = JSON.stringify(buildResumePayload(), null, 2);
  const blob = new Blob([content], { type: 'application/json;charset=utf-8' });
  saveAs(blob, 'resume-data.json');
  ElMessage.success('已导出 JSON');
};

const openExportDialog = () => {
  showExportDialog.value = true;
};

const confirmExport = async () => {
  if (exporting.value) return;
  exporting.value = true;
  try {
    if (exportFormat.value === 'json') {
      exportResumeData();
    } else {
      await exportResume(exportFormat.value);
    }
    showExportDialog.value = false;
  } finally {
    exporting.value = false;
  }
};

const openImportDialog = () => {
  if (importingPdf.value) {
    return;
  }
  importMatches.value = [];
  importedFileName.value = '';
  showImportModal.value = true;
};

const triggerPdfSelect = () => {
  if (importingPdf.value) return;
  if (pdfInputRef.value) {
    pdfInputRef.value.value = '';
    pdfInputRef.value.click();
  }
};

const onPdfFileChange = async (event) => {
  const file = event.target.files && event.target.files[0];
  if (!file) return;
  await importResumeFromPdf(file);
};

const onDragEnter = () => {
  if (!importingPdf.value) {
    dropActive.value = true;
  }
};

const onDragOver = () => {
  if (!importingPdf.value) {
    dropActive.value = true;
  }
};

const onDragLeave = () => {
  dropActive.value = false;
};

const onDropPdf = async (event) => {
  dropActive.value = false;
  if (importingPdf.value) return;
  const file = event.dataTransfer?.files?.[0];
  if (!file) return;
  await importResumeFromPdf(file);
};

const importResumeFromPdf = async (file) => {
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.warning('请上传 PDF 文件');
    return;
  }

  importingPdf.value = true;
  importedFileName.value = file.name;

  try {
    const arrayBuffer = await file.arrayBuffer();
    const text = await parsePdfText(arrayBuffer);
    if (!text || text.trim().length < 20) {
      throw new Error('未识别到有效简历文本，请确认 PDF 内容可复制');
    }

    const parsed = extractResumeFromText(text);
    const updatedCount = applyParsedResume(parsed);
    importMatches.value = buildImportMatches(parsed);

    if (updatedCount > 0) {
      ElMessage.success(`导入成功，已更新 ${updatedCount} 个字段`);
    } else {
      ElMessage.warning('未识别到可映射字段，请检查简历关键词是否完整');
    }
  } catch (error) {
    ElMessage.error(error.message || '导入失败');
  } finally {
    importingPdf.value = false;
  }
};

const parsePdfText = async (arrayBuffer) => {
  const loadingTask = pdfjsLib.getDocument({ data: arrayBuffer });
  const pdf = await loadingTask.promise;
  const pageTexts = [];

  for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber += 1) {
    const page = await pdf.getPage(pageNumber);
    const content = await page.getTextContent();
    const lines = [];
    let lineBuffer = '';

    content.items.forEach((item) => {
      const chunk = (item.str || '').trim();
      if (chunk) {
        lineBuffer += `${chunk} `;
      }
      if (item.hasEOL && lineBuffer.trim()) {
        lines.push(lineBuffer.trim());
        lineBuffer = '';
      }
    });

    if (lineBuffer.trim()) {
      lines.push(lineBuffer.trim());
    }

    const text = lines.length
      ? lines.join('\n').replace(/\n{2,}/g, '\n').trim()
      : content.items.map(item => item.str).join(' ').replace(/\s+/g, ' ').trim();

    if (text) {
      pageTexts.push(text);
    }
  }

  return pageTexts.join('\n');
};

const getCapture = (text, regex) => {
  if (!text) return '';
  const match = text.match(regex);
  return match?.[1] || '';
};

const extractByLabels = (text, labels, maxLen = 40) => {
  if (!text) return '';
  const escaped = labels
    .slice()
    .sort((a, b) => b.length - a.length)
    .map(escapeRegex)
    .join('|');

  if (!escaped) return '';

  const directPattern = new RegExp(`(?:${escaped})\\s*(?:[:：]|是|为)?\\s*([^\\n，,；;。]{1,${maxLen}})`, 'i');
  const directMatch = text.match(directPattern);
  if (directMatch?.[1]) {
    return directMatch[1];
  }

  const oneLine = text.replace(/\n/g, ' ');
  const inlinePattern = new RegExp(`(?:${escaped})\\s*(?:[:：]|是|为)?\\s*([^，,；;。]{1,${maxLen}})`, 'i');
  const inlineMatch = oneLine.match(inlinePattern);
  return inlineMatch?.[1] || '';
};

const firstNonEmpty = (values) => {
  for (let i = 0; i < values.length; i += 1) {
    const normalized = cleanValue(values[i]);
    if (normalized) {
      return normalized;
    }
  }
  return '';
};

const cleanValue = (value) => {
  if (!value || typeof value !== 'string') return '';
  return value
    .replace(/\s+/g, ' ')
    .replace(/^[：:，,；;。\s]+|[：:，,；;。\s]+$/g, '')
    .trim();
};

const trimLongText = (value, maxLen) => {
  const normalized = cleanValue(value);
  if (!normalized) return '';
  if (normalized.length <= maxLen) {
    return normalized;
  }
  return normalized.slice(0, maxLen);
};

const escapeRegex = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

const extractNameFromLines = (lines) => {
  if (!Array.isArray(lines) || !lines.length) return '';

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    if (/^[\u4e00-\u9fa5]{2,4}$/.test(line)) {
      const nearby = `${lines[i + 1] || ''} ${lines[i - 1] || ''}`;
      if (/(男|女|岁|1[3-9]\d{9}|@)/.test(nearby)) {
        return line;
      }
    }
  }
  return '';
};

const extractEducationFromTopLine = (lines) => {
  const result = { school: '', degree: '', major: '' };
  if (!Array.isArray(lines) || !lines.length) return result;

  const top = lines.slice(0, 4).join(' ');
  result.school = getCapture(top, /([\u4e00-\u9fa5A-Za-z0-9]{2,40}(?:大学|学院|学校))/);
  result.degree = getCapture(top, /(博士|硕士|本科|大专|专科)/);
  result.major = getCapture(top, /(?:博士|硕士|本科|大专|专科)\s*([\u4e00-\u9fa5A-Za-z0-9]{2,20})/);

  return result;
};

const extractSkillSentences = (lines) => {
  if (!Array.isArray(lines) || !lines.length) return '';
  const skillKeys = /(熟悉|掌握|了解|能够|精通|使用|开发框架|数据库|测试工具|持续集成)/;
  const skillLines = lines
    .filter((line) => skillKeys.test(line) && line.length >= 8)
    .slice(0, 8);
  return skillLines.join('；');
};

const extractAwardLines = (lines) => {
  if (!Array.isArray(lines) || !lines.length) return '';
  const awardKeys = /(蓝桥杯|获奖|一等奖|二等奖|三等奖|证书|CET-4|CET-6|英语六级|英语四级)/i;
  const awardLines = lines
    .filter((line) => awardKeys.test(line))
    .slice(0, 6);
  return awardLines.join('；');
};

const getSectionByKeywords = (text, keywords) => {
  if (!text) return '';
  const startIndexes = keywords
    .map((keyword) => text.indexOf(keyword))
    .filter(index => index >= 0)
    .sort((a, b) => a - b);

  if (!startIndexes.length) return '';
  const start = startIndexes[0];

  const sectionTitles = ['基本信息', '求职意向', '教育经历', '教育背景', '专业技能', '技能', '工作经历', '实习经历', '项目经历', '荣誉奖项', '获奖情况', '自我评价'];
  const endCandidates = sectionTitles
    .map((title) => text.indexOf(title, start + 2))
    .filter(index => index > start)
    .sort((a, b) => a - b);

  const end = endCandidates.length ? endCandidates[0] : Math.min(text.length, start + 800);
  return text.slice(start, end);
};

const extractDateRange = (text) => {
  if (!text) return [];
  const match = text.match(/((?:19|20)\d{2}(?:[./-年]\d{1,2})?)\s*(?:至|到|~|—|-|–)\s*((?:19|20)\d{2}(?:[./-年]\d{1,2})?|至今|现在)/);
  if (!match) return [];

  const start = parseDateToken(match[1], false);
  const end = parseDateToken(match[2], true);
  if (!start || !end) return [];
  return [start, end];
};

const parseDateToken = (token, isEnd) => {
  if (!token) return null;
  if (token.includes('至今') || token.includes('现在')) {
    return new Date();
  }

  const normalized = token.replace(/年|\./g, '-').replace(/月/g, '').replace(/\/+/g, '-').trim();
  const parts = normalized.split('-').filter(Boolean);
  const year = Number(parts[0]);
  const month = Number(parts[1] || (isEnd ? 12 : 1));
  if (!year || month < 1 || month > 12) {
    return null;
  }
  return new Date(year, month - 1, 1);
};

const extractResumeFromText = (rawText) => {
  const text = rawText
    .replace(/[\u3000\t]+/g, ' ')
    .replace(/\r/g, '')
    .replace(/\n{2,}/g, '\n');
  const compactText = text.replace(/\n/g, ' ');
  const textLines = text.split('\n').map((line) => cleanValue(line)).filter(Boolean);

  const mobileMatch = compactText.match(/(?:\+?86[-\s]?)?(1[3-9]\d{9})/);
  const emailMatch = compactText.match(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/);

  const name = firstNonEmpty([
    getCapture(text, /(?:姓名|Name)\s*[:：]?\s*([\u4e00-\u9fa5A-Za-z·]{2,20})/i),
    getCapture(text, /([\u4e00-\u9fa5]{2,4})\s*(?:求职简历|个人简历)/),
    extractNameFromLines(textLines)
  ]);

  const jobIntentSection = getSectionByKeywords(text, ['求职意向', '岗位意向', '应聘职位', '目标职位']);
  const educationSection = getSectionByKeywords(text, ['教育经历', '教育背景', '教育']);
  const skillsSection = getSectionByKeywords(text, ['专业技能', '技能', '专业能力']);
  const workSection = getSectionByKeywords(text, ['工作经历', '实习经历', '职业经历']);
  const projectSection = getSectionByKeywords(text, ['项目经历', '项目经验']);
  const awardSection = getSectionByKeywords(text, ['荣誉奖项', '获奖情况', '奖励证书', '证书']);

  const jobStatus = firstNonEmpty([
    compactText.includes('应届生') ? '应届生' : '',
    compactText.includes('在职') ? '在职' : '',
    compactText.includes('离职') ? '离职' : ''
  ]);

  const jobTitle = firstNonEmpty([
    extractByLabels(jobIntentSection, ['期望职位', '意向岗位', '应聘职位', '目标职位', '岗位意向'], 30),
    extractByLabels(text, ['期望职位', '意向岗位', '应聘职位', '目标职位', '岗位意向'], 30),
    getCapture(compactText, /(前端开发|后端开发|测试开发|测试工程师|后端工程师|前端工程师|Java开发|产品经理|运维工程师|数据分析师)/)
  ]);

  const salaryExpectation = firstNonEmpty([
    extractByLabels(jobIntentSection, ['期望薪资', '薪资要求', '薪酬要求', '期望月薪'], 30),
    extractByLabels(text, ['期望薪资', '薪资要求', '薪酬要求', '期望月薪'], 30)
  ]);

  const educationRange = extractDateRange(educationSection || text);
  const workRange = extractDateRange(workSection || text);
  const projectRange = extractDateRange(projectSection || text);

  return {
    name: cleanValue(name),
    phone: mobileMatch?.[1] || '',
    email: emailMatch?.[0] || '',
    jobStatus,
    jobTitle: cleanValue(jobTitle),
    salaryExpectation: cleanValue(salaryExpectation),
    education: {
      school: cleanValue(firstNonEmpty([
        extractByLabels(educationSection, ['学校', '院校', '毕业院校', '毕业学校'], 40),
        extractEducationFromTopLine(textLines).school,
        getCapture(educationSection, /([\u4e00-\u9fa5A-Za-z0-9]{4,40}(?:大学|学院|学校))/)
      ])),
      major: cleanValue(firstNonEmpty([
        extractByLabels(educationSection, ['专业', '所学专业'], 30),
        extractEducationFromTopLine(textLines).major
      ])),
      degree: cleanValue(firstNonEmpty([
        extractByLabels(educationSection, ['学历', '学位', '最高学历'], 10),
        extractEducationFromTopLine(textLines).degree,
        getCapture(educationSection, /(博士|硕士|本科|大专|专科)/)
      ])),
      studyPeriod: educationRange
    },
    profession: {
      skill: cleanValue(trimLongText(firstNonEmpty([
        skillsSection,
        extractSkillSentences(textLines)
      ]), 700))
    },
    work: {
      company: cleanValue(firstNonEmpty([
        extractByLabels(workSection, ['公司名称', '任职公司', '公司', '工作单位'], 40),
        getCapture(workSection, /([\u4e00-\u9fa5A-Za-z0-9]{2,40}(?:公司|集团|科技|信息|网络))/)
      ])),
      department: cleanValue(extractByLabels(workSection, ['部门', '所属部门'], 30)),
      position: cleanValue(firstNonEmpty([
        extractByLabels(workSection, ['职位', '岗位', '职务'], 30),
        getCapture(workSection, /(开发工程师|测试工程师|产品经理|项目经理|运营|设计师|算法工程师|前端工程师|后端工程师)/)
      ])),
      period: workRange,
      details: cleanValue(trimLongText(workSection, 900))
    },
    project: {
      name: cleanValue(firstNonEmpty([
        extractByLabels(projectSection, ['项目名称', '项目', '项目名'], 60),
        getCapture(projectSection, /([\u4e00-\u9fa5A-Za-z0-9]{2,40}(?:系统|平台|项目))/)
      ])),
      period: projectRange,
      details: cleanValue(trimLongText(projectSection, 900))
    },
    award: {
      details: cleanValue(trimLongText(firstNonEmpty([
        awardSection,
        extractAwardLines(textLines)
      ]), 600))
    }
  };
};

const applyParsedResume = (parsed) => {
  let updated = 0;
  if (!Array.isArray(resume.value.educationList) || !resume.value.educationList.length) {
    resume.value.educationList = [createEducationItem()];
  }
  if (!Array.isArray(resume.value.workList) || !resume.value.workList.length) {
    resume.value.workList = [createWorkItem()];
  }
  if (!Array.isArray(resume.value.projectList) || !resume.value.projectList.length) {
    resume.value.projectList = [createProjectItem()];
  }

  const primaryEducation = resume.value.educationList[0];
  const primaryWork = resume.value.workList[0];
  const primaryProject = resume.value.projectList[0];

  const setIfPresent = (getter, setter) => {
    const value = cleanValue(getter());
    if (!value) return;
    setter(value);
    updated += 1;
  };

  setIfPresent(() => parsed.name, (v) => { resume.value.name = v; });
  setIfPresent(() => parsed.phone, (v) => { resume.value.phone = v; });
  setIfPresent(() => parsed.email, (v) => { resume.value.email = v; });
  setIfPresent(() => parsed.jobStatus, (v) => { resume.value.jobStatus = v; });
  setIfPresent(() => parsed.jobTitle, (v) => { resume.value.jobTitle = v; });
  setIfPresent(() => parsed.salaryExpectation, (v) => { resume.value.salaryExpectation = v; });

  setIfPresent(() => parsed.education?.school, (v) => { primaryEducation.school = v; });
  setIfPresent(() => parsed.education?.major, (v) => { primaryEducation.major = v; });
  setIfPresent(() => parsed.education?.degree, (v) => { primaryEducation.degree = v; });
  if (Array.isArray(parsed.education?.studyPeriod) && parsed.education.studyPeriod.length === 2) {
    primaryEducation.studyPeriod = parsed.education.studyPeriod;
    updated += 1;
  }

  setIfPresent(() => parsed.profession?.skill, (v) => { resume.value.profession.skill = v; });

  setIfPresent(() => parsed.work?.company, (v) => { primaryWork.company = v; });
  setIfPresent(() => parsed.work?.department, (v) => { primaryWork.department = v; });
  setIfPresent(() => parsed.work?.position, (v) => { primaryWork.position = v; });
  if (Array.isArray(parsed.work?.period) && parsed.work.period.length === 2) {
    primaryWork.period = parsed.work.period;
    updated += 1;
  }
  setIfPresent(() => parsed.work?.details, (v) => { primaryWork.details = v; });

  setIfPresent(() => parsed.project?.name, (v) => { primaryProject.name = v; });
  if (Array.isArray(parsed.project?.period) && parsed.project.period.length === 2) {
    primaryProject.period = parsed.project.period;
    updated += 1;
  }
  setIfPresent(() => parsed.project?.details, (v) => { primaryProject.details = v; });

  setIfPresent(() => parsed.award?.details, (v) => { resume.value.award.details = v; });
  return updated;
};

const buildImportMatches = (parsed) => {
  const rows = [];
  const pushIfExists = (label, value) => {
    const normalized = cleanValue(value);
    if (normalized) {
      rows.push({ label, value: normalized });
    }
  };

  pushIfExists('姓名', parsed.name);
  pushIfExists('电话', parsed.phone);
  pushIfExists('邮箱', parsed.email);
  pushIfExists('当前状态', parsed.jobStatus);
  pushIfExists('期望职位', parsed.jobTitle);
  pushIfExists('期望薪资', parsed.salaryExpectation);
  pushIfExists('毕业院校', parsed.education?.school);
  pushIfExists('专业', parsed.education?.major);
  pushIfExists('学历', parsed.education?.degree);
  pushIfExists('专业技能', parsed.profession?.skill);
  pushIfExists('公司名称', parsed.work?.company);
  pushIfExists('部门', parsed.work?.department);
  pushIfExists('工作职位', parsed.work?.position);
  pushIfExists('项目名称', parsed.project?.name);
  pushIfExists('荣誉奖项', parsed.award?.details);
  return rows;
};

const showOptimizeAllDialog = async () => {
  optimizeForm.value.targetPosition = optimizeForm.value.targetPosition || resume.value.jobTitle || '';
  if (!Array.isArray(optimizeForm.value.focusModules) || optimizeForm.value.focusModules.length === 0) {
    optimizeForm.value.focusModules = [...optimizeModuleOptions];
  }
  showOptimizeModal.value = true;
};

const confirmOptimizeAll = async () => {
  if (!optimizeForm.value.focusModules.length) {
    ElMessage.warning('请至少选择一个重点模块');
    return;
  }
  showOptimizeModal.value = false;
  await optimizeFullResume();
};

const optimizeFullResume = async () => {
  try {
    optimizingAll.value = true;
    loading.value = true;
    loadingText.value = 'AI 正在优化您的简历...';

    const toneLabelMap = {
      balanced: '专业均衡',
      result: '结果导向',
      technical: '技术深度'
    };
    const strengthLabelMap = {
      1: '轻润色',
      2: '标准',
      3: '深优化'
    };
    const optimizationHint = `风格:${toneLabelMap[optimizeForm.value.tone]} 强度:${strengthLabelMap[optimizeForm.value.rewriteStrength]} 重点:${optimizeForm.value.focusModules.join('、')}`;

    const result = await postJson(`${API_BASE_URL}/ai/resume/optimize-all`, {
      resumeData: {
        jobStatus: resume.value.jobStatus,
        jobTitle: resume.value.jobTitle,
        salaryExpectation: resume.value.salaryExpectation,
        education: resume.value.educationList,
        profession: resume.value.profession,
        work: resume.value.workList,
        project: resume.value.projectList,
        award: resume.value.award
      },
      targetPosition: `${optimizeForm.value.targetPosition || resume.value.jobTitle || '目标岗位'} ${optimizationHint}`
    });

    if (!result.success) {
      throw new Error(result.error || '优化失败');
    }

    pendingOptimizedResume.value = result.optimizedResume || null;
    optimizeResultCandidates.value = [...optimizeForm.value.focusModules];
    optimizeResultSelection.value = [...optimizeForm.value.focusModules];
    optimizeResultRows.value = collectOptimizeRows(result.optimizedResume, optimizeForm.value.focusModules);

    if (!optimizeResultRows.value.length) {
      applyOptimizedResume(result.optimizedResume, optimizeForm.value.focusModules);
      ElMessage.success('已应用 AI 优化内容');
      return;
    }

    showOptimizeResultModal.value = true;
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '优化失败');
    }
  } finally {
    optimizingAll.value = false;
    loading.value = false;
    loadingText.value = '';
  }
};

const cancelOptimizeResult = () => {
  showOptimizeResultModal.value = false;
  pendingOptimizedResume.value = null;
  optimizeResultSelection.value = [];
  optimizeResultCandidates.value = [];
  optimizeResultRows.value = [];
};

const applyOptimizeResult = () => {
  if (!pendingOptimizedResume.value) {
    cancelOptimizeResult();
    return;
  }
  if (!optimizeResultSelection.value.length) {
    ElMessage.warning('请至少选择一个应用模块');
    return;
  }

  applyOptimizedResume(pendingOptimizedResume.value, optimizeResultSelection.value);
  ElMessage.success('已应用 AI 优化内容');
  cancelOptimizeResult();
};

const applyOptimizedResume = (optimizedData, focusModules = optimizeModuleOptions) => {
  if (!optimizedData) return;

  const include = (name) => focusModules.includes(name);

  if (include('求职意向')) {
    if (optimizedData.jobStatus) resume.value.jobStatus = optimizedData.jobStatus;
    if (optimizedData.jobTitle) resume.value.jobTitle = optimizedData.jobTitle;
    if (optimizedData.salaryExpectation) resume.value.salaryExpectation = optimizedData.salaryExpectation;
  }

  if (include('教育经历') && optimizedData.education) {
    const firstEducation = Array.isArray(optimizedData.education) ? optimizedData.education[0] : optimizedData.education;
    if (firstEducation) {
      if (!resume.value.educationList.length) {
        resume.value.educationList = [createEducationItem()];
      }
      resume.value.educationList[0] = {
        ...resume.value.educationList[0],
        school: firstEducation.school || resume.value.educationList[0].school,
        major: firstEducation.major || resume.value.educationList[0].major,
        degree: firstEducation.degree || resume.value.educationList[0].degree
      };
    }
  }
  if (include('专业技能') && optimizedData.profession?.skill) {
    resume.value.profession.skill = optimizedData.profession.skill;
  }
  if (include('工作经历') && optimizedData.work) {
    const firstWork = Array.isArray(optimizedData.work) ? optimizedData.work[0] : optimizedData.work;
    if (firstWork) {
      if (!resume.value.workList.length) {
        resume.value.workList = [createWorkItem()];
      }
      resume.value.workList[0] = {
        ...resume.value.workList[0],
        company: firstWork.company || resume.value.workList[0].company,
        department: firstWork.department || resume.value.workList[0].department,
        position: firstWork.position || resume.value.workList[0].position,
        details: firstWork.details || resume.value.workList[0].details
      };
    }
  }
  if (include('项目经历') && optimizedData.project) {
    const firstProject = Array.isArray(optimizedData.project) ? optimizedData.project[0] : optimizedData.project;
    if (firstProject) {
      if (!resume.value.projectList.length) {
        resume.value.projectList = [createProjectItem()];
      }
      resume.value.projectList[0] = {
        ...resume.value.projectList[0],
        name: firstProject.name || resume.value.projectList[0].name,
        details: firstProject.details || resume.value.projectList[0].details
      };
    }
  }
  if (include('荣誉奖项') && optimizedData.award?.details) {
    resume.value.award.details = optimizedData.award.details;
  }
};

const optimizeSingleModule = async (moduleType, content) => {
  try {
    optimizingModule.value = moduleType;

    const result = await postJson(`${API_BASE_URL}/ai/resume/optimize`, {
      moduleType,
      content,
      targetPosition: resume.value.jobTitle
    });

    if (!result.success) {
      throw new Error(result.error || '优化失败');
    }
    return result.optimizedContent;
  } catch (error) {
    ElMessage.error(error.message || '优化失败');
    return null;
  } finally {
    optimizingModule.value = '';
  }
};

const handleOptimizeProfession = async () => {
  if (!resume.value.profession.skill) {
    ElMessage.warning('请先填写技能描述');
    return;
  }
  const optimized = await optimizeSingleModule('profession', resume.value.profession.skill);
  if (optimized) {
    resume.value.profession.skill = optimized;
    ElMessage.success('专业技能润色完成');
  }
};

const handleOptimizeWork = async () => {
  const firstWork = resume.value.workList[0] || createWorkItem();
  if (!firstWork.details) {
    ElMessage.warning('请先填写工作内容');
    return;
  }
  const optimized = await optimizeSingleModule('work', firstWork);
  if (optimized && typeof optimized === 'object') {
    if (!resume.value.workList.length) {
      resume.value.workList = [createWorkItem()];
    }
    resume.value.workList[0] = {
      ...resume.value.workList[0],
      company: optimized.company || resume.value.workList[0].company,
      department: optimized.department || resume.value.workList[0].department,
      position: optimized.position || resume.value.workList[0].position,
      details: optimized.details || resume.value.workList[0].details
    };
    ElMessage.success('工作经历润色完成');
  }
};

const handleOptimizeProject = async () => {
  const firstProject = resume.value.projectList[0] || createProjectItem();
  if (!firstProject.details) {
    ElMessage.warning('请先填写项目描述');
    return;
  }
  const optimized = await optimizeSingleModule('project', firstProject);
  if (optimized && typeof optimized === 'object') {
    if (!resume.value.projectList.length) {
      resume.value.projectList = [createProjectItem()];
    }
    resume.value.projectList[0] = {
      ...resume.value.projectList[0],
      name: optimized.name || resume.value.projectList[0].name,
      details: optimized.details || resume.value.projectList[0].details
    };
    ElMessage.success('项目经历润色完成');
  }
};

const handleOptimizeAward = async () => {
  if (!resume.value.award.details) {
    ElMessage.warning('请先填写奖项内容');
    return;
  }
  const optimized = await optimizeSingleModule('award', resume.value.award.details);
  if (optimized) {
    resume.value.award.details = optimized;
    ElMessage.success('荣誉奖项润色完成');
  }
};

const loadGeneratedResume = async () => {
  const savedResumeData = localStorage.getItem('resumeData');
  if (!savedResumeData) return;

  try {
    loading.value = true;
    loadingText.value = '正在加载生成结果...';
    updateResumeData(JSON.parse(savedResumeData));
    localStorage.removeItem('resumeData');
    ElMessage.success('简历数据加载完成');
  } catch (error) {
    ElMessage.error(`加载简历数据失败: ${error.message}`);
  } finally {
    loading.value = false;
    loadingText.value = '';
  }
};

const syncResumeFromStorage = async () => {
  const savedResumeData = localStorage.getItem('resumeData');
  if (!savedResumeData) return;

  await loadGeneratedResume();
  await loadProfileData();
};

const loadLatestResume = async () => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }

  try {
    loading.value = true;
    loadingText.value = '正在加载最新简历...';
    const response = await fetch(`${API_BASE_URL}/ai/resume/latest?userId=${userId}`);
    const result = await response.json();
    if (!result.success) {
      throw new Error(result.error || '加载失败');
    }
    updateResumeData(result.content || {});
  } catch (error) {
    ElMessage.error(error.message || '加载简历失败');
  } finally {
    loading.value = false;
    loadingText.value = '';
  }
};

const saveResumeChanges = async () => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    ElMessage.warning('请先登录');
    return;
  }

  try {
    updating.value = true;
    const result = await postJson(`${API_BASE_URL}/ai/resume/update`, {
      userId,
      resumeData: buildResumePayload()
    });

    if (!result.success) {
      throw new Error(result.error || '更新失败');
    }
    ElMessage.success('简历更新成功');
  } catch (error) {
    ElMessage.error(error.message || '更新失败');
  } finally {
    updating.value = false;
  }
};

const updateResumeData = (data) => {
  const educationList = normalizeEducationList(data.education).map((item) => ({ ...createEducationItem(), ...item }));
  const workList = normalizeWorkList(data.work).map((item) => ({ ...createWorkItem(), ...item }));
  const projectList = normalizeProjectList(data.project).map((item) => ({ ...createProjectItem(), ...item }));

  resume.value = {
    name: data.name || '',
    phone: data.phone || '',
    email: data.email || '',
    avatar: data.avatar || '',
    jobStatus: data.jobStatus || '',
    jobTitle: data.jobTitle || '',
    salaryExpectation: data.salaryExpectation || '',
    educationList,
    profession: data.profession || { skill: '' },
    workList,
    projectList,
    award: data.award || { details: '' }
  };
};

const loadProfileData = async () => {
  const userId = localStorage.getItem('userId');
  if (!userId) return;

  try {
    const response = await fetch(`${API_BASE_URL}/api/profile/resume-data/${userId}`);
    const result = await response.json();
    if (!result.success || !result.data) return;

    const data = result.data;
    if (!resume.value.name && data.name) resume.value.name = data.name;
    if (!resume.value.phone && data.phone) resume.value.phone = data.phone;
    if (!resume.value.email && data.email) resume.value.email = data.email;
    if (!resume.value.jobStatus && data.jobStatus) resume.value.jobStatus = data.jobStatus;
    if (!resume.value.jobTitle && data.jobTitle) resume.value.jobTitle = data.jobTitle;
    if (!resume.value.salaryExpectation && data.salaryExpectation) resume.value.salaryExpectation = data.salaryExpectation;

    if (data.education) {
      if (!resume.value.educationList.length) {
        resume.value.educationList = [createEducationItem()];
      }
      if (!resume.value.educationList[0].school && data.education.school) resume.value.educationList[0].school = data.education.school;
      if (!resume.value.educationList[0].major && data.education.major) resume.value.educationList[0].major = data.education.major;
      if (!resume.value.educationList[0].degree && data.education.degree) resume.value.educationList[0].degree = data.education.degree;
    }

    if (data.profession?.skill && !resume.value.profession.skill) {
      resume.value.profession.skill = data.profession.skill;
    }
  } catch {
    // 个人信息补充失败不阻断主流程
  }
};

onMounted(async () => {
  loadEditorPreferences();
  window.addEventListener(RESUME_DATA_EVENT, syncResumeFromStorage);

  const savedResumeData = localStorage.getItem('resumeData');
  if (savedResumeData) {
    await loadGeneratedResume();
  } else {
    await loadLatestResume();
  }
  await loadProfileData();
});

onBeforeUnmount(() => {
  window.removeEventListener(RESUME_DATA_EVENT, syncResumeFromStorage);
});

watch([sectionMetaList, selectedModules, previewZoom, activeTemplate], () => {
  saveEditorPreferences();
}, { deep: true });
</script>

<style scoped>
.resume-builder-page {
  min-height: calc(100vh - 56px);
  background:
    radial-gradient(circle at 20% 20%, rgba(245, 214, 157, 0.35), transparent 30%),
    radial-gradient(circle at 80% 15%, rgba(142, 215, 204, 0.35), transparent 35%),
    #f5f7f2;
  color: #1f2937;
  font-family: 'Source Han Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 22px;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
  position: sticky;
  top: 0;
  z-index: 12;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-tag {
  font-size: 11px;
  font-weight: 700;
  color: #0f766e;
  border: 1px solid #5eead4;
  background: #ccfbf1;
  padding: 2px 7px;
  border-radius: 999px;
  letter-spacing: 0.3px;
}

.title-group h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.2px;
}

.title-group p {
  margin: 4px 0 0;
  color: #475569;
  font-size: 13px;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

:deep(.top-actions .el-button) {
  border-radius: 999px;
  font-weight: 600;
  border-color: #d4dde8;
  transition: all 0.2s ease;
}

:deep(.top-actions .el-button .el-icon) {
  transition: transform 0.2s ease;
}

:deep(.top-actions .el-button--default) {
  background: #ffffff;
  color: #445264;
}

:deep(.top-actions .import-btn) {
  border-color: #b8c9d8;
  background: #f7fbff;
  color: #34506a;
}

:deep(.top-actions .import-btn:hover) {
  border-color: #8fb2cf;
  background: #edf5ff;
}

:deep(.top-actions .el-button--primary) {
  background: #2f8f86;
  border-color: #2f8f86;
}

:deep(.top-actions .el-button--success) {
  background: #4b9a72;
  border-color: #4b9a72;
}

:deep(.top-actions .el-button--warning) {
  background: #d29a4d;
  border-color: #d29a4d;
}

:deep(.top-actions .el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
}

:deep(.top-actions .el-button:hover .el-icon) {
  transform: scale(1.08);
}

:deep(.top-actions .el-button:active) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.12);
}

.builder-grid {
  display: grid;
  grid-template-columns: 240px minmax(520px, 1fr) 460px;
  gap: 16px;
  padding: 16px;
  align-items: start;
}

.pane-card,
.editor-section,
.preview-pane {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.nav-pane {
  position: sticky;
  top: 132px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pane-card {
  padding: 14px;
}

.pane-card h3 {
  margin: 0 0 12px;
  font-size: 14px;
}

.pane-header {
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 0;
}

.pane-header h3 {
  margin: 0;
}

.pane-toggle {
  width: 24px;
  height: 24px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #334155;
  font-weight: 700;
}

.pane-body {
  margin-top: 10px;
}

.section-nav {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.section-nav-btn {
  width: 100%;
  text-align: left;
  border: 1px solid #d6dee8;
  border-radius: 10px;
  background: #fff;
  padding: 8px 10px;
  cursor: pointer;
  transition: 0.2s ease;
  font-size: 13px;
}

.section-nav-btn:hover,
.section-nav-btn.active {
  border-color: #1d4ed8;
  background: #eff6ff;
  color: #1e3a8a;
}

.module-switches {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.zoom-control {
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
}

.template-switch {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.template-btn {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #475569;
  border-radius: 8px;
  padding: 8px;
  font-size: 12px;
  cursor: pointer;
}

.template-btn.active {
  border-color: #0f766e;
  color: #0f766e;
  background: #ccfbf1;
}

.order-title {
  margin-top: 6px !important;
}

.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.order-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #334155;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 6px 8px;
  cursor: move;
}

.order-item.dragging {
  border-color: #0f766e;
  background: #ecfeff;
}

.order-actions {
  display: flex;
  gap: 4px;
}

.order-btn {
  width: 22px;
  height: 22px;
  border: 1px solid #cbd5e1;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  color: #475569;
}

.order-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.editor-pane {
  display: grid;
  gap: 12px;
  max-height: calc(100vh - 146px);
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.section-head h3 {
  margin: 0;
  font-size: 16px;
}

.experience-card {
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
  background: #fcfdff;
}

.experience-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.experience-add-row {
  display: flex;
  justify-content: center;
  margin-top: 4px;
}

:deep(.add-experience-btn.el-button) {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  padding: 0;
  font-size: 18px;
  line-height: 1;
}

.avatar-uploader {
  width: 100px;
  height: 100px;
  border: 2px dashed #cbd5e1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  overflow: hidden;
  cursor: pointer;
}

.avatar-file-input {
  display: none;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-pane {
  position: sticky;
  top: 76px;
  height: calc(100vh - 146px);
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
  padding: 16px;
  background: #edf2f7;
}

.resume-paper {
  width: 100%;
  min-height: 720px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #dbe3ee;
  padding: 22px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.paper-header {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  border-bottom: 2px solid #1d4ed8;
  padding-bottom: 12px;
}

.paper-header h2 {
  margin: 0;
  font-size: 24px;
}

.paper-header p {
  margin: 6px 0 0;
  color: #475569;
  font-size: 13px;
}

.paper-avatar {
  width: 84px;
  height: 84px;
  min-width: 84px;
  min-height: 84px;
  max-width: 84px;
  max-height: 84px;
  flex: 0 0 84px;
  aspect-ratio: 1 / 1;
  display: block;
  align-self: flex-start;
  border-radius: 10px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  overflow: hidden;
  border: 1px solid #cbd5e1;
}

.paper-section {
  margin-top: 14px;
}

.paper-entry {
  margin-bottom: 10px;
}

.paper-section h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #1d4ed8;
}

.paper-section p {
  margin: 4px 0;
  color: #1f2937;
  line-height: 1.6;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-word;
}

.paper-item-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.paper-item-main {
  flex: 1;
  min-width: 0;
  font-weight: 600;
}

.paper-item-period {
  flex: 0 0 auto;
  margin: 0;
  font-size: 12px;
  color: #64748b;
  letter-spacing: 0.2px;
  font-family: 'Consolas', 'SFMono-Regular', 'Menlo', monospace;
}

.resume-paper.template-modern {
  border-radius: 16px;
  border: 1px solid #bfdbfe;
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 24%);
}

.resume-paper.template-modern .paper-header {
  border-bottom-color: #0f766e;
}

.resume-paper.template-modern .paper-section h4 {
  color: #0f766e;
}

.resume-paper.template-ats {
  border-radius: 0;
  border: 1px solid #9ca3af;
  box-shadow: none;
  background: #ffffff;
  font-family: 'Arial', 'Helvetica', sans-serif;
}

.resume-paper.template-ats .paper-header {
  border-bottom: 1px solid #374151;
}

.resume-paper.template-ats .paper-header h2 {
  font-size: 22px;
  letter-spacing: 0;
}

.resume-paper.template-ats .paper-section h4 {
  color: #111827;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  border-bottom: 1px solid #d1d5db;
  padding-bottom: 4px;
}

.resume-paper.template-ats .paper-section p {
  font-size: 12px;
  line-height: 1.5;
}

.resume-paper.pdf-export-mode {
  box-shadow: none;
  border-radius: 0;
}

.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  z-index: 2000;
}

.loading-text {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #0f172a;
  color: #fff;
  padding: 12px 18px;
  border-radius: 10px;
}

.import-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.import-tip {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.hidden-file-input {
  display: none;
}

.drop-zone {
  border: 2px dashed #9ad6cd;
  border-radius: 12px;
  padding: 28px 18px;
  text-align: center;
  cursor: pointer;
  background: #f4fbfa;
  transition: all 0.2s ease;
}

.drop-zone.active {
  border-color: #2f8f86;
  background: #e8f8f5;
}

.drop-zone.loading {
  cursor: wait;
  opacity: 0.7;
}

.drop-title {
  color: #0f766e;
  font-size: 15px;
  font-weight: 600;
}

.drop-subtitle {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.import-result {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
}

.import-result h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #0f172a;
}

.match-list {
  max-height: 220px;
  overflow-y: auto;
}

.match-item {
  display: flex;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #e2e8f0;
}

.match-item:last-child {
  border-bottom: none;
}

.match-label {
  min-width: 80px;
  color: #475569;
  font-size: 13px;
}

.match-value {
  color: #0f172a;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}

.export-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 4px;
}

.export-option {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
  padding: 12px 14px;
  border: 1px solid #dbe1ee;
  border-radius: 10px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;
}

.export-option:hover {
  border-color: #b7ddd6;
  background: #f7fcfb;
}

.export-option.active {
  border-color: #69b9ad;
  background: #eef8f6;
  box-shadow: 0 0 0 2px rgba(105, 185, 173, 0.16);
}

.option-badge {
  min-width: 56px;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.4px;
  padding: 7px 8px;
  border-radius: 8px;
}

.option-badge-pdf {
  color: #991b1b;
  background: #fee2e2;
}

.option-badge-word {
  color: #1d4ed8;
  background: #dbeafe;
}

.option-badge-json {
  color: #0f766e;
  background: #d8f3ed;
}

.option-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.option-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.option-desc {
  color: #64748b;
  font-size: 12px;
}

.optimize-form-wrap {
  background: linear-gradient(180deg, #f6fbf9 0%, #ffffff 58%);
  border: 1px solid #dbeee8;
  border-radius: 12px;
  padding: 14px;
}

.optimize-tip {
  margin: 0 0 12px;
  color: #34506a;
  font-size: 13px;
  line-height: 1.6;
}

.optimize-form {
  margin-top: 4px;
}

.tone-group {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

:deep(.tone-group .el-radio-button__inner) {
  width: 100%;
  border-radius: 10px !important;
  border: 1px solid #c9ddd7 !important;
  color: #35556f;
  box-shadow: none;
}

:deep(.tone-group .el-radio-button.is-active .el-radio-button__inner) {
  background: #2f8f86;
  border-color: #2f8f86 !important;
  color: #ffffff;
}

.focus-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 10px;
}

:deep(.focus-grid .el-checkbox) {
  margin-right: 0;
}

:deep(.focus-grid .el-checkbox__label) {
  color: #1f3d55;
}

.strength-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  width: 100%;
}

.strength-tag {
  min-width: 60px;
  text-align: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: #def4ef;
  color: #0f766e;
  font-size: 12px;
  font-weight: 600;
}

.optimize-result-wrap {
  background: linear-gradient(180deg, #f8fcfb 0%, #ffffff 56%);
  border: 1px solid #dceee9;
  border-radius: 12px;
  padding: 14px;
}

.result-tip {
  margin: 0 0 10px;
  color: #3d556d;
  font-size: 13px;
}

.optimize-result-form {
  margin-bottom: 10px;
}

.result-preview-list {
  max-height: 300px;
  overflow-y: auto;
  display: grid;
  gap: 8px;
  padding-right: 4px;
}

.result-preview-item {
  border: 1px solid #d9e8e3;
  border-radius: 10px;
  background: #ffffff;
  padding: 10px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.preview-label {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.preview-tag {
  color: #0f766e;
  background: #def4ef;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
}

.preview-text {
  margin: 8px 0 0;
  color: #334155;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1400px) {
  .builder-grid {
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

  .nav-pane {
    top: 120px;
  }
}

@media (max-width: 900px) {
  .topbar {
    position: static;
  }

  .builder-grid {
    grid-template-columns: 1fr;
  }

  .nav-pane {
    position: static;
  }

  .preview-pane {
    position: static;
  }

  .editor-pane {
    max-height: none;
    overflow: visible;
  }

  .tone-group,
  .focus-grid {
    grid-template-columns: 1fr;
  }

}
</style>
