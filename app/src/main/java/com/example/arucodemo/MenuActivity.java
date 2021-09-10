package com.example.arucodemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

//A menu activity to select the different AR examples
public class MenuActivity extends Activity {

    @Override //Function when app initially opened
    protected void onCreate(Bundle savedInstanceState) {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 100); //Camera permission requested if needed
        super.onCreate(savedInstanceState);

        //Connect to the appropriate layout file
        setContentView(R.layout.mainmenu);

        //Initialise the buttons on screen
        Button openCV2DButton = (Button) findViewById(R.id.openCV2D);
        Button openCV3DButton = (Button) findViewById(R.id.openCV3D);
        Button ARCoreButton = (Button) findViewById(R.id.ARcore);
    }
    //The buttons on_click functions are defined in the layout file

    public void switchCV2D(View view) {
        Intent switchActivityIntent = new Intent(this, OpenCV2DActivity.class);
        startActivity(switchActivityIntent);
    }

    public void switchCV3D(View view) {
        //Asks the user if they want to calibrate OpenCV for their camera or use default values.

        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MenuActivity.this);
        // Set a title for alert dialog
        builder.setTitle("Select your answer.");
        // Ask the final question
        builder.setMessage("Calibrate camera or use default values?");

        // Set the alert dialog yes button click listener
        builder.setPositiveButton("Calibrate", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent switchActivityIntent = new Intent(MenuActivity.this, CalibrationActivity.class);
                startActivity(switchActivityIntent);
            }
        });

        // Set the alert dialog no button click listener
        builder.setNegativeButton("Default Values", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent switchActivityIntent = new Intent(MenuActivity.this, OpenCV3DActivity.class);
                switchActivityIntent.putExtra("calibrated", "false");
                startActivity(switchActivityIntent);
            }
        });
        AlertDialog dialog = builder.create();
        // Display the alert dialog on interface
        dialog.show();
    }

    public void switchARCore(View view) {
        Intent switchActivityIntent = new Intent(this, ARCoreActivity.class);
        startActivity(switchActivityIntent);
    }
}
