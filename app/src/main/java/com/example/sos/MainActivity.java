package com.example.sos;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int SMS_PERMISSION_REQUEST_CODE = 2;
    private static final int REQUEST_CHECK_SETTINGS = 3;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    double latitude = 0.0;
    double longitude = 0.0;
    Button add, show, get;
    ImageView imageView, imageView1, imageView2, imageView3;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseHelper databaseHelper;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private CameraManager cameraManager;
    private boolean isCapturing = false;
    private Bitmap frontCameraBitmap, backCameraBitmap;
    private boolean isSwitchingCamera = false;
    private List<File> capturedImageFiles = new ArrayList<>();


    private boolean isTrackingLocation = false;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;


    private boolean isLocationAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        add = findViewById(R.id.add);
        show = findViewById(R.id.show);
        get = findViewById(R.id.get);
        imageView = findViewById(R.id.imvv4);
        imageView1 = findViewById(R.id.imvv3);
        imageView2 = findViewById(R.id.imvv2);
        imageView3 = findViewById(R.id.imvv1);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(3000);


        getCurrentLocation();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                triggerSiren(MainActivity.this);
            }
        });

        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendSMSWithLocation();
            }
        });

        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openEmailApp();
            }
        });

        imageView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    captureAndSendEmail();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST_CODE);
                }
            }
        });

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddContect.class);
                startActivity(intent);
            }
        });

        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ShowContect.class);
                startActivity(intent);
            }
        });


        get.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                } else {

                    startLocationTrackingAndSendSMS();
                }
            }
        });
    }

    private void triggerSiren(Context context) {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.siren);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                Log.d("Siren", "Siren started.");
            } else {
                Log.e("Siren", "Failed to initialize MediaPlayer.");
            }
        } catch (Exception e) {
            Log.e("Siren", "Error playing siren: " + e.getMessage());
        }
    }


    private void sendSMSWithLocation() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
            return;
        }


        if (latitude == 0.0 || longitude == 0.0 || !isLocationAvailable) {

            Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show();
            getLocationForSMS();
        } else {

            sendSmsToAllContactsForAlert();
        }
    }


    private void getLocationForSMS() {
        LocationRequest tempRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(tempRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    isLocationAvailable = true;

                    SharedPreferences sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("LastLatitude", String.valueOf(latitude));
                    editor.putString("LastLongitude", String.valueOf(longitude));
                    editor.apply();

                    Log.d("MainActivity", "Location for SMS: " + latitude + ", " + longitude);


                    sendSmsToAllContactsForAlert();
                } else {

                    fusedLocationClient.requestLocationUpdates(tempRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (locationResult != null && locationResult.getLocations().size() > 0) {
                                Location loc = locationResult.getLastLocation();
                                if (loc != null) {
                                    latitude = loc.getLatitude();
                                    longitude = loc.getLongitude();
                                    isLocationAvailable = true;

                                    SharedPreferences sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("LastLatitude", String.valueOf(latitude));
                                    editor.putString("LastLongitude", String.valueOf(longitude));
                                    editor.apply();


                                    fusedLocationClient.removeLocationUpdates(this);


                                    sendSmsToAllContactsForAlert();
                                }
                            }
                        }
                    }, Looper.getMainLooper());
                }
            });
        }).addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ((ResolvableApiException) e).startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e("MainActivity", "Error resolving location settings: " + sendEx.getMessage());
                }
            }
        });
    }

    private void getCurrentLocation() {
        LocationRequest tempRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(tempRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    isLocationAvailable = true;

                    SharedPreferences sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("LastLatitude", String.valueOf(latitude));
                    editor.putString("LastLongitude", String.valueOf(longitude));
                    editor.apply();

                    Log.d("MainActivity", "Initial Location: " + latitude + ", " + longitude);
                }
            });
        }).addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ((ResolvableApiException) e).startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e("MainActivity", "Error resolving location settings: " + sendEx.getMessage());
                }
            }
        });
    }


    private void startLocationTrackingAndSendSMS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }


        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                isLocationAvailable = true;


                SharedPreferences sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("LastLatitude", String.valueOf(latitude));
                editor.putString("LastLongitude", String.valueOf(longitude));
                editor.apply();


                String locationStr = String.format(Locale.getDefault(), "Lat: %.6f, Lon: %.6f", latitude, longitude);
                Toast.makeText(this, "Location: " + locationStr, Toast.LENGTH_LONG).show();


                startContinuousLocationTracking();


                sendTrackingSMS();

            } else {
                Toast.makeText(this, "Unable to get location. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Location error: " + e.getMessage());
        });
    }


    private void startContinuousLocationTracking() {
        if (isTrackingLocation) {
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        isLocationAvailable = true;


                        SharedPreferences sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("LastLatitude", String.valueOf(latitude));
                        editor.putString("LastLongitude", String.valueOf(longitude));


                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(new Date());
                        editor.putString("LastUpdateTime", timestamp);
                        editor.apply();

                        Log.d("MainActivity", "Tracked Location: " + latitude + ", " + longitude + " at " + timestamp);
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            isTrackingLocation = true;
            Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
        }
    }


    private void stopLocationTracking() {
        if (isTrackingLocation && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isTrackingLocation = false;
            Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show();
        }
    }


    private void sendSmsToAllContactsForAlert() {


        ArrayList<ContactsAdapter.Contact> contacts = databaseHelper.getAllContacts();

        if (contacts.isEmpty()) {
            Log.d("TestActivity", "No contacts found in the database.");
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        String message = "My current location: https://maps.google.com/?q=" + latitude + "," + longitude;

        for (ContactsAdapter.Contact contact : contacts) {
            String phoneNumber = contact.getPhone();

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                try {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                    Log.d("TestActivity", "SMS sent to " + contact.getName() + " (" + phoneNumber + ")");
                } catch (Exception e) {
                    Log.e("TestActivity", "Failed to send SMS to " + contact.getName() + " (" + phoneNumber + ")", e);
                }
            } else {
                Log.w("TestActivity", "Invalid phone number for contact: " + contact.getName());
            }
        }
    }


    private void sendSmsWithoutLocation() {
        ArrayList<ContactsAdapter.Contact> contacts = databaseHelper.getAllContacts();

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No contacts found", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "🚨 EMERGENCY ALERT! 🚨\n" +
                "I need immediate help!\n\n" +
                "📍 Location not available\n" +
                "📱 Sent via SOS Emergency App";

        SmsManager smsManager = SmsManager.getDefault();
        int sentCount = 0;

        for (ContactsAdapter.Contact contact : contacts) {
            String phoneNumber = contact.getPhone();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                try {
                    String formattedNumber = phoneNumber.replaceAll("[^0-9+]", "");

                    if (formattedNumber.length() >= 10) {
                        smsManager.sendTextMessage(formattedNumber, null, message, null, null);
                        sentCount++;
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to send SMS", e);
                }
            }
        }

        if (sentCount > 0) {
            Toast.makeText(this, "Emergency alert sent to " + sentCount + " contacts (no location)", Toast.LENGTH_LONG).show();
        }
    }


    private void sendTrackingSMS() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
            return;
        }

        ArrayList<ContactsAdapter.Contact> contacts = databaseHelper.getAllContacts();

        if (contacts.isEmpty()) {
            Log.d("MainActivity", "No contacts found in the database.");
            Toast.makeText(this, "No contacts found. Please add contacts first.", Toast.LENGTH_SHORT).show();
            return;
        }


        String googleMapsLink = "https://maps.google.com/?q=" + latitude + "," + longitude + "&z=15";

        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        String message = "📍 LIVE LOCATION TRACKING 📍\n" +
                "I'm sharing my live location with you!\n\n" +
                "🔗 Click to view on map:\n" +
                googleMapsLink + "\n\n" +
                "📊 Coordinates:\n" +
                "Latitude: " + String.format("%.6f", latitude) + "\n" +
                "Longitude: " + String.format("%.6f", longitude) + "\n\n" +
                "🕐 Time: " + timestamp + "\n" +
                "📱 From SOS Tracking App\n\n" +
                "⚠️ Note: Click the link above to see my location on Google Maps.";

        SmsManager smsManager = SmsManager.getDefault();
        int sentCount = 0;

        for (ContactsAdapter.Contact contact : contacts) {
            String phoneNumber = contact.getPhone();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                try {
                    String formattedNumber = phoneNumber.replaceAll("[^0-9+]", "");

                    if (formattedNumber.length() >= 10) {

                        if (message.length() > 160) {
                            ArrayList<String> parts = smsManager.divideMessage(message);
                            smsManager.sendMultipartTextMessage(formattedNumber, null, parts, null, null);
                        } else {
                            smsManager.sendTextMessage(formattedNumber, null, message, null, null);
                        }
                        sentCount++;
                        Log.d("MainActivity", "Tracking SMS sent to " + contact.getName());
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to send tracking SMS to " + contact.getName(), e);
                }
            }
        }

        if (sentCount > 0) {
            Toast.makeText(this, "Live location shared with " + sentCount + " contacts", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "📍 Location tracking is ACTIVE", Toast.LENGTH_LONG).show();
        }
    }

    private void openEmailApp() {
        ArrayList<ContactsAdapter.Contact> contacts = databaseHelper.getAllContacts();

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No contacts found. Please add contacts with email first.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder emailAddresses = new StringBuilder();
        for (ContactsAdapter.Contact contact : contacts) {
            String email = contact.getEmail();
            if (email != null && !email.trim().isEmpty()) {
                if (emailAddresses.length() > 0) {
                    emailAddresses.append(", ");
                }
                emailAddresses.append(email.trim());
            }
        }

        if (emailAddresses.length() == 0) {
            Toast.makeText(this, "No email addresses found in contacts", Toast.LENGTH_SHORT).show();
            return;
        }

        String googleMapsLink = "https://maps.google.com/?q=" + latitude + "," + longitude;

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, emailAddresses.toString().split(", "));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "EMERGENCY ALERT - SOS App");

        String emailBody = "🚨 EMERGENCY ALERT! 🚨\n\n" +
                "I need immediate help!\n\n" +
                "📍 My current location:\n" +
                googleMapsLink + "\n\n" +
                "📱 Sent via SOS Emergency App";
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send emergency email via:"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found. Please install an email app.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case LOCATION_PERMISSION_REQUEST_CODE:

                    if (get != null && get.isPressed()) {

                        startLocationTrackingAndSendSMS();
                    } else {

                        getLocationForSMS();
                    }
                    break;

                case SMS_PERMISSION_REQUEST_CODE:

                    if (get != null && get.isPressed()) {

                        sendTrackingSMS();
                    } else {

                        sendSMSWithLocation();
                    }
                    break;

                case CAMERA_PERMISSION_REQUEST_CODE:
                    captureAndSendEmail();
                    break;
            }
        } else {
            switch (requestCode) {
                case LOCATION_PERMISSION_REQUEST_CODE:
                    Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
                    break;

                case SMS_PERMISSION_REQUEST_CODE:
                    Toast.makeText(this, "SMS permission is required to send alerts", Toast.LENGTH_SHORT).show();
                    break;

                case CAMERA_PERMISSION_REQUEST_CODE:
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }


    private void captureAndSendEmail() {
        try {
            capturedImageFiles.clear();
            String frontCameraId = getCameraId(cameraManager, CameraCharacteristics.LENS_FACING_FRONT);
            if (frontCameraId == null) {
                Toast.makeText(this, "Front camera not available", Toast.LENGTH_SHORT).show();
                return;
            }
            openCamera(frontCameraId, true);
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error initializing camera: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void saveImageToGallery(Bitmap bitmap, boolean isFrontCamera) {
        if (isCapturing) {
            return;
        }
        isCapturing = true;

        try {
            File tempFile = File.createTempFile(
                    isFrontCamera ? "front_camera_" : "back_camera_",
                    ".jpg",
                    getExternalCacheDir()
            );

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
            }

            capturedImageFiles.add(tempFile);

            if (isFrontCamera) {
                frontCameraBitmap = bitmap;
            } else {
                backCameraBitmap = bitmap;
            }

            String imageName = isFrontCamera ? "front_camera_image.jpg" : "back_camera_image.jpg";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SOS_App");

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (imageUri != null) {
                try (OutputStream fos = getContentResolver().openOutputStream(imageUri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.flush();
                    runOnUiThread(() -> Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show());
                }
            }

            if (capturedImageFiles.size() == 2) {
                runOnUiThread(() -> openEmailWithAttachments());
            }

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show());
        }

        isCapturing = false;
    }

    private void openEmailWithAttachments() {
        if (capturedImageFiles.isEmpty()) {
            Toast.makeText(this, "No images captured", Toast.LENGTH_SHORT).show();
            return;
        }

        String googleMapsLink = "https://maps.google.com/?q=" + latitude + "," + longitude;

        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"andriodprojectfantasy@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "EMERGENCY - Camera Images");

        String emailBody = "🚨 EMERGENCY ALERT! 🚨\n\n" +
                "I need immediate help!\n\n" +
                "📍 My current location:\n" +
                googleMapsLink + "\n\n" +
                "📷 Camera images attached.\n" +
                "📱 Sent via SOS Emergency App";
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);

        ArrayList<Uri> imageUris = new ArrayList<>();
        for (File imageFile : capturedImageFiles) {
            Uri imageUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    imageFile
            );
            imageUris.add(imageUri);
        }

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send images via:"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private String getCameraId(CameraManager cameraManager, int facing) throws Exception {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing != null && lensFacing == facing) {
                return cameraId;
            }
        }
        return null;
    }

    private void openCamera(String cameraId, boolean isFrontCamera) throws Exception {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }

        if (cameraId == null) {
            runOnUiThread(() -> Toast.makeText(this, "Camera ID is null", Toast.LENGTH_SHORT).show());
            return;
        }

        if (backgroundHandler == null) {
            backgroundHandler = new Handler();
        }

        cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                createCaptureSession(isFrontCamera);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                camera.close();
                cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                closeCamera();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Camera error: " + error, Toast.LENGTH_SHORT).show());
                camera.close();
                cameraDevice = null;
            }
        }, backgroundHandler);
    }

    private void createCaptureSession(boolean isFrontCamera) {
        try {
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                try (Image image = reader.acquireLatestImage()) {
                    if (image == null) return;
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    saveImageToGallery(bitmap, isFrontCamera);

                    image.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, backgroundHandler);

            List<Surface> outputSurfaces = Collections.singletonList(imageReader.getSurface());
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        captureSession.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                if (isSwitchingCamera) return;
                                isSwitchingCamera = true;
                                if (isFrontCamera) {
                                    try {
                                        Log.d("CameraSwitch", "Closing front camera and opening back camera");
                                        closeCamera();
                                        String backCameraId = getCameraId(cameraManager, CameraCharacteristics.LENS_FACING_BACK);
                                        if (backCameraId != null) {
                                            openCamera(backCameraId, false);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        isSwitchingCamera = false;
                                    }
                                }
                            }
                        }, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e("Camera", "Failed to configure camera session");
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopLocationTracking();


        closeCamera();

        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacksAndMessages(null);
        }


        for (File file : capturedImageFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
        capturedImageFiles.clear();
    }
}
