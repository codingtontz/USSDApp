package com.tonevellah.ussdapp1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //call permission launcher
        ActivityResultLauncher<String> callPermissionLauncher;
        callPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                //permission granted run the ussd code
                runUssdCode();
            }
        });

        //on run button click, check call permission  & either run ussd code or request call permission
        Button runBtn = findViewById(R.id.runBtn);
        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
            }
        });
    }


    private void runUssdCode() {
        //get user input
        EditText codeBtn = findViewById(R.id.codeBtn);
        String USSDCode = codeBtn.getText().toString();
        //code must start *(star) and with # hash
        if (!USSDCode.startsWith("*") && !USSDCode.endsWith("#")){
            showToast("Enter a valid ussd code");
            return;
        }

        //check if the API if is >=26 or not
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //check permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //use Telephony Manager
            TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

            //show status dialog
            View view = LayoutInflater.from(this).inflate(R.layout.status_dialog,null);
            CircularProgressIndicator progressIndicator = view.findViewById(R.id.progressIndicator);
            TextView messageView = view.findViewById(R.id.message);
            TextView okBtn = view.findViewById(R.id.okBtn);

            //build dialog
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setView(view);
            alertBuilder.setCancelable(false);
            final AlertDialog dialog = alertBuilder.show(); //show method return a AlertDialog.so I can dismiss it by:dialog.dismiss();

            //hide the ok btn
            okBtn.setVisibility(View.GONE);
            okBtn.setOnClickListener(v1 -> dialog.dismiss());


            //run ussd code
            manager.sendUssdRequest(USSDCode, new TelephonyManager.UssdResponseCallback() {
                @Override
                public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                    super.onReceiveUssdResponse(telephonyManager, request, response);
                   //showToast("success with: "+ response.toString());
                    progressIndicator.setVisibility(View.GONE);
                   messageView.setText(response.toString());
                   okBtn.setVisibility(View.VISIBLE);

                }
                @Override
                public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                    super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                    //showToast("Failed");
                    progressIndicator.setVisibility(View.GONE);
                    messageView.setText("Failed, Try again later");
                    okBtn.setVisibility(View.VISIBLE);
                }
            }, new Handler());
        }else{

            //encode the ussd string to a valid format to run
            //remove the last # from the ussd code so we can encode it. so *102# becomes *102
            USSDCode = USSDCode.substring(0, USSDCode.length() - 1);
            //encode
            USSDCode =USSDCode + Uri.encode("#");
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" +USSDCode));
            startActivity(callIntent);
        }

    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}