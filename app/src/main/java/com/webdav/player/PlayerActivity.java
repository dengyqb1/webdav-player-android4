package com.webdav.player;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class PlayerActivity extends Activity implements
        SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener {

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_IS_VIDEO = "is_video";

    private MediaPlayer mediaPlayer;
    private SurfaceView videoSurface;
    private SurfaceHolder surfaceHolder;
    private SeekBar seekBar;
    private TextView timeDisplay;
    private TextView titleView;
    private Button playPauseButton;

    private Handler handler = new Handler();
    private boolean isPrepared = false;
    private boolean isVideo;
    private String streamUrl;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            if (mediaPlayer != null && isPrepared) {
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        streamUrl = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        isVideo = getIntent().getBooleanExtra(EXTRA_IS_VIDEO, false);

        titleView = (TextView) findViewById(R.id.nowPlayingTitle);
        titleView.setText(title);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        timeDisplay = (TextView) findViewById(R.id.timeDisplay);
        playPauseButton = (Button) findViewById(R.id.playPauseButton);

        videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
        surfaceHolder = videoSurface.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Hide video surface if audio-only
        if (!isVideo) {
            videoSurface.setVisibility(View.GONE);
        }

        // Play/Pause
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });

        // Rewind 10s
        findViewById(R.id.rewindButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPrepared && mediaPlayer != null) {
                    int pos = mediaPlayer.getCurrentPosition();
                    mediaPlayer.seekTo(Math.max(0, pos - 10000));
                }
            }
        });

        // Forward 10s
        findViewById(R.id.forwardButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPrepared && mediaPlayer != null) {
                    int pos = mediaPlayer.getCurrentPosition();
                    mediaPlayer.seekTo(Math.min(mediaPlayer.getDuration(), pos + 10000));
                }
            }
        });

        // SeekBar user seek
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Only update time display on user interaction to avoid jitter
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isPrepared && mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });

        initPlayer();
    }

    private void initPlayer() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            if (isVideo) {
                mediaPlayer.setDisplay(surfaceHolder);
            }

            mediaPlayer.setDataSource(streamUrl);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.prepareAsync();

            playPauseButton.setText("⏸");
        } catch (IOException e) {
            Toast.makeText(this, "播放失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        seekBar.setMax(mp.getDuration());
        handler.post(updateRunnable);
        mp.start();
        playPauseButton.setText("⏸");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handler.removeCallbacks(updateRunnable);
        playPauseButton.setText("▶");
        seekBar.setProgress(0);
        timeDisplay.setText("00:00 / " + MediaUtils.formatTime(mp.getDuration()));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, "播放出错 (code: " + what + ")", Toast.LENGTH_SHORT).show();
        handler.removeCallbacks(updateRunnable);
        finish();
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mediaPlayer != null && isVideo) {
            mediaPlayer.setDisplay(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private void togglePlayPause() {
        if (mediaPlayer == null || !isPrepared) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setText("▶");
            handler.removeCallbacks(updateRunnable);
        } else {
            mediaPlayer.start();
            playPauseButton.setText("⏸");
            handler.post(updateRunnable);
        }
    }

    private void updateProgress() {
        if (mediaPlayer == null || !isPrepared) return;
        int current = mediaPlayer.getCurrentPosition();
        int duration = mediaPlayer.getDuration();
        seekBar.setProgress(current);
        timeDisplay.setText(MediaUtils.formatTime(current) + " / " + MediaUtils.formatTime(duration));
    }

    @Override
    protected void onPause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setText("▶");
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateRunnable);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
