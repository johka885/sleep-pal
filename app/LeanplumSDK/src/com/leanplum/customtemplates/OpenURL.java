// Copyright 2014, Leanplum, Inc.

package com.leanplum.customtemplates;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.Leanplum;
import com.leanplum.LeanplumActivityHelper;
import com.leanplum.callbacks.ActionCallback;
import com.leanplum.customtemplates.MessageTemplates.Args;
import com.leanplum.customtemplates.MessageTemplates.Values;

/**
 * Registers a Leanplum action that opens a particular URL.
 * If the URL cannot be handled by the system URL handler, you can add your own
 * action responder using {@link Leanplum#onAction}
 * that handles the URL how you want.
 *
 * @author Andrew First
 */
class OpenURL {
  private static final String NAME = "Open URL";

  public static void register() {
    Leanplum.defineAction(NAME, Leanplum.ACTION_KIND_ACTION,
        new ActionArgs().with(Args.URL, Values.DEFAULT_URL),
        new ActionCallback() {
      @Override
      public boolean onResponse(ActionContext context) {
        String url = context.objectNamed(Args.URL);
        Intent uriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
          LeanplumActivityHelper.getCurrentActivity().startActivity(uriIntent);
          return true;
        } catch (ActivityNotFoundException e) {
          Log.e("Leanplum", "Unable to handle URL " + url);
          return false;
        }
      }
    });
  }
}
