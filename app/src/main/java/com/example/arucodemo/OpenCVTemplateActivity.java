package com.example.arucodemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class OpenCVTemplateActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraHelper mOpenCvCameraView;
    private List<Camera.Size> mResolutionList;

    //This is the callback object used when we initialize the OpenCV //library asynchronously


    @Override //Function when app initially opened
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.opencv2d);

        mOpenCvCameraView = (CameraHelper) findViewById(R.id.camera_surface_view);

        //Set the view as visible
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        //Register your activity as the callback object to handle //camera frames
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onResume() {

        super.onResume();

        //Intialise OpenCV manager on mobile
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    //Function run when the camera is drawn to the screen
    public void onCameraViewStarted(int a, int b) {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 100); //Camera permission requested if needed
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mOpenCvCameraView.setResolution(mResolutionList.get(0)); //Set resolution to max size

    }

    //Implementing CvCameraViewListener2, Returns a Mat to be displayed given an inputFrame
    //Most important function
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        return inputFrame.rgba();
    }

    //Implemeted function
    public void onCameraViewStopped() {

    }



    //OpenCV Manager setup
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        //This is the callback method called once the OpenCV //manager is connected
        public void onManagerConnected(int status) {
            switch (status) {
                //Once the OpenCV manager is successfully connected we can enable the camera interaction with the defined OpenCV camera view
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
}