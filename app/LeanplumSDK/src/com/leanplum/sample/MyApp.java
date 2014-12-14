package com.leanplum.sample;

import com.leanplum.Leanplum;
import com.leanplum.LeanplumApplication;
import com.leanplum.customtemplates.MessageTemplates;

public class MyApp extends LeanplumApplication {
  @Override
  public void onCreate() {
    super.onCreate();
    if (BuildConfig.DEBUG) {
      Leanplum.setAppIdForDevelopmentMode(
          [INSERT APP ID HERE],
          [INSERT DEVELOPMENT KEY HERE]);
    } else {
      Leanplum.setAppIdForProductionMode(
          [INSERT APP ID HERE],
          [INSERT PRODUCTION KEY HERE]);
    }

    // (Advanced). Uncomment to use the provided open-source templates rather than the templates
    // built into the SDK. Use this if you are modifying the existing templates.
    // MessageTemplates.register(getApplicationContext());
  }
}
