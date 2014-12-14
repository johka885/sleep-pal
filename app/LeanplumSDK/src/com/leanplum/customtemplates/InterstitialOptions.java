// Copyright 2014, Leanplum, Inc.

package com.leanplum.customtemplates;

import android.content.Context;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;

/**
 * Options used by {@link Interstitial}.
 * @author Martin Yanakiev
 */
public class InterstitialOptions extends BaseMessageOptions {
  public InterstitialOptions(ActionContext context) {
    super(context);
    // Set specific properties for interstitial popup.
  }

  public static ActionArgs toArgs(Context currentContext) {
    return BaseMessageOptions.toArgs(currentContext);
    // Add specific args for interstitial popup.
  }
}
