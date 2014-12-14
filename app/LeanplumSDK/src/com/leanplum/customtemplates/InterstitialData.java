// Copyright 2014, Leanplum, Inc.

package com.leanplum.customtemplates;

import android.content.Context;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;

public class InterstitialData extends BaseMessageData{

	public InterstitialData() {
		this(null);
	}
	
	public InterstitialData(ActionContext context) {
		super(context);
		//set specific properties for interstitial popup
	}

	@Override
	public ActionArgs toArgs(Context currentContext) {
		ActionArgs args =  super.toArgs(currentContext);
		//import specific args for interstitial popup
		return args;
	}
}
