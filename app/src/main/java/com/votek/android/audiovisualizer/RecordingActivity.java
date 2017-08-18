package com.votek.android.audiovisualizer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;

public class RecordingActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int AUDIO_REQUEST_CODE = 200;
    private static final String TAG = "RecordingActivity";

    public static final int PLAYED = 1;
    public static final int PAUSED = 0;

    private SiriWaveView mHighWaveLayout;
    private SiriWaveView mMediumWaveLayout;
    private SiriWaveView mLowWaveLayout;

    private LinearLayout mHighWaveContainer;
    private LinearLayout mMediumWaveContainer;
    private LinearLayout mLowWaveContainer;

    private Button mControlButton;
    private MyRecorder recorder;
    private String audioFilePath;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private SharedPreferences mSharedPref;

    private int audioInputKey;


    private static int mState = PAUSED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording_activity);

        mHighWaveLayout = findViewById(R.id.highWaves);
        mMediumWaveLayout = findViewById(R.id.mediumWaves);
        mLowWaveLayout = findViewById(R.id.lowWaves);

        mControlButton = findViewById(R.id.startButton);

        mHighWaveContainer = findViewById(R.id.highWavesContainer);
        mMediumWaveContainer = findViewById(R.id.medWavesContainer);
        mLowWaveContainer = findViewById(R.id.lowWavesContainer);


        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        audioInputKey = Integer.parseInt(mSharedPref.getString("INPUT_TYPE", "0"));
        if(audioInputKey == 1)    mControlButton.setText(R.string.play_file); else    mControlButton.setText(R.string.play_audio);

        boolean visible = mSharedPref.getBoolean("HIGH_WAVE_STATE", true);
        String colorString = mSharedPref.getString("HIGH_WAVE_COLORS", "#FF0000");

        setWavesContainer(mHighWaveLayout, mHighWaveContainer, visible, colorString);

        visible = mSharedPref.getBoolean("MEDIUM_WAVE_STATE", true);
        colorString = mSharedPref.getString("MEDIUM_WAVE_COLORS", "#FFA500");

        setWavesContainer(mMediumWaveLayout, mMediumWaveContainer, visible, colorString);

        visible = mSharedPref.getBoolean("LOW_WAVE_STATE", true);
        colorString = mSharedPref.getString("LOW_WAVE_COLORS", "#00FF00");

        setWavesContainer(mLowWaveLayout, mLowWaveContainer, visible, colorString);

        mSharedPref.registerOnSharedPreferenceChangeListener(this);

        recorder = new MyRecorder();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Sorry! this app can't run without these permissions", Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                Toast.makeText(this, "this app needs to record audio", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        0);
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                Toast.makeText(this, "this app needs to read audio file", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this ,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            }
        }
    }

    private void setWavesContainer(SiriWaveView waves, LinearLayout container, boolean visible, String wavesColorString) {
        Log.d(TAG, "setWavesContainer: visible =" + visible + " color=" + wavesColorString);
        if (visible) {
            container.setVisibility(View.VISIBLE);
            waves.setWaveColor(Color.parseColor(wavesColorString.toLowerCase()));
        } else {
            container.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void controlButtonClicked(View view) {
        if (mState == PLAYED) {
            mState = PAUSED;
            recorder.cancel(true);
            if (audioInputKey == 1) {
                if (audioFilePath != null) mediaPlayer.pause();
                mControlButton.setText(R.string.play_file);
            } else {
                mControlButton.setText(R.string.play_audio);
            }
        } else {
            mState = PLAYED;
            if (audioInputKey == 1) {
                if (audioFilePath == null) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(intent, AUDIO_REQUEST_CODE);
                        mControlButton.setText(R.string.pause_file);
                        return;
                    }
                } else {
                    mediaPlayer.start();
                }
            } else {
                mControlButton.setText(R.string.pause_audio);
            }
            if (recorder.isCancelled()) recorder = new MyRecorder();
            recorder.execute();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == AUDIO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri pickedImage = data.getData();
                String[] filePath = {MediaStore.Audio.Media.DATA};
                Cursor cursor = getContentResolver().query(pickedImage, filePath, null, null, null);
                cursor.moveToFirst();
                audioFilePath = cursor.getString(cursor.getColumnIndex(filePath[0]));
                try {
                    mediaPlayer.setDataSource(audioFilePath);
                    mediaPlayer.prepare();
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                    if (recorder.isCancelled()) recorder = new MyRecorder();
                    mControlButton.setText(R.string.pause_audio);
                    recorder.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        } else {
            //Error
        }
        super.onActivityResult(requestCode, resultCode, data);

    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean visible;
        String colorString;
        if (key.equals("INPUT_TYPE")) {
            audioInputKey = Integer.parseInt(sharedPreferences.getString(key, "0"));
            if(audioInputKey == 1) mControlButton.setText(R.string.play_file); else {
                mControlButton.setText(R.string.play_audio);
                audioFilePath=null;
            }
        } else {
            String updatedkey = key.split("_WAVE_")[0];
            switch (updatedkey) {
                case "HIGH":
                    visible = mSharedPref.getBoolean("HIGH_WAVE_STATE", true);
                    colorString = mSharedPref.getString("HIGH_WAVE_COLORS", "#FF0000");
                    setWavesContainer(mHighWaveLayout, mHighWaveContainer, visible, colorString);
                    break;
                case "MEDIUM":
                    visible = mSharedPref.getBoolean("MEDIUM_WAVE_STATE", true);
                    colorString = mSharedPref.getString("MEDIUM_WAVE_COLORS", "#FFA500");
                    setWavesContainer(mMediumWaveLayout, mMediumWaveContainer, visible, colorString);

                    break;
                case "LOW":
                    visible = mSharedPref.getBoolean("LOW_WAVE_STATE", true);
                    colorString = mSharedPref.getString("LOW_WAVE_COLORS", "#00FF00");
                    setWavesContainer(mLowWaveLayout, mLowWaveContainer, visible, colorString);

                    break;
            }
        }
    }


    public class MyRecorder extends AsyncTask<Void, short[], Void> {

        private static final int RECORDER_SAMPLE_RATE = 8000;
        private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        private static final int MIN_FREQUENCY = 300;
        private static final int MAX_FREQUENCY = 3000;

        int BufferElements2Rec = 1024;
        int BytesPerElement = 2;
        int blockSize = 2048;

        private AudioRecord audioRecord;

        @Override
        protected void onCancelled() {
            super.onCancelled();
            audioRecord.stop();
            audioRecord.release();
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
                final short[] buffer = new short[blockSize];
                audioRecord.startRecording();
                while (mState == PLAYED) {
                    audioRecord.read(buffer, 0, blockSize);
                    Thread.sleep(10);
                    publishProgress(buffer);
                }
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }


        @Override
        protected void onProgressUpdate(short[]... buffer) {
            super.onProgressUpdate(buffer);
            float freq = calculate(RECORDER_SAMPLE_RATE, buffer[0]);

            if (!(freq > MIN_FREQUENCY && freq < MAX_FREQUENCY)) freq = 0;
            mHighWaveLayout.updateWaveFrequency((float) (freq * 0.1));
            mMediumWaveLayout.updateWaveFrequency((float) (freq * 0.3));
            mLowWaveLayout.updateWaveFrequency((float) (freq * 0.9));

        }


        public float calculate(int sampleRate, short[] audioData) {
            int numSamples = audioData.length;
            int numCrossing = 0;
            for (int p = 0; p < numSamples - 1; p++) {
                if ((audioData[p] > 0 && audioData[p + 1] <= 0) ||
                        (audioData[p] < 0 && audioData[p + 1] >= 0)) {
                    numCrossing++;
                }
            }
            float numSecondsRecorded = (float) numSamples / (float) sampleRate;
            float numCycles = numCrossing / 2;
            float frequency = numCycles / numSecondsRecorded;


            return frequency;
        }
    }
}
