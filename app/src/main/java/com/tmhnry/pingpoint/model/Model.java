package com.tmhnry.pingpoint.model;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.tmhnry.pingpoint.database.Firebase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Model<T> {
    private static final int APPEND = 0;
    private static final int REMOVE = 1;
    private static final int UPDATE = 2;
    public static final String KEY = "key";
    public static final String ID = "id";

    public String key;
    Integer id;

    public Model(Integer id, String key) {
        this.id = id;
        this.key = key;
    }

    public void updateKeyAndId(String key) {
        this.key = key;
        this.id = key.hashCode();
    }

    public Integer getId() {
        return id;
    }

    public static Map<String, Object> Map(Object... values) {
        assert (values.length % 2 == 0);
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < values.length / 2; i++) {
            data.put((String) values[2 * i], values[2 * i + 1]);
        }
        return data;
    }


    public static <T> List<T> List(T model) {
        List<T> models = new ArrayList<>();
        models.add(model);
        return models;
    }


    public abstract Map<String, Object> toMap();

    public static abstract class Queryables<T extends Model<T>> {
        public final Map<Integer, T> models;
        public final String name;
        public final DatabaseReference reference;
        public FirebaseQueryListener listener;
        // not query specific requestCode
        private int requestCode = -1;

        public abstract T Model(Map<String, Object> data);

        public Queryables(String name, Map<Integer, T> models, FirebaseQueryListener listener) {
            this.reference = Firebase.getChildReference(name);
            this.listener = listener;
            this.name = name;
            this.models = models;
        }

        public void setRequestCode(int requestCode){
            this.requestCode = requestCode;
        }

        public String requestKey() {
            return reference.push().getKey();
        }

        public final OnFailureListener onFailureListener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception error) {
                error.printStackTrace();
                listener.onFailQuery(name, requestCode);
                // revert requestCode to its original value
                requestCode = - 1;
            }
        };

        private final OnSuccessListener<Void> onSuccessListener = new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                listener.onSuccessQuery(name, requestCode);
                // revert requestCode to its original value
                requestCode = -1;
            }
        };

        private void updateReference(Map<String, Object> updates) {
            reference.updateChildren(updates)
                    .addOnSuccessListener(onSuccessListener)
                    .addOnFailureListener(onFailureListener);
        }

        private void createUpdates(List<T> models, int flag) {
            listener.onStartQuery(name, requestCode);
            Map<String, Object> updates = new HashMap<>();
            for (T model : models) {
                if (flag == APPEND) {
                    String key = (model.key != null) ? model.key : reference.push().getKey();
                    assert key != null;
                    model.updateKeyAndId(key);
                    this.models.put(model.id, model);
                }
                if (flag != REMOVE) {
                    updates.put("/" + model.key, model.toMap());
                } else {
                    this.models.remove(model.id);
                    updates.put("/" + model.key, null);
                }
            }
            updateReference(updates);
        }

        public void clearAndAppend(Map<String, Object> data) {
            clear();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> value = (Map<String, Object>) entry.getValue();
                value.put(KEY, entry.getKey());
                T model = Model(value);
                append(model.id, model);
            }

            data.clear();
        }

        public void clear() {
            models.clear();
        }

        public void append(Integer id, T model) {
            models.put(id, model);
        }

        public void updateCloudDatabase(List<T> models) {
            createUpdates(models, UPDATE);
        }

        public void removeFromCloudDatabase(List<T> models) {
            createUpdates(models, REMOVE);
        }

        public void appendToCloudDatabase(List<T> models) {
            createUpdates(models, APPEND);
        }
    }

    public interface FirebaseQueryListener {

        void onStartQuery(String name, int requestCode);

        void onSuccessQuery(String name, int requestCode);

        void onFailQuery(String name, int requestCode);

        static void retrieve() {
        }
    }
}
