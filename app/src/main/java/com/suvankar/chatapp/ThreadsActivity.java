package com.suvankar.chatapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.suvankar.chatapp.adapters.ThreadListAdapter;
import com.suvankar.chatapp.models.UserDataModel;

import java.util.ArrayList;
import java.util.Arrays;

public class ThreadsActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1;

    private ArrayList<String> contacts;
    private ArrayList<UserDataModel> users;
    private ThreadListAdapter threadListAdapter;
    private RecyclerView recyclerView;
    private ShimmerFrameLayout shimmerFrameLayout;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserDatabaseReference;
    private ValueEventListener mValueEventListener;
    private FirebaseAuth.AuthStateListener mAuthStateListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_threads);
        FirebaseApp.initializeApp(this);

        shimmerFrameLayout = findViewById(R.id.shimmer_container);

        users = new ArrayList<>();
        contacts = new ArrayList<>();
        fetchEmails();
        Log.e("CONTACTS", contacts.toString());

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
//        mfirebaseStorage = FirebaseStorage.getInstance();

        mUserDatabaseReference = mFirebaseDatabase.getReference().child("users");

        recyclerView = findViewById(R.id.userList);
        threadListAdapter = new ThreadListAdapter(ThreadsActivity.this, users);
        recyclerView.setAdapter(threadListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(ThreadsActivity.this));

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mFirebaseUser = firebaseAuth.getCurrentUser();
                if (mFirebaseUser != null) {
                    UserDataModel user = new UserDataModel(mFirebaseUser);
                    mUserDatabaseReference.child(mFirebaseUser.getUid()).setValue(user);
                    onSignedInInitialize();
                } else {
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .setTheme(R.style.AppTheme)
                                    .build(), RC_SIGN_IN);
                }
            }
        };

//        AccessContact();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(ThreadsActivity.this, "Sign in successful", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(ThreadsActivity.this, "Sign in cancelled.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void attachDatabaseReadListener() {
        if (mValueEventListener == null) {
            mValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    users.clear();
                    UserDataModel user;
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        user = postSnapshot.getValue(UserDataModel.class);
                        if (!user.getId().equals(mFirebaseAuth.getCurrentUser().getUid()) && contacts.contains(user.getEmail()))
                            users.add(user);
                    }
                    threadListAdapter.notifyDataSetChanged();
                    if (shimmerFrameLayout.isAnimationStarted()) {
                        shimmerFrameLayout.stopShimmerAnimation();
                        shimmerFrameLayout.setVisibility(View.GONE);
                    }
                    Log.e("USERS", users.toString());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
            mUserDatabaseReference.addValueEventListener(mValueEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if (mValueEventListener != null) {
            mUserDatabaseReference.removeEventListener(mValueEventListener);
            mValueEventListener = null;
        }
    }

    public void onSignedInInitialize() {
        attachDatabaseReadListener();
    }

    public void onSignedOutCleanup() {
        users.clear();
        detachDatabaseReadListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shimmerFrameLayout.stopShimmerAnimation();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        if (mValueEventListener != null) {
            mUserDatabaseReference.removeEventListener(mValueEventListener);
        }
        detachDatabaseReadListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        shimmerFrameLayout.startShimmerAnimation();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fetchEmails() {

        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    null,
                    null,
                    null,
                    null);
            if (cur.getCount() > 0) {
                Log.i("Content provider", "Reading contact  emails");
                while (cur.moveToNext()) {
                    String contactId = cur.getString(cur
                            .getColumnIndex(ContactsContract.Contacts._ID));
                    // Create query to use CommonDataKinds classes to fetch emails
                    Cursor emails = cr.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID
                                    + " = " + contactId, null, null);

                    while (emails.moveToNext()) {
                        // This would allow you get several email addresses
                        String emailAddress = emails.getString(emails
                                .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                        contacts.add(emailAddress);
                    }
                    emails.close();
                }
            }
            cur.close();
        } catch (Exception e) {
        }
    }

//    private void AccessContact() {
//        List<String> permissionsNeeded = new ArrayList<String>();
//        final List<String> permissionsList = new ArrayList<String>();
//        if (!addPermission(permissionsList, Manifest.permission.READ_CONTACTS))
//            permissionsNeeded.add("Read Contacts");
//        if (!addPermission(permissionsList, Manifest.permission.WRITE_CONTACTS))
//            permissionsNeeded.add("Write Contacts");
//        if (permissionsList.size() > 0) {
//            if (permissionsNeeded.size() > 0) {
//                String message = "You need to grant access to " + permissionsNeeded.get(0);
//                for (int i = 1; i < permissionsNeeded.size(); i++)
//                    message = message + ", " + permissionsNeeded.get(i);
//                showMessageOKCancel(message,
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                    requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
//                                            REQUEST_MULTIPLE_PERMISSIONS);
//                                }
//                            }
//                        });
//                return;
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
//                        REQUEST_MULTIPLE_PERMISSIONS);
//            }
//            return;
//        }
//    }
//
//    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
//        new AlertDialog.Builder(ThreadsActivity.this)
//                .setMessage(message)
//                .setPositiveButton("OK", okListener)
//                .setNegativeButton("Cancel", null)
//                .create()
//                .show();
//    }
//
//    private boolean addPermission(List<String> permissionsList, String permission) {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
//                permissionsList.add(permission);
//
//                if (!shouldShowRequestPermissionRationale(permission))
//                    return false;
//            }
//        }
//        return true;
//    }

}
