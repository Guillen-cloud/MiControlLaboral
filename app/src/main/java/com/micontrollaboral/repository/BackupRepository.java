package com.micontrollaboral.repository;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.micontrollaboral.database.AppDatabase;
import com.micontrollaboral.database.EmploymentEntity;
import com.micontrollaboral.database.PaymentAllocationEntity;
import com.micontrollaboral.database.PaymentEntity;
import com.micontrollaboral.database.WorkSessionEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupRepository {
    private static final int BACKUP_VERSION = 1;

    public interface Callback {
        void onSuccess(String message);

        void onError(String message);
    }

    private final AppDatabase database;
    private final ContentResolver contentResolver;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public BackupRepository(AppDatabase database, ContentResolver contentResolver) {
        this.database = database;
        this.contentResolver = contentResolver;
    }

    public void exportTo(Uri destination, Callback callback) {
        executor.execute(() -> {
            try (OutputStream output = contentResolver.openOutputStream(destination)) {
                if (output == null) {
                    postError(callback, "No se pudo abrir el archivo de respaldo.");
                    return;
                }
                output.write(createBackup().toString().getBytes(StandardCharsets.UTF_8));
                postSuccess(callback, "Respaldo exportado correctamente.");
            } catch (IOException | JSONException exception) {
                postError(callback, "No se pudo exportar el respaldo: " + exception.getMessage());
            }
        });
    }

    public void importFrom(Uri source, Callback callback) {
        executor.execute(() -> {
            try (InputStream input = contentResolver.openInputStream(source)) {
                if (input == null) {
                    postError(callback, "No se pudo abrir el respaldo.");
                    return;
                }
                JSONObject backup = new JSONObject(readAll(input));
                validateBackup(backup);
                restoreBackup(backup);
                postSuccess(callback, "Respaldo restaurado correctamente.");
            } catch (IOException | JSONException | IllegalArgumentException exception) {
                postError(callback, "Respaldo rechazado: " + exception.getMessage());
            }
        });
    }

    private JSONObject createBackup() throws JSONException {
        JSONObject backup = new JSONObject();
        backup.put("format", "mi-control-laboral");
        backup.put("version", BACKUP_VERSION);
        backup.put("createdAt", System.currentTimeMillis());
        backup.put("employments", employmentsToJson(database.employmentDao().getAll()));
        backup.put("workSessions", workSessionsToJson(database.workSessionDao().getAll()));
        backup.put("payments", paymentsToJson(database.paymentDao().getAll()));
        backup.put("paymentAllocations", allocationsToJson(database.paymentAllocationDao().getAll()));
        return backup;
    }

    private void validateBackup(JSONObject backup) {
        if (!"mi-control-laboral".equals(backup.optString("format")) || backup.optInt("version", -1) != BACKUP_VERSION) {
            throw new IllegalArgumentException("formato o versión no compatibles");
        }
        if (!backup.has("employments") || !backup.has("workSessions") || !backup.has("payments") || !backup.has("paymentAllocations")) {
            throw new IllegalArgumentException("faltan colecciones de datos");
        }
    }

    private void restoreBackup(JSONObject backup) throws JSONException {
        List<EmploymentEntity> employments = employmentEntities(backup.getJSONArray("employments"));
        List<WorkSessionEntity> workSessions = workSessionEntities(backup.getJSONArray("workSessions"));
        List<PaymentEntity> payments = paymentEntities(backup.getJSONArray("payments"));
        List<PaymentAllocationEntity> allocations = allocationEntities(backup.getJSONArray("paymentAllocations"));
        database.runInTransaction(() -> {
            database.paymentAllocationDao().deleteAll();
            database.paymentDao().deleteAll();
            database.workSessionDao().deleteAll();
            database.employmentDao().deleteAll();
            database.employmentDao().insertAll(employments);
            database.workSessionDao().insertAll(workSessions);
            database.paymentDao().insertAll(payments);
            database.paymentAllocationDao().insertAll(allocations);
        });
    }

    private JSONArray employmentsToJson(List<EmploymentEntity> values) throws JSONException {
        JSONArray result = new JSONArray();
        for (EmploymentEntity value : values) {
            JSONObject item = new JSONObject();
            item.put("id", value.id).put("name", value.name).put("fullRateCents", value.fullRateCents).put("usualDays", value.usualDays).put("active", value.active).put("createdAt", value.createdAt).put("updatedAt", value.updatedAt).put("deletedAt", value.deletedAt == null ? JSONObject.NULL : value.deletedAt);
            result.put(item);
        }
        return result;
    }

    private JSONArray workSessionsToJson(List<WorkSessionEntity> values) throws JSONException {
        JSONArray result = new JSONArray();
        for (WorkSessionEntity value : values) {
            JSONObject item = new JSONObject();
            item.put("id", value.id).put("employmentId", value.employmentId).put("workedDate", value.workedDate).put("sessionType", value.sessionType).put("amountCents", value.amountCents).put("note", value.note == null ? JSONObject.NULL : value.note).put("createdAt", value.createdAt).put("updatedAt", value.updatedAt).put("deletedAt", value.deletedAt == null ? JSONObject.NULL : value.deletedAt);
            result.put(item);
        }
        return result;
    }

    private JSONArray paymentsToJson(List<PaymentEntity> values) throws JSONException {
        JSONArray result = new JSONArray();
        for (PaymentEntity value : values) {
            JSONObject item = new JSONObject();
            item.put("id", value.id).put("employmentId", value.employmentId).put("receivedAt", value.receivedAt).put("totalAmountCents", value.totalAmountCents).put("note", value.note == null ? JSONObject.NULL : value.note).put("createdAt", value.createdAt).put("updatedAt", value.updatedAt).put("deletedAt", value.deletedAt == null ? JSONObject.NULL : value.deletedAt);
            result.put(item);
        }
        return result;
    }

    private JSONArray allocationsToJson(List<PaymentAllocationEntity> values) throws JSONException {
        JSONArray result = new JSONArray();
        for (PaymentAllocationEntity value : values) {
            JSONObject item = new JSONObject();
            item.put("id", value.id).put("paymentId", value.paymentId).put("workSessionId", value.workSessionId).put("allocatedAmountCents", value.allocatedAmountCents);
            result.put(item);
        }
        return result;
    }

    private List<EmploymentEntity> employmentEntities(JSONArray values) throws JSONException {
        List<EmploymentEntity> result = new ArrayList<>();
        for (int index = 0; index < values.length(); index++) {
            JSONObject item = values.getJSONObject(index);
            EmploymentEntity value = new EmploymentEntity();
            value.id = item.getString("id"); value.name = item.getString("name"); value.fullRateCents = item.getInt("fullRateCents"); value.usualDays = item.getString("usualDays"); value.active = item.getBoolean("active"); value.createdAt = item.getLong("createdAt"); value.updatedAt = item.getLong("updatedAt"); value.deletedAt = nullableLong(item, "deletedAt"); result.add(value);
        }
        return result;
    }

    private List<WorkSessionEntity> workSessionEntities(JSONArray values) throws JSONException {
        List<WorkSessionEntity> result = new ArrayList<>();
        for (int index = 0; index < values.length(); index++) {
            JSONObject item = values.getJSONObject(index);
            WorkSessionEntity value = new WorkSessionEntity();
            value.id = item.getString("id"); value.employmentId = item.getString("employmentId"); value.workedDate = item.getString("workedDate"); value.sessionType = item.getString("sessionType"); value.amountCents = item.getInt("amountCents"); value.note = nullableString(item, "note"); value.createdAt = item.getLong("createdAt"); value.updatedAt = item.getLong("updatedAt"); value.deletedAt = nullableLong(item, "deletedAt"); result.add(value);
        }
        return result;
    }

    private List<PaymentEntity> paymentEntities(JSONArray values) throws JSONException {
        List<PaymentEntity> result = new ArrayList<>();
        for (int index = 0; index < values.length(); index++) {
            JSONObject item = values.getJSONObject(index);
            PaymentEntity value = new PaymentEntity();
            value.id = item.getString("id"); value.employmentId = item.getString("employmentId"); value.receivedAt = item.getString("receivedAt"); value.totalAmountCents = item.getInt("totalAmountCents"); value.note = nullableString(item, "note"); value.createdAt = item.getLong("createdAt"); value.updatedAt = item.getLong("updatedAt"); value.deletedAt = nullableLong(item, "deletedAt"); result.add(value);
        }
        return result;
    }

    private List<PaymentAllocationEntity> allocationEntities(JSONArray values) throws JSONException {
        List<PaymentAllocationEntity> result = new ArrayList<>();
        for (int index = 0; index < values.length(); index++) {
            JSONObject item = values.getJSONObject(index);
            PaymentAllocationEntity value = new PaymentAllocationEntity();
            value.id = item.getString("id"); value.paymentId = item.getString("paymentId"); value.workSessionId = item.getString("workSessionId"); value.allocatedAmountCents = item.getInt("allocatedAmountCents"); result.add(value);
        }
        return result;
    }

    private String readAll(InputStream input) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    private String nullableString(JSONObject item, String key) {
        return item.isNull(key) ? null : item.optString(key, null);
    }

    private Long nullableLong(JSONObject item, String key) {
        return item.isNull(key) ? null : item.optLong(key);
    }

    private void postSuccess(Callback callback, String message) {
        mainHandler.post(() -> callback.onSuccess(message));
    }

    private void postError(Callback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}