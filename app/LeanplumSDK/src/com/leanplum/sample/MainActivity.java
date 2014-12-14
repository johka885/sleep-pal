package com.leanplum.sample;

import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.leanplum.Leanplum;
import com.leanplum.Var;
import com.leanplum.activities.LeanplumActivity;
import com.leanplum.annotations.Variable;
import com.leanplum.callbacks.VariablesChangedCallback;

public class MainActivity extends LeanplumActivity {
  /**
   * Unused, but shows you how to create a list variable.
   */
  @Variable
  public static List<Double> prices = Arrays.asList(0.99, 1.99, 2.99, 4.99);

  /**
   * Title of the heading.
   */
  @Variable(name="Text.Welcome Text") public static String title = "Welcome to Leanplum!";

  /**
   * Text of main screen.
   */
  @Variable(group="Text") public static String mainText = "Hello, world!";

  /**
   * URL pointing to the Leanplum Tree app.
   */
  @Variable
  public static String MARKET_URL = "market://details?id=com.leanplum.leanplumtree";

  /**
   * Whether the promo can be tapped.
   */
  @Variable public static boolean promoTouchEnabled = true;

  /**
   * Promo asset.
   */
  private Var<String> promo = Var.defineAsset("Promo Image", "promo_green.png");

  /**
   * Callback for when Leanplum is ready with all variables and assets.
   * Also fires any time there is a change.
   */
  private VariablesChangedCallback varsAndFilesChanged = null;

  private ImageView imageView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // This optional line prints out a lot of stuff to the
    // console, so you can see how Leanplum works.
    // Note that in development mode, events don't get batched, so that
    // it's easier to test analytics in realtime. Event batching
    // happens in production.
    Leanplum.enableVerboseLoggingInDevelopmentMode();

    // Syncs all layout files and starts Leanplum.
    Leanplum.syncResources(Arrays.asList("layout/.*"), null);

    Leanplum.start(this);

    // Sets the contentView after Leanplum is started, so that
    // if one of the views changes and is cached across sessions,
    // it is ready immediately.
    setContentView(R.layout.loading);

    // Listens to when variables and assets change.
    setLeanplumCallback();
  }

  /**
   * Registers the Leanplum callback for when variables and assets change.
   */
  private void setLeanplumCallback() {
    if (varsAndFilesChanged != null) {
      Leanplum.removeVariablesChangedAndNoDownloadsPendingHandler(varsAndFilesChanged);
    }
    varsAndFilesChanged = new VariablesChangedCallback() {
      @Override
      public void variablesChanged() {
        init();
      }
    };
    Leanplum.addVariablesChangedAndNoDownloadsPendingHandler(varsAndFilesChanged);
  }

  /**
   * Initialize the view.
   */
  private void init() {
    // By extending LeanplumActivity, Leanplum will automatically
    // swap out the layout if it's changed on the dashboard.
    // For fragments, you'd use LeanplumInflater instead.
    setContentView(R.layout.activity_main);
    setTitle(title);

    TextView textView = (TextView) findViewById(R.id.textView);
    textView.setText(mainText);

    imageView = (ImageView) findViewById(R.id.imageView1);
    imageView.setImageBitmap(BitmapFactory.decodeStream(promo.stream()));
    imageView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View arg0) {
        Leanplum.track("Clicked Promo");
        if (promoTouchEnabled) {
          Intent marketIntent = new Intent(Intent.ACTION_VIEW);
          marketIntent.setData(Uri.parse(MARKET_URL));
          startActivity(marketIntent);
        }
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }
}
