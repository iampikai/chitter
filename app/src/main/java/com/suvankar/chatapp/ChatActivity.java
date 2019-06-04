package com.suvankar.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.MessagingAnalytics;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.suvankar.chatapp.adapters.MessageListAdapter;
import com.suvankar.chatapp.models.MessageDataModel;
import com.suvankar.chatapp.models.UserDataModel;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String RECIPIENT_NAME = "recipientName";
    public static final String RECIPIENT_ID = "recipientId";
    private static final int RC_PHOTO_PICKER = 2;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private MessageListAdapter mMessageListAdapter;

    private RecyclerView mMessageRecyclerView;
    private EditText mMessageEditText;
    private Button mSendButton;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private StorageReference mChatPhotosStorageReference;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;

    private UserDataModel currentUser;
    private List<MessageDataModel> friendlyMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String recipientId = getIntent().getExtras().getString(RECIPIENT_ID);
        String recipientName = getIntent().getExtras().getString(RECIPIENT_NAME);

        getSupportActionBar().setTitle(recipientName);

        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseStorage mFirebaseStorage = FirebaseStorage.getInstance();

        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        currentUser = new UserDataModel(mFirebaseUser);


        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages")
                .child(getChatChild(mFirebaseUser.getUid(), recipientId));
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        //attachDatabaseReadListener();

        mMessageRecyclerView = findViewById(R.id.messageListView);
        ImageButton mPhotoPickerButton = findViewById(R.id.photoPickerButton);
        mMessageEditText = findViewById(R.id.messageEditText);
        mSendButton = findViewById(R.id.sendButton);

        friendlyMessages = new ArrayList<>();
        mMessageListAdapter = new MessageListAdapter(this, friendlyMessages, currentUser);
        mMessageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecyclerView.setAdapter(mMessageListAdapter);

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageDataModel message = new MessageDataModel(currentUser, mMessageEditText.getText().toString(), null);
                mMessagesDatabaseReference.push().setValue(message);
                mMessageEditText.setText("");
            }
        });

        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_PHOTO_PICKER) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();

                    MessageDataModel tempMessage = new MessageDataModel(currentUser, null, "https://www.google.com/images/spin-32.gif");

                    mMessagesDatabaseReference.push()
                            .setValue(tempMessage, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError,
                                                       DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        String key = databaseReference.getKey();
                                        putImageInStorage(uri, key);
                                    } else {
                                        Log.w("ERROR", "Unable to write message to database.",
                                                databaseError.toException());
                                    }
                                }
                            });
                }
            }
        }


//        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
//            Uri selectedImageUri = data.getData();
//            Log.e("PHOTO", selectedImageUri.toString());
//            StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
//            photoRef.putFile(selectedImageUri);
//            mChatPhotosStorageReference.putFile(selectedImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
//                @Override
//                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
//                    if (!task.isSuccessful()) {
//                        throw task.getException();
//                    }
//                    return mChatPhotosStorageReference.getDownloadUrl();
//                }
//            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
//                @Override
//                public void onComplete(@NonNull Task<Uri> task) {
//                    if (task.isSuccessful()) {
//                        Uri downloadUri = task.getResult();
//                        MessageDataModel message = new MessageDataModel(currentUser, null, downloadUri.toString());
//                        mMessagesDatabaseReference.push().setValue(message);
//                    }
//                }
//            });
//        }
    }

    private void putImageInStorage(Uri uri, final String key) {
        mChatPhotosStorageReference.putFile(uri).addOnCompleteListener(ChatActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            task.getResult().getMetadata().getReference().getDownloadUrl()
                                    .addOnCompleteListener(ChatActivity.this,
                                            new OnCompleteListener<Uri>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Uri> task) {
                                                    if (task.isSuccessful()) {
                                                        MessageDataModel friendlyMessage =
                                                                new MessageDataModel(currentUser, null,
                                                                        task.getResult().toString());
                                                        mMessagesDatabaseReference.child(key)
                                                                .setValue(friendlyMessage);
                                                    }
                                                }
                                            });
                        } else {
                            Log.w("PHOTO", "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }


    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    MessageDataModel message = dataSnapshot.getValue(MessageDataModel.class);
                    friendlyMessages.add(message);
                    Log.e("MESSAGES", friendlyMessages.toString());
                    mMessageListAdapter.notifyDataSetChanged();
                    mMessageRecyclerView.scrollToPosition(friendlyMessages.size() - 1);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachDatabaseReadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
        }
        detachDatabaseReadListener();
        friendlyMessages.clear();
    }

    private String getChatChild(String uid1, String uid2) {
        if (uid1.compareTo(uid2) > 0) {
            return uid1 + uid2;
        } else
            return uid2 + uid1;
    }
}
