package com.aixq.sparkinbox;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.UUID;

final class RecordStore {
    private static final String PREFS = "spark_inbox";
    private static final String KEY_RECORDS = "records_v1";

    private final SharedPreferences prefs;

    RecordStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    ArrayList<InboxRecord> loadRecords() {
        String raw = prefs.getString(KEY_RECORDS, "");
        if (raw == null || raw.isEmpty()) {
            return seedRecords();
        }

        try {
            JSONArray array = new JSONArray(raw);
            ArrayList<InboxRecord> records = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                records.add(InboxRecord.fromJson(array.getJSONObject(i)));
            }
            return records;
        } catch (JSONException ignored) {
            return seedRecords();
        }
    }

    void saveRecords(ArrayList<InboxRecord> records) {
        JSONArray array = new JSONArray();
        for (InboxRecord record : records) {
            try {
                array.put(record.toJson());
            } catch (JSONException ignored) {
                // Skip malformed records rather than losing the whole list.
            }
        }
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply();
    }

    private ArrayList<InboxRecord> seedRecords() {
        long now = System.currentTimeMillis();
        long minute = 60_000L;
        ArrayList<InboxRecord> records = new ArrayList<>();

        InboxRecord car = new InboxRecord(
            UUID.randomUUID().toString(),
            "智能小车：先想清楚循迹和避障第一版怎么取舍",
            InboxRecord.STATUS_ACTIVE,
            now - minute * 180
        );
        car.startedAt = now - minute * 90;
        records.add(car);

        records.add(new InboxRecord(
            UUID.randomUUID().toString(),
            "HTML视频标签要查 controls、poster、source 三个点",
            InboxRecord.STATUS_OPEN,
            now - minute * 260
        ));

        records.add(new InboxRecord(
            UUID.randomUUID().toString(),
            "酒馆学习可能需要先整理一个最小可开始清单",
            InboxRecord.STATUS_OPEN,
            now - minute * 620
        ));

        InboxRecord completed = new InboxRecord(
            UUID.randomUUID().toString(),
            "app 不应该只是记录，更应该是想法的暂存入口",
            InboxRecord.STATUS_COMPLETED,
            now - minute * 1400
        );
        completed.startedAt = now - minute * 1000;
        completed.completedAt = now - minute * 620;
        records.add(completed);

        InboxRecord abandoned = new InboxRecord(
            UUID.randomUUID().toString(),
            "先不做复杂优先级矩阵，第一版会太重",
            InboxRecord.STATUS_ABANDONED,
            now - minute * 1800
        );
        abandoned.abandonedAt = now - minute * 1200;
        records.add(abandoned);

        saveRecords(records);
        return records;
    }
}
