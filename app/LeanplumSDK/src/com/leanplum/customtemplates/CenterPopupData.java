// Copyright 2014, Leanplum, Inc.

package com.leanplum.customtemplates;

import android.content.Context;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;

public class CenterPopupData extends BaseMessageData{

	public CenterPopupData() {
		this(null);
	}
	
	public CenterPopupData(ActionContext context) {
		super(context);
		//set specific properties for center popup
	}

	@Override
	public ActionArgs toArgs(Context currentContext) {
		ActionArgs args =  super.toArgs(currentContext);
		//import specific args for center popup
		return args;
	}
}
