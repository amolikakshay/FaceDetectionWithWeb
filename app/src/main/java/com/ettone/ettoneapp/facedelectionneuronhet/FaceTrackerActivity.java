package com.ettone.ettoneapp.facedelectionneuronhet;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ettone.ettoneapp.MainActivity;
import com.ettone.ettoneapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

public class FaceTrackerActivity extends AppCompatActivity implements FaceDetectedListener,View.OnClickListener
{

    private static final String TAG = "MultiTracker";

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private WebView _homeWebView;
    private LinearLayout _sliderLayout;
    private Animation _animation;
    private Handler _sliderHandler;
    private ImageView _arrowImage;
    private boolean _isArrowClicked;
    private Button _openAppButton;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_face_main );

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);


        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        _homeWebView = (WebView) findViewById(R.id.homeWebView);

        WebSettings webSettings = _homeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        _homeWebView.setWebViewClient(new HomeWebViewClient());
        _homeWebView.loadUrl( "https://mypalava.in");
        _sliderLayout = (LinearLayout)findViewById( R.id.layoutSlide );
        LinearLayout layoutSliderWithArrow =findViewById( R.id.layoutSliderWithArrow );
        _arrowImage = (ImageView)layoutSliderWithArrow.findViewById( R.id.arrowImage );
        _arrowImage.setOnClickListener( this );


        _animation = AnimationUtils.loadAnimation(this, R.anim.item_animation_from_right);
        _animation.setInterpolator(new AccelerateDecelerateInterpolator());
        _sliderLayout.setAnimation(_animation);
       // animation.start();
        _sliderHandler =new Handler(  );

        _openAppButton = (Button) layoutSliderWithArrow.findViewById( R.id.openAppButton );
        _openAppButton.setOnClickListener( this );
        
    }


    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }



    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {


        Context context = getApplicationContext();

        FaceDetector faceDetector = new FaceDetector.Builder(context).build();
        FaceTrackerFactory faceFactory = new FaceTrackerFactory(mGraphicOverlay,FaceTrackerActivity.this);


        faceDetector.setProcessor(
                new MultiProcessor.Builder<>(faceFactory).build());

        MultiDetector multiDetector = new MultiDetector.Builder()
                .add(faceDetector)
                .build();

        if (!multiDetector.isOperational()) {
            IntentFilter lowstorageFilter = new IntentFilter( Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        mCameraSource = new CameraSource.Builder(getApplicationContext(), multiDetector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
               // .setRequestedPreviewSize(getResources().getInteger( R.integer.cameraPreviewHeight ), getResources().getInteger( R.integer.cameraPreviewWidth ))
                .setRequestedFps(15.0f)
                .setAutoFocusEnabled( true )
                .build();


    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }


    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }
    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
                mPreview.setVisibility( View.INVISIBLE);

            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public boolean isFaceDetected(boolean isDetected)
    {


        if(_sliderLayout.getVisibility()== View.GONE && !_isArrowClicked)
        {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    _sliderHandler.removeCallbacks( _sliderVisibilityGone );
                    showSlider();
                    _sliderHandler.postDelayed( _sliderVisibilityGone, 10000 );
                }
            } );
        }
        return false;
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.arrowImage:

                _sliderHandler.removeCallbacks( _sliderVisibilityGone );

                if(_sliderLayout.getVisibility()== View.VISIBLE)
                {

                    _isArrowClicked= true;
                    hideSlider();
                    _sliderHandler.postDelayed( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            _isArrowClicked=false;
                        }
                    } ,30000);
                }
                else
                {
                    _isArrowClicked= false;

                    showSlider();
                }


                break;
                
            case R.id.openAppButton:
                openApp();
                break;
        }
    }

    private void openApp()
    {
        Intent intent =new Intent( FaceTrackerActivity.this, MainActivity.class );
        startActivity( intent );
    }

    private class HomeWebViewClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && _homeWebView.canGoBack()) {
            _homeWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }


    private Runnable _sliderVisibilityGone =new Runnable()
    {
        @Override
        public void run()
        {
            hideSlider();
        }
    };


    private void hideSlider()
    {

        _arrowImage.setAnimation( outToRightAnimation() );
        _sliderLayout.startAnimation( outToRightAnimation() );

        _sliderHandler.postDelayed( new Runnable()
        {
            @Override
            public void run()
            {
                _sliderLayout.setVisibility( View.GONE );
            }
        } ,500);
    }

    private Animation inFromRightAnimation() {

        Animation inFromRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromRight.setDuration(500);
        inFromRight.setInterpolator(new AccelerateInterpolator());
        return inFromRight;
    }


    private Animation outToRightAnimation() {
        Animation outtoRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, +0.8f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outtoRight.setDuration(500);
        outtoRight.setInterpolator(new AccelerateInterpolator());
        return outtoRight;
    }

    private void showSlider()
    {

        _sliderLayout.setVisibility( View.VISIBLE);
        _arrowImage.setAnimation( inFromRightAnimation() );
        _sliderLayout.startAnimation( inFromRightAnimation() );
    }


}
