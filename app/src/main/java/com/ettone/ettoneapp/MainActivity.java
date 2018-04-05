package com.ettone.ettoneapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.ettone.ettoneapp.facedelectionneuronhet.FaceTrackerActivity;
import com.wang.avi.AVLoadingIndicatorView;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private AVLoadingIndicatorView progressDialog;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    private PermissionRequest myRequest;
    private ImageView _backArrow;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new myWebViewClient());

       webView.getSettings().setJavaScriptEnabled(true);
       webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

       webView.getSettings().setSaveFormData(true);
       webView.getSettings().setSupportZoom(false);
       webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
       webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                myRequest = request;

                for (String permission : request.getResources()) {
                    switch (permission) {
                        case "android.webkit.resource.AUDIO_CAPTURE": {
                            askForPermission(request.getOrigin().toString(), Manifest.permission.RECORD_AUDIO, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                            break;
                        }
                    }
                }
            }
        });
        webView.loadUrl("http://www.neuronet.ai/palavabot/palava#");
        progressDialog = findViewById(R.id.avi);
        progressDialog.setVisibility(View.VISIBLE);
//        progressDialog.setMessage("Loading...");
//        progressDialog.show();

        _backArrow = (ImageView)findViewById( R.id.backArrow );

        _backArrow.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent =new Intent( MainActivity.this, FaceTrackerActivity.class );
                startActivity( intent );
                finish();
            }
        } );
    }


    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else
            super.onBackPressed();
    }

    public class myWebViewClient extends WebViewClient {


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // TODO Auto-generated method stub
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // TODO Auto-generated method stub
            super.onPageFinished(view, url);
            progressDialog.setVisibility(View.GONE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                Log.d("WebView", "PERMISSION FOR AUDIO");
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    myRequest.grant(myRequest.getResources());
                   webView.loadUrl("http://www.neuronet.ai/palavabot/palava#");

                } else {

                    Toast.makeText(this, "Please allow audio record permission from setting.", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    public void askForPermission(String origin, String permission, int requestCode) {
        Log.d("WebView", "inside askForPermission for" + origin + "with" + permission);

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    permission)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{permission},
                        requestCode);
            }
        } else {
            myRequest.grant(myRequest.getResources());
        }
    }
}