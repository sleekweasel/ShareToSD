package uk.org.baverstock.sharetosd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 *  Redirects to the service without starting, because nobody wants to see a download happening..
 */

public class ServiceRedirect extends Activity
{
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, DownloadService.class);
		intent.putExtra("intent", getIntent());
		startService(intent);
		finish();
	}
}
