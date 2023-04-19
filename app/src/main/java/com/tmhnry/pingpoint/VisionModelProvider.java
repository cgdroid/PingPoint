package com.tmhnry.pingpoint;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.tmhnry.pingpoint.vision.CameraUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class VisionModelProvider {
    private static VisionModelProvider instance;
    private LoggingBenchmark inferenceBenchmark;
    private TransferLearningModelWrapper model;
    private boolean isReady;
    Map<String, Integer> categoryCount;
    Map<String, Integer> rotations;
    Map<String, Bitmap> bitmaps;
    Map<String, String> categories;
    SQLiteDatabase database;

    public static Map<String, Integer> getCategoryCount() {
        assert (instance != null);
        return instance.categoryCount;
    }

    public static VisionModelProvider setModel(TransferLearningModelWrapper model) {
        assert (instance != null);
        instance.model = model;
        return instance;
    }

    public static boolean isReady() {
        return instance.isReady;
    }

    public VisionModelProvider() {
        isReady = false;
        inferenceBenchmark = new LoggingBenchmark("InferenceBench");
        rotations = new ConcurrentHashMap<>();
        bitmaps = new ConcurrentHashMap<>();
        categories = new ConcurrentHashMap<>();
        categoryCount = new ConcurrentHashMap<>();
    }

    public static VisionModelProvider Create() {
        if (instance == null) {
            instance = new VisionModelProvider();
        }
        return instance;
    }

    public VisionModelProvider connectToDB(Context context) {
        database = context.openOrCreateDatabase("EmployeeTracker", MODE_PRIVATE, null);
        String
                category = "category",
                id = "id",
                image = "image",
                name = "name",
                rotation = "rotation";
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS images" +
                "(" +
                id + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                image + " VARCHAR(255)," +
                name + " VARCHAR(255)," +
                category + " VARCHAR(255)," +
                rotation + " INTEGER" +
                ")";
        database.execSQL(CREATE_TABLE);
        return this;
    }

    public static void deleteData(Context context) {
        assert (instance != null);
        instance.database.delete("images", null, null);
        context.deleteDatabase("EmployeeTracker");
        File sharedPrefs = context.getDir("shared_prefs", MODE_PRIVATE);
        File sslCache = context.getDir("sslcache", MODE_PRIVATE);
        File dataset = context.getDir("dataset", MODE_PRIVATE);
        File databases = context.getDir("databases", MODE_PRIVATE);
        File cache = context.getCacheDir();
        File codeCache = context.getCodeCacheDir();
        deleteRecursive(sslCache);
        deleteRecursive(dataset);
        deleteRecursive(databases);
        deleteRecursive(cache);
        deleteRecursive(codeCache);
//        deleteRecursive(sharedPrefs);
        if (instance.model != null) {
            instance.model.deleteSamples();
            instance.model.close();
            instance.model = null;
        }
    }

    public static void deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }

    public static LoggingBenchmark getBenchmark() {
        assert (instance != null);
        return instance.inferenceBenchmark;
    }

    public static TransferLearningModelWrapper getModel() {
        assert (instance != null);
        return instance.model;
    }

    public void retrieveLocalSamples(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                categoryCount.clear();
                isReady = false;
                Cursor cursor = database.rawQuery("SELECT * FROM images ORDER BY category", null);
                while (cursor.moveToNext()) {
                    String image = cursor.getString(1);
                    String name = cursor.getString(2);
                    String category = cursor.getString(3);
                    int rotation = cursor.getInt(4);
                    File imageDir = context.getDir("dataset", MODE_PRIVATE);
                    File file = new File(imageDir, image + ".png");
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                    inferenceBenchmark.startStage(name, "preprocess");
                    float[][][] array =
                            CameraUtils.prepareCameraImage(bitmap, rotation);
                    inferenceBenchmark.endStage(name, "preprocess");

                    Map<String, Object> data = new HashMap<>();
                    data.put("rotation", rotation);
                    data.put("id", name);
                    data.put("image", array);
                    data.put("bitmap", bitmap);
                    data.put("category", category);

                    addSampleToModel(false, data);
                }
                isReady = true;
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }).start();
    }


    public String addSampleToModel(boolean save, Map<String, Object> data) {
        float[][][] image = (float[][][]) data.get("image");
        Integer rotation = (Integer) data.get("rotation");
        Bitmap bitmap = (Bitmap) data.get("bitmap");
        String id = (String) data.get("id");
        String category = (String) data.get("category");

        int currentNumber;
        if (categoryCount.containsKey(category)) {
            currentNumber = categoryCount.get(category);
        } else {
            currentNumber = 0;
        }
        categoryCount.put(category, currentNumber + 1);

        inferenceBenchmark.startStage(id, "addSample");
        try {
            if (model == null) {
                throw new NullPointerException();
            }
            model.addSample(image, category).get();
            if (save) {
                categories.put(id, category);
                bitmaps.put(id, bitmap);
                rotations.put(id, rotation);
            }
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to add sample to model", e.getCause());
        } catch (InterruptedException e) {
            // no-op
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        inferenceBenchmark.endStage(id, "addSample");
        return category;
    }

    public void saveSamplesLocally(Context context) {
        new Thread(() -> {
            File imageDir = context.getDir("dataset", MODE_PRIVATE);
            String q = "'";
            Iterator<String> iterator = bitmaps.keySet().iterator();
            while (iterator.hasNext()) {
                String id = iterator.next();
                String image = String.valueOf(id.hashCode());
                File file = new File(imageDir, String.valueOf(id.hashCode()) + ".png");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmaps.get(id).compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String query = "INSERT INTO images(image, name, category, rotation) VALUES('" +
                        image + "' , '" +
                        id + "' , '" +
                        categories.get(id) + "' , " +
                        rotations.get(id) + ")";
                database.execSQL(query);
            }
        }).start();
    }

}
