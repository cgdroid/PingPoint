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

public class Entity extends Model<Entity> {

    public static Map<Integer, Entity> getModels() {
        return Entities.instance.models;
    }

    public static void initModels(Context context) {
        Entities.Create(context);
    }

    public static void update(List<Entity> models, boolean updateDatabase, int requestCode) {
        if (updateDatabase) {
            update(models, requestCode);
        }
    }

    public static String getCompanyCode() {
        return getModels().values().stream().findAny().get().companyCode;
    }

    public static String getKey(){
        return getModels().values().stream().findAny().get().key;
    }


    public static String getType(){
        return getModels().values().stream().findAny().get().type;
    }

    public static void update(List<Entity> models, int requestCode) {
        Entities instance = Entities.instance;
        instance.setRequestCode(requestCode);
        instance.updateCloudDatabase(models);
    }

    public static String getFullName(){
        return getModels().values().stream().findAny().get().fullName;
    }

    public static String requestKey() {
        return Entities.instance.requestKey();
    }

    public static void append(List<Entity> models, int requestCode) {
        Entities instance = Entities.instance;
        instance.setRequestCode(requestCode);
        instance.appendToCloudDatabase(models);
    }

    public static void retrieve(Map<String, Object> args) {
        Entities models = Entities.instance;
        Map<String, Object> data = (Map<String, Object>) args.get(Keys.DATA);
        Integer requestCode = (Integer) args.get(Keys.REQUEST_CODE);
        assert (requestCode != null);
        String userKey = (String) data.get(USER_KEY);
        assert (userKey != null);
        Query query = Firebase.getChildReference(TABLE_NAME).orderByChild(USER_KEY).equalTo(userKey);
        query.get().addOnSuccessListener(snapshot -> {
            if (snapshot.hasChildren()) {
                Map<String, Object> _data = (Map<String, Object>) snapshot.getValue();
                models.clearAndAppend(_data);
                models.listener.onSuccessQuery(TABLE_NAME,requestCode);
            } else {
                models.listener.onFailQuery(TABLE_NAME, requestCode);
            }
        }).addOnFailureListener(models.onFailureListener);
    }

    public static void reset() {
        Entities.instance = null;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String _joinDate = dateFormat.format(joinDate);
        data.put(JOIN_DATE, _joinDate);
        data.put(USER_KEY, userKey);
        data.put(COMPANY_CODE, companyCode);
        data.put(COMPANY_KEY, companyKey);
        data.put(MOBILE, mobile);
        data.put(GENDER, gender.code);
        data.put(FULL_NAME, fullName);
        data.put(COMPANY_NAME, companyName);
        data.put(ADDRESS, address);
        data.put(TYPE, type);
        return data;
    }

    public static Entity Model(Map<String, Object> data) {
        return Entities.instance.Model(data);
    }

    public static int getRandomPublicId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        int randomId = prefs.getInt(RANDOM_ID, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(RANDOM_ID, randomId++);
        editor.apply();
        return randomId;
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

    private Double toDouble(Object value) {
        assert (value != null);
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        }
        throw new ClassCastException();
    }

    public Entity(Integer id, String key, Map<String, Object> data) {
        super(id, key);

        userKey = (String) data.get(USER_KEY);
        companyKey = (String) data.get(COMPANY_KEY);
        companyCode = (String) data.get(COMPANY_CODE);
        address = (String) data.get(ADDRESS);
        fullName = (String) data.get(FULL_NAME);
        companyName = (String) data.get(COMPANY_NAME);
        mobile = (String) data.get(MOBILE);
        type = (String) data.get(TYPE);

        Object gender = data.get(GENDER);
        if (gender instanceof Long) {
            this.gender = Gender.getGender(toInteger(gender));
        } else if (gender instanceof Gender) {
            this.gender = (Gender) gender;
        } else {
            throw new ClassCastException();
        }

        Object joinDate = data.get(JOIN_DATE);
        if (joinDate instanceof String) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            try {
                this.joinDate = format.parse((String) joinDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            this.joinDate = (Date) joinDate;
        }

    }

    public static class Entities extends Queryables<Entity> {
        private static Entities instance;

        public static Entities getInstance() {
            return instance;
        }

        @Override
        public Entity Model(Map<String, Object> data) {
            String key = (String) data.get(KEY);
            Integer id = (Integer) data.get(ID);
            if (key != null) {
                id = key.hashCode();
            }
            assert id != null;
            return new Entity(id, key, data);
        }

        public Entities(String name, Map<Integer, Entity> models, FirebaseQueryListener listener) {
            super(name, models, listener);
        }

        public static Entities Create(Context context) {
            if (instance == null) {
                @SuppressWarnings("unchecked")
                FirebaseQueryListener listener = (FirebaseQueryListener) context;
                instance = new Entities(TABLE_NAME, new HashMap<>(), listener);
            }
            return instance;
        }

        public static Entities Create(FirebaseQueryListener listener) {
            if (instance == null) {
                instance = new Entities(TABLE_NAME, new HashMap<>(), listener);
            }
            return instance;
        }
    }

    public enum Gender {
        MALE(1),
        FEMALE(0);
        int code;

        Gender(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            if (code == 0) {
                return "Female";
            }
            return "Male";
        }

        public static Gender getGender(int code) {
            if (code == 0) {
                return FEMALE;
            }
            return MALE;
        }
    }

    public enum MaritalStatus {
        SINGLE(2),
        MARRIED(1),
        DIVORCED(0);
        int code;

        MaritalStatus(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static MaritalStatus getMaritalStatus(int code) {
            if (code == 0) {
                return DIVORCED;
            } else if (code == 1) {
                return MARRIED;
            } else {
                return SINGLE;
            }
        }

        public String getName() {
            switch (code) {
                case 0:
                    return "Single";
                case 1:
                    return "Married";
                default:
                    return "Divorced";
            }
        }
    }

    public static final String TABLE_NAME = "entities";
    private static final String PREFERENCES = "entity-preferences";
    private static final String RANDOM_ID = "random-id";

    public static final String USER_KEY = "entity-user-key";
    public static final String FULL_NAME = "entity-full-name";
    public static final String GENDER = "entity-gender";
    public static final String JOIN_DATE = "entity-join-date";
    public static final String MOBILE = "entity-mobile";
    public static final String ADDRESS = "entity-address";
    public static final String COMPANY_NAME = "entity-company-name";
    public static final String COMPANY_CODE = "entity-company-code";
    public static final String COMPANY_KEY = "entity-company-key";
    public static final String TYPE = "entity-type";
    public static final String ADMIN = "Admin";
    public static final String MEMBER = "Member";

    public String userKey;
    public String fullName;
    public String companyName;
    public String type;
    public String address;
    public String companyKey;
    public String companyCode;
    public String mobile;
    public Date joinDate;
    public Gender gender;
}
