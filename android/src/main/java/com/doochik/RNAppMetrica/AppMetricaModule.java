/*
 * Version for React Native
 * © 2020 YANDEX
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://yandex.com/legal/appmetrica_sdk_agreement/
 */

package com.yandex.metrica.plugin.reactnative;

import android.app.Activity;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.profile.Attribute;
import com.yandex.metrica.profile.GenderAttribute;
import com.yandex.metrica.profile.UserProfile;
import com.yandex.metrica.profile.UserProfileUpdate;

public class AppMetricaModule extends ReactContextBaseJavaModule {

    private static final String TAG = "AppMetricaModule";

    private final ReactApplicationContext reactContext;

    public AppMetricaModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "AppMetrica";
    }

    @ReactMethod
    public void activate(ReadableMap configMap) {
        YandexMetrica.activate(reactContext, Utils.toYandexMetricaConfig(configMap));
        enableActivityAutoTracking();
    }

    private void enableActivityAutoTracking() {
        Activity activity = getCurrentActivity();
        if (activity != null) { // TODO: check
            YandexMetrica.enableActivityAutoTracking(activity.getApplication());
        } else {
            Log.w(TAG, "Activity is not attached");
        }
    }

    @ReactMethod
    public void getLibraryApiLevel(Promise promise) {
        promise.resolve(YandexMetrica.getLibraryApiLevel());
    }

    @ReactMethod
    public void getLibraryVersion(Promise promise) {
        promise.resolve(YandexMetrica.getLibraryVersion());
    }

    @ReactMethod
    public void pauseSession() {
        YandexMetrica.pauseSession(getCurrentActivity());
    }

    @ReactMethod
    public void reportAppOpen(String deeplink) {
        YandexMetrica.reportAppOpen(deeplink);
    }

    @ReactMethod
    public void reportError(String message) {
        try {
            Integer.valueOf("00xffWr0ng");
        } catch (Throwable error) {
            YandexMetrica.reportError(message, error);
        }
    }

    @ReactMethod
    public void reportEvent(String eventName, ReadableMap attributes) {
        if (attributes == null) {
            YandexMetrica.reportEvent(eventName);
        } else {
            YandexMetrica.reportEvent(eventName, attributes.toHashMap());
        }
    }

    @ReactMethod
    public void reportReferralUrl(String referralUrl) {
        YandexMetrica.reportReferralUrl(referralUrl);
    }

    @ReactMethod
    public void requestAppMetricaDeviceID(Callback listener) {
        YandexMetrica.requestAppMetricaDeviceID(new ReactNativeAppMetricaDeviceIDListener(listener));
    }

    @ReactMethod
    public void resumeSession() {
        YandexMetrica.resumeSession(getCurrentActivity());
    }

    @ReactMethod
    public void sendEventsBuffer() {
        YandexMetrica.sendEventsBuffer();
    }

    @ReactMethod
    public void setLocation(ReadableMap locationMap) {
        YandexMetrica.setLocation(Utils.toLocation(locationMap));
    }

    @ReactMethod
    public void setLocationTracking(boolean enabled) {
        YandexMetrica.setLocationTracking(enabled);
    }

    @ReactMethod
    public void setStatisticsSending(boolean enabled) {
        YandexMetrica.setStatisticsSending(reactContext, enabled);
    }

    @ReactMethod
    public void setUserProfileID(String userProfileID) {
        YandexMetrica.setUserProfileID(userProfileID);
    }

    @ReactMethod
    public void reportUserProfile(String userProfileID, ReadableMap userProfileParam, Promise promise) {
        if(userProfileID == null) {
            promise.reject("-101", "UserProfileId can't be null");
        }

        setUserProfileID(userProfileID);

        if(userProfileParam != null) {
            UserProfile.Builder userProfileBuilder = UserProfile.newBuilder();
            ReadableMapKeySetIterator iterator = userProfileParam.keySetIterator();

            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();

                switch (key) {
                    case "name": {
                        UserProfileUpdate name = Attribute.name().withValue(userProfileParam.getString(key));
                        userProfileBuilder.apply(name);
                        break;
                    }
                    case "gender": {
                        String genderProp = userProfileParam.getString(key);
                        UserProfileUpdate gender = null;

                        if(genderProp == "male") {
                            gender = Attribute.gender().withValue(GenderAttribute.Gender.MALE);
                        } else if(genderProp == "female") {
                            gender = Attribute.gender().withValue(GenderAttribute.Gender.FEMALE);
                        } else {
                            gender = Attribute.gender().withValue(GenderAttribute.Gender.OTHER);
                        }

                        userProfileBuilder.apply(gender);

                        break;
                    }
                    case "birthDate": {
                        UserProfileUpdate birthDate = Attribute.birthDate().withBirthDate(userProfileParam.getInt(key));
                        userProfileBuilder.apply(birthDate);
                        break;
                    }
                    case "notificationsEnabled": {
                        UserProfileUpdate notificationsEnabled = Attribute.notificationsEnabled().withValue(userProfileParam.getBoolean(key));
                        userProfileBuilder.apply(notificationsEnabled);
                        break;
                    }
                    default: {
                        ReadableType keyType = userProfileParam.getType(key);
                        UserProfileUpdate customAttribute = null;

                        if(keyType == ReadableType.String) {
                            customAttribute = Attribute.customString(key).withValue(userProfileParam.getString(key));
                        } else if(keyType == ReadableType.Number) {
                            customAttribute = Attribute.customNumber(key).withValue(userProfileParam.getInt(key));
                        } else if(keyType == ReadableType.Boolean) {
                            customAttribute = Attribute.customBoolean(key).withValue(userProfileParam.getBoolean(key));
                        }

                        if(customAttribute != null) {
                            userProfileBuilder.apply(customAttribute);
                        }

                        break;
                    }
                }
            }

            UserProfile userProfile = userProfileBuilder.build();

            if(userProfile.getUserProfileUpdates().size() > 0) {
                YandexMetrica.reportUserProfile(userProfile);
            }
        }
    }
}
