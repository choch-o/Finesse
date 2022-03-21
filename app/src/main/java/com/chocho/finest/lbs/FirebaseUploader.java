package com.chocho.finest.lbs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.chocho.finest.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import kr.ac.kaist.nmsl.xdroid.util.Singleton;

public class FirebaseUploader {
    private static final Singleton<FirebaseUploader> singleton = new Singleton<FirebaseUploader>() {
        @Override
        protected FirebaseUploader create() {
            return new FirebaseUploader();
        }
    };

    private static final String TAG = "Firebase";
    private static final String FIREBASE_UPLOADER_CONF = "firebase-uploader-conf";
    private static final String TO_BE_UPLOADED = "to-be-uploaded";

    GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth mAuth;
    StorageReference mStorageRef;
    SharedPreferences mSharedPref;
    String mUsername;

    private FirebaseUploader() {
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mSharedPref = null;
    }

    public static FirebaseUploader getInstance() {
        return singleton.get();
    }

    public void init(Context context, String username) {
        mSharedPref = context.getSharedPreferences(FIREBASE_UPLOADER_CONF, 0);
//        initAuth(context);
        mUsername = username;

        mAuth.signInAnonymously();

        uploadAll();
    }

    public void enqueue(File file) {
        if (mSharedPref == null)
            return;

        SharedPreferences.Editor editor = mSharedPref.edit();
        Set<String> set;
        if (mSharedPref.contains(TO_BE_UPLOADED))
            set = new HashSet<>(mSharedPref.getStringSet(TO_BE_UPLOADED, new HashSet<String>()));
        else
            set = new HashSet<>();
        if (set.contains(file.getAbsolutePath()))
            return;
        set.add(file.getAbsolutePath());
        editor.putStringSet(TO_BE_UPLOADED, set);
        editor.apply();

        uploadAll();
    }

    public void enqueue(File[] files) {
        if (mSharedPref == null || files == null)
            return;

        SharedPreferences.Editor editor = mSharedPref.edit();
        Set<String> set;
        if (mSharedPref.contains(TO_BE_UPLOADED))
            set = new HashSet<>(mSharedPref.getStringSet(TO_BE_UPLOADED, new HashSet<String>()));
        else
            set = new HashSet<>();
        for (File file : files) {
            if (set.contains(file.getAbsolutePath()))
                continue;
            set.add(file.getAbsolutePath());
        }
        editor.putStringSet(TO_BE_UPLOADED, set);
        editor.apply();

        uploadAll();
    }

    public void dequeue(File file) {
        if (mSharedPref == null)
            return;

        SharedPreferences.Editor editor = mSharedPref.edit();
        Set<String> set;
        if (mSharedPref.contains(TO_BE_UPLOADED))
            set = new HashSet<>(mSharedPref.getStringSet(TO_BE_UPLOADED, new HashSet<String>()));
        else
            return;
        if (!set.contains(file.getAbsolutePath()))
            return;
        set.remove(file.getAbsolutePath());
        editor.putStringSet(TO_BE_UPLOADED, set);
        editor.apply();
    }

    public void uploadAll() {
        if (mSharedPref == null)
            return;

        if (!mSharedPref.contains(TO_BE_UPLOADED))
            return;

        Set<String> set = mSharedPref.getStringSet(TO_BE_UPLOADED, new HashSet<String>());
        if (set == null)
            return;

        for (final String path : set) {
            final File file = new File(path);
            if (!file.isFile())
                continue;

            String refPath = "/user/" + mUsername + '/' + file.getName();
//            Log.d(TAG, "Upload path: " + refPath);
            Uri uri = Uri.fromFile(file);
            StorageReference fileRef = mStorageRef.child(refPath);

            fileRef.putFile(uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        String mPath = path;

                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Get a URL to the uploaded content
//                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            File file = new File(mPath);
                            FirebaseUploader.getInstance().dequeue(file);
                            if (file.isFile())
                                file.delete();
                            uploadAll();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                            // ...
                            Log.w(TAG, exception);
                        }
                    });

            // 200908 dhkim: We upload one at a time. Instead, when an upload succeeds this method
            // is invoked again for the next upload. Such a mechanism is devised to avoid memory
            // shortage that could happen when we upload all files at once.
            // Drawback: Any tiny failure on error checking or an unsuccessful upload could break
            // the chain of uploadAll() calls and it could leave files in local storage. However,
            // everytime a new file to upload is created (i.e., new 10 MB of log is zipped),
            // uploadAll() call chain will be initiated again.
            // Concern 1: One might ask why not calling uploadAll() after an unsuccessful upload.
            // This is very valid question because if one particular file has upload problem then
            // the upload of all other file could be affected. I am currently ignoring this because
            // our expected upload targets are in the same zip format & have similar file sizes.
            // Concern 2: Since uploadAll() is chained asynchronously, a file in the middle of
            // an uploading process initiated by a uploadAll() chain could also be visited by
            // another uploadAll() chain. As far as I understand, Firebase handles such a double
            // upload request; i.e., multiple putFile() call with the same file.
            break;
        }
    }

    public void initAuth(Context context) {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getResources().getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public FirebaseUser getFireAuthUser() {
        return mAuth.getCurrentUser();
    }

    public Intent getLoginIntent(Context context) {
        if (mGoogleSignInClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getResources().getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            // Build a GoogleSignInClient with the options specified by gso.
            mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        }
        return mGoogleSignInClient.getSignInIntent();
    }

    public void tryLogin(Intent data, OnCompleteListener onCompleteListener){
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            // Google Sign In was successful, authenticate with Firebase
            GoogleSignInAccount account = task.getResult(ApiException.class);

//            Log.d("GoogleLogIn", "firebaseAuthWithGoogle:" + account.getId());

            assert account != null;
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(onCompleteListener);

        } catch (ApiException e) {
            // Google Sign In failed, update UI appropriately
//            Log.w("GoogleLogIn", "Google sign in failed", e);
            Log.e("GoogleLogin", "Google sign in failed", e);
            // ...
        }
    }
}
