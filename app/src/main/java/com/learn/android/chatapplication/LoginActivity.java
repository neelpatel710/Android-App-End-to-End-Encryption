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

import java.security.MessageDigest;
import java.util.Base64;

public class LoginActivity extends AppCompatActivity {

    String userName, password;
    DatabaseReference myRef;
    ValueEventListener myValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Authenticating
        findViewById(R.id.login_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Fetching user entered details
                userName = ((EditText) findViewById(R.id.username_edittext)).getText().toString();
                password = ((EditText) findViewById(R.id.pass_edittext)).getText().toString();

                if (userName.isEmpty()) {
                    ((EditText) findViewById(R.id.username_edittext)).setError("Username is required");
                } else if (password.isEmpty()) {
                    ((EditText) findViewById(R.id.pass_edittext)).setError("Password is required");
                } else {
                    findViewById(R.id.progress).setVisibility(View.VISIBLE);
                    myRef = FirebaseDatabase.getInstance().getReference("Users");
                    myValueEventListener = new ValueEventListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            DataSnapshot UserUsernameChecked = null;
                            for (DataSnapshot eachChild : dataSnapshot.getChildren()) {
                                UserDetails obj = eachChild.getValue(UserDetails.class);
                                if (obj.getUserName().equals(userName)) {
                                    UserUsernameChecked = eachChild;
                                }
                            }
//                            Toast.makeText(LoginActivity.this, UserUsernameChecked.toString(), Toast.LENGTH_LONG).show();
                            if (UserUsernameChecked == null) {
                                findViewById(R.id.progress).setVisibility(View.INVISIBLE);
                                Toast.makeText(LoginActivity.this, "Username does not exists", Toast.LENGTH_LONG).show();
                            } else {
                                UserDetails checkObj = UserUsernameChecked.getValue(UserDetails.class);

                                //Checking Password
                                if (checkObj.getPassword().equals(Base64.getEncoder().encodeToString(password.getBytes()))) {
                                    Intent newI = new Intent(LoginActivity.this, UserShowActivity.class);
                                    newI.putExtra("loggedUser", userName);
                                    startActivity(newI);
                                    myRef.removeEventListener(myValueEventListener);
                                    finish();
                                } else {
                                    findViewById(R.id.progress).setVisibility(View.INVISIBLE);
                                    Toast.makeText(LoginActivity.this, "Incorrect Password", Toast.LENGTH_LONG).show();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    };
                    myRef.addValueEventListener(myValueEventListener);
//                    Toast.makeText(LoginActivity.this, "Details: " + userName + ", " + password, Toast.LENGTH_LONG).show();
                }
            }
        });


        findViewById(R.id.signup_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            }
        });
    }
}
