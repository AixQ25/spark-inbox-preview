const STORAGE_KEY = "spark-inbox-preview-state-v2";

const REMINDER_OPTIONS = [
  { value: "none", label: "不提醒" },
  { value: "tonight", label: "今晚" },
  { value: "tomorrow", label: "明天" },
  { value: "weekend", label: "周末" },
];

let state = loadState();
let activeView = "capture";
let activeItemStatus = "open";
let selectedKind = "idea";
let selectedReminder = "none";
let selectedTopicId = "";
let toastTimer;

const elements = {
  dateLine: document.querySelector("#dateLine"),
  views: document.querySelectorAll(".view"),
  viewButtons: document.querySelectorAll("[data-view-target]"),
  captureForm: document.querySelector("#captureForm"),
  captureInput: document.querySelector("#captureInput"),
  kindTabs: document.querySelector("#kindTabs"),
  topicSelect: document.querySelector("#topicSelect"),
  quickTopicButton: document.querySelector("#quickTopicButton"),
  reminderChips: document.querySelector("#reminderChips"),
  activeTopicCount: document.querySelector("#activeTopicCount"),
  activeTopicRail: document.querySelector("#activeTopicRail"),
  newTopicButton: document.querySelector("#newTopicButton"),
  itemTabs: document.querySelector("#itemTabs"),
  itemListTitle: document.querySelector("#itemListTitle"),
  itemCount: document.querySelector("#itemCount"),
  itemList: document.querySelector("#itemList"),
  reminderList: document.querySelector("#reminderList"),
  searchInput: document.querySelector("#searchInput"),
  searchList: document.querySelector("#searchList"),
  trashList: document.querySelector("#trashList"),
  toast: document.querySelector("#toast"),
  recordTemplate: document.querySelector("#recordTemplate"),
  topicTemplate: document.querySelector("#topicTemplate"),
};

function createSeedState() {
  const now = Date.now();
  const hour = 1000 * 60 * 60;
  const day = hour * 24;

  const topics = [
    {
      id: "topic-car",
      title: "智能小车",
      status: "active",
      createdAt: now - day * 4,
      startedAt: now - hour * 7,
      updatedAt: now - hour,
    },
    {
      id: "topic-video",
      title: "HTML视频",
      status: "active",
      createdAt: now - day * 2,
      startedAt: now - hour * 3,
      updatedAt: now - hour * 2,
    },
    {
      id: "topic-tavern",
      title: "酒馆学习",
      status: "backlog",
      createdAt: now - day,
      updatedAt: now - day,
    },
    {
      id: "topic-english",
      title: "英语输出连接句",
      status: "completed",
      createdAt: now - day * 6,
      startedAt: now - day * 5,
      completedAt: now - day * 2,
      updatedAt: now - day * 2,
    },
  ];

  const notes = [
    {
      id: "note-car-1",
      content: "智能小车先问 AI：循迹和避障第一版应该怎么取舍？",
      kind: "idea",
      topicId: "topic-car",
      status: "open",
      createdAt: now - hour,
      updatedAt: now - hour,
      remindAt: createReminderTime("tomorrow", now),
    },
    {
      id: "note-video-1",
      content: "HTML 视频标签要重点查 controls、poster、source 三个点。",
      kind: "todo",
      topicId: "topic-video",
      status: "open",
      createdAt: now - hour * 2,
      updatedAt: now - hour * 2,
      remindAt: createReminderTime("tonight", now),
    },
    {
      id: "note-loose-1",
      content: "之后可以和 AI 聊一下：轻量 app 到底要不要有每日整理？",
      kind: "idea",
      topicId: "",
      status: "open",
      createdAt: now - hour * 4,
      updatedAt: now - hour * 4,
    },
    {
      id: "note-tavern-1",
      content: "酒馆学习可能需要先整理一个最小可开始清单。",
      kind: "todo",
      topicId: "topic-tavern",
      status: "open",
      createdAt: now - day + hour * 3,
      updatedAt: now - day + hour * 3,
      remindAt: createReminderTime("weekend", now),
    },
    {
      id: "note-old-1",
      content: "优先级矩阵先不要做，手机端会太重。",
      kind: "idea",
      topicId: "",
      status: "trashed",
      createdAt: now - day * 3,
      updatedAt: now - day * 3,
      trashedAt: now - day,
    },
  ];

  return { topics, notes };
}

function createId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
}

function loadState() {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (!saved) return createSeedState();

  try {
    const parsed = JSON.parse(saved);
    if (!Array.isArray(parsed.topics) || !Array.isArray(parsed.notes)) {
      return createSeedState();
    }
    return parsed;
  } catch {
    return createSeedState();
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function persistAndRender() {
  saveState();
  render();
}

function createReminderTime(type, base = Date.now()) {
  if (type === "none") return null;

  const date = new Date(base);
  if (type === "tonight") {
    date.setHours(21, 0, 0, 0);
    if (date.getTime() <= base) date.setDate(date.getDate() + 1);
    return date.getTime();
  }

  if (type === "tomorrow") {
    date.setDate(date.getDate() + 1);
    date.setHours(9, 0, 0, 0);
    return date.getTime();
  }

  const day = date.getDay();
  const daysUntilSaturday = (6 - day + 7) % 7 || 7;
  date.setDate(date.getDate() + daysUntilSaturday);
  date.setHours(10, 0, 0, 0);
  return date.getTime();
}

function formatDate() {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "long",
    day: "numeric",
    weekday: "long",
  }).format(new Date());
}

function formatTime(timestamp) {
  if (!timestamp) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(timestamp));
}

function topicLabel(topicId) {
  const topic = getTopic(topicId);
  return topic ? topic.title : "未归属";
}

function getTopic(topicId) {
  return state.topics.find((topic) => topic.id === topicId);
}

function getVisibleTopics() {
  return state.topics.filter((topic) => topic.status !== "trashed");
}

function getVisibleNotes() {
  return state.notes.filter((note) => note.status !== "trashed");
}

function getTopicNotes(topicId, includeTrashed = false) {
  return state.notes
    .filter((note) => note.topicId === topicId && (includeTrashed || note.status !== "trashed"))
    .sort((a, b) => b.createdAt - a.createdAt);
}

function topicStatusLabel(status) {
  return {
    active: "进行中",
    backlog: "未处理",
    completed: "已完成",
    trashed: "垃圾箱",
  }[status];
}

function noteKindLabel(kind) {
  return kind === "todo" ? "待做" : "想法";
}

function setView(viewName) {
  activeView = viewName;
  elements.views.forEach((view) => {
    view.classList.toggle("is-active", view.dataset.view === viewName);
  });

  document.querySelectorAll(".bottom-nav [data-view-target]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.viewTarget === viewName);
  });

  render();
}

function setCaptureTopic(topicId) {
  selectedTopicId = topicId;
  setView("capture");
  elements.captureInput.focus();
}

function addNote(content) {
  const now = Date.now();
  state.notes.unshift({
    id: createId(),
    content,
    kind: selectedKind,
    topicId: selectedTopicId,
    status: "open",
    createdAt: now,
    updatedAt: now,
    remindAt: createReminderTime(selectedReminder, now),
  });

  if (selectedTopicId) {
    state.topics = state.topics.map((topic) =>
      topic.id === selectedTopicId ? { ...topic, updatedAt: now } : topic,
    );
  }

  selectedReminder = "none";
  persistAndRender();
  showToast(selectedTopicId ? `已保存到「${topicLabel(selectedTopicId)}」` : "已保存到收集箱");
}

function createTopic(title, status = "backlog") {
  const trimmed = title.trim();
  if (!trimmed) return null;

  const now = Date.now();
  const topic = {
    id: createId(),
    title: trimmed,
    status,
    createdAt: now,
    updatedAt: now,
  };

  if (status === "active") {
    topic.startedAt = now;
  }

  state.topics.unshift(topic);
  saveState();
  return topic;
}

function promptForTopic(status = "backlog") {
  const title = window.prompt("主题名，比如：智能小车");
  if (title === null) return;

  const topic = createTopic(title, status);
  if (!topic) return;

  selectedTopicId = topic.id;
  activeItemStatus = topic.status === "active" ? "active" : "open";
  persistAndRender();
  showToast(`已新建主题「${topic.title}」`);
}

function startTopic(id) {
  const now = Date.now();
  state.topics = state.topics.map((topic) =>
    topic.id === id
      ? { ...topic, status: "active", startedAt: topic.startedAt || now, updatedAt: now }
      : topic,
  );
  activeItemStatus = "active";
  persistAndRender();
}

function startNoteAsTopic(id) {
  const note = state.notes.find((item) => item.id === id);
  if (!note) return;

  const now = Date.now();
  const topic = {
    id: createId(),
    title: noteTitleFromContent(note.content),
    status: "active",
    createdAt: now,
    startedAt: now,
    updatedAt: now,
  };

  state.topics.unshift(topic);
  state.notes = state.notes.map((item) =>
    item.id === id ? { ...item, topicId: topic.id, updatedAt: now } : item,
  );
  activeItemStatus = "active";
  selectedTopicId = topic.id;
  persistAndRender();
  showToast(`已开始「${topic.title}」`);
}

function noteTitleFromContent(content) {
  const compact = content.replace(/\s+/g, " ").trim();
  return compact.length > 18 ? `${compact.slice(0, 18)}…` : compact;
}

function pauseTopic(id) {
  const now = Date.now();
  state.topics = state.topics.map((topic) =>
    topic.id === id ? { ...topic, status: "backlog", updatedAt: now } : topic,
  );
  activeItemStatus = "open";
  persistAndRender();
}

function completeTopic(id) {
  const now = Date.now();
  state.topics = state.topics.map((topic) =>
    topic.id === id
      ? { ...topic, status: "completed", completedAt: now, updatedAt: now }
      : topic,
  );
  activeItemStatus = "completed";
  persistAndRender();
  showToast("已标记为已完成");
}

function trashTopic(id) {
  const now = Date.now();
  state.topics = state.topics.map((topic) =>
    topic.id === id ? { ...topic, status: "trashed", trashedAt: now, updatedAt: now } : topic,
  );
  state.notes = state.notes.map((note) =>
    note.topicId === id ? { ...note, status: "trashed", trashedAt: now, updatedAt: now } : note,
  );
  persistAndRender();
}

function restoreTopic(id) {
  const now = Date.now();
  state.topics = state.topics.map((topic) =>
    topic.id === id
      ? { ...topic, status: "backlog", trashedAt: undefined, updatedAt: now }
      : topic,
  );
  state.notes = state.notes.map((note) =>
    note.topicId === id ? { ...note, status: "open", trashedAt: undefined, updatedAt: now } : note,
  );
  persistAndRender();
}

function completeNote(id) {
  const now = Date.now();
  state.notes = state.notes.map((note) =>
    note.id === id
      ? { ...note, status: "completed", completedAt: now, updatedAt: now, remindAt: null }
      : note,
  );
  activeItemStatus = "completed";
  persistAndRender();
  showToast("已完成");
}

function trashNote(id) {
  const now = Date.now();
  state.notes = state.notes.map((note) =>
    note.id === id ? { ...note, status: "trashed", trashedAt: now, updatedAt: now } : note,
  );
  persistAndRender();
}

function restoreNote(id) {
  const now = Date.now();
  state.notes = state.notes.map((note) =>
    note.id === id
      ? { ...note, status: "open", trashedAt: undefined, updatedAt: now }
      : note,
  );
  persistAndRender();
}

function snoozeNote(id) {
  const now = Date.now();
  state.notes = state.notes.map((note) =>
    note.id === id ? { ...note, remindAt: createReminderTime("tomorrow", now), updatedAt: now } : note,
  );
  persistAndRender();
  showToast("已改到明天提醒");
}

function renderKindTabs() {
  elements.kindTabs.querySelectorAll("button").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.kind === selectedKind);
  });
}

function renderTopicSelect() {
  const previousValue = selectedTopicId;
  elements.topicSelect.replaceChildren();

  const emptyOption = document.createElement("option");
  emptyOption.value = "";
  emptyOption.textContent = "不归属主题";
  elements.topicSelect.append(emptyOption);

  getVisibleTopics().forEach((topic) => {
    const option = document.createElement("option");
    option.value = topic.id;
    option.textContent = topic.status === "active" ? `${topic.title} · 进行中` : topic.title;
    elements.topicSelect.append(option);
  });

  selectedTopicId = getTopic(previousValue)?.status !== "trashed" ? previousValue : "";
  elements.topicSelect.value = selectedTopicId;
}

function renderReminderChips() {
  elements.reminderChips.replaceChildren();
  REMINDER_OPTIONS.forEach((option) => {
    const button = document.createElement("button");
    button.className = "tag-chip";
    button.type = "button";
    button.textContent = option.label;
    button.classList.toggle("is-active", selectedReminder === option.value);
    button.addEventListener("click", () => {
      selectedReminder = option.value;
      renderReminderChips();
    });
    elements.reminderChips.append(button);
  });
}

function renderCapture() {
  renderKindTabs();
  renderTopicSelect();
  renderReminderChips();

  const activeTopics = state.topics
    .filter((topic) => topic.status === "active")
    .sort((a, b) => (b.updatedAt || b.startedAt || b.createdAt) - (a.updatedAt || a.startedAt || a.createdAt));

  elements.activeTopicCount.textContent = String(activeTopics.length);
  renderTopicList(elements.activeTopicRail, activeTopics, "rail");
}

function renderFocus() {
  elements.itemTabs.querySelectorAll("button").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.itemStatus === activeItemStatus);
  });

  const items = getItemsByStatus(activeItemStatus);
  elements.itemListTitle.textContent = {
    open: "未处理",
    active: "进行中",
    completed: "已完成",
  }[activeItemStatus];
  elements.itemCount.textContent = String(items.length);
  renderMixedList(elements.itemList, items, activeItemStatus);
}

function getItemsByStatus(status) {
  if (status === "open") {
    const backlogTopics = state.topics
      .filter((topic) => topic.status === "backlog")
      .map((topic) => ({
        type: "topic",
        item: topic,
        sortAt: topic.updatedAt || topic.createdAt,
      }));

    const looseNotes = state.notes
      .filter((note) => note.status === "open" && !note.topicId)
      .map((note) => ({
        type: "note",
        item: note,
        sortAt: note.updatedAt || note.createdAt,
      }));

    return [...looseNotes, ...backlogTopics].sort((a, b) => b.sortAt - a.sortAt);
  }

  if (status === "active") {
    return state.topics
      .filter((topic) => topic.status === "active")
      .map((topic) => ({
        type: "topic",
        item: topic,
        sortAt: topic.updatedAt || topic.startedAt || topic.createdAt,
      }))
      .sort((a, b) => b.sortAt - a.sortAt);
  }

  const completedTopics = state.topics
    .filter((topic) => topic.status === "completed")
    .map((topic) => ({
      type: "topic",
      item: topic,
      sortAt: topic.completedAt || topic.updatedAt || topic.createdAt,
    }));

  const completedLooseNotes = state.notes
    .filter((note) => note.status === "completed" && !note.topicId)
    .map((note) => ({
      type: "note",
      item: note,
      sortAt: note.completedAt || note.updatedAt || note.createdAt,
    }));

  return [...completedTopics, ...completedLooseNotes].sort((a, b) => b.sortAt - a.sortAt);
}

function renderReminders() {
  const notes = getVisibleNotes()
    .filter((note) => note.status === "open" && note.remindAt)
    .sort((a, b) => a.remindAt - b.remindAt);
  renderRecordList(elements.reminderList, notes, "reminder");
}

function renderSearch() {
  const query = elements.searchInput.value.trim().toLowerCase();
  const topics = getVisibleTopics()
    .filter((topic) => topic.title.toLowerCase().includes(query))
    .map((topic) => ({
      type: "topic",
      item: topic,
      sortAt: topic.updatedAt || topic.completedAt || topic.startedAt || topic.createdAt,
    }));

  const notes = getVisibleNotes()
    .filter((note) => {
      const topic = getTopic(note.topicId);
      const topicMatch = topic?.title.toLowerCase().includes(query);
      return note.content.toLowerCase().includes(query) || topicMatch;
    })
    .map((note) => ({ type: "note", item: note, sortAt: note.updatedAt || note.createdAt }));

  renderMixedList(elements.searchList, [...topics, ...notes].sort((a, b) => b.sortAt - a.sortAt), "search");
}

function renderTrash() {
  const topics = state.topics
    .filter((topic) => topic.status === "trashed")
    .map((topic) => ({ type: "topic", item: topic, sortAt: topic.trashedAt || topic.updatedAt }));
  const notes = state.notes
    .filter((note) => note.status === "trashed")
    .map((note) => ({ type: "note", item: note, sortAt: note.trashedAt || note.updatedAt }));

  renderMixedList(elements.trashList, [...topics, ...notes].sort((a, b) => b.sortAt - a.sortAt), "trash");
}

function renderTopicList(container, topics, variant) {
  container.replaceChildren();
  if (topics.length === 0) {
    container.append(createEmptyState(variant === "rail" ? "还没有进行中的主题。" : "这里暂时没有主题。"));
    return;
  }

  topics.forEach((topic) => container.append(createTopicCard(topic, variant)));
}

function createTopicCard(topic, variant) {
  const node = elements.topicTemplate.content.firstElementChild.cloneNode(true);
  const title = node.querySelector(".topic-title-row h3");
  const status = node.querySelector(".topic-status");
  const meta = node.querySelector(".meta-row");
  const notes = node.querySelector(".topic-notes");
  const actions = node.querySelector(".record-actions");

  node.classList.toggle("is-active-topic", topic.status === "active");
  node.classList.toggle("is-completed", topic.status === "completed");
  node.classList.toggle("is-trashed", topic.status === "trashed");

  title.textContent = topic.title;
  status.textContent = topicStatusLabel(topic.status);
  status.classList.add(topic.status);
  meta.textContent = topicMetaText(topic);

  const recentNotes = getTopicNotes(topic.id, variant === "trash").slice(0, 2);
  if (recentNotes.length === 0) {
    const empty = document.createElement("div");
    empty.className = "topic-note empty";
    empty.textContent = "还没有相关记录";
    notes.append(empty);
  } else {
    recentNotes.forEach((note) => {
      const item = document.createElement("div");
      item.className = "topic-note";
      item.textContent = note.content;
      notes.append(item);
    });
  }

  if (variant === "rail") {
    actions.append(
      makeActionButton("记一条", "capture", () => setCaptureTopic(topic.id)),
      makeActionButton("完成", "complete", () => completeTopic(topic.id)),
    );
  } else if (variant === "active") {
    actions.append(
      makeActionButton("记一条", "capture", () => setCaptureTopic(topic.id)),
      makeActionButton("完成", "complete", () => completeTopic(topic.id)),
      makeActionButton("稍后", "pause", () => pauseTopic(topic.id)),
      makeActionButton("舍弃", "trash", () => trashTopic(topic.id)),
    );
  } else if (variant === "backlog" || variant === "open") {
    actions.append(
      makeActionButton("开始", "start", () => startTopic(topic.id)),
      makeActionButton("记一条", "capture", () => setCaptureTopic(topic.id)),
      makeActionButton("舍弃", "trash", () => trashTopic(topic.id)),
    );
  } else if (variant === "trash") {
    actions.append(
      makeActionButton("恢复", "start", () => restoreTopic(topic.id)),
      makeActionButton("复制", "copy", () => copyMarkdown(topicMarkdown(topic))),
    );
  } else {
    const restartLabel = topic.status === "completed" ? "重新进行" : "开始";
    actions.append(
      makeActionButton("复制", "copy", () => copyMarkdown(topicMarkdown(topic))),
      makeActionButton(restartLabel, "start", () => startTopic(topic.id)),
      makeActionButton("舍弃", "trash", () => trashTopic(topic.id)),
    );
  }

  return node;
}

function topicMetaText(topic) {
  const parts = [`创建 ${formatTime(topic.createdAt)}`];
  if (topic.startedAt) parts.push(`开始 ${formatTime(topic.startedAt)}`);
  if (topic.completedAt) parts.push(`完成 ${formatTime(topic.completedAt)}`);
  if (topic.trashedAt) parts.push(`舍弃 ${formatTime(topic.trashedAt)}`);
  return parts.join(" · ");
}

function renderRecordList(container, notes, variant = "full") {
  container.replaceChildren();
  if (notes.length === 0) {
    container.append(createEmptyState("这里暂时没有记录。"));
    return;
  }

  notes.forEach((note) => container.append(createRecordCard(note, variant)));
}

function createRecordCard(note, variant) {
  const node = elements.recordTemplate.content.firstElementChild.cloneNode(true);
  const dot = node.querySelector(".kind-dot");
  const content = node.querySelector(".record-main p");
  const meta = node.querySelector(".meta-row");
  const actions = node.querySelector(".record-actions");

  node.classList.toggle("is-trashed", note.status === "trashed");
  dot.classList.toggle("todo", note.kind === "todo");
  content.textContent = note.content;
  meta.textContent = noteMetaText(note);

  if (variant === "compact") return node;

  if (variant === "trash") {
    actions.append(
      makeActionButton("恢复", "start", () => restoreNote(note.id)),
      makeActionButton("复制", "copy", () => copyMarkdown(noteMarkdown(note))),
    );
    return node;
  }

  if (variant === "open") {
    actions.append(
      makeActionButton("开始", "start", () => startNoteAsTopic(note.id)),
      makeActionButton("完成", "complete", () => completeNote(note.id)),
      makeActionButton("舍弃", "trash", () => trashNote(note.id)),
      makeActionButton("复制", "copy", () => copyMarkdown(noteMarkdown(note))),
    );
    return node;
  }

  if (variant === "completed") {
    actions.append(
      makeActionButton("重新开始", "start", () => startNoteAsTopic(note.id)),
      makeActionButton("复制", "copy", () => copyMarkdown(noteMarkdown(note))),
      makeActionButton("舍弃", "trash", () => trashNote(note.id)),
    );
    return node;
  }

  if (variant === "reminder") {
    actions.append(
      makeActionButton("完成", "complete", () => completeNote(note.id)),
      makeActionButton("明天提醒", "snooze", () => snoozeNote(note.id)),
      makeActionButton("舍弃", "trash", () => trashNote(note.id)),
      makeActionButton("复制", "copy", () => copyMarkdown(noteMarkdown(note))),
    );
    return node;
  }

  actions.append(
    makeActionButton("完成", "complete", () => completeNote(note.id)),
    makeActionButton("复制", "copy", () => copyMarkdown(noteMarkdown(note))),
    makeActionButton("舍弃", "trash", () => trashNote(note.id)),
  );
  return node;
}

function noteMetaText(note) {
  const parts = [noteKindLabel(note.kind), `创建 ${formatTime(note.createdAt)}`];
  if (note.topicId) parts.push(`主题 ${topicLabel(note.topicId)}`);
  if (note.remindAt && note.status === "open") parts.push(`提醒 ${formatTime(note.remindAt)}`);
  if (note.completedAt) parts.push(`完成 ${formatTime(note.completedAt)}`);
  if (note.trashedAt) parts.push(`舍弃 ${formatTime(note.trashedAt)}`);
  return parts.join(" · ");
}

function renderMixedList(container, items, variant) {
  container.replaceChildren();
  if (items.length === 0) {
    container.append(createEmptyState(variant === "trash" ? "垃圾箱是空的。" : "没有找到相关内容。"));
    return;
  }

  items.forEach(({ type, item }) => {
    container.append(type === "topic" ? createTopicCard(item, variant) : createRecordCard(item, variant));
  });
}

function makeActionButton(label, action, onClick) {
  const button = document.createElement("button");
  button.type = "button";
  button.dataset.action = action;
  button.textContent = label;
  button.addEventListener("click", onClick);
  return button;
}

function createEmptyState(text) {
  const node = document.createElement("div");
  node.className = "empty-state";
  node.textContent = text;
  return node;
}

function noteMarkdown(note) {
  const lines = [
    `- ${note.content}`,
    `  - 类型：${noteKindLabel(note.kind)}`,
    `  - 创建时间：${formatTime(note.createdAt)}`,
  ];
  if (note.topicId) lines.push(`  - 主题：${topicLabel(note.topicId)}`);
  if (note.remindAt) lines.push(`  - 提醒：${formatTime(note.remindAt)}`);
  if (note.completedAt) lines.push(`  - 完成时间：${formatTime(note.completedAt)}`);
  return lines.join("\n");
}

function topicMarkdown(topic) {
  const lines = [
    `## ${topic.title}`,
    `- 状态：${topicStatusLabel(topic.status)}`,
    `- 创建时间：${formatTime(topic.createdAt)}`,
  ];

  if (topic.startedAt) lines.push(`- 开始时间：${formatTime(topic.startedAt)}`);
  if (topic.completedAt) lines.push(`- 完成时间：${formatTime(topic.completedAt)}`);

  const notes = getTopicNotes(topic.id, true);
  if (notes.length) {
    lines.push("", "### 相关记录");
    notes.forEach((note) => lines.push(noteMarkdown(note)));
  }

  return lines.join("\n");
}

async function copyMarkdown(markdown) {
  if (!markdown) {
    showToast("没有可复制的内容");
    return;
  }

  try {
    await navigator.clipboard.writeText(markdown);
  } catch {
    const textarea = document.createElement("textarea");
    textarea.value = markdown;
    document.body.append(textarea);
    textarea.select();
    document.execCommand("copy");
    textarea.remove();
  }

  showToast("已复制 Markdown");
}

function showToast(message) {
  window.clearTimeout(toastTimer);
  elements.toast.textContent = message;
  elements.toast.classList.add("is-visible");
  toastTimer = window.setTimeout(() => {
    elements.toast.classList.remove("is-visible");
  }, 1800);
}

function render() {
  elements.dateLine.textContent = formatDate();
  renderCapture();
  renderFocus();
  renderReminders();
  renderSearch();
  renderTrash();
}

elements.viewButtons.forEach((button) => {
  button.addEventListener("click", () => setView(button.dataset.viewTarget));
});

elements.kindTabs.addEventListener("click", (event) => {
  const button = event.target.closest("button[data-kind]");
  if (!button) return;
  selectedKind = button.dataset.kind;
  renderKindTabs();
});

elements.topicSelect.addEventListener("change", (event) => {
  selectedTopicId = event.target.value;
});

elements.quickTopicButton.addEventListener("click", () => promptForTopic("active"));
elements.newTopicButton.addEventListener("click", () => {
  promptForTopic(activeItemStatus === "active" ? "active" : "backlog");
});

elements.itemTabs.addEventListener("click", (event) => {
  const button = event.target.closest("button[data-item-status]");
  if (!button) return;
  activeItemStatus = button.dataset.itemStatus;
  renderFocus();
});

elements.captureForm.addEventListener("submit", (event) => {
  event.preventDefault();
  const content = elements.captureInput.value.trim();
  if (!content) {
    elements.captureInput.focus();
    return;
  }

  addNote(content);
  elements.captureInput.value = "";
  elements.captureInput.focus();
});

elements.searchInput.addEventListener("input", renderSearch);

render();
