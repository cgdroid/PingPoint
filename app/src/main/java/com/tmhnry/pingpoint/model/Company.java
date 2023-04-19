package com.tmhnry.pingpoint.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.database.Query;
import com.tmhnry.pingpoint.Keys;
import com.tmhnry.pingpoint.database.Firebase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Company extends Model<Company> {
    private static final String PREFERENCES = "company-preferences";
    private static final String RANDOM_ID = "random-id";
    public static final String TABLE_NAME = "companies";
    public static final String CODE = "company-code";
    public static final String NAME = "company-name";
    public static final String ADDRESS = "company-address";
    public static final String USER_KEY = "company-user-key";
    public String code;
    public String name;
    public String address;
    public String userKey;

    public static void initModels(Context context) {
        Companies.Create(context);
    }

    public static void reset() {
        Companies.instance = null;
    }

    public static Map<Integer, Company> getModels() {
        return Companies.instance.models;
    }

    public static String getKey() {
        return getModels().values().stream().findAny().get().key;
    }

    public static String getName(){
        return getModels().values().stream().findAny().get().name;
    }

    public static String getCode(){
        return getModels().values().stream().findAny().get().code;
    }

    public static void update(List<Company> models, boolean updateDatabase, int requestCode) {
        if (updateDatabase) {
            update(models, requestCode);
        }
    }

    public static void signUp(Map<String, Object> args) {
        Companies models = Companies.instance;
        Map<String, Object> data = (Map<String, Object>) args.get(Keys.DATA);
        Integer requestCode = (Integer) args.get(Keys.REQUEST_CODE);
        assert (requestCode != null);
        String code = (String) data.get(CODE);
        assert (code != null);
        String userKey = (String) data.get(USER_KEY);
        assert (userKey != null);
        models.listener.onStartQuery(TABLE_NAME, requestCode);
        Query query = Firebase.getChildReference(TABLE_NAME).orderByChild(CODE).equalTo(code).limitToFirst(1);
        query.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.hasChildren()) {
                        models.listener.onFailQuery(TABLE_NAME, requestCode);
                    } else {
                        data.put(Model.KEY, requestKey());
                        append(Model.List(Company.Model(data)), requestCode);
                    }
                })
                .addOnFailureListener(models.onFailureListener);
    }


    public static String requestKey() {
        return Companies.getInstance().requestKey();
    }

    public static void update(List<Company> models, int requestCode) {
        Companies instance = Companies.instance;
        instance.setRequestCode(requestCode);
        instance.updateCloudDatabase(models);
    }

    public static void append(List<Company> models, int requestCode) {
        Companies instance = Companies.instance;
        instance.setRequestCode(requestCode);
        instance.appendToCloudDatabase(models);
    }

    public static void retrieve(Map<String, Object> args) {
        Companies models = Companies.instance;
        Map<String, Object> data = (Map<String, Object>) args.get(Keys.DATA);
        Integer requestCode = (Integer) args.get(Keys.REQUEST_CODE);
        assert (requestCode != null);
        String code = (String) data.get(CODE);
        assert (code != null);
        models.listener.onStartQuery(TABLE_NAME, requestCode);
        Query query = Firebase.getChildReference(TABLE_NAME).orderByChild(CODE).equalTo(code).limitToFirst(1);
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

    public static Company getAttendance(String key) {
        return getModels()
                .values()
                .stream()
                .filter((Predicate<Model<Company>>) transaction -> transaction.key.equals(key))
                .collect(Collectors.toList()).get(0);
    }


    public static Company Model(Map<String, Object> data) {
        return Companies.instance.Model(data);
    }

    public Company(Integer id, String key, Map<String, Object> data) {
        super(id, key);
        userKey = (String) data.get(USER_KEY);
        code = (String) data.get(CODE);
        name = (String) data.get(NAME);
        address = (String) data.get(ADDRESS);
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

    public static class Companies extends Queryables<Company> {

        private static Companies instance;

        @Override
        public Company Model(Map<String, Object> data) {
            String key = (String) data.get(KEY);
            Integer id = (Integer) data.get(ID);
            if (key != null) {
                id = key.hashCode();
            }
            assert id != null;
            return new Company(id, key, data);
        }

        public Companies(String name,
                         Map<Integer, Company> models,
                         FirebaseQueryListener listener) {
            super(name, models, listener);
        }

        public static Companies getInstance() {
            return instance;
        }

        public static Companies Create(Context context) {
            if (instance == null) {
                @SuppressWarnings("unchecked")
                FirebaseQueryListener listener = (FirebaseQueryListener) context;
                instance = new Companies(TABLE_NAME, new HashMap<>(), listener);
            }
            instance.listener = (FirebaseQueryListener) context;
            return instance;
        }

        public static Companies Create(FirebaseQueryListener listener) {
            if (instance == null) {
                instance = new Companies(TABLE_NAME, new HashMap<>(), listener);
            }
            instance.listener = listener;
            return instance;
        }
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        data.put(CODE, code);
        data.put(NAME, name);
        data.put(ADDRESS, address);
        data.put(USER_KEY, userKey);
        return data;
    }
}
