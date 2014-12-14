// Copyright 2014, Leanplum, Inc.

package com.leanplum.customtemplates;

import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import com.leanplum.ActionArgs;
import com.leanplum.ActionContext;
import com.leanplum.customtemplates.MessageTemplates.Args;
import com.leanplum.utils.BitmapUtil;

public abstract class BaseMessageData {
  private String title;
  private int titleColor;
  private String messageText;
  private int messageColor;
  private Bitmap backgroundImage;
  private int backgroundColor;
  private String acceptBtnText;
  private int acceptBtnBackColor;
  private int acceptBtnTextColor;
  private String acceptAction;
  private int width;
  private int height;

  protected BaseMessageData(ActionContext context) {
    if (context == null) {
      return;
    }
    setTitle(context.stringNamed(Args.TITLE_TEXT));
    setTitleColor((Integer) context.numberNamed(Args.TITLE_COLOR));
    setMessageText(context.stringNamed(Args.MESSAGE_TEXT));
    setMessageColor((Integer) context.numberNamed(Args.MESSAGE_COLOR));
    InputStream imageStream = context.streamNamed(Args.BACKGROUND_IMAGE);
    if (imageStream != null) {
      try {
        setBackgroundImage(BitmapFactory.decodeStream(imageStream));
      } catch (Exception e) {
        Log.e("Leanplum", "Error loading background image", e);
      }
    }
    setBackgroundColor((Integer) context.numberNamed(Args.BACKGROUND_COLOR));
    setAcceptBtnText(context.stringNamed(Args.ACCEPT_BUTTON_TEXT));
    setAcceptBtnBackColor((Integer) context
        .numberNamed(Args.ACCEPT_BUTTON_BACKGROUND_COLOR));
    setAcceptBtnTextColor((Integer) context
        .numberNamed(Args.ACCEPT_BUTTON_TEXT_COLOR));
    setAcceptAction(context.stringNamed(Args.ACCEPT_ACTION));
    setWidth((Integer) context.numberNamed(Args.LAYOUT_WIDTH));
    setHeight((Integer) context.numberNamed(Args.LAYOUT_HEIGHT));
  }

  public int getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(int backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  public String getAcceptBtnText() {
    return acceptBtnText;
  }

  public void setAcceptBtnText(String acceptBtnText) {
    this.acceptBtnText = acceptBtnText;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getTitleColor() {
    return titleColor;
  }

  public void setTitleColor(int titleColor) {
    this.titleColor = titleColor;
  }

  public String getMessageText() {
    return messageText;
  }

  public void setMessageText(String messageText) {
    this.messageText = messageText;
  }

  public int getMessageColor() {
    return messageColor;
  }

  public void setMessageColor(int messageColor) {
    this.messageColor = messageColor;
  }

  public Bitmap getBackgroundImage() {
    return backgroundImage;
  }

  public Bitmap getBackgroundImageRounded(int pixels) {
    return BitmapUtil.getRoundedCornerBitmap(backgroundImage, pixels);
  }

  public void setBackgroundImage(Bitmap backgroundImage) {
    this.backgroundImage = backgroundImage;
  }

  public int getAcceptBtnBackColor() {
    return acceptBtnBackColor;
  }

  public void setAcceptBtnBackColor(int acceptBtnBackColor) {
    this.acceptBtnBackColor = acceptBtnBackColor;
  }

  public int getAcceptBtnTextColor() {
    return acceptBtnTextColor;
  }

  public void setAcceptBtnTextColor(int acceptBtnTextColor) {
    this.acceptBtnTextColor = acceptBtnTextColor;
  }

  public String getAcceptAction() {
    return acceptAction;
  }

  public void setAcceptAction(String acceptAction) {
    this.acceptAction = acceptAction;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public ActionArgs toArgs(Context currentContext) {
    return new ActionArgs()
        .with(Args.TITLE_TEXT,
            MessageTemplates.getApplicationName(currentContext))
        .withColor(Args.TITLE_COLOR, Color.BLACK)
        .with(Args.MESSAGE_TEXT, MessageTemplates.Values.POPUP_MESSAGE)
        .withColor(Args.MESSAGE_COLOR, Color.BLACK)
        .withFile(Args.BACKGROUND_IMAGE, null)
        .withColor(Args.BACKGROUND_COLOR, Color.WHITE)
        .with(Args.ACCEPT_BUTTON_TEXT, MessageTemplates.Values.OK_TEXT)
        .withColor(Args.ACCEPT_BUTTON_BACKGROUND_COLOR, Color.WHITE)
        .withColor(Args.ACCEPT_BUTTON_TEXT_COLOR, Color.argb(255, 0, 122, 255))
        .withAction(Args.ACCEPT_ACTION, null)
        .with(Args.LAYOUT_WIDTH, MessageTemplates.Values.CENTER_POPUP_WIDTH)
        .with(Args.LAYOUT_HEIGHT, MessageTemplates.Values.CENTER_POPUP_HEIGHT);
  }
}
