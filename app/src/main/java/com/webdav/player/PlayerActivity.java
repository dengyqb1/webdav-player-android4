package com.webdav.player;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.KeyEvent;
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

        if (!isVideo) {
            videoSurface.setVisibility(View.GONE);
        }

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });

        findViewById(R.id.rewindButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekRelative(-10000);
            }
        });

        findViewById(R.id.forwardButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekRelative(10000);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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

    // TV remote key handler
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER) {
            togglePlayPause();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            seekRelative(-10000);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            seekRelative(10000);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            togglePlayPause();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void seekRelative(int ms) {
        if (isPrepared && mediaPlayer != null) {
            int pos = mediaPlayer.getCurrentPosition();
            int target = Math.max(0, Math.min(mediaPlayer.getDuration(), pos + ms));
            mediaPlayer.seekTo(target);
        }
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

            playPauseButton.setText("\u23F8");
        } catch (IOException e) {
            Toast.makeText(this, "\u64AD\u653E\u5931\u8D25: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        seekBar.setMax(mp.getDuration());
        handler.post(updateRunnable);
        mp.start();
        playPauseButton.setText("\u23F8");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handler.removeCallbacks(updateRunnable);
        playPauseButton.setText("\u25B6");
        seekBar.setProgress(0);
        if (mp != null) {
            timeDisplay.setText("00:00 / " + MediaUtils.formatTime(mp.getDuration()));
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(this, "\u64AD\u653E\u51FA\u9519 (code: " + what + ")", Toast.LENGTH_SHORT).show();
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
            playPauseButton.setText("\u25B6");
            handler.removeCallbacks(updateRunnable);
        } else {
            mediaPlayer.start();
            playPauseButton.setText("\u23F8");
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
            playPauseButton.setText("\u25B6");
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
