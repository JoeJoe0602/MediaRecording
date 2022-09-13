package comp5216.sydney.edu.au.mediarecording;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

import java.io.File;

public class MediaShowActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_show);

        int type = getIntent().getIntExtra("type", 1);
        String url = getIntent().getStringExtra("url");

        VideoView mVideoView = findViewById(R.id.videoview);
        ImageView ivPreview = findViewById(R.id.photopreview);

        mVideoView.setVisibility(View.GONE);
        ivPreview.setVisibility(View.GONE);

        if (type == 1) {
            File file = new File(url);
            // by this point we have the camera photo on disk
            Bitmap takenImage = BitmapFactory.decodeFile(file.getAbsolutePath());
            // Load the taken image into a preview
            ivPreview.setImageBitmap(takenImage);
            ivPreview.setVisibility(View.VISIBLE);
        } else {
            mVideoView.setVisibility(View.VISIBLE);
            mVideoView.setVideoPath(url);
            mVideoView.requestFocus();
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                // Close the progress bar and play the video
                public void onPrepared(MediaPlayer mp) {
                    mVideoView.start();
                }
            });
        }


    }
}