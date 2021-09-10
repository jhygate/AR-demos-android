package com.example.arucodemo;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

//Calibrates the Camera. The user takes at least 20 photos from a variety of angles and orientations to improve marker recognition.
//Docs (https://opencv-java-tutorials.readthedocs.io/en/latest/09-camera-calibration.html))
public class CalibrationActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {


    private CameraHelper mOpenCvCameraView; //Camera View

    private Button CalibrateSnapshotButton; //Takes a calibration photo
    private Button CalibrateButton; //Computes the calibration variables and starts the next activity

    private Boolean calibrateSnapshotPressed = false; //Used for synchronising button presses with camera frames
    private Boolean calibratePressed = false;

    private TextView picturesTakenView; //Used to display the amount of calibration photos taken

    //ChessBoard calibration board size
    private final int numCornersHor = 9;
    private final int numCornersVer = 6;
    private final Size boardSize = new Size(numCornersVer, numCornersHor);

    private MatOfPoint2f imageCorners; //Stores the 2D onscreen coordinates of the calibration board

    private int calibrateSuccesses = 0; //Keeps track of calibration photos taken
    private final int calibrateBoardsNumber = 20; //Minimum number of calibration photos needed

    private List<Mat> calibrateImagePoints;
    private List<Mat> calibrateObjectPoints;
    private MatOfPoint3f obj; //Stores the individual squares values

    private Size imageSize; //Size of camera frame

    private Mat intrinsic; //Camera calibration values passed forward to the app
    private Mat distCoeffs;
    //This is the callback object used when we initialize the OpenCV //library asynchronously


    @Override //Function when app initially opened
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.calibration);//Link to the correct layout file


        mOpenCvCameraView = (CameraHelper) findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        //Register your activity as the callback object to handle //camera frames
        mOpenCvCameraView.setCvCameraViewListener(this);


        CalibrateSnapshotButton = (Button) findViewById(R.id.CalibrateSnapshot);
        CalibrateSnapshotButton.setOnClickListener(v -> calibrateSnapshotPressed = true);
        CalibrateSnapshotButton.setEnabled(false);

        CalibrateButton = (Button) findViewById(R.id.CalibrateButton);
        CalibrateButton.setOnClickListener(v -> calibratePressed = true);
        CalibrateButton.setEnabled(false);

        picturesTakenView = findViewById(R.id.PicturesTaken);


        // Gives instrutions
        AlertDialog.Builder builder = new AlertDialog.Builder(CalibrationActivity.this);
        builder.setTitle("Calibration");
        builder.setMessage("Using the chessboard calibration image, take 20 or more calibration photos from a range of angles to calibrate the camera");
        // Set the alertbutton click listener
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    public void onResume() {

        super.onResume();

        //Initialise OpenCV manager on mobile
        OpenCVLoader.initDebug();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }


    //Function run when the camera is drawn to the screen
    public void onCameraViewStarted(int a, int b) {
        List<Camera.Size> mResolutionList = mOpenCvCameraView.getResolutionList();
        mOpenCvCameraView.setResolution(mResolutionList.get(0)); //Set resolution to max size


        imageCorners = new MatOfPoint2f(); //Intialise Matrices
        intrinsic = new Mat(3, 3, CvType.CV_32FC1);
        distCoeffs = new MatOfDouble();
        obj = new MatOfPoint3f();
        calibrateImagePoints = new ArrayList<>();
        calibrateObjectPoints = new ArrayList<>();

        int numSquares = numCornersHor * numCornersVer;
        for (int j = 0; j < numSquares; j++) //Intiailses the squares values
            obj.push_back(new MatOfPoint3f(new Point3(j / numCornersHor, j % numCornersVer, 0.0f)));

    }

    //Implementing CvCameraViewListener2, Returns a Mat to be displayed given an inputFrame
    //Most important function

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat outputImage = inputFrame.rgba();
        imageSize = outputImage.size();

        boolean found = Calib3d.findChessboardCorners(inputFrame.gray(), boardSize, imageCorners,
                Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK); //Finds image corners in camera frame

        if (found) {
            if (calibrateSnapshotPressed) {
                calibrateSnapshotPressed = false;

                calibrateImagePoints.add(imageCorners);
                imageCorners = new MatOfPoint2f();
                calibrateObjectPoints.add(obj);
                calibrateSuccesses++;

                //Updates number of photos taken
                runOnUiThread(new Runnable() { //Needed to change UI elements in this thread
                    @Override
                    public void run() {
                        picturesTakenView.setText(calibrateSuccesses + " / " + calibrateBoardsNumber);
                    }
                });

                if (calibrateSuccesses == calibrateBoardsNumber) { //Allows the calibrate button to be pressed if enough photos taken
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CalibrateButton.setEnabled(true);
                        }
                    });
                }
            }


            runOnUiThread(new Runnable() { //Allows the user to take a new photo if possible
                @Override
                public void run() {
                    CalibrateSnapshotButton.setEnabled(true);
                }
            });
            Calib3d.drawChessboardCorners(outputImage, boardSize, imageCorners, found);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CalibrateSnapshotButton.setEnabled(false);
                }
            });
        }



        if (calibratePressed & (calibrateSuccesses >= calibrateBoardsNumber)) {
            calibratePressed = false;
            calibrateCamera();
            switchActivities();
        }
        return outputImage;
    }

    //Impleneted function
    public void onCameraViewStopped() {

    }


    private void calibrateCamera() {
        List<Mat> rvecs = new ArrayList<>(); // Return containers for calibration values
        List<Mat> tvecs = new ArrayList<>();
        intrinsic.put(0, 0, 1);
        intrinsic.put(1, 1, 1);

        // calibrate!
        Calib3d.calibrateCamera(calibrateObjectPoints, calibrateImagePoints, imageSize, intrinsic, distCoeffs, rvecs, tvecs);
    }


    private void switchActivities() {
        Intent switchActivityIntent = new Intent(this, OpenCV3DActivity.class);
        long addr = intrinsic.getNativeObjAddr(); //an address used to reconstruct the Matrices
        switchActivityIntent.putExtra("camAddr", addr);
        addr = distCoeffs.getNativeObjAddr();
        switchActivityIntent.putExtra("distAddr", addr);
        startActivity(switchActivityIntent);
        switchActivityIntent.putExtra("calibrated", "true");
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

