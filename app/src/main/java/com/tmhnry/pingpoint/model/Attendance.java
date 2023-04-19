package com.tmhnry.pingpoint.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.database.Query;
import com.tmhnry.pingpoint.Keys;
import com.tmhnry.pingpoint.database.Firebase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Attendance extends Model<Attendance> {
    private static final String PREFERENCES = "attendance-preferences";
    private static final String RANDOM_ID = "random-id";
    public static final String TABLE_NAME = "attendances";
    public static final String DATE = "date";
    public static final String ENTITY_KEY = "entity-key";

    public static final String TARGET_KEY = "target-key";
    public static final String LOCATION = "location";
    public static final String ENTITY_NAME = "entity-name";
    public Date date;
    public String entityKey;
    public String location;
    public String entityName;

    public String targetKey;

    public static void initModels(Context context) {
        Attendances.Create(context);
    }

    public static void reset() {
        Attendances.instance = null;
    }

    public static Map<Integer, Attendance> getModels() {
        return Attendances.instance.models;
    }


    public static void update(List<Attendance> models, boolean updateDatabase, int requestCode) {
        if (updateDatabase) {
            update(models, requestCode);
        }
    }

    public static String requestKey() {
        return Attendances.instance.requestKey();
    }

    public static void update(List<Attendance> models, int requestCode) {
        Attendances instance = Attendances.instance;
        instance.setRequestCode(requestCode);
        instance.updateCloudDatabase(models);
    }

    public static void append(List<Attendance> models, int requestCode) {
        Attendances instance = Attendances.instance;
        instance.setRequestCode(requestCode);
        instance.appendToCloudDatabase(models);
    }


    public static void retrieve(Map<String, Object> args) {
        Attendances models = Attendances.instance;
        Map<String, Object> data = (Map<String, Object>) args.get(Keys.DATA);
        Integer requestCode = (Integer) args.get(Keys.REQUEST_CODE);
        assert (requestCode != null);
        String entityKey = (String) data.get(ENTITY_KEY);
        String targetKey = (String) data.get(TARGET_KEY);
        assert (entityKey != null || targetKey != null);
        models.listener.onStartQuery(TABLE_NAME, requestCode);
        Query query;
        if (entityKey != null) {
            query = Firebase.getChildReference(TABLE_NAME).orderByChild(ENTITY_KEY).equalTo(entityKey);
        } else {
            query = Firebase.getChildReference(TABLE_NAME).orderByChild(TARGET_KEY).equalTo(targetKey);
        }
        query.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.hasChildren()) {
                        Map<String, Object> _data = (Map<String, Object>) snapshot.getValue();
                        models.clearAndAppend(_data);
                        models.listener.onSuccessQuery(TABLE_NAME, requestCode);
                    } else {
                        models.listener.onFailQuery(TABLE_NAME, requestCode);
                    }
                })
                .addOnFailureListener(models.onFailureListener);
    }

    public static int getRandomPublicId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        int randomId = prefs.getInt(RANDOM_ID, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(RANDOM_ID, randomId + 1);
        editor.apply();
        return randomId;
    }

    public static Attendance getAttendance(String key) {
        return getModels()
                .values()
                .stream()
                .filter((Predicate<Model<Attendance>>) transaction -> transaction.key.equals(key))
                .collect(Collectors.toList()).get(0);
    }


    public static Attendance Model(Map<String, Object> data) {
        return Attendances.instance.Model(data);
    }

    public Attendance(Integer id, String key, Map<String, Object> data) {
        super(id, key);

        Object date = data.get(DATE);
        if (date instanceof String) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            try {
                this.date = format.parse((String) date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            this.date = (Date) date;
        }

        targetKey = (String) data.get(TARGET_KEY);
        entityKey = (String) data.get(ENTITY_KEY);
        location = (String) data.get(LOCATION);
        entityName = (String) data.get(ENTITY_NAME);
    }

    private Integer toInteger(Object value) {
        assert (value != null);
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        } else if (value instanceof Float) {
            return ((Float) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new ClassCastException();
    }

    public static class Attendances extends Queryables<Attendance> {

        private static Attendances instance;

        @Override
        public Attendance Model(Map<String, Object> data) {
            String key = (String) data.get(KEY);
            Integer id = (Integer) data.get(ID);
            if (key != null) {
                id = key.hashCode();
            }
            assert id != null;
            return new Attendance(id, key, data);
        }

        public Attendances(String name,
                           Map<Integer, Attendance> models,
                           FirebaseQueryListener listener) {
            super(name, models, listener);
        }

        public static Attendances getInstance() {
            return instance;
        }

        public static Attendances Create(Context context) {
            if (instance == null) {
                @SuppressWarnings("unchecked")
                FirebaseQueryListener listener = (FirebaseQueryListener) context;
                instance = new Attendances(TABLE_NAME, new HashMap<>(), listener);
            }

            instance.listener = (FirebaseQueryListener) context;
            return instance;
        }

        public static Attendances Create(FirebaseQueryListener listener) {
            if (instance == null) {
                instance = new Attendances(TABLE_NAME, new HashMap<>(), listener);
            }
            instance.listener = listener;
            return instance;
        }
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String _date = dateFormat.format(date);
        data.put(DATE, _date);
        data.put(TARGET_KEY, targetKey);
        data.put(ENTITY_KEY, entityKey);
        data.put(LOCATION, location);
        data.put(ENTITY_NAME, entityName);
        return data;
    }
}
