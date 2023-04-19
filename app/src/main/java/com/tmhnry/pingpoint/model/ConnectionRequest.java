package com.tmhnry.pingpoint.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.tmhnry.pingpoint.database.Firebase;
import com.tmhnry.pingpoint.Keys;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConnectionRequest extends Model<ConnectionRequest> {
    private static final String PREFERENCES = "notification-preferences";
    private static final String RANDOM_ID = "random-id";
    public static final String TABLE_NAME = "connection-requests";
    public static final String MESSAGE = "message";
    public static final String TITLE = "title";
    public static final String DATE = "date";
    public static final String OPENED = "opened";
    public static final String COMPANY_KEY = "company-key";
    public static final String SENDER_KEY = "sender-key";
    public static final String SENDER_NAME = "sender-name";
    public static final String GENDER = "gender";
    public static final String ADDRESS = "address";
    public static final String MOBILE = "mobile";
    public String companyKey;
    public String message;
    public String title;
    public String senderKey;
    public String senderName;
    public String gender;
    public String address;
    public String mobile;
    public Boolean opened;
    public Date date;

    public static void initModels(Context context) {
        Applications.Create(context);
    }

    public static void reset() {
        Applications.instance = null;
    }

    public static Map<Integer, ConnectionRequest> getModels() {
        return Applications.instance.models;
    }


    public static void update(List<ConnectionRequest> models, boolean updateDatabase) {
        if (updateDatabase) {
            update(models);
        }
    }

    public static void update(List<ConnectionRequest> models) {
        Applications instance = Applications.instance;
        instance.updateCloudDatabase(models);
    }

    public static void append(List<ConnectionRequest> models) {
        Applications instance = Applications.instance;
        instance.appendToCloudDatabase(models);
    }


    public static void retrieve(Context context, Map<String, Object> args) {
        Applications models = Applications.instance;
        Object requestCode = args.get(Keys.REQUEST_CODE);
        assert (requestCode instanceof Integer);
        Object companyKey = args.get(COMPANY_KEY);
        assert (companyKey instanceof String);
        Query query = Firebase.getChildReference(TABLE_NAME).orderByChild(COMPANY_KEY).equalTo((String) companyKey);
        models.listener.onStartQuery(TABLE_NAME, (int) requestCode);
        query
                .get()
                .addOnSuccessListener(all -> {
                    if (!all.exists()) {
                        models.listener.onFailQuery(TABLE_NAME, (int) requestCode);
                    } else {
                        Map<String, Object> data = new HashMap<>();
                        for (DataSnapshot snapshot : all.getChildren()) {
                            data.put(snapshot.getKey(), snapshot.getValue());
                        }
                        models.clearAndAppend(data);
                        models.listener.onSuccessQuery(TABLE_NAME, (int) requestCode);
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

    public static ConnectionRequest getNotification(String key) {
        return getModels()
                .values()
                .stream()
                .filter((Predicate<Model<ConnectionRequest>>) transaction -> transaction.key.equals(key))
                .collect(Collectors.toList()).get(0);
    }


    public static ConnectionRequest Model(Map<String, Object> data) {
        return Applications.instance.Model(data);
    }

    public ConnectionRequest(Integer id, String key, Map<String, Object> data) {
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

        gender = (String) data.get(GENDER);
        address = (String) data.get(ADDRESS);
        companyKey = (String) data.get(COMPANY_KEY);
        senderKey = (String) data.get(SENDER_KEY);
        senderName = (String) data.get(SENDER_NAME);
        mobile = (String) data.get(MOBILE);
        opened = (Boolean) data.get(OPENED);
        title = (String) data.get(TITLE);
        message = (String) data.get(MESSAGE);
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

    public static class Applications extends Queryables<ConnectionRequest> {

        private static Applications instance;

        @Override
        public ConnectionRequest Model(Map<String, Object> data) {
            return null;
        }

        public Applications(String name,
                            Map<Integer, ConnectionRequest> models,
                            FirebaseQueryListener listener) {
            super(name, models, listener);
        }

        public static Applications getInstance() {
            return instance;
        }

        public static Applications Create(Context context) {
            if (instance == null) {
                @SuppressWarnings("unchecked")
                FirebaseQueryListener listener = (FirebaseQueryListener) context;
                instance = new Applications(TABLE_NAME, new HashMap<>(), listener);
            }
            instance.listener = (FirebaseQueryListener) context;
            return instance;
        }

        public static Applications Create(FirebaseQueryListener listener) {
            if (instance == null) {
                instance = new Applications(TABLE_NAME, new HashMap<>(), listener);
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
        data.put(MESSAGE, message);
        data.put(TITLE, title);
        data.put(OPENED, opened);
        data.put(COMPANY_KEY, companyKey);
        data.put(SENDER_NAME, senderName);
        data.put(SENDER_KEY, senderKey);
        data.put(GENDER, gender);
        data.put(ADDRESS, address);
        data.put(MOBILE, mobile);
        return data;
    }
}
