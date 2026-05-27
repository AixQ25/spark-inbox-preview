package com.aixq.sparkinbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int COLOR_BG = 0xFF0D1110;
    private static final int COLOR_APP = 0xFF101513;
    private static final int COLOR_CARD = 0xFF171D1A;
    private static final int COLOR_CARD_SOFT = 0xFF131916;
    private static final int COLOR_TEXT = 0xFFEDF7F2;
    private static final int COLOR_MUTED = 0xFF8FA099;
    private static final int COLOR_FAINT = 0xFF586862;
    private static final int COLOR_LINE = 0xFF26312C;
    private static final int COLOR_GREEN = 0xFF56C997;
    private static final int COLOR_GREEN_STRONG = 0xFF7BE0B3;
    private static final int COLOR_GREEN_SOFT = 0x2256C997;
    private static final int COLOR_AMBER = 0xFFDCA247;
    private static final int COLOR_AMBER_SOFT = 0x22DCA247;
    private static final int COLOR_RED = 0xFFDC6B61;
    private static final int COLOR_RED_SOFT = 0x22DC6B61;
    private static final int COLOR_PURPLE = 0xFFA78BFA;
    private static final int COLOR_PURPLE_SOFT = 0x22A78BFA;

    private RecordStore store;
    private ArrayList<InboxRecord> records;
    private String activeTab = "capture";
    private String captureType = InboxRecord.TYPE_TASK;
    private String taskFilter = "all";
    private String ideaFilter = "all";
    private static final int RECENT_DAYS = 7;
    private final Set<String> monthCollapsed = new HashSet<>();

    private FrameLayout contentFrame;
    private LinearLayout captureView;
    private LinearLayout taskView;
    private LinearLayout ideaView;
    private EditText captureInput;
    private TextView saveButton;
    private TextView typeTaskBtn;
    private TextView typeIdeaBtn;
    private LinearLayout homeActiveList;
    private EditText taskSearch;
    private LinearLayout taskRecordList;
    private EditText ideaSearch;
    private LinearLayout ideaRecordList;
    private TextView navCapture;
    private TextView navTask;
    private TextView navIdea;
    private final ArrayList<TextView> taskFilterButtons = new ArrayList<>();
    private final ArrayList<TextView> ideaFilterButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        store = new RecordStore(this);
        records = store.loadRecords();
        buildUi();
        render();
        captureInput.requestFocus();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(COLOR_APP);
        window.setNavigationBarColor(COLOR_BG);
        if (Build.VERSION.SDK_INT >= 23) {
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_APP);
        root.setLayoutParams(match());

        root.setPadding(0, 0, 0, dp(12));
        contentFrame = new FrameLayout(this);
        root.addView(contentFrame, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));

        captureView = buildCaptureView();
        taskView = buildTaskView();
        ideaView = buildIdeaView();
        contentFrame.addView(captureView, matchFrame());
        contentFrame.addView(taskView, matchFrame());
        contentFrame.addView(ideaView, matchFrame());

        root.addView(buildBottomNav(), new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56)
        ));
        setContentView(root);
    }

    // ── Capture View ──

    private LinearLayout buildCaptureView() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(20), dp(40), dp(20), dp(10));
        screen.setBackgroundColor(COLOR_APP);

        TextView date = label(todayText(), 16, COLOR_GREEN_STRONG, Typeface.BOLD);
        date.setId(View.generateViewId());
        date.setTag("date");
        LinearLayout.LayoutParams dateParams = wrap();
        dateParams.setMargins(dp(6), dp(2), 0, dp(10));
        screen.addView(date, dateParams);

        LinearLayout inputCard = new LinearLayout(this);
        inputCard.setOrientation(LinearLayout.VERTICAL);
        inputCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        inputCard.setBackground(rounded(COLOR_CARD, COLOR_LINE, 16));
        inputCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                captureInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(captureInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        captureInput = new EditText(this);
        captureInput.setHint("写下刚想到的东西...");
        captureInput.setHintTextColor(COLOR_FAINT);
        captureInput.setTextColor(COLOR_TEXT);
        captureInput.setTextSize(18);
        captureInput.setGravity(Gravity.TOP | Gravity.START);
        captureInput.setMinLines(5);
        captureInput.setMaxLines(10);
        captureInput.setSingleLine(false);
        captureInput.setBackgroundColor(0x00000000);
        captureInput.setPadding(0, 0, 0, 0);
        captureInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateInputState();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        inputCard.addView(captureInput, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        screen.addView(inputCard, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            0.82f
        ));

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(2), 0, dp(2), 0);

        LinearLayout typeGroup = new LinearLayout(this);
        typeGroup.setOrientation(LinearLayout.HORIZONTAL);

        typeTaskBtn = typePill("任务", true);
        typeTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                captureType = InboxRecord.TYPE_TASK;
                updateTypePills();
            }
        });
        LinearLayout.LayoutParams taskTypeParams = new LinearLayout.LayoutParams(dp(68), dp(40));
        taskTypeParams.setMargins(0, 0, dp(8), 0);
        typeGroup.addView(typeTaskBtn, taskTypeParams);

        typeIdeaBtn = typePill("灵感", false);
        typeIdeaBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                captureType = InboxRecord.TYPE_IDEA;
                updateTypePills();
            }
        });
        typeGroup.addView(typeIdeaBtn, new LinearLayout.LayoutParams(dp(68), dp(40)));

        footer.addView(typeGroup, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));

        saveButton = pill("保存", COLOR_GREEN, 0xFF07100C);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                addRecord();
            }
        });
        footer.addView(saveButton, new LinearLayout.LayoutParams(dp(92), dp(40)));
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(40)
        );
        footerParams.setMargins(0, dp(12), 0, dp(18));
        screen.addView(footer, footerParams);

        LinearLayout activeSection = new LinearLayout(this);
        activeSection.setOrientation(LinearLayout.VERTICAL);
        activeSection.setPadding(dp(2), dp(14), dp(2), 0);

        LinearLayout activeHeader = new LinearLayout(this);
        activeHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("进行中", 16, COLOR_TEXT, Typeface.BOLD);
        activeHeader.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView more = actionText("查看更多", COLOR_GREEN_STRONG);
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                taskFilter = InboxRecord.STATUS_ACTIVE;
                switchTab("task");
            }
        });
        activeHeader.addView(more, wrap());
        activeSection.addView(activeHeader, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        homeActiveList = new LinearLayout(this);
        homeActiveList.setOrientation(LinearLayout.VERTICAL);
        homeActiveList.setPadding(0, dp(10), 0, 0);
        LinearLayout.LayoutParams homeListParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        );
        homeListParams.setMargins(0, 0, 0, 0);
        activeSection.addView(homeActiveList, homeListParams);
        screen.addView(activeSection, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.08f
        ));

        return screen;
    }

    private void updateTypePills() {
        boolean task = InboxRecord.TYPE_TASK.equals(captureType);
        typeTaskBtn.setBackground(rounded(task ? COLOR_GREEN_SOFT : 0x00000000, task ? COLOR_GREEN : COLOR_LINE, 999));
        typeTaskBtn.setTextColor(task ? COLOR_GREEN_STRONG : COLOR_MUTED);
        typeIdeaBtn.setBackground(rounded(!task ? COLOR_PURPLE_SOFT : 0x00000000, !task ? COLOR_PURPLE : COLOR_LINE, 999));
        typeIdeaBtn.setTextColor(!task ? COLOR_PURPLE : COLOR_MUTED);
    }

    private TextView typePill(String text, boolean active) {
        TextView view = label(text, 13, active ? COLOR_GREEN_STRONG : COLOR_MUTED, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(rounded(active ? COLOR_GREEN_SOFT : 0x00000000, active ? COLOR_GREEN : COLOR_LINE, 999));
        return view;
    }

    // ── Task View ──

    private LinearLayout buildTaskView() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(18), dp(42), dp(18), 0);
        screen.setBackgroundColor(COLOR_APP);

        taskSearch = new EditText(this);
        taskSearch.setHint("搜索任务...");
        taskSearch.setHintTextColor(COLOR_FAINT);
        taskSearch.setTextColor(COLOR_TEXT);
        taskSearch.setTextSize(15);
        taskSearch.setSingleLine(true);
        taskSearch.setPadding(dp(15), 0, dp(15), 0);
        taskSearch.setBackground(rounded(COLOR_CARD, COLOR_LINE, 999));
        taskSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderTaskList();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(46)
        );
        searchParams.setMargins(0, 0, 0, dp(12));
        screen.addView(taskSearch, searchParams);

        HorizontalScrollView filtersScroll = new HorizontalScrollView(this);
        filtersScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filtersScroll.addView(filters, wrap());
        addTaskFilter(filters, "全部", "all");
        addTaskFilter(filters, "未处理", InboxRecord.STATUS_OPEN);
        addTaskFilter(filters, "进行中", InboxRecord.STATUS_ACTIVE);
        addTaskFilter(filters, "已完成", InboxRecord.STATUS_COMPLETED);
        addTaskFilter(filters, "已放弃", InboxRecord.STATUS_ABANDONED);
        screen.addView(filtersScroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(42)
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        taskRecordList = new LinearLayout(this);
        taskRecordList.setOrientation(LinearLayout.VERTICAL);
        taskRecordList.setPadding(0, dp(10), 0, dp(24));
        scroll.addView(taskRecordList, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        screen.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));

        return screen;
    }

    private void addTaskFilter(LinearLayout filters, String label, final String value) {
        final TextView button = actionText(label, COLOR_MUTED);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(13), 0, dp(13), 0);
        button.setBackground(rounded(COLOR_CARD, COLOR_LINE, 999));
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                taskFilter = value;
                renderTaskList();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dp(34)
        );
        params.setMargins(0, 0, dp(7), 0);
        filters.addView(button, params);
        taskFilterButtons.add(button);
        button.setTag(value);
    }

    // ── Idea View ──

    private LinearLayout buildIdeaView() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(18), dp(42), dp(18), 0);
        screen.setBackgroundColor(COLOR_APP);

        ideaSearch = new EditText(this);
        ideaSearch.setHint("搜索灵感...");
        ideaSearch.setHintTextColor(COLOR_FAINT);
        ideaSearch.setTextColor(COLOR_TEXT);
        ideaSearch.setTextSize(15);
        ideaSearch.setSingleLine(true);
        ideaSearch.setPadding(dp(15), 0, dp(15), 0);
        ideaSearch.setBackground(rounded(COLOR_CARD, COLOR_LINE, 999));
        ideaSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderIdeaList();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(46)
        );
        searchParams.setMargins(0, 0, 0, dp(12));
        screen.addView(ideaSearch, searchParams);

        HorizontalScrollView filtersScroll = new HorizontalScrollView(this);
        filtersScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filtersScroll.addView(filters, wrap());
        addIdeaFilter(filters, "全部", "all");
        addIdeaFilter(filters, "未处理", InboxRecord.STATUS_UNPROCESSED);
        addIdeaFilter(filters, "已整理", InboxRecord.STATUS_ARCHIVED);
        screen.addView(filtersScroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(42)
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        ideaRecordList = new LinearLayout(this);
        ideaRecordList.setOrientation(LinearLayout.VERTICAL);
        ideaRecordList.setPadding(0, dp(10), 0, dp(24));
        scroll.addView(ideaRecordList, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        screen.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));

        return screen;
    }

    private void addIdeaFilter(LinearLayout filters, String label, final String value) {
        final TextView button = actionText(label, COLOR_MUTED);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(13), 0, dp(13), 0);
        button.setBackground(rounded(COLOR_CARD, COLOR_LINE, 999));
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ideaFilter = value;
                renderIdeaList();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dp(34)
        );
        params.setMargins(0, 0, dp(7), 0);
        filters.addView(button, params);
        ideaFilterButtons.add(button);
        button.setTag(value);
    }

    // ── Bottom Nav ──

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, 0, 0, dp(14));
        nav.setBackground(topBorderFill(COLOR_BG));

        navCapture = navItem("+\n记");
        navCapture.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                switchTab("capture");
            }
        });
        nav.addView(navCapture, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        navTask = navItem("△\n任务");
        navTask.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                switchTab("task");
            }
        });
        nav.addView(navTask, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        navIdea = navItem("☆\n灵感");
        navIdea.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                switchTab("idea");
            }
        });
        nav.addView(navIdea, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        return nav;
    }

    // ── Tab Switching ──

    private void switchTab(String tab) {
        activeTab = tab;
        render();
        if ("capture".equals(tab)) {
            captureInput.requestFocus();
        } else {
            hideKeyboard();
        }
    }

    private void render() {
        captureView.setVisibility("capture".equals(activeTab) ? View.VISIBLE : View.GONE);
        taskView.setVisibility("task".equals(activeTab) ? View.VISIBLE : View.GONE);
        ideaView.setVisibility("idea".equals(activeTab) ? View.VISIBLE : View.GONE);
        navCapture.setTextColor("capture".equals(activeTab) ? COLOR_GREEN : COLOR_FAINT);
        navTask.setTextColor("task".equals(activeTab) ? COLOR_GREEN : COLOR_FAINT);
        navIdea.setTextColor("idea".equals(activeTab) ? COLOR_PURPLE : COLOR_FAINT);
        renderHome();
        renderTaskList();
        renderIdeaList();
        updateTaskFilterButtons();
        updateIdeaFilterButtons();
        updateInputState();
    }

    // ── Home / Active Preview ──

    private void renderHome() {
        TextView date = findDateLabel(captureView);
        if (date != null) {
            date.setText(todayText());
        }

        ArrayList<InboxRecord> active = new ArrayList<>();
        for (InboxRecord record : records) {
            if (InboxRecord.TYPE_TASK.equals(record.type) && InboxRecord.STATUS_ACTIVE.equals(record.status)) {
                active.add(record);
            }
        }
        Collections.sort(active, new Comparator<InboxRecord>() {
            @Override public int compare(InboxRecord a, InboxRecord b) {
                long at = a.startedAt > 0 ? a.startedAt : a.createdAt;
                long bt = b.startedAt > 0 ? b.startedAt : b.createdAt;
                return Long.compare(bt, at);
            }
        });

        homeActiveList.removeAllViews();
        homeActiveList.setGravity(Gravity.TOP);
        homeActiveList.setPadding(0, dp(10), 0, 0);
        if (active.isEmpty()) {
            TextView empty = label("还没有进行中的任务。", 12, COLOR_MUTED, Typeface.NORMAL);
            homeActiveList.addView(empty, wrap());
            return;
        }

        int visible = Math.min(3, active.size());
        for (int i = 0; i < visible; i++) {
            homeActiveList.addView(homeRecordCard(active.get(i)));
        }
        if (active.size() > visible) {
            TextView more = label("还有 " + (active.size() - visible) + " 条，去「任务」页面处理。", 12, COLOR_MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams params = wrap();
            params.setMargins(0, dp(4), 0, 0);
            homeActiveList.addView(more, params);
        }
    }

    private View homeRecordCard(final InboxRecord record) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.TOP);
        card.setPadding(dp(12), dp(11), dp(12), dp(11));
        card.setBackground(rounded(COLOR_CARD, COLOR_LINE, 14));

        StatusCircle circle = new StatusCircle(this);
        circle.setStatus(record.status);
        circle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cycleTask(record);
            }
        });
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        circleParams.setMargins(0, dp(3), dp(10), 0);
        card.addView(circle, circleParams);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        TextView title = label(record.content, 14, COLOR_TEXT, Typeface.BOLD);
        title.setMaxLines(2);
        body.addView(title, wrap());
        body.addView(label("开始 " + formatDateTime(record.startedAt > 0 ? record.startedAt : record.createdAt), 12, COLOR_FAINT, Typeface.NORMAL), wrap());
        TextView edit = actionText("编辑", COLOR_MUTED);
        edit.setPadding(0, dp(8), dp(12), 0);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                editRecord(record);
            }
        });
        body.addView(edit, wrap());

        card.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);
        return card;
    }

    // ── Task List ──

    private void renderTaskList() {
        if (taskRecordList == null) return;
        updateTaskFilterButtons();
        taskRecordList.removeAllViews();

        ArrayList<InboxRecord> filtered = filteredTasks();
        if (filtered.isEmpty()) {
            String text = taskSearch.getText().toString().trim().isEmpty()
                ? "还没有任务\n去「记」页面写下第一条"
                : "没有找到相关任务";
            TextView empty = label(text, 14, COLOR_FAINT, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            taskRecordList.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(160)
            ));
            return;
        }

        ArrayList<InboxRecord> recent = new ArrayList<>();
        ArrayList<InboxRecord> old = new ArrayList<>();
        for (InboxRecord r : filtered) {
            if (isRecent(r.createdAt)) recent.add(r);
            else old.add(r);
        }

        if (!recent.isEmpty()) {
            LinkedHashMap<String, ArrayList<InboxRecord>> dayGroups = groupedByDay(recent);
            for (Map.Entry<String, ArrayList<InboxRecord>> entry : dayGroups.entrySet()) {
                TextView day = label(dayLabel(entry.getKey()), 14, COLOR_MUTED, Typeface.BOLD);
                LinearLayout.LayoutParams dayParams = wrap();
                dayParams.setMargins(0, dp(10), 0, dp(10));
                taskRecordList.addView(day, dayParams);
                for (InboxRecord record : entry.getValue()) {
                    taskRecordList.addView(taskCard(record));
                }
            }
        }

        if (!old.isEmpty()) {
            LinkedHashMap<String, ArrayList<InboxRecord>> monthGroups = groupedByMonth(old);
            for (Map.Entry<String, ArrayList<InboxRecord>> entry : monthGroups.entrySet()) {
                String mk = entry.getKey();
                boolean collapsed = monthCollapsed.contains(mk);
                taskRecordList.addView(monthHeader(mk, entry.getValue(), collapsed, true));
                if (!collapsed) {
                    LinkedHashMap<String, ArrayList<InboxRecord>> dayGroups = groupedByDay(entry.getValue());
                    for (Map.Entry<String, ArrayList<InboxRecord>> dayEntry : dayGroups.entrySet()) {
                        TextView day = label(dayLabel(dayEntry.getKey()), 14, COLOR_MUTED, Typeface.BOLD);
                        LinearLayout.LayoutParams dayParams = wrap();
                        dayParams.setMargins(0, dp(10), 0, dp(10));
                        taskRecordList.addView(day, dayParams);
                        for (InboxRecord record : dayEntry.getValue()) {
                            taskRecordList.addView(taskCard(record));
                        }
                    }
                }
            }
        }
    }

    private View monthHeader(final String mk, ArrayList<InboxRecord> records, boolean collapsed, final boolean isTask) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(10), dp(12), dp(10));
        header.setBackground(rounded(COLOR_CARD_SOFT, COLOR_LINE, 14));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.HORIZONTAL);
        left.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = label(monthLabel(mk), 15, COLOR_TEXT, Typeface.BOLD);
        left.addView(title, wrap());

        TextView count = label(records.size() + "条", 12, COLOR_FAINT, Typeface.NORMAL);
        LinearLayout.LayoutParams countParams = wrap();
        countParams.setMargins(dp(8), 0, 0, 0);
        left.addView(count, countParams);

        header.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Status dots
        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER_VERTICAL);
        if (isTask) {
            int openC = 0, activeC = 0, completedC = 0;
            for (InboxRecord r : records) {
                if (InboxRecord.STATUS_OPEN.equals(r.status)) openC++;
                else if (InboxRecord.STATUS_ACTIVE.equals(r.status)) activeC++;
                else if (InboxRecord.STATUS_COMPLETED.equals(r.status)) completedC++;
            }
            if (openC > 0) dots.addView(statusDot(COLOR_FAINT), dotParams(dots));
            if (activeC > 0) dots.addView(statusDot(COLOR_AMBER), dotParams(dots));
            if (completedC > 0) dots.addView(statusDot(COLOR_GREEN), dotParams(dots));
        } else {
            int unprocC = 0, archC = 0;
            for (InboxRecord r : records) {
                if (InboxRecord.STATUS_UNPROCESSED.equals(r.status)) unprocC++;
                else if (InboxRecord.STATUS_ARCHIVED.equals(r.status)) archC++;
            }
            if (unprocC > 0) dots.addView(statusDot(COLOR_FAINT), dotParams(dots));
            if (archC > 0) dots.addView(statusDot(COLOR_GREEN), dotParams(dots));
        }
        LinearLayout.LayoutParams dotsParams = wrap();
        dotsParams.setMargins(0, 0, dp(10), 0);
        header.addView(dots, dotsParams);

        // Chevron
        TextView chevron = label(collapsed ? "▸" : "▾", 16, COLOR_FAINT, Typeface.NORMAL);
        header.addView(chevron, new LinearLayout.LayoutParams(dp(20), ViewGroup.LayoutParams.WRAP_CONTENT));

        header.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (monthCollapsed.contains(mk)) monthCollapsed.remove(mk);
                else monthCollapsed.add(mk);
                if (isTask) renderTaskList();
                else renderIdeaList();
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(14), 0, dp(6));
        header.setLayoutParams(params);
        return header;
    }

    private View statusDot(int color) {
        View dot = new View(this);
        dot.setBackground(rounded(color, 0x00000000, 999));
        return dot;
    }

    private LinearLayout.LayoutParams dotParams(LinearLayout parent) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(6), dp(6));
        if (parent.getChildCount() > 0) p.setMargins(dp(4), 0, 0, 0);
        return p;
    }

    private View taskCard(final InboxRecord record) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.TOP);
        card.setPadding(dp(14), dp(15), dp(10), dp(13));
        card.setBackground(rounded(COLOR_CARD, COLOR_LINE, 14));
        card.setAlpha(InboxRecord.STATUS_COMPLETED.equals(record.status) ? 0.72f : InboxRecord.STATUS_ABANDONED.equals(record.status) ? 0.64f : 1f);

        StatusCircle circle = new StatusCircle(this);
        circle.setStatus(record.status);
        circle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                cycleTask(record);
            }
        });
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        circleParams.setMargins(0, dp(2), dp(12), 0);
        card.addView(circle, circleParams);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        TextView content = label(record.content, 15, COLOR_TEXT, Typeface.NORMAL);
        content.setMaxLines(4);
        if (InboxRecord.STATUS_COMPLETED.equals(record.status)) {
            content.setPaintFlags(content.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        body.addView(content, wrap());
        body.addView(label(taskMetaText(record), 12, COLOR_FAINT, Typeface.NORMAL), wrap());
        body.addView(taskActionsFor(record), wrap());
        card.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView delete = actionText("×", COLOR_FAINT);
        delete.setTextSize(22);
        delete.setGravity(Gravity.CENTER);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                deleteRecord(record);
            }
        });
        card.addView(delete, new LinearLayout.LayoutParams(dp(32), dp(32)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);
        return card;
    }

    private View taskActionsFor(final InboxRecord record) {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(9), 0, 0);
        actions.addView(actionBtn("编辑", COLOR_MUTED, new View.OnClickListener() {
            @Override public void onClick(View v) {
                editRecord(record);
            }
        }));

        if (InboxRecord.STATUS_OPEN.equals(record.status)) {
            actions.addView(actionBtn("完成", COLOR_MUTED, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    completeTask(record);
                }
            }));
            actions.addView(actionBtn("放弃", COLOR_RED, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    abandonTask(record);
                }
            }));
        } else if (InboxRecord.STATUS_ACTIVE.equals(record.status)) {
            actions.addView(actionBtn("放回未处理", COLOR_MUTED, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    pauseTask(record);
                }
            }));
            actions.addView(actionBtn("放弃", COLOR_RED, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    abandonTask(record);
                }
            }));
        } else if (InboxRecord.STATUS_COMPLETED.equals(record.status)) {
            actions.addView(actionBtn("放弃", COLOR_RED, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    abandonTask(record);
                }
            }));
        } else if (InboxRecord.STATUS_ABANDONED.equals(record.status)) {
            actions.addView(actionBtn("恢复", COLOR_MUTED, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    reopenTask(record);
                }
            }));
        }

        return actions;
    }

    // ── Idea List ──

    private void renderIdeaList() {
        if (ideaRecordList == null) return;
        updateIdeaFilterButtons();
        ideaRecordList.removeAllViews();

        ArrayList<InboxRecord> filtered = filteredIdeas();
        if (filtered.isEmpty()) {
            String text = ideaSearch.getText().toString().trim().isEmpty()
                ? "还没有灵感\n去「记」页面写下第一条"
                : "没有找到相关灵感";
            TextView empty = label(text, 14, COLOR_FAINT, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            ideaRecordList.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(160)
            ));
            return;
        }

        ArrayList<InboxRecord> recent = new ArrayList<>();
        ArrayList<InboxRecord> old = new ArrayList<>();
        for (InboxRecord r : filtered) {
            if (isRecent(r.createdAt)) recent.add(r);
            else old.add(r);
        }

        if (!recent.isEmpty()) {
            LinkedHashMap<String, ArrayList<InboxRecord>> dayGroups = groupedByDay(recent);
            for (Map.Entry<String, ArrayList<InboxRecord>> entry : dayGroups.entrySet()) {
                TextView day = label(dayLabel(entry.getKey()), 14, COLOR_MUTED, Typeface.BOLD);
                LinearLayout.LayoutParams dayParams = wrap();
                dayParams.setMargins(0, dp(10), 0, dp(10));
                ideaRecordList.addView(day, dayParams);
                for (InboxRecord record : entry.getValue()) {
                    ideaRecordList.addView(ideaCard(record));
                }
            }
        }

        if (!old.isEmpty()) {
            LinkedHashMap<String, ArrayList<InboxRecord>> monthGroups = groupedByMonth(old);
            for (Map.Entry<String, ArrayList<InboxRecord>> entry : monthGroups.entrySet()) {
                String mk = entry.getKey();
                boolean collapsed = monthCollapsed.contains(mk);
                ideaRecordList.addView(monthHeader(mk, entry.getValue(), collapsed, false));
                if (!collapsed) {
                    LinkedHashMap<String, ArrayList<InboxRecord>> dayGroups = groupedByDay(entry.getValue());
                    for (Map.Entry<String, ArrayList<InboxRecord>> dayEntry : dayGroups.entrySet()) {
                        TextView day = label(dayLabel(dayEntry.getKey()), 14, COLOR_MUTED, Typeface.BOLD);
                        LinearLayout.LayoutParams dayParams = wrap();
                        dayParams.setMargins(0, dp(10), 0, dp(10));
                        ideaRecordList.addView(day, dayParams);
                        for (InboxRecord record : dayEntry.getValue()) {
                            ideaRecordList.addView(ideaCard(record));
                        }
                    }
                }
            }
        }
    }

    private View ideaCard(final InboxRecord record) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.TOP);
        card.setPadding(dp(14), dp(15), dp(10), dp(13));
        card.setBackground(rounded(COLOR_CARD, COLOR_LINE, 14));
        boolean archived = InboxRecord.STATUS_ARCHIVED.equals(record.status);
        card.setAlpha(archived ? 0.72f : 1f);

        final ArchiveCircle circle = new ArchiveCircle(this);
        circle.setArchived(archived);
        circle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                toggleIdeaArchive(record);
            }
        });
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        circleParams.setMargins(0, dp(2), dp(12), 0);
        card.addView(circle, circleParams);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        TextView content = label(record.content, 15, archived ? COLOR_MUTED : COLOR_TEXT, Typeface.NORMAL);
        content.setMaxLines(4);
        if (archived) {
            content.setPaintFlags(content.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        body.addView(content, wrap());
        body.addView(label(ideaMetaText(record), 12, COLOR_FAINT, Typeface.NORMAL), wrap());

        LinearLayout ideaActions = new LinearLayout(this);
        ideaActions.setOrientation(LinearLayout.HORIZONTAL);
        ideaActions.setPadding(0, dp(9), 0, 0);
        ideaActions.addView(actionBtn("编辑", COLOR_MUTED, new View.OnClickListener() {
            @Override public void onClick(View v) {
                editRecord(record);
            }
        }));
        body.addView(ideaActions, wrap());
        card.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView delete = actionText("×", COLOR_FAINT);
        delete.setTextSize(22);
        delete.setGravity(Gravity.CENTER);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                deleteRecord(record);
            }
        });
        card.addView(delete, new LinearLayout.LayoutParams(dp(32), dp(32)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);
        return card;
    }

    // ── Record Operations ──

    private void addRecord() {
        String content = captureInput.getText().toString().trim();
        if (content.isEmpty()) return;

        InboxRecord record;
        if (InboxRecord.TYPE_TASK.equals(captureType)) {
            record = new InboxRecord(UUID.randomUUID().toString(), content, InboxRecord.TYPE_TASK, InboxRecord.STATUS_OPEN, System.currentTimeMillis());
        } else {
            record = new InboxRecord(UUID.randomUUID().toString(), content, InboxRecord.TYPE_IDEA, InboxRecord.STATUS_UNPROCESSED, System.currentTimeMillis());
        }
        records.add(0, record);
        captureInput.setText("");
        persistAndRender("已保存");
    }

    private void cycleTask(InboxRecord record) {
        if (InboxRecord.STATUS_OPEN.equals(record.status)) {
            startTask(record);
        } else if (InboxRecord.STATUS_ACTIVE.equals(record.status)) {
            completeTask(record);
        } else {
            reopenTask(record);
        }
    }

    private void startTask(InboxRecord record) {
        long now = System.currentTimeMillis();
        record.status = InboxRecord.STATUS_ACTIVE;
        if (record.startedAt == 0L) record.startedAt = now;
        record.updatedAt = now;
        persistAndRender("已开始");
    }

    private void completeTask(InboxRecord record) {
        long now = System.currentTimeMillis();
        record.status = InboxRecord.STATUS_COMPLETED;
        if (record.startedAt == 0L) record.startedAt = now;
        record.completedAt = now;
        record.abandonedAt = 0L;
        record.updatedAt = now;
        persistAndRender("已完成");
    }

    private void pauseTask(InboxRecord record) {
        record.status = InboxRecord.STATUS_OPEN;
        record.updatedAt = System.currentTimeMillis();
        persistAndRender("已放回未处理");
    }

    private void abandonTask(InboxRecord record) {
        long now = System.currentTimeMillis();
        record.status = InboxRecord.STATUS_ABANDONED;
        record.completedAt = 0L;
        record.abandonedAt = now;
        record.updatedAt = now;
        persistAndRender("已放弃");
    }

    private void reopenTask(InboxRecord record) {
        record.status = InboxRecord.STATUS_OPEN;
        record.startedAt = 0L;
        record.completedAt = 0L;
        record.abandonedAt = 0L;
        record.updatedAt = System.currentTimeMillis();
        persistAndRender("已恢复为未处理");
    }

    private void toggleIdeaArchive(InboxRecord record) {
        if (InboxRecord.STATUS_ARCHIVED.equals(record.status)) {
            record.status = InboxRecord.STATUS_UNPROCESSED;
            record.archivedAt = 0L;
            persistAndRender("已标记为未处理");
        } else {
            record.status = InboxRecord.STATUS_ARCHIVED;
            record.archivedAt = System.currentTimeMillis();
            persistAndRender("已标记为已整理");
        }
    }

    private void editRecord(final InboxRecord record) {
        final EditText editor = new EditText(this);
        editor.setText(record.content);
        editor.setSelection(editor.getText().length());
        editor.setMinLines(3);
        editor.setMaxLines(6);
        editor.setTextColor(COLOR_TEXT);
        editor.setHintTextColor(COLOR_FAINT);
        editor.setBackgroundColor(COLOR_CARD);
        editor.setPadding(dp(12), dp(10), dp(12), dp(10));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("编辑记录")
            .setView(editor)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    String content = editor.getText().toString().trim();
                    if (content.isEmpty()) {
                        toast("内容不能为空");
                        return;
                    }
                    record.content = content;
                    record.updatedAt = System.currentTimeMillis();
                    persistAndRender("已更新");
                }
            })
            .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialogInterface) {
                dialog.getWindow().setBackgroundDrawable(rounded(COLOR_CARD, COLOR_LINE, 14));
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(COLOR_GREEN);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(COLOR_MUTED);
            }
        });
        dialog.show();
    }

    private void deleteRecord(final InboxRecord record) {
        new AlertDialog.Builder(this)
            .setTitle("删除记录？")
            .setMessage(record.content)
            .setNegativeButton("取消", null)
            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    records.remove(record);
                    persistAndRender("已删除");
                }
            })
            .show();
    }

    // ── Filter & Data Helpers ──

    private ArrayList<InboxRecord> filteredTasks() {
        String query = taskSearch == null ? "" : taskSearch.getText().toString().trim().toLowerCase(Locale.CHINA);
        ArrayList<InboxRecord> filtered = new ArrayList<>();
        for (InboxRecord record : records) {
            if (!InboxRecord.TYPE_TASK.equals(record.type)) continue;
            if ("all".equals(taskFilter) && InboxRecord.STATUS_ABANDONED.equals(record.status)) continue;
            if (!"all".equals(taskFilter) && !taskFilter.equals(record.status)) continue;
            if (!query.isEmpty() && !record.content.toLowerCase(Locale.CHINA).contains(query)) continue;
            filtered.add(record);
        }
        Collections.sort(filtered, new Comparator<InboxRecord>() {
            @Override public int compare(InboxRecord a, InboxRecord b) {
                return Long.compare(b.createdAt, a.createdAt);
            }
        });
        return filtered;
    }

    private ArrayList<InboxRecord> filteredIdeas() {
        String query = ideaSearch == null ? "" : ideaSearch.getText().toString().trim().toLowerCase(Locale.CHINA);
        ArrayList<InboxRecord> filtered = new ArrayList<>();
        for (InboxRecord record : records) {
            if (!InboxRecord.TYPE_IDEA.equals(record.type)) continue;
            if (!"all".equals(ideaFilter) && !ideaFilter.equals(record.status)) continue;
            if (!query.isEmpty() && !record.content.toLowerCase(Locale.CHINA).contains(query)) continue;
            filtered.add(record);
        }
        Collections.sort(filtered, new Comparator<InboxRecord>() {
            @Override public int compare(InboxRecord a, InboxRecord b) {
                return Long.compare(b.createdAt, a.createdAt);
            }
        });
        return filtered;
    }

    private LinkedHashMap<String, ArrayList<InboxRecord>> groupedByDay(ArrayList<InboxRecord> list) {
        LinkedHashMap<String, ArrayList<InboxRecord>> map = new LinkedHashMap<>();
        for (InboxRecord record : list) {
            String key = dateKey(record.createdAt);
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<InboxRecord>());
            }
            map.get(key).add(record);
        }
        return map;
    }

    private String monthKey(long timestamp) {
        return new SimpleDateFormat("yyyy-M", Locale.CHINA).format(timestamp);
    }

    private boolean isRecent(long timestamp) {
        long now = System.currentTimeMillis();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long diff = today.getTimeInMillis() - timestamp;
        return diff < (long) RECENT_DAYS * 24 * 60 * 60 * 1000;
    }

    private String monthLabel(String mk) {
        String[] parts = mk.split("-");
        if (parts.length == 2) {
            return parts[0] + "年" + Integer.parseInt(parts[1]) + "月";
        }
        return mk;
    }

    private LinkedHashMap<String, ArrayList<InboxRecord>> groupedByMonth(ArrayList<InboxRecord> list) {
        LinkedHashMap<String, ArrayList<InboxRecord>> map = new LinkedHashMap<>();
        for (InboxRecord record : list) {
            String key = monthKey(record.createdAt);
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<InboxRecord>());
            }
            map.get(key).add(record);
        }
        return map;
    }

    // ── Persistence ──

    private void persistAndRender(String message) {
        store.saveRecords(records);
        render();
        toast(message);
    }

    // ── UI Update Helpers ──

    private void updateTaskFilterButtons() {
        for (TextView button : taskFilterButtons) {
            boolean active = taskFilter.equals(button.getTag());
            button.setTextColor(active ? COLOR_GREEN_STRONG : COLOR_MUTED);
            button.setBackground(rounded(active ? COLOR_GREEN_SOFT : COLOR_CARD, active ? COLOR_GREEN : COLOR_LINE, 999));
        }
    }

    private void updateIdeaFilterButtons() {
        for (TextView button : ideaFilterButtons) {
            boolean active = ideaFilter.equals(button.getTag());
            button.setTextColor(active ? COLOR_PURPLE : COLOR_MUTED);
            button.setBackground(rounded(active ? COLOR_PURPLE_SOFT : COLOR_CARD, active ? COLOR_PURPLE : COLOR_LINE, 999));
        }
    }

    private void updateInputState() {
        boolean enabled = captureInput.getText().toString().trim().length() > 0;
        saveButton.setEnabled(enabled);
        saveButton.setTextColor(enabled ? 0xFF07100C : 0xFF6F837A);
        saveButton.setBackground(rounded(enabled ? COLOR_GREEN : 0xFF213029, 0x00000000, 999));
    }

    // ── Text Formatting ──

    private String taskMetaText(InboxRecord record) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add(statusLabel(record.status) + " · 记录 " + formatDateTime(record.createdAt));
        if (record.startedAt > 0L) parts.add("开始 " + formatDateTime(record.startedAt));
        if (record.completedAt > 0L) parts.add("完成 " + formatDateTime(record.completedAt));
        if (record.abandonedAt > 0L) parts.add("放弃 " + formatDateTime(record.abandonedAt));
        return join(parts, " · ");
    }

    private String ideaMetaText(InboxRecord record) {
        String text = "记录 " + formatDateTime(record.createdAt);
        if (record.archivedAt > 0L) {
            text += " · 整理 " + formatDateTime(record.archivedAt);
        }
        return text;
    }

    private String statusLabel(String status) {
        if (InboxRecord.STATUS_ACTIVE.equals(status)) return "进行中";
        if (InboxRecord.STATUS_COMPLETED.equals(status)) return "已完成";
        if (InboxRecord.STATUS_ABANDONED.equals(status)) return "已放弃";
        return "未处理";
    }

    private String dateKey(long timestamp) {
        return new SimpleDateFormat("yyyy-M-d", Locale.CHINA).format(timestamp);
    }

    private String dayLabel(String key) {
        String today = dateKey(System.currentTimeMillis());
        Calendar yesterday = Calendar.getInstance(Locale.CHINA);
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayKey = dateKey(yesterday.getTimeInMillis());
        if (today.equals(key)) return "今天";
        if (yesterdayKey.equals(key)) return "昨天";
        String[] parts = key.split("-");
        if (parts.length == 3) {
            return parts[1] + "月" + parts[2] + "日";
        }
        return key;
    }

    private String todayText() {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        String[] weekdays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        return (calendar.get(Calendar.MONTH) + 1) + "月" + calendar.get(Calendar.DAY_OF_MONTH) + "日 · " + weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private String formatDateTime(long timestamp) {
        return new SimpleDateFormat("MM/dd HH:mm", Locale.CHINA).format(timestamp);
    }

    private String join(ArrayList<String> items, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) builder.append(separator);
            builder.append(items.get(i));
        }
        return builder.toString();
    }

    // ── View Helpers ──

    private TextView findDateLabel(ViewGroup root) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if ("date".equals(child.getTag()) && child instanceof TextView) {
                return (TextView) child;
            }
        }
        return null;
    }

    private TextView label(String text, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0f, 1.08f);
        if (style != Typeface.NORMAL) {
            view.setTypeface(Typeface.DEFAULT, style);
        }
        return view;
    }

    private TextView actionText(String text, int color) {
        TextView view = label(text, 13, color, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setMinHeight(dp(28));
        return view;
    }

    private TextView actionBtn(String text, int color, View.OnClickListener listener) {
        TextView view = actionText(text, color);
        view.setPadding(0, 0, dp(14), 0);
        view.setOnClickListener(listener);
        return view;
    }

    private TextView pill(String text, int fill, int color) {
        TextView view = label(text, 15, color, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(rounded(fill, 0x00000000, 999));
        return view;
    }

    private TextView navItem(String text) {
        TextView view = label(text, 12, COLOR_FAINT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setLineSpacing(0f, 1.3f);
        return view;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (stroke != 0x00000000) {
            drawable.setStroke(dp(1), stroke);
        }
        return drawable;
    }

    private GradientDrawable topBorderFill(int fill) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setStroke(dp(1), COLOR_LINE);
        return drawable;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private ViewGroup.LayoutParams match() {
        return new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    private FrameLayout.LayoutParams matchFrame() {
        return new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // ── Status Circle (for tasks) ──

    public static final class StatusCircle extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path checkPath = new Path();
        private String status = InboxRecord.STATUS_OPEN;

        public StatusCircle(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void setStatus(String status) {
            this.status = status;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            float cx = width / 2f;
            float cy = height / 2f;
            float radius = Math.min(width, height) / 2f - 2f;

            int stroke = COLOR_FAINT;
            int fill = 0x00000000;
            if (InboxRecord.STATUS_ACTIVE.equals(status)) {
                stroke = COLOR_AMBER;
                fill = COLOR_AMBER_SOFT;
            } else if (InboxRecord.STATUS_COMPLETED.equals(status)) {
                stroke = COLOR_GREEN;
                fill = COLOR_GREEN;
            } else if (InboxRecord.STATUS_ABANDONED.equals(status)) {
                stroke = COLOR_RED;
                fill = COLOR_RED_SOFT;
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fill);
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(stroke);
            canvas.drawCircle(cx, cy, radius, paint);

            if (InboxRecord.STATUS_ACTIVE.equals(status)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(COLOR_AMBER);
                canvas.drawCircle(cx, cy, radius * 0.36f, paint);
            } else if (InboxRecord.STATUS_COMPLETED.equals(status)) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.5f);
                paint.setColor(0xFF07100C);
                checkPath.reset();
                float s = radius * 0.45f;
                checkPath.moveTo(cx - s * 0.8f, cy);
                checkPath.lineTo(cx - s * 0.2f, cy + s * 0.5f);
                checkPath.lineTo(cx + s, cy - s * 0.6f);
                canvas.drawPath(checkPath, paint);
            } else if (InboxRecord.STATUS_ABANDONED.equals(status)) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3f);
                paint.setColor(COLOR_RED);
                float s = radius * 0.5f;
                canvas.drawLine(cx - s, cy, cx + s, cy, paint);
            }
        }
    }

    // ── Archive Circle (for ideas) ──

    public static final class ArchiveCircle extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path checkPath = new Path();
        private boolean archived = false;

        public ArchiveCircle(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void setArchived(boolean archived) {
            this.archived = archived;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            float cx = width / 2f;
            float cy = height / 2f;
            float radius = Math.min(width, height) / 2f - 2f;

            if (archived) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(COLOR_PURPLE);
                canvas.drawCircle(cx, cy, radius, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.5f);
                paint.setColor(0xFFFFFFFF);
                checkPath.reset();
                float s = radius * 0.45f;
                checkPath.moveTo(cx - s * 0.8f, cy);
                checkPath.lineTo(cx - s * 0.2f, cy + s * 0.5f);
                checkPath.lineTo(cx + s, cy - s * 0.6f);
                canvas.drawPath(checkPath, paint);
            } else {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3f);
                paint.setColor(COLOR_FAINT);
                canvas.drawCircle(cx, cy, radius, paint);
            }
        }
    }
}
