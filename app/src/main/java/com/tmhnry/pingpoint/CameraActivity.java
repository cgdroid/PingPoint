/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.tmhnry.pingpoint;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import com.tmhnry.pingpoint.fragment.EmployeeHomeFragment;
import com.tmhnry.pingpoint.fragment.HomeFragment;
import com.tmhnry.pingpoint.model.Attendance;
import com.tmhnry.pingpoint.model.Company;
import com.tmhnry.pingpoint.model.Entity;
import com.tmhnry.pingpoint.model.Model;
import com.tmhnry.pingpoint.model.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main activity of the classifier demo app.
 */
public class CameraActivity extends FragmentActivity implements Model.FirebaseQueryListener {
    private static int CREATE_ATTENDANCE = 9;
    public boolean handlerStarted = false;
    private Dialog prompt;
    private Dialog loading;
    private Dialog reset;
    LocationRequest request;
    TextView alertPos, alertNeg;
    TextView alertTitle, alertMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // If we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        PermissionsFragment firstFragment = new PermissionsFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, firstFragment)
                .commit();

        getSupportFragmentManager()
                .addFragmentOnAttachListener(
                        (fragmentManager, fragment) -> {
                            if (fragment instanceof PermissionsFragment) {
                                ((PermissionsFragment) fragment)
                                        .setOnPermissionsAcquiredListener(
                                                () -> {
                                                    CameraFragment cameraFragment = new CameraFragment();

                                                    getSupportFragmentManager()
                                                            .beginTransaction()
                                                            .replace(R.id.fragment_container, cameraFragment)
                                                            .commit();
                                                });
                            }
                        });
//
        Attendance.initModels(this);

        request = LocationRequest.create();
        prompt = new Dialog(this);
        prompt.setContentView(R.layout.dialog_alert_notification);
        prompt.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertTitle = prompt.findViewById(R.id.alert_title);
        alertPos = prompt.findViewById(R.id.alert_pos);
        alertNeg = prompt.findViewById(R.id.alert_neg);
        alertMessage = prompt.findViewById(R.id.alert_message);

        if (VisionModelProvider.getCategoryCount().size() == 0) {
            alertTitle.setText("Welcome");
            alertMessage.setText("We need enough face samples for identification. Long press the identified button until you get at least 40 and at most 50 samples");
            prompt.show();
        } else {
            alertTitle.setText("How to track");
            alertMessage.setText("Configure the system by clicking the Train button. After training, click the pause button. Put your face in front of the camera and start tracking by clicking the track button above");
            prompt.show();
        }


        alertPos.setOnClickListener(view -> {
            prompt.cancel();
        });
        alertNeg.setOnClickListener(view -> {
            prompt.cancel();
        });
        alertMessage = prompt.findViewById(R.id.alert_message);

        loading = new Dialog(this);
        loading.setContentView(R.layout.dialog_loading_indicator);
        loading.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    private void onDataSave(String message) {
        ((TextView) loading.findViewById(R.id.txt_loading_message)).setText(message);
        loading.show();
        new Handler().postDelayed(() -> {
            loading.cancel();
            VisionModelProvider.getModel().close();
            VisionModelProvider.setModel(null);
            onBackPressed();
        }, 3000);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(CameraActivity.this, MainActivity.class));
        finish();
    }

    public void onIdentityVerified() {
        handlerStarted = true;
        reset = new Dialog(this);
        reset.setContentView(R.layout.dialog_alert_notification);
        reset.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView alertPos, alertNeg;
        TextView alertTitle, alertMessage;
        alertTitle = reset.findViewById(R.id.alert_title);
        alertPos = reset.findViewById(R.id.alert_pos);
        alertNeg = reset.findViewById(R.id.alert_neg);
        alertMessage = reset.findViewById(R.id.alert_message);
        alertTitle.setText("Continue?");
        alertMessage.setText("You have been identitied as " + User.getFullName() + ". Do you want to send this to the server? Click yes to continue. Click no to restart tracking");
        alertNeg.setOnClickListener(view -> {
            handlerStarted = false;
            reset.cancel();
        });
        alertPos.setOnClickListener(view -> {
            reset.cancel();
            onAddAttendance();
            onDataSave("Please wait while data linking is in progress...");
        });
        reset.show();
    }

    public void onAddAttendance() {
        Toast.makeText(this, "Getting location, please wait", Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            Fragment fragment = getSupportFragmentManager().findFragmentByTag(HomeFragment.TAG);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    LocationServices.getFusedLocationProviderClient(CameraActivity.this)
                            .requestLocationUpdates(request, new LocationCallback() {
                                @Override
                                public void onLocationResult(@NonNull LocationResult result) {
                                    super.onLocationResult(result);
                                    LocationServices.getFusedLocationProviderClient(CameraActivity.this).removeLocationUpdates(this);

                                    if (result != null && result.getLocations().size() > 0) {
                                        int index = result.getLocations().size() - 1;
                                        Location location = result.getLastLocation();
                                        updateLocation(location);
//                                        if (fragment != null) {
//                                            ((HomeFragment) fragment).updateAttendances();
//                                        }
                                    }
                                }
                            }, Looper.getMainLooper());
                } else {
                    turnOnGPS();
                }
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }


    public void updateLocation(Location location) {
        Geocoder coder;
        List<Address> addresses;
        String line = "";
        coder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            addresses = coder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.isEmpty()) {
                throw new IOException();
            }
            Address address = addresses.get(0);
            line = address.getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Object> data = new HashMap<>();
        data.put(Attendance.KEY, Attendance.requestKey());
        data.put(Attendance.ENTITY_NAME, User.getFullName());
        data.put(Attendance.ENTITY_KEY, User.getKey());
        data.put(Attendance.TARGET_KEY, "");
        data.put(Attendance.LOCATION, line);
        data.put(Attendance.DATE, Chrono.now());

        List<Attendance> attendances = Model.List(Attendance.Model(data));
        Attendance.append(attendances, CREATE_ATTENDANCE);
    }

    private boolean isGPSEnabled() {
        LocationManager manager = null;

        if (manager == null) {
            manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        boolean isEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;

    }

    private void turnOnGPS() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(request);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices
                .getSettingsClient(this)
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(task -> {
            try {
                LocationSettingsResponse response = task.getResult(ApiException.class);
                Toast.makeText(CameraActivity.this, "GPS is already turned on", Toast.LENGTH_SHORT).show();
            } catch (ApiException e) {
                switch (e.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException ex = (ResolvableApiException) e;
                            ex.startResolutionForResult(CameraActivity.this, 2);

                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isGPSEnabled()) {
                    onAddAttendance();
                } else {
                    turnOnGPS();
                }
            }
        }
    }


    @Override
    public void onStartQuery(String name, int requestCode) {

    }

    @Override
    public void onSuccessQuery(String name, int requestCode) {

    }

    @Override
    public void onFailQuery(String name, int requestCode) {

    }
}
