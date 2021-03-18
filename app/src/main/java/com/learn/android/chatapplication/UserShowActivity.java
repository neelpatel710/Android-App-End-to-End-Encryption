package com.learn.android.chatapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class UserShowActivity extends AppCompatActivity {

    ArrayList<UserDetails> userDetailsArrayList;
    String loggedInUser;
    DatabaseReference myRef;
    ValueEventListener myValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_show);
        getSupportActionBar().setTitle("List of All Users");
        loggedInUser = getIntent().getStringExtra("loggedUser");

        //Fetching & Displaying all users.
        myRef = FirebaseDatabase.getInstance().getReference("Users");
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.usershow_list);
        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(UserShowActivity.this);
        recyclerView.setLayoutManager(linearLayoutManager);
        myValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int i = 0;
                userDetailsArrayList = new ArrayList<UserDetails>();
                for (DataSnapshot eachChild : dataSnapshot.getChildren()) {
                    UserDetails user = eachChild.getValue(UserDetails.class);
                    if (!user.getUserName().equals(loggedInUser)) {
                        userDetailsArrayList.add(user);
                    }
                }
                myCustomAdapter myAdapter = new myCustomAdapter(userDetailsArrayList, UserShowActivity.this);
                recyclerView.setAdapter(myAdapter);
                myAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        myRef.addValueEventListener(myValueEventListener);

        findViewById(R.id.logout_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(UserShowActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        myRef.addValueEventListener(myValueEventListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        myRef.removeEventListener(myValueEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        myRef.removeEventListener(myValueEventListener);
    }

    public class myCustomAdapter extends RecyclerView.Adapter<myCustomAdapter.myViewHolder> {

        private ArrayList<UserDetails> allUsers;
        private Context context;

        myCustomAdapter(ArrayList<UserDetails> obj, Context c) {
            this.allUsers = obj;
            this.context = c;
        }

        @Override
        public myViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View v = layoutInflater.inflate(R.layout.user_show_custom_list, parent, false);
            return new myViewHolder(v);
        }

        @Override
        public void onBindViewHolder(myViewHolder holder, int position) {
            UserDetails obj = this.allUsers.get(position);
            String fullName = obj.getFname() + " " + obj.getLname();
            holder.setFullName(fullName);
            holder.setUserNameList(obj.getUserName());
        }

        @Override
        public int getItemCount() {
            return allUsers.size();
        }

        public class myViewHolder extends RecyclerView.ViewHolder {
            TextView fullNameList;
            TextView userNameList;

            myViewHolder(View eachItem) {
                super(eachItem);
                fullNameList = eachItem.findViewById(R.id.fullname_list);
                userNameList = eachItem.findViewById(R.id.username_list);
                eachItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = getAdapterPosition();
                        Intent i = new Intent(UserShowActivity.this, ChatActivity.class);
                        i.putExtra("user1_id", loggedInUser);
                        i.putExtra("user2_id", allUsers.get(position).getUserName());
                        i.putExtra("user2_fullname", allUsers.get(position).getFname() + " " + allUsers.get(position).getLname());
                        startActivity(i);

                    }
                });
            }

            public void setFullName(String s) {
                fullNameList.setText(s);
            }

            public void setUserNameList(String s) {
                userNameList.setText(s);
            }
        }
    }
}
