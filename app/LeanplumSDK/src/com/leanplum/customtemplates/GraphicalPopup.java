// Copyright 2014, Leanplum, Inc.

package com.leanplum.customtemplates;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

public class GraphicalPopup extends DialogFragment {
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    Context currentContext = getActivity();
    LinearLayout view = new LinearLayout(currentContext);
    view.addView(new Button(currentContext));
    view.setLayoutParams(new WindowManager.LayoutParams(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT));
    view.setBackgroundColor(Color.MAGENTA);
    return view;
  }

  /** The system calls this only when creating the layout in a dialog. */
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // The only reason you might override this method when using onCreateView() is
    // to modify any dialog characteristics. For example, the dialog includes a
    // title by default, but your custom layout might not need it. So here you can
    // remove the dialog title, but you must call the superclass to get the Dialog.
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  public static void showDialog(Activity activity, boolean fullscreen) {
    FragmentManager fragmentManager = activity.getFragmentManager();
    GraphicalPopup newFragment = new GraphicalPopup();
    if (fullscreen) {
      FragmentTransaction transaction = fragmentManager.beginTransaction();
      transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
      // To make it fullscreen, use the 'content' root view as the container
      // for the fragment, which is always the root view for the activity
      transaction.add(android.R.id.content, newFragment).addToBackStack(null).commit();
    } else {
      newFragment.show(fragmentManager, "dialog");
    }
  }
}
