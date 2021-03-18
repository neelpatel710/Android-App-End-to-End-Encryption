package com.learn.android.chatapplication;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;

public class RegistrationActivity extends AppCompatActivity {

    String userName, password, fname, lname;
    UserDetails newUser;
    int userID;
    DatabaseReference myRef = null;
    ValueEventListener myValueEventListener;

    //Keys Generation
    KeyPairGenerator keyPairGenerator;
    KeyPair keyPair;
    String publicKeyString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        final DatabaseReference idRef = FirebaseDatabase.getInstance().getReference().child("UserIDCount");
        idRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userID = dataSnapshot.getValue(Integer.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //Storing in Firebase
        findViewById(R.id.register_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Fetching user entered details
                fname = ((EditText) findViewById(R.id.fname_edittext)).getText().toString();
                lname = ((EditText) findViewById(R.id.lname_edittext)).getText().toString();
                userName = ((EditText) findViewById(R.id.username_edittext)).getText().toString();
                password = ((EditText) findViewById(R.id.pass_edittext)).getText().toString();

                if (fname.isEmpty()) {
                    ((EditText) findViewById(R.id.fname_edittext)).setError("Firstname is required");
                } else if (lname.isEmpty()) {
                    ((EditText) findViewById(R.id.lname_edittext)).setError("Lastname is required");
                } else if (userName.isEmpty()) {
                    ((EditText) findViewById(R.id.username_edittext)).setError("Username is required");
                } else if (password.isEmpty()) {
                    ((EditText) findViewById(R.id.pass_edittext)).setError("Password is required");
                } else if (password.length() < 6) {
                    ((EditText) findViewById(R.id.pass_edittext)).setError("Password length should be greater than 6");
                } else {
                    findViewById(R.id.progress).setVisibility(View.VISIBLE);
                    newUser = new UserDetails(fname, lname, userName);

                    myRef = FirebaseDatabase.getInstance().getReference().child("Users");
                    myValueEventListener = new ValueEventListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            int flag = 0;
                            for (DataSnapshot eachChild : dataSnapshot.getChildren()) {
                                UserDetails obj = eachChild.getValue(UserDetails.class);
                                if (obj.getUserName().equals(userName)) {
                                    flag = 1;
                                    findViewById(R.id.progress).setVisibility(View.INVISIBLE);
                                    Toast.makeText(RegistrationActivity.this, "Username already exists", Toast.LENGTH_LONG).show();
                                    break;
                                }
                            }
                            if (flag == 0) {
                                try {
                                    keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                                    keyPairGenerator.initialize(1024);
                                    keyPair = keyPairGenerator.generateKeyPair();
                                    publicKeyString = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
                                } catch (Exception e) {
                                    Toast.makeText(RegistrationActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                                }

                                //Storing Private Key to Local Storage
                                File path = RegistrationActivity.this.getFilesDir();
                                File file = new File(path, userName + "_private");
                                try {
                                    FileOutputStream stream = new FileOutputStream(file);
                                    stream.write(keyPair.getPrivate().getEncoded());
                                    stream.close();
                                } catch (Exception e) {
                                    Toast.makeText(RegistrationActivity.this, "File Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                                }

                                //Simple Encoding the Password
                                String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
                                newUser.setPassword(encodedPassword);
                                newUser.setPublicKey(publicKeyString);
                                myRef.child("M" + userID).setValue(newUser);
                                idRef.setValue(userID + 1);
                                findViewById(R.id.progress).setVisibility(View.INVISIBLE);
                                Toast.makeText(RegistrationActivity.this, "Registered successfully", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                                myRef.removeEventListener(myValueEventListener);
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    };
                    myRef.addValueEventListener(myValueEventListener);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (myRef != null) {
            myRef.removeEventListener(myValueEventListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (myRef != null) {
            myRef.removeEventListener(myValueEventListener);
        }
    }
}
