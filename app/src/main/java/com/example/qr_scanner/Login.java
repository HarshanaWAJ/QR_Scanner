package com.example.qr_scanner;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class Login extends AppCompatActivity {


    private MaterialButton cameraBtn;
    private MaterialButton galleryBtn;
    private ImageView imageView;
    private MaterialButton scanBtn;
    private TextView resultTv;

    private static final int CAMERA_REQ_CODE = 100;
    private static final int STORAGE_REQ_CODE = 101;

    private String[] cameraPermissions;
    private String[] storagePermissions;

    //uri of the image
    private Uri imageUri = null;

    private static final String TAG = "MAIN_TAG";

    private BarcodeScannerOptions barcodeScannerOptions;
    private BarcodeScanner  barcodeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        imageView = findViewById(R.id.imageView);
        scanBtn = findViewById(R.id.scanBtn);
        resultTv = findViewById(R.id.resultTv);


        //Permission for camera and gallery
        cameraPermissions = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //Permission for storage
        storagePermissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};

        barcodeScannerOptions = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();

        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCameraPermission()) {
                    pickImageCamera();
                }
                else {
                    requestCameraPermission();
                }
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkStoragePermission()) {
                    pickImageGallery();
                }
                else {
                    requestStoragePermission();
                }
            }
        });

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (imageUri == null) {
                    Toast.makeText(Login.this, "Pick image first", Toast.LENGTH_LONG).show();
                }
                else {
                    detectResultFromImage();
                }
            }
        });
    }

    private void detectResultFromImage() {
        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            Task<List<Barcode>> barcodeResult = barcodeScanner.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            extractBarcodeInfo(barcodes);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(Login.this, "Failed scanning due to "+e.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    });
        }
        catch (Exception e) {
            Toast.makeText(Login.this,"Failed scanning due to "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    private void extractBarcodeInfo(List<Barcode> barcodes) {
        for (Barcode barcode: barcodes) {
            Rect bounds = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            String rawValue = barcode.getRawValue();
            Log.d(TAG, "ExtractBarcodeInfo: rawValue: "+rawValue);

            int valueType = barcode.getValueType();

            switch (valueType) {
                case Barcode.TYPE_WIFI: {
                    Barcode.WiFi typeWifi = barcode.getWifi();

                    String ssid = typeWifi.getSsid();
                    String password = typeWifi.getPassword();
                    String encrptType = "" + typeWifi.getEncryptionType();

                    Log.d(TAG, "ExtractBarcodeInfo: title: TYPE_WIFI");
                    Log.d(TAG, "ExtractBarcodeInfo: ssid: "+ssid);
                    Log.d(TAG, "ExtractBarcodeInfo: password: "+password);
                    Log.d(TAG, "ExtractBarcodeInfo: encryptType: "+encrptType);

                    resultTv.setText("TYPE: WIFI \n ssid: "+ssid + "\n password: "+password +"\n Encrypt Type: "+encrptType);
                }
                break;
                case Barcode.TYPE_URL: {
                    Barcode.UrlBookmark typeUrl = barcode.getUrl();

                    String title = ""+ typeUrl.getTitle();
                    String url = ""+ typeUrl.getUrl();

                    Log.d(TAG, "ExtractBarcodeInfo: title: TYPE_URL");
                    Log.d(TAG, "ExtractBarcodeInfo: title: "+title);
                    Log.d(TAG, "ExtractBarcodeInfo: url: "+url);

                    resultTv.setText("TYPE: URL \n ssid: "+title + "\n password: "+url +"\n");

                }
                break;
                default: {
                    resultTv.setText(getString(R.string.raw_value) + rawValue);
                }
            }
        }
    }

    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
    }

    private final ActivityResultLauncher<Intent> gallaryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode()== Activity.RESULT_OK) {
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                        imageView.setImageURI(imageUri);

                    }
                    else {
                        Toast.makeText(Login.this, "Canceled", Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    private void pickImageCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Image Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);

    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher =  registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Log.d(TAG, "onActivityResult: imageUri: "+imageUri);
                        imageView.setImageURI(imageUri);
                    }
                    else {
                        Toast.makeText(Login.this, "Cancelled", Toast.LENGTH_LONG).show();
                    }
                }
            }

    );

    public boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        return result;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQ_CODE);
    }

    private boolean checkCameraPermission() {
        boolean resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean resultStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        return resultCamera && resultStorage;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQ_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted) {
                        pickImageCamera();
                    }
                    else {
                        Toast.makeText(this, "Camera & Storage permissions are required...", Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
            case STORAGE_REQ_CODE: {
                if (grantResults.length > 0) {
                    boolean storagePermission = grantResults[0] ==PackageManager.PERMISSION_GRANTED;
                    if (storagePermission) {
                        pickImageGallery();
                    }
                    else {
                        Toast.makeText(this, "Storage permissions is required...",Toast.LENGTH_LONG).show();
                    }
                }
            }
            break;
        }

    }
}