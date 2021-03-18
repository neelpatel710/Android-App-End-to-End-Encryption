package com.learn.android.chatapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;

public class ChatActivity extends AppCompatActivity {

    DatabaseReference myRef;
    ValueEventListener myValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("Chats");
        setContentView(R.layout.activity_chat);
        Intent i = getIntent();
        final String user1_id = i.getStringExtra("user1_id");
        final String user2_id = i.getStringExtra("user2_id");
        String user2_fullname = i.getStringExtra("user2_fullname");
        getSupportActionBar().setTitle(user2_fullname);

        //Generating Root Child for Chats
        char sorted[] = (user1_id + user2_id).toCharArray();
        Arrays.sort(sorted);
        final String rootChild = new String(sorted);

        //Fetching Sender & Receiver's Details
        final UserDetails[] senderDetails = new UserDetails[1];
        final UserDetails[] receiverDetails = new UserDetails[1];
        DatabaseReference refPublicKeyFetch = FirebaseDatabase.getInstance().getReference().child("Users");
        refPublicKeyFetch.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot eachChild : dataSnapshot.getChildren()) {
                    UserDetails obj = eachChild.getValue(UserDetails.class);
                    if (obj.getUserName().equals(user2_id)) {
                        receiverDetails[0] = eachChild.getValue(UserDetails.class);
                    } else if (obj.getUserName().equals(user1_id)) {
                        senderDetails[0] = eachChild.getValue(UserDetails.class);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        //Setting Up RecyclerView For Loading Messages
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.chat_recyclerview);
        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        myRef = FirebaseDatabase.getInstance().getReference().child("Chats").child(rootChild);
        myValueEventListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Messages> messageList = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot eachChild : dataSnapshot.getChildren()) {
                        Messages obj = eachChild.getValue(Messages.class);

                        //Decrypting Message
                        messageList.add(obj);
                        myCustomAdapter adapter = new myCustomAdapter(messageList, ChatActivity.this, senderDetails[0]);
                        recyclerView.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "Be the First to Message!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        myRef.addValueEventListener(myValueEventListener);

        //Click Listener on Send Button
        findViewById(R.id.send_btn).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                String userMessage = ((EditText) findViewById(R.id.user_message_edittext)).getText().toString();
                if (userMessage.isEmpty()) {
                    Toast.makeText(ChatActivity.this, "No message entered.", Toast.LENGTH_SHORT).show();
                } else {
                    //Encrypting Message
                    try {
                        byte[] keyBytes = Base64.getDecoder().decode(receiverDetails[0].getPublicKey());
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.ENCRYPT_MODE, kf.generatePublic(spec));
                        userMessage = Base64.getEncoder().encodeToString(cipher.doFinal(userMessage.getBytes("UTF-8")));
                    } catch (Exception e) {
                        Toast.makeText(ChatActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                    //Storing encrypted Message on Server
                    Messages MessagesObject = new Messages(user1_id, userMessage);
                    myRef.push().setValue(MessagesObject);
                }
                ((EditText) findViewById(R.id.user_message_edittext)).setText("");
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public class myCustomAdapter extends RecyclerView.Adapter {

        final int VIEW_TYPE_SENT = 1;
        final int VIEW_TYPE_RECEIVE = 2;
        ArrayList<Messages> fetchMessages;
        Context context;
        UserDetails currentViewer;


        myCustomAdapter(ArrayList<Messages> array, Context c, UserDetails currentViewer) {
            this.fetchMessages = array;
            this.context = c;
            this.currentViewer = currentViewer;
        }

        @Override
        public int getItemViewType(int position) {
            if (this.fetchMessages.get(position).getUserID().equals(currentViewer.getUserName())) {
                return VIEW_TYPE_SENT;
            } else {
                return VIEW_TYPE_RECEIVE;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_SENT) {
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                View view = layoutInflater.inflate(R.layout.message_display_sent, parent, false);
                return new myViewHolderSent(view);
            } else {
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                View view = layoutInflater.inflate(R.layout.message_display_receive, parent, false);
                return new myViewHolderReceive(view, currentViewer);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Messages obj = this.fetchMessages.get(position);
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_SENT:
                    ((myViewHolderSent) holder).setShowMessage(obj.getMessage());
                    break;
                case VIEW_TYPE_RECEIVE:
                    ((myViewHolderReceive) holder).setShowMessage(obj.getMessage());
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return fetchMessages.size();
        }

        private class myViewHolderSent extends RecyclerView.ViewHolder {

            TextView showMessage;

            myViewHolderSent(View eachItem) {
                super(eachItem);
                showMessage = eachItem.findViewById(R.id.message_textview);
                eachItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(context, "You don't have the rights!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            public void setShowMessage(String s) {
                showMessage.setText(s);
            }
        }

        private class myViewHolderReceive extends RecyclerView.ViewHolder {

            TextView showMessage;
            UserDetails currentViewer;

            myViewHolderReceive(View eachItem, final UserDetails current) {
                super(eachItem);
                this.currentViewer = current;
                showMessage = eachItem.findViewById(R.id.message_textview);
                eachItem.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onClick(View view) {
                        try {
                            String decryptMessage = getShowMessage();
                            byte[] keyBytes = new byte[1024];
                            File path = ChatActivity.this.getFilesDir();
                            File file = new File(path, current.getUserName() + "_private");
                            try {
                                FileInputStream in = new FileInputStream(file);
                                in.read(keyBytes);
                                in.close();
                            } catch (Exception e) {
                                Toast.makeText(ChatActivity.this, "File Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                            }
                            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
                            KeyFactory kf = KeyFactory.getInstance("RSA");
                            Cipher cipher = Cipher.getInstance("RSA");
                            cipher.init(Cipher.DECRYPT_MODE, kf.generatePrivate(spec));
                            decryptMessage = new String(cipher.doFinal(Base64.getDecoder().decode(decryptMessage)), "UTF-8");
                            setShowMessage(decryptMessage);
                        } catch (Exception e) {
                            Toast.makeText(ChatActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            public String getShowMessage() {
                return showMessage.getText().toString();
            }

            public void setShowMessage(String s) {
                showMessage.setText(s);
            }
        }
    }
}