package com.soultabcaregiver.sendbird_calls;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.soultabcaregiver.WebService.APIS;
import com.soultabcaregiver.sendbird_calls.utils.PrefUtils;
import com.sendbird.android.SendBird;
import com.sendbird.calls.AuthenticateParams;
import com.sendbird.calls.SendBirdCall;
import com.sendbird.calls.SendBirdException;
import com.soultabcaregiver.utils.Utility;


public class SendBirdAuthentication {

	private static final String TAG = "SendBirdAuthentication";

	public static void registerPushToken(Context context, String pushToken, PushTokenHandler handler) {
		Log.i(TAG, "[PushUtils] registerPushToken(pushToken: " + pushToken + ")");

		SendBirdCall.registerPushToken(pushToken, false, e -> {
			if (e != null) {
				Log.i(TAG, "[PushUtils] registerPushToken() => e: " + e.getMessage());
				Utility.setSharedPreference(context, Utility.FCM_TOKEN, pushToken);

				if (handler != null) {
					handler.onResult(e);
				}
				return;
			}

			Log.i(TAG, "[PushUtils] registerPushToken() => OK");
			Utility.setSharedPreference(context, Utility.FCM_TOKEN, pushToken);

			if (handler != null) {
				handler.onResult(null);
			}
		});
	}

	public static void autoAuthenticate(Context context, AutoAuthenticateHandler handler) {
		Log.i(TAG, "[AuthenticationUtils] autoAuthenticate()");

		if (SendBirdCall.getCurrentUser() != null) {
			Log.i(TAG,
					"[AuthenticationUtils] autoAuthenticate(userId: " + SendBirdCall.getCurrentUser().getUserId() + ") => OK (SendBirdCall.getCurrentUser() != null)");
			if (handler != null) {
				handler.onResult(SendBirdCall.getCurrentUser().getUserId());
			}
			return;
		}

		String userId =  Utility.getSharedPreferences(context, APIS.caregiver_id);
		String userName = Utility.getSharedPreferences(context, APIS.Caregiver_name);
		String fcmToken = Utility.getSharedPreferences(context, Utility.FCM_TOKEN);

		if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(fcmToken)) {
			Log.i(TAG,
					"[AuthenticationUtils] autoAuthenticate() => authenticate(userId: " + userId + ")");
			SendBirdCall.authenticate(new AuthenticateParams(userId).setAccessToken(null),
					(user, e) -> {
						if (e != null) {
							Log.i(TAG,
									"[AuthenticationUtils] autoAuthenticate() => authenticate() " + "=>" + " Failed (e: " + e.getMessage() + ")");

							if (handler != null) {
								handler.onResult(null);
							}
							return;
						}

						Log.i(TAG,
								"[AuthenticationUtils] autoAuthenticate() => registerPushToken" +
										"(pushToken: " + fcmToken + ")");
						SendBirdCall.registerPushToken(fcmToken, false, e1 -> {
							if (e1 != null) {
								Log.i(TAG,
										"[AuthenticationUtils] autoAuthenticate() => " +
												"registerPushToken() => Failed (e1: " + e1.getMessage() + ")");


								if (handler != null) {
									handler.onResult(null);
								}
								return;
							}

							Log.i(TAG,
									"[AuthenticationUtils] autoAuthenticate() => authenticate() " + "=>" + " OK (Authenticated)");
							if (handler != null) {
								SendBird.connect(userId, (user1, e2) -> {
									SendBird.updateCurrentUserInfo(userName, "",
											e3 -> handler.onResult(userId));

								});
							}
						});
					});
		} else {
			Log.i(TAG,
					"[AuthenticationUtils] autoAuthenticate() => Failed (No userId and " +
							"pushToken)");
			if (handler != null) {
				handler.onResult(null);
			}
		}
	}

	public static void authenticate(Context context, String userId, String accessToken,
									String userName, AuthenticateHandler handler) {
		if (userId == null) {
			if (handler != null) {
				handler.onResult(false);
			}
			return;
		}

		String fcmToken = Utility.getSharedPreferences(context, Utility.FCM_TOKEN);

		deauthenticate(context, isSuccess -> {

			SendBirdCall.authenticate(new AuthenticateParams(userId).setAccessToken(accessToken),
					(user, e1) -> {
						if (e1 != null) {
							Log.e(TAG, e1.toString());
							if (handler != null) {
								handler.onResult(false);
							}
							return;
						}

						Log.i(TAG,
								"[AuthenticationUtils] authenticate() => registerPushToken" +
										"(pushToken: " + fcmToken + ")");
						/*SendBirdCall.registerPushToken(fcmToken, false, e2 -> {
							if (e2 != null) {
								Log.e(TAG, e2.toString());
								if (handler != null) {
									handler.onResult(false);
								}
								return;
							}
							if (handler != null) {
								if (SendBird.getConnectionState() != SendBird.ConnectionState.OPEN) {
									SendBird.connect(userId, (user1, e3) -> {
										SendBird.updateCurrentUserInfo(userName, "",
												e -> handler.onResult(true));
									});
								}
							}
						});*/

						SendBirdCall.registerPushToken(fcmToken, false, e2 -> {
							if (e2 != null) {
								Log.i(TAG, "[AuthenticationUtils] authenticate() => registerPushToken() => Failed (e2: " + e2.getMessage() + ")");

								if (handler != null) {
									handler.onResult(false);
								}
								return;
							}

							PrefUtils.setAppId(context, SendBirdCall.getApplicationId());
							PrefUtils.setUserId(context, userId);
							PrefUtils.setAccessToken(context, accessToken);
							PrefUtils.setPushToken(context, fcmToken);

							Log.i(TAG, "[AuthenticationUtils] authenticate() => OK");
							if (handler != null) {
								handler.onResult(true);
							}
						});
					});
		});
	}

	public static void deauthenticate(Context context, DeauthenticateHandler handler) {
		if (SendBirdCall.getCurrentUser() == null) {
			if (handler != null) {
				handler.onResult(false);
			}
			return;
		}

		Log.i(TAG, "[AuthenticationUtils] deauthenticate(userId: " + SendBirdCall.getCurrentUser().getUserId() + ")");
		String pushToken = PrefUtils.getPushToken(context);
		if (!TextUtils.isEmpty(pushToken)) {
			Log.i(TAG, "[AuthenticationUtils] deauthenticate() => unregisterPushToken(pushToken: " + pushToken + ")");
			SendBirdCall.unregisterPushToken(pushToken, e -> {
				if (e != null) {
					Log.i(TAG, "[AuthenticationUtils] unregisterPushToken() => Failed (e: " + e.getMessage() + ")");
				}

				doDeauthenticate(context, handler);
			});
		} else {
			doDeauthenticate(context, handler);
		}
	}

	private static void doDeauthenticate(Context context, DeauthenticateHandler handler) {
		SendBirdCall.deauthenticate(e -> {
			if (e != null) {
				Log.i(TAG, "[AuthenticationUtils] deauthenticate() => Failed (e: " + e.getMessage() + ")");
			} else {
				Log.i(TAG, "[AuthenticationUtils] deauthenticate() => OK");
			}

			PrefUtils.setUserId(context, null);
			PrefUtils.setAccessToken(context, null);
			PrefUtils.setCalleeId(context, null);
			PrefUtils.setPushToken(context, null);

			if (handler != null) {
				handler.onResult(e == null);
			}
		});
	}

	public interface PushTokenHandler {
		void onResult(SendBirdException e);
	}

	public interface AutoAuthenticateHandler {
		void onResult(String userId);
	}

	public interface AuthenticateHandler {

		void onResult(boolean isSuccess);
	}

	public interface DeauthenticateHandler {

		void onResult(boolean isSuccess);
	}
}
