package me.chayan.mediametadataretriever;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 101;
    private static final int FILE_PICKER_REQUEST_CODE = 102;

    public Button button;
    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
        text = findViewById(R.id.text);

        button.setOnClickListener(v -> checkPermissionsAndOpenFilePicker());
    }

    private void checkPermissionsAndOpenFilePicker() {
        boolean permissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (permissionGranted) {
            openFilePicker();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showError();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
                }
            }
        }
    }

    private void showError() {
        Toast.makeText(this, "Allow external storage reading", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                showError();
            }
        }
    }

    private void openFilePicker() {
        new MaterialFilePicker()
                .withActivity(this)
                .withTitle("Select Video")
                .withFilter(Pattern.compile(".*\\.(mp4|mkv)$"))
                .withRequestCode(FILE_PICKER_REQUEST_CODE)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);

            extractVideoInformation(filePath);
        }
    }

    private void extractVideoInformation(String filePath) {

        // load data file
        File videoFile = new File(filePath);

        // filePath is of type String which holds the path of file
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(videoFile.getPath());

        // get video info
        String videoDuration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        String videoRotation = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        String videoWidth = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String videoHeight = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String videoDate = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
        String videoBitRate = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);

        long duration = Long.parseLong(videoDuration);
        long rotation = Integer.parseInt(videoRotation);
        int width = Integer.parseInt(videoWidth);
        int height = Integer.parseInt(videoHeight);

        StringBuilder builder = new StringBuilder();
        // convert duration to minute:seconds
        String seconds = String.valueOf((duration % 60000) / 1000);
        String minutes = String.valueOf(duration / 60000);
        String length = minutes + ":" + seconds;

        builder.append("File").append("\n");
        builder.append("Name: ").append(videoFile.getName()).append("\n");
        builder.append("Location: ").append(filePath).append("\n");
        builder.append("Size: ").append(getMediaSize(videoFile)).append("\n");
        builder.append("Date: ").append(formatMediaDate(videoDate)).append("\n\n");

        builder.append("Media").append("\n");
        builder.append("Duration: ").append(length).append("\n");
        builder.append("Rotation: ").append(rotation).append("\n");
        builder.append("Resolution: ").append(width).append(" x ").append(height).append("\n");
        builder.append("Bit rate: ").append(videoBitRate).append("\n");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String videoFrameRate = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            builder.append("Frame rate: ").append(videoFrameRate).append("\n");
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            String videoSampleRate = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE);
            builder.append("Sample rate: ").append(videoSampleRate).append("\n");
        }


        // print all data
        text.setText(builder.toString());

        // close object
        metaRetriever.release();
    }

    private String getMediaSize(File file) {
        long size = file.length() / 1000; // Get size and convert bytes into KB.
        if (size >= 1024) {
            return (size / 1024) + " MB";
        } else {
            return size + " KB";
        }
    }

    private String formatMediaDate(String date) {
        String inputPattern = "yyyyMMdd'T'HHmmss";
        String outputPattern = "MMMM dd, yyyy, h:mm a";
        SimpleDateFormat inputFormat = new SimpleDateFormat(inputPattern, Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat(outputPattern, Locale.getDefault());
        Date formattedTimestamp = new Date();
        try {
            formattedTimestamp = inputFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return outputFormat.format(formattedTimestamp != null ? formattedTimestamp : date);
    }
}