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


public class OpenCV2DActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {


    private int filterType = 0; //Set the filter Used

    private CameraHelper mOpenCvCameraView;
    private List<Camera.Size> mResolutionList;


    //A Tag to filter the log messages
    private static final String TAG = "HelloVision";


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

        switch (filterType) { //Different filters applied based on choice
            case (0):
                return inputFrame.rgba();
            case (1):
                return inputFrame.gray();
            case (2):
                return (detectEdges(inputFrame.rgba()));
            case (3):
                return fillShapes(inputFrame.rgba());

        }
        return inputFrame.rgba();
    }

    //Implemeted function
    public void onCameraViewStopped() {

    }

    //Example filter functions
    private Mat detectEdges(Mat rgba) {

        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.Canny(edges, edges, 80, 100);

        return edges;

    }

    //Example filter, fills the 30 largest polygons
    private Mat fillShapes(Mat rgba) {

        Mat processMat = detectEdges(rgba);
        Imgproc.blur(processMat, processMat, new Size(3, 3));

        List<MatOfPoint> contours = new ArrayList<>();
        List<MatOfPoint> largeContours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(processMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat drawing = Mat.zeros(processMat.size(), CvType.CV_8UC3);


        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);
            if (Imgproc.contourArea(contour) > 30) {
                largeContours.add(contour);
            }

        }
        Collections.sort(largeContours, (a, b) -> Double.compare(Imgproc.contourArea(b), Imgproc.contourArea(a)));

        if (largeContours.size() >= 30) {
            for (int i = 0; i < 30; i++) {
                int r = (int) (Math.round((Math.sin(2 * i * Math.PI / 30) + 1) * 200));
                int g = (int) (Math.round((Math.sin(2 * i * Math.PI / 30 + Math.PI * 2 / 3) + 1) * 200));
                int b = (int) (Math.round((Math.sin(2 * i * Math.PI / 30 + Math.PI) + 1) * 200));
                Log.i(TAG, Integer.toString(r));
                Scalar color = new Scalar(r, g, b);
                Scalar white = new Scalar(0, 0, 0);
                Imgproc.drawContours(drawing, largeContours, i, color, -1, Imgproc.LINE_8, hierarchy, 0, new Point());
                Imgproc.drawContours(drawing, largeContours, i, white, 3, Imgproc.LINE_8, hierarchy, 0, new Point());

            }
        }
        return drawing;
    }

    //OnClick functions for buttons defined
    public void RGBASwitch(View view) {
        filterType = 0;
    }

    public void greySwitch(View view) {
        filterType = 1;
    }

    public void edgeSwitch(View view) {
        filterType = 2;
    }

    public void FillSwitch(View view) {
        filterType = 3;
    }


    //OpenCV Manager setup
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        //This is the callback method called once the OpenCV //manager is connected
        public void onManagerConnected(int status) {
            switch (status) {
                //Once the OpenCV manager is successfully connected we can enable the camera interaction with the defined OpenCV camera view
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
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