/*
 * Copyright (C) 2020 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.speaker;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;

import java.io.IOException;

public class ClearSpeakerFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = ClearSpeakerFragment.class.getSimpleName();
    private static final String PREF_CLEAR_SPEAKER = "clear_speaker_pref";
    private static final int STOP_PLAYING_DELAY_MS = 30000;

    private AudioManager mAudioManager;
    private Handler mHandler;
    private MediaPlayer mMediaPlayer;
    private SwitchPreference mClearSpeakerPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.clear_speaker_settings);

        mClearSpeakerPref = findPreference(PREF_CLEAR_SPEAKER);
        mClearSpeakerPref.setOnPreferenceChangeListener(this);

        mHandler = new Handler(Looper.getMainLooper());
        mAudioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mClearSpeakerPref) {
            boolean value = (Boolean) newValue;
            if (value && startPlaying()) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(this::stopPlaying, STOP_PLAYING_DELAY_MS);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPlaying();
    }

    public boolean startPlaying() {
        mAudioManager.setParameters("status_earpiece_clean=on");
        mMediaPlayer = new MediaPlayer();
        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        mMediaPlayer.setLooping(true);
        try (AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.clear_speaker_sound)) {
            mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            mClearSpeakerPref.setEnabled(false);
            mMediaPlayer.setVolume(1.0f, 1.0f);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to play speaker clean sound!", e);
            return false;
        }
        return true;
    }

    public void stopPlaying() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            try {
                mMediaPlayer.stop();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to stop media player!", e);
            } finally {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
        mAudioManager.setParameters("status_earpiece_clean=off");
        mClearSpeakerPref.setEnabled(true);
        mClearSpeakerPref.setChecked(false);
    }
}
