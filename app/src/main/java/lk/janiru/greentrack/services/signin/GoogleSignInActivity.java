package lk.janiru.greentrack.services.signin;


/**
 *
 * Project Name : ${PROJECT}
 * Created by Janiru on 3/29/2019 12:44 AM.
 *
 */

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;


import java.util.ArrayList;

import androidx.annotation.NonNull;
import lk.janiru.greentrack.BaseActivity;
import lk.janiru.greentrack.R;
import lk.janiru.greentrack.main.ui.MainActivity;

/**
 * Demonstrate Firebase Authentication using a Google ID Token.
 */
public class GoogleSignInActivity extends BaseActivity implements
        View.OnClickListener {


    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;

    // [START declare_auth]
    public static FirebaseAuth mAuth;
    // [END declare_auth]

    public static GoogleSignInClient mGoogleSignInClient;
    private TextView mStatusTextView;
    private TextView mDetailTextView;
    public static ArrayList<String> allowed_emails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(lk.janiru.greentrack.R.layout.activity_google);


        // Views
        mStatusTextView = findViewById(lk.janiru.greentrack.R.id.status);
        mDetailTextView = findViewById(lk.janiru.greentrack.R.id.detail);

        // Button listeners
        findViewById(lk.janiru.greentrack.R.id.signInButton).setOnClickListener(this);
        findViewById(lk.janiru.greentrack.R.id.signOutButton).setOnClickListener(this);
        findViewById(lk.janiru.greentrack.R.id.disconnectButton).setOnClickListener(this);

        // [START config_signin]
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // [START initialize_auth]
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        MainActivity.FIREBASE_USER = currentUser;
        updateUI(MainActivity.FIREBASE_USER);
        // [END initialize_auth]
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.

        if(MainActivity.FIREBASE_USER!=null){
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }
    // [END on_start_check_user]

    // [START onactivityresult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);

                MainActivity.FIREBASE_USER = mAuth.getCurrentUser();
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // [START_EXCLUDE]
                updateUI(null);
                // [END_EXCLUDE]
            }
        }
    }
    // [END onactivityresult]

    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if(!allowed_emails.contains(user.getEmail())){
                                Toast.makeText(GoogleSignInActivity.this,"You don't have permission to use this app",Toast.LENGTH_LONG).show();
                                signOut();
                            }else {
                                MainActivity.FIREBASE_USER = user;
                            }updateUI(user);
                            startActivity(new Intent(GoogleSignInActivity.this,MainActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(lk.janiru.greentrack.R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END auth_with_google]

    // [START signin]
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signin]

    public void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        hideProgressDialog();
        if (user != null) {
            System.out.println("******* Name : " + user.getDisplayName());
            System.out.println("******* Phone : " + user.getPhoneNumber());
            System.out.println("******* Image : " + user.getPhotoUrl().toString());



            mStatusTextView.setText(getString(lk.janiru.greentrack.R.string.google_status_fmt, user.getEmail()));
            mDetailTextView.setText(getString(lk.janiru.greentrack.R.string.firebase_status_fmt, user.getUid()));

            findViewById(lk.janiru.greentrack.R.id.signInButton).setVisibility(View.GONE);
            findViewById(lk.janiru.greentrack.R.id.signOutAndDisconnect).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == lk.janiru.greentrack.R.id.signInButton) {
            signIn();
        } else if (i == lk.janiru.greentrack.R.id.signOutButton) {
            signOut();
        } else if (i == lk.janiru.greentrack.R.id.disconnectButton) {
            revokeAccess();
        }
    }
}
