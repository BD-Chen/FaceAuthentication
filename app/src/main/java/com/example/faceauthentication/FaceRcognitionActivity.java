package com.example.faceauthentication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.widget.Toast;
import android.graphics.Bitmap;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;


import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceRcognitionActivity extends AppCompatActivity {

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private Interpreter tflite;
    private float[] registeredFaceEmbedding;
    private static final String PREFERENCES_NAME = "FaceAuthenticationPrefs";
    private static final String EMBEDDING_KEY = "registered_embedding";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_rcognition);

        Button btnRegisterFace = findViewById(R.id.btnRegisterFace);
        Button btnAuthenticateFace = findViewById(R.id.btnAuthenticateFace);
        previewView = findViewById(R.id.previewView);

        cameraExecutor = Executors.newSingleThreadExecutor();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        } else {
            initializeCamera();
        }

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            Log.e("FaceRecognition", "加载模型失败", e);
        }

        btnRegisterFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureAndRegisterFace();
            }
        });

        btnAuthenticateFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureAndAuthenticateFace();
            }
        });

    }

    private void captureAndRegisterFace() {
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageToBitmap(image);
                registeredFaceEmbedding = getFaceEmbedding(bitmap);
                if (registeredFaceEmbedding != null) {
                    runOnUiThread(() -> Toast.makeText(FaceRcognitionActivity.this, "Face is registered", Toast.LENGTH_SHORT).show());
                }
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(FaceRcognitionActivity.this, "Failed to take a picture.", Toast.LENGTH_SHORT).show());
                Log.e("FaceRecognition", "Failed to take a picture.", exception);
            }
        });
    }


    private void captureAndAuthenticateFace() {
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageToBitmap(image);
                float[] faceEmbedding = getFaceEmbedding(bitmap);
                if (faceEmbedding != null && registeredFaceEmbedding != null) {
                    float similarity = calculateSimilarity(faceEmbedding, registeredFaceEmbedding);
                    runOnUiThread(() -> {
                        if (similarity > 0.9) {
                            Toast.makeText(FaceRcognitionActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(FaceRcognitionActivity.this, SuccessActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(FaceRcognitionActivity.this, "Login Failure", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(FaceRcognitionActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(FaceRcognitionActivity.this, "No registered face found.", Toast.LENGTH_SHORT).show());
                }
                image.close();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(FaceRcognitionActivity.this, "Failed to take a picture.", Toast.LENGTH_SHORT).show());
                Log.e("FaceRecognition", "Failed to take a picture.", exception);
            }
        });
    }



    private float[] getFaceEmbedding(Bitmap bitmap) {
        ByteBuffer byteBuffer = bitmapToByteBuffer(bitmap);
        float[][] embedding = new float[1][512]; // Suppose the model outputs a 512-dimensional vector
        // Convert images to model input format (requires image preprocessing)
        tflite.run(byteBuffer, embedding);
        return embedding[0];
    }

    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        int inputSize = 160; // Assuming the model input is an image of size 160x160
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3); // RGB 通道
        byteBuffer.order(ByteOrder.nativeOrder());

        // Resize the bitmap to the size required by the model
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true);

        // Iterates over the bitmap data and fills the ByteBuffer.
        int[] intValues = new int[inputSize * inputSize];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                // Preprocessing step: convert to floating point values in the range [0, 1].
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f); // Red
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);  // Green
                byteBuffer.putFloat((val & 0xFF) / 255.0f);         // Blue
            }
        }
        return byteBuffer;
    }

    private float calculateSimilarity(float[] emb1, float[] emb2) {
        if (emb1.length != emb2.length) {
            Log.e("FaceRecognition", "Embedding vectors have different lengths: " + emb1.length + " and " + emb2.length);
            return 0; // Returns 0 or throws an exception for dissimilarity or length mismatch.
        }

        float dotProduct = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < emb1.length; i++) {
            dotProduct += emb1[i] * emb2[i];
            norm1 += emb1[i] * emb1[i];
            norm2 += emb2[i] * emb2[i];
        }
        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("facenet.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Log.e("FaceRecognition", "Camera privileges denied");
            }
        }
    }
    private void initializeCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // Get ProcessCameraProvider
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Setting up camera previews, etc. (assuming a PreviewView previewView has been created)
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Setting ImageCapture
                imageCapture = new ImageCapture.Builder().build();

                // Using front camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                // Bind to lifecycle
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("FaceRecognition", "Failed to initialize camera", e);
            }
        }, ContextCompat.getMainExecutor(this));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (tflite != null) {
            tflite.close();
        }
    }
}