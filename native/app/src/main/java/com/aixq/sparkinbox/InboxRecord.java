package com.aixq.sparkinbox;

import org.json.JSONException;
import org.json.JSONObject;

final class InboxRecord {
    static final String TYPE_TASK = "task";
    static final String TYPE_IDEA = "idea";

    static final String STATUS_OPEN = "open";
    static final String STATUS_ACTIVE = "active";
    static final String STATUS_COMPLETED = "completed";
    static final String STATUS_ABANDONED = "abandoned";
    static final String STATUS_UNPROCESSED = "unprocessed";
    static final String STATUS_ARCHIVED = "archived";

    String id;
    String content;
    String type;
    String status;
    long createdAt;
    long startedAt;
    long completedAt;
    long abandonedAt;
    long archivedAt;
    long updatedAt;

    InboxRecord(String id, String content, String type, String status, long createdAt) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    static InboxRecord fromJson(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String content = json.getString("content");
        String type = json.optString("type", TYPE_TASK);
        String status = json.optString("status", STATUS_OPEN);
        long createdAt = json.optLong("createdAt", System.currentTimeMillis());

        InboxRecord record = new InboxRecord(id, content, type, status, createdAt);
        record.startedAt = json.optLong("startedAt", 0L);
        record.completedAt = json.optLong("completedAt", 0L);
        record.abandonedAt = json.optLong("abandonedAt", 0L);
        record.archivedAt = json.optLong("archivedAt", 0L);
        record.updatedAt = json.optLong("updatedAt", 0L);
        return record;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("content", content);
        json.put("type", type);
        json.put("status", status);
        json.put("createdAt", createdAt);
        json.put("startedAt", startedAt);
        json.put("completedAt", completedAt);
        json.put("abandonedAt", abandonedAt);
        json.put("archivedAt", archivedAt);
        json.put("updatedAt", updatedAt);
        return json;
    }
}
