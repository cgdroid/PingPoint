package com.tmhnry.pingpoint.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.tmhnry.pingpoint.Keys;
import com.tmhnry.pingpoint.database.Firebase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class User extends Model<User> {
    private static final String PREFERENCES = "user-preferences";
    private static final String RANDOM_ID = "random-id";
    public static final String TABLE_NAME = "users";
    public static final String FULL_NAME = "user-full-name";
    public static final String EMAIL_ADDRESS = "user-email-address";
    public static final String SIGN_UP_DATE = "user-sign-up-date";
    public static final String PASSWORD = "user-password";
    public static final String PHONE = "user-phone";
    public String fullName;
    public String emailAddress;
    public Date signUpDate;
    public String password;
    public String phone;

    public static void initModels(Context context) {
        Users.Create(context);
    }

    public static void reset() {
        Users.instance = null;
    }

    public static Map<Integer, User> getModels() {
        return Users.instance.models;
    }

    public static String getKey() {
        return getModels().values().stream().findAny().get().key;
    }

    public static String getMobileNumber() {
        return getModels().values().stream().findAny().get().phone;
    }


    public static String getFullName() {
        User user = getModels().values().stream().findAny().get();
        return user.fullName;
    }

    public static void update(List<User> models, boolean updateDatabase, int requestCode) {
        if (updateDatabase) {
            update(models, requestCode);
        }
    }

    public static void signOut() {
        Firebase.getAuth().signOut();
        Users.instance.clear();
    }

    public static User getUser() {
        for (User user : User.getModels().values()) {
            return user;
        }
        return null;
    }

    public static void signIn(Map<String, Object> args) {
        Users models = Users.instance;
        Map<String, Object> data = (Map<String, Object>) args.get(Keys.DATA);
        Integer requestCode = (Integer) args.get(Keys.REQUEST_CODE);
        assert (requestCode != null);
        String email = (String) data.get(EMAIL_ADDRESS);
        assert (email != null);
        String password = (String) data.get(PASSWORD);
        assert (password != null);

        models.listener.onStartQuery(TABLE_NAME, requestCode);
        Query query = Firebase.getChildReference(TABLE_NAME).orderByChild(EMAIL_ADDRESS).equalTo(email).limitToFirst(1);
        query.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.hasChildren()) {
                        int matches = 0;
                        for (DataSnapshot obj : snapshot.getChildren()) {
                            Map<String, Object> value = (Map<String, Object>) obj.getValue();
                            boolean match = password.equals(value.get(PASSWORD));
                            if (match) {
                                matches += 1;
                                break;
                            }
                        }
                        if (matches > 0) {
                            Firebase.signInUser(email, password).addOnSuccessListener(authResult -> {
                                retrieve(args);
                            });
                        } else {
                            models.listener.onFailQuery(TABLE_NAME, requestCode);
                        }
                    } else {
                        models.listener.onFailQuery(TABLE_NAME, requestCode);
                    }
                })
                .addOnFailureListener(models.onFailureListener);
    }

    public static void signUp(Map<String, Object> args) {
        Users models = Users.instance;
        Map<String, Object> data = (Map<String, Object>) args.get(Keys.DATA);
        Integer requestCode = (Integer) args.get(Keys.REQUEST_CODE);
        assert (requestCode != null);
        String email = (String) data.get(EMAIL_ADDRESS);
        assert (email != null);
        String password = (String) data.get(PASSWORD);
        assert (password != null);
        models.listener.onStartQuery(TABLE_NAME, requestCode);
        Query query = Firebase.getChildReference(TABLE_NAME).orderByChild(EMAIL_ADDRESS).equalTo(email).limitToFirst(1);
        query.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.hasChildren()) {
                        models.listener.onFailQuery(TABLE_NAME, requestCode);
                    } else {
                        data.put(Model.KEY, User.requestKey());
                        data.put(User.SIGN_UP_DATE, Calendar.getInstance(Locale.getDefault()).getTime());
                        Firebase.createUser(email, password).addOnSuccessListener(authResult ->
                                {
                                    append(Model.List(User.Model(data)), requestCode);
                                }
                        );
                    }
                })
                .addOnFailureListener(models.onFailureListener);
    }

    public static void update(List<User> models, int requestCode) {
        Users instance = Users.instance;
        instance.setRequestCode(requestCode);
        instance.updateCloudDatabase(models);
    }

    public static void append(List<User> models, int requestCode) {
        Users instance = Users.instance;
        instance.setRequestCode(requestCode);
        instance.appendToCloudDatabase(models);
    }

    public static String requestKey() {
        return Users.getInstance().requestKey();
    }

    public static void retrieve(Map<String, Object> args) {
        Users models = Users.instance;
        Map<String, Object> data = (Map<String, Object>) args.get(Keys.DATA);
        Integer requestCode = (Integer) args.get(Keys.REQUEST_CODE);
        assert (requestCode != null);
        String email = (String) data.get(EMAIL_ADDRESS);
        assert (email != null);
        models.listener.onStartQuery(TABLE_NAME, (int) requestCode);

        Firebase.getChildReference(TABLE_NAME).orderByChild(EMAIL_ADDRESS)
                .equalTo((String) email).limitToFirst(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.hasChildren()) {
                        Map<String, Object> _data = (Map<String, Object>) snapshot.getValue();
                        models.clearAndAppend(_data);
                        models.listener.onSuccessQuery(TABLE_NAME, (int) requestCode);
                    } else {
                        models.listener.onFailQuery(TABLE_NAME, (int) requestCode);
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

    public static User getAttendance(String key) {
        return getModels()
                .values()
                .stream()
                .filter((Predicate<Model<User>>) transaction -> transaction.key.equals(key))
                .collect(Collectors.toList()).get(0);
    }


    public static User Model(Map<String, Object> data) {
        return Users.instance.Model(data);
    }

    public User(Integer id, String key, Map<String, Object> data) {
        super(id, key);
        fullName = (String) data.get(FULL_NAME);
        emailAddress = (String) data.get(EMAIL_ADDRESS);
        password = (String) data.get(PASSWORD);
        phone = (String) data.get(PHONE);

        Object signUpDate = data.get(SIGN_UP_DATE);
        if (signUpDate instanceof String) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            try {
                this.signUpDate = format.parse((String) signUpDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            this.signUpDate = (Date) signUpDate;
        }
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

    public static class Users extends Queryables<User> {

        private static Users instance;

        @Override
        public User Model(Map<String, Object> data) {
            String key = (String) data.get(KEY);
            Integer id = (Integer) data.get(ID);
            if (key != null) {
                id = key.hashCode();
            }
            assert id != null;
            return new User(id, key, data);
        }

        public Users(String name,
                     Map<Integer, User> models,
                     FirebaseQueryListener listener) {
            super(name, models, listener);
        }

        public static Users getInstance() {
            return instance;
        }

        public static Users Create(Context context) {
            if (instance == null) {
                @SuppressWarnings("unchecked")
                FirebaseQueryListener listener = (FirebaseQueryListener) context;
                instance = new Users(TABLE_NAME, new HashMap<>(), listener);
            }
            instance.listener = (FirebaseQueryListener) context;
            return instance;
        }

        public static Users Create(FirebaseQueryListener listener) {
            if (instance == null) {
                instance = new Users(TABLE_NAME, new HashMap<>(), listener);
            }
            instance.listener = listener;
            return instance;
        }
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String _signUpDate = dateFormat.format(signUpDate);
        data.put(FULL_NAME, fullName);
        data.put(EMAIL_ADDRESS, emailAddress);
        data.put(SIGN_UP_DATE, _signUpDate);
        data.put(PASSWORD, password);
        data.put(PHONE, phone);
        return data;
    }
}
