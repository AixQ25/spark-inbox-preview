package com.aixq.sparkinbox;

import org.json.JSONException;
import org.json.JSONObject;

final class InboxRecord {
    static final String STATUS_OPEN = "open";
    static final String STATUS_ACTIVE = "active";
    static final String STATUS_COMPLETED = "completed";
    static final String STATUS_ABANDONED = "abandoned";

    String id;
    String content;
    String status;
    long createdAt;
    long startedAt;
    long completedAt;
    long abandonedAt;
    long updatedAt;

    InboxRecord(String id, String content, String status, long createdAt) {
        this.id = id;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }

    static InboxRecord fromJson(JSONObject json) throws JSONException {
        InboxRecord record = new InboxRecord(
            json.getString("id"),
            json.getString("content"),
            json.optString("status", STATUS_OPEN),
            json.optLong("createdAt", System.currentTimeMillis())
        );
        record.startedAt = json.optLong("startedAt", 0L);
        record.completedAt = json.optLong("completedAt", 0L);
        record.abandonedAt = json.optLong("abandonedAt", 0L);
        record.updatedAt = json.optLong("updatedAt", 0L);
        return record;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("content", content);
        json.put("status", status);
        json.put("createdAt", createdAt);
        json.put("startedAt", startedAt);
        json.put("completedAt", completedAt);
        json.put("abandonedAt", abandonedAt);
        json.put("updatedAt", updatedAt);
        return json;
    }
}
