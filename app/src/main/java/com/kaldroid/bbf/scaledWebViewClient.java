package com.kaldroid.bbf;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class scaledWebViewClient extends WebViewClient {

	@Override
	public void onScaleChanged(WebView view, float oldScale, float newScale) {
		super.onScaleChanged(view, oldScale, newScale);
		BreadBootReceiver.scale = (float) (100.0 * newScale);
	}
}
