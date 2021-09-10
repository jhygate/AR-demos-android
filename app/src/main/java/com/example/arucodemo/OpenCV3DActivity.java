package com.example.arucodemo;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ToggleButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;


import org.opencv.aruco.*;

import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;


import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;
import org.opencv.xfeatures2d.SURF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;


public class OpenCV3DActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraHelper mOpenCvCameraView;
    private static final String TAG = "HelloVision"; //Tag used for debug messages


    private Mat outputImage; //Stores the frame to be displayed

    //Used to store the marker locations
    private Mat markerIds;
    private List<Mat> markerCorners;

    //Dictionarys to store marker info
    private Dictionary dictionaryBoard;
    private Dictionary dictionarySingleMarker;
    private GridBoard board;

    private Bitmap smilebMap; //Stores the bitmap of the smile image
    private Mat smileImage;

    int imgWidth;
    int imgHeight;

    private MatOfPoint2f pts_src;

    private Mat cameraMatrix;

    private MatOfDouble distCoeffs;

    private ToggleButton DrawMarkerButton;
    private ToggleButton BoardSingleButton;
    private ToggleButton DrawAxisButton;
    private ToggleButton MarkerImageButton;
    private ToggleButton DrawModelButton;


    private Mat rvec; //Rotation Matrix of marker
    private Mat tvec; //Translation Matrix of marker
    private Intent intent;


    @Override //Function when app initially opened
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intent = getIntent(); //Info passed from last activity

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        DrawMarkerButton = findViewById(R.id.ToggleDrawMarker); // initiate a toggle button
        BoardSingleButton = findViewById(R.id.ToggleBoardSingleMarker); // initiate a toggle button
        DrawAxisButton = findViewById(R.id.ToggleDrawAxis); // initiate a toggle button
        MarkerImageButton = findViewById(R.id.ToggleMarkerImage); // initiate a toggle button
        DrawModelButton = findViewById(R.id.ToggleDrawModel); // initiate a toggle button


        mOpenCvCameraView = findViewById(R.id.camera_surface_view);

        //Set the view as visible
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        //Register your activity as the callback object to handle //camera frames
        mOpenCvCameraView.setCvCameraViewListener(this);
        smilebMap = BitmapFactory.decodeResource(getResources(), R.drawable.smile);

    }

    @Override
    public void onResume() {

        super.onResume();

        //Intialise OpenCV manager on mobile
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    //Implemeted function
    public void onCameraViewStopped() {

    }



    //Function run when the camera is drawn to the screen
    public void onCameraViewStarted(int a, int b) {
        List<Camera.Size> mResolutionList = mOpenCvCameraView.getResolutionList();
        mOpenCvCameraView.setResolution(mResolutionList.get(0)); //Set resolution to max size

        markerIds = new Mat();
        markerCorners = new ArrayList<>();

        rvec = new Mat();
        tvec = new Mat();

        dictionaryBoard = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_250); //Setup Dictionaries
        dictionarySingleMarker = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        board = GridBoard.create(5, 7, 0.02f, 0.001f, dictionaryBoard);

        smileImage = new Mat();
        Utils.bitmapToMat(smilebMap, smileImage);

        imgWidth = smileImage.width();
        imgHeight = smileImage.height();

        //Setup bitmap data
        List<Point> pts_src_list = new ArrayList<Point>();
        pts_src_list.add(new Point(0, 0));
        pts_src_list.add(new Point(imgWidth, 0));
        pts_src_list.add(new Point(imgWidth, imgHeight));
        pts_src_list.add(new Point(0, imgHeight));

        pts_src = new MatOfPoint2f();
        pts_src.fromList(pts_src_list);

        //Default camera calibration values
        cameraMatrix = Mat.eye(3, 3, CvType.CV_32FC1);
        cameraMatrix.put(0, 0,
                719.9602633448541, 0, 447,
                0, 719.9602633448541, 335.5,
                0, 0, 1);
        distCoeffs = new MatOfDouble();
        double[] distCoeffsArray = {-0.1007,
                0.2118,
                0,
                0,
                -0.6476};
        distCoeffs.fromArray(distCoeffsArray);

        //Sets the calibration values if the user calibrated
        String calibrated = intent.getStringExtra("calibrated");
        if (calibrated.equals("true")) {

            long addr = intent.getLongExtra("camAddr", 0);
            Mat tempImg = new Mat(addr);
            cameraMatrix = tempImg.clone();

            addr = intent.getLongExtra("distAddr", 0);
            Mat testValue = new Mat(addr);
            Log.i(TAG, testValue.dump() + " words");
            distCoeffs = Mat_to_MatOfDouble(testValue);
        }

    }

    //Implementing CvCameraViewListener2, Returns a Mat to be displayed given an inputFrame
    //Most important function
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        outputImage = inputFrame.rgba();

        rvec = new Mat(); //Reset rotation and translations
        tvec = new Mat();

        try { //Returns the input frame if detection errors

            Dictionary dictionary;
            if (BoardSingleButton.isChecked()) { //Sets the correct marker type
                dictionary = dictionarySingleMarker;
            } else {
                dictionary = dictionaryBoard;
            }


            Aruco.detectMarkers(inputFrame.gray(), dictionary, markerCorners, markerIds); //Detects marker location values

            if (MarkerImageButton.isChecked() & markerIds.rows() > 0) { //Draws marker image if selected
                outputImage = drawMarkerCovers(outputImage);
            }

            Imgproc.cvtColor(outputImage, outputImage, Imgproc.COLOR_RGBA2BGR);

            if (BoardSingleButton.isChecked()) { //If a single marker is detected
                if (markerIds.rows() > 0) {
                    Aruco.estimatePoseSingleMarkers(markerCorners, 0.01f, cameraMatrix, distCoeffs, rvec, tvec); //Calculate rotation and translation matrices


                    if (DrawAxisButton.isChecked()) { //If axis is being drawn
                        Aruco.drawAxis(outputImage, cameraMatrix, distCoeffs, rvec, tvec, 0.01f);
                    }
                    if (DrawModelButton.isChecked()) { //If cube is being drawn
                        float[] translation = {0, 0, 0};
                        int scale = 1;
                        outputImage = drawCube(outputImage, rvec, tvec, translation, scale);
                    }
                    if (DrawMarkerButton.isChecked()) { //Draw marker borders
                        Aruco.drawDetectedMarkers(outputImage, markerCorners, markerIds, new Scalar(0, 255, 0)); //BGR needed to draw markers... for some reason
                    }

                }

            } else if (!BoardSingleButton.isChecked()) { //If the marker board is being detected
                if (markerIds.rows() > 0) {
                    Aruco.estimatePoseBoard(markerCorners, markerIds, board, cameraMatrix, distCoeffs, rvec, tvec); //Calculate rotation and translation matrices

                    if (DrawAxisButton.isChecked()) {

                        Aruco.drawAxis(outputImage, cameraMatrix, distCoeffs, rvec, tvec, 0.02f);
                    }
                    if (DrawModelButton.isChecked()) {
                        float[] translation = {0.05f, -0.05f, 0};
                        int scale = 4;
                        outputImage = drawCube(outputImage, rvec, tvec, translation, scale);
                    }
                    if (DrawMarkerButton.isChecked()) { // check current state of a toggle button (true or false).
                        //BGR needed to draw markers... for some reason
                        Aruco.drawDetectedMarkers(outputImage, markerCorners, markerIds, new Scalar(0, 255, 0)); //BGR needed to draw markers... for some reason
                    }
                }
            }
            Imgproc.cvtColor(outputImage, outputImage, Imgproc.COLOR_BGR2RGBA);

        }catch (Exception e){
            Log.e(TAG, String.valueOf(e));
        }
        return outputImage;
    }



    public Mat drawMarkerCovers(Mat inputImage) {
        //Draws the smile.png over each marker onscreen

        for (int i = 0; i < markerIds.rows(); i++) {

            MatOfPoint2f pts_dst = Mat_to_MatOfPoint2f(markerCorners.get(i)); //Type conversion needed for findHomography
            MatOfPoint pts_dst1 = Mat_to_MatOfPoint(markerCorners.get(i));

            Mat matTransform = Calib3d.findHomography(pts_src, pts_dst); //Calculates the transformation needed to apply the bitmap to the marker
            Mat warpedImg = Mat.zeros(inputImage.rows(), outputImage.cols(), outputImage.type());

            Imgproc.warpPerspective(smileImage, warpedImg, matTransform, outputImage.size());//Warps the smileImage using matTranform

            Mat mask = Mat.zeros(inputImage.rows(), inputImage.cols(), CvType.CV_8UC1);
            Imgproc.fillConvexPoly(mask, pts_dst1, new Scalar(255, 255, 255));//Creates a mask over the onscreen marker

            warpedImg.copyTo(inputImage, mask);//copies the warped image only to the mask of the output
        }
        return inputImage;
    }

    public Mat drawCube(Mat img, Mat rvecsIn, Mat tvecsIn, float[] translation, int scale) {
        //Draws a 3D cube over the marker that is affected by its pose

        try {

            //Generates a cube of points and puts it into a matrix axis
            List<Point3> axisList = new ArrayList<>();
            float[][] axisArray = {{-0.005f, -0.005f, 0}, {-0.005f, 0.005f, 0}, {0.005f, 0.005f, 0}, {0.005f, -0.005f, 0},
                    {-0.005f, -0.005f, 0.01f}, {-0.005f, 0.005f, 0.01f}, {0.005f, 0.005f, 0.01f}, {0.005f, -0.005f, 0.01f}};
            int[][] relations = {{0, 1, 2, 3}, {0, 4, 7, 3}, {0, 1, 5, 4}, {6, 5, 1, 2}, {6, 2, 3, 7}, {4, 5, 6, 7}};


            for (int point = 0; point < axisArray.length; point++) {
                axisArray[point][0] = (axisArray[point][0] * scale) + translation[0];
                axisArray[point][1] = (axisArray[point][1] * scale) + translation[1];
                axisArray[point][2] = (axisArray[point][2] * scale) + translation[2];
                axisList.add(new Point3(axisArray[point][0], axisArray[point][1], axisArray[point][2]));
            }
            MatOfPoint3f axis = new MatOfPoint3f();
            axis.fromList(axisList);

            MatOfPoint2f imagePoints = new MatOfPoint2f();
            Calib3d.projectPoints(axis, rvecsIn, tvecsIn, cameraMatrix, distCoeffs, imagePoints); //Projects the 3D values to 3D values translated and rotated to fit the pose (rvecs, tvecs)

            //Using the relations array, draws contours around each face
            for (int faceVal = 0; faceVal < relations.length; faceVal++) {

                MatOfPoint faceContour = new MatOfPoint();
                List<Point> face = new ArrayList<>();
                for (int pointVal = 0; pointVal < relations[faceVal].length; pointVal++) {
                    face.add(new Point(imagePoints.get(relations[faceVal][pointVal], 0)[0], imagePoints.get(relations[faceVal][pointVal], 0)[1]));
                }

                faceContour.fromList(face);
                List<MatOfPoint> finalPoints = new ArrayList<MatOfPoint>();
                finalPoints.add(faceContour);
                Imgproc.drawContours(img, finalPoints, -1, new Scalar((60 * (faceVal + 1)) % 255, (30 * (faceVal + 1)) % 255, (115 * (faceVal + 1)) % 255), 5);
            }

            return img;
        } catch (Exception e) {
            return img;
        }
    }

    //The java implementation of OpenCV does not have some type conversions by default
    //The following functions convert between matrix types by accessing individual elements

    public MatOfPoint2f Mat_to_MatOfPoint2f(Mat inMat) {
        MatOfPoint2f mat2 = new MatOfPoint2f();
        List<Point> inMatList = new ArrayList<>();
        for (int i = 0; i < inMat.cols(); i++) {
            inMatList.add(new Point(inMat.get(0, i)));
        }
        mat2.fromList(inMatList);
        return mat2;
    }

    public MatOfPoint Mat_to_MatOfPoint(Mat inMat) {
        MatOfPoint mat = new MatOfPoint();
        List<Point> inMatList = new ArrayList<>();
        for (int i = 0; i < inMat.cols(); i++) {
            inMatList.add(new Point(inMat.get(0, i)));
        }
        mat.fromList(inMatList);
        return mat;
    }

    public MatOfDouble Mat_to_MatOfDouble(Mat inMat) {
        MatOfDouble mat = new MatOfDouble();
        List<Double> inMatList = new ArrayList<>();
        for (int i = 0; i < inMat.cols(); i++) {
            inMatList.add(inMat.get(0, i)[0]);
        }
        mat.fromList(inMatList);
        return mat;
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