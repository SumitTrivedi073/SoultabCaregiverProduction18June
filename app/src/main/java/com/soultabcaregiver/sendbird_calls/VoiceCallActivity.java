package com.soultabcaregiver.sendbird_calls;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.soultabcaregiver.R;
import com.soultabcaregiver.sendbird_calls.utils.TimeUtils;
import com.sendbird.calls.AcceptParams;
import com.sendbird.calls.AudioDevice;
import com.sendbird.calls.CallOptions;
import com.sendbird.calls.DialParams;
import com.sendbird.calls.DirectCall;
import com.sendbird.calls.SendBirdCall;
import com.soultabcaregiver.utils.Utility;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;



public class VoiceCallActivity extends CallActivity {
	private final String TAG = "VoiceCallActivity";

	private Timer mCallDurationTimer;

	//+ Views
	private ImageView mImageViewSpeakerphone;
	//- Views

	@Override
	protected int getLayoutResourceId() {
		return R.layout.activity_voice_call;
	}

	@Override
	protected void initViews() {
		super.initViews();
		Log.i(TAG, "[VoiceCallActivity] initViews()");

		mImageViewSpeakerphone = findViewById(R.id.image_view_speakerphone);
	}

	@Override
	protected void setViews() {
		super.setViews();

		mImageViewSpeakerphone.setOnClickListener(view -> {
			if (mDirectCall != null) {
				mImageViewSpeakerphone.setSelected(!mImageViewSpeakerphone.isSelected());
				if (mImageViewSpeakerphone.isSelected()) {
					mDirectCall.selectAudioDevice(AudioDevice.SPEAKERPHONE, e -> {
						if (e != null) {
							mImageViewSpeakerphone.setSelected(false);
						}
					});
				} else {
					mDirectCall.selectAudioDevice(AudioDevice.WIRED_HEADSET, e -> {
						if (e != null) {
							mDirectCall.selectAudioDevice(AudioDevice.EARPIECE, null);
						}
					});
				}
			}
		});

		mImageViewBluetooth.setEnabled(false);
		mImageViewBluetooth.setOnClickListener(view -> {
			if (mDirectCall != null) {
				mImageViewBluetooth.setSelected(!mImageViewBluetooth.isSelected());
				if (mImageViewBluetooth.isSelected()) {
					mDirectCall.selectAudioDevice(AudioDevice.BLUETOOTH, e -> {
						if (e != null) {
							mImageViewBluetooth.setSelected(false);
						}
					});
				} else {
					mDirectCall.selectAudioDevice(AudioDevice.WIRED_HEADSET, e -> {
						if (e != null) {
							mDirectCall.selectAudioDevice(AudioDevice.EARPIECE, null);
						}
					});
				}
			}
		});
	}

	@Override
	protected void setAudioDevice(AudioDevice currentAudioDevice, Set<AudioDevice> availableAudioDevices) {
		if (currentAudioDevice == AudioDevice.SPEAKERPHONE) {
			mImageViewSpeakerphone.setSelected(true);
			mImageViewBluetooth.setSelected(false);
		} else if (currentAudioDevice == AudioDevice.BLUETOOTH) {
			mImageViewSpeakerphone.setSelected(false);
			mImageViewBluetooth.setSelected(true);
		} else {
			mImageViewSpeakerphone.setSelected(false);
		}

		if (availableAudioDevices.contains(AudioDevice.SPEAKERPHONE)) {
			mImageViewSpeakerphone.setEnabled(true);
		} else if (!mImageViewSpeakerphone.isSelected()) {
			mImageViewSpeakerphone.setEnabled(false);
		}

		if (availableAudioDevices.contains(AudioDevice.BLUETOOTH)) {
			mImageViewBluetooth.setEnabled(true);
		} else if (!mImageViewBluetooth.isSelected()) {
			mImageViewBluetooth.setEnabled(false);
		}
	}

	@Override
	protected void startCall(boolean amICallee) {
		CallOptions callOptions = new CallOptions();
		callOptions.setAudioEnabled(mIsAudioEnabled);

		if (amICallee) {
			Log.i(TAG, "[VoiceCallActivity] accept()");
			if (mDirectCall != null) {
				mDirectCall.accept(new AcceptParams().setCallOptions(callOptions));
			}
		} else {
			Log.i(TAG, "[VoiceCallActivity] dial()");
			mDirectCall = SendBirdCall.dial(new DialParams(mCalleeIdToDial).setVideoCall(mIsVideoCall).setCallOptions(callOptions), (call, e) -> {
				if (e != null) {
					Log.i(TAG, "[VoiceCallActivity] dial() => e: " + e.getMessage());
					if (e.getMessage() != null) {
						Utility.ShowToast(mContext, e.getMessage());
					}

					finishWithEnding(e.getMessage());
					return;
				}

				Log.i(TAG, "[VoiceCallActivity] dial() => OK");
				updateCallService();
			});
			setListener(mDirectCall);
		}
	}

	@SuppressLint("SourceLockedOrientationActivity")
	@TargetApi(18)
	@Override
	protected boolean setState(STATE state, DirectCall call) {
		if (!super.setState(state, call)) {
			return false;
		}

		switch (mState) {
			case STATE_ACCEPTING:
				cancelCallDurationTimer();
				break;

			case STATE_CONNECTED: {
				setInfo(call, "");
				mLinearLayoutInfo.setVisibility(View.VISIBLE);
				setCallDurationTimer(call);
				break;
			}

			case STATE_ENDING:
			case STATE_ENDED: {
				cancelCallDurationTimer();
				break;
			}
		}
		return true;
	}

	private void setCallDurationTimer(final DirectCall call) {
		if (mCallDurationTimer == null) {
			mCallDurationTimer = new Timer();
			mCallDurationTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					runOnUiThread(() -> {
						String callDuration = TimeUtils.getTimeString(call.getDuration());
						mTextViewStatus.setText(callDuration);
					});
				}
			}, 0, 1000);
		}
	}

	private void cancelCallDurationTimer() {
		if (mCallDurationTimer != null) {
			mCallDurationTimer.cancel();
			mCallDurationTimer = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "[VoiceCallActivity] onDestroy()");
		cancelCallDurationTimer();
	}
}
