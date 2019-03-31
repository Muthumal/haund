package lk.janiru.greentrack.main.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import lk.janiru.greentrack.R;
import lk.janiru.greentrack.db.entiity.User;
import lk.janiru.greentrack.services.signin.GoogleSignInActivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener {

    public static FirebaseUser FIREBASE_USER = null;

    private static final String TAG = "MainActivity";

    private static final int ERROR_DIALOG_REQUEST = 9001;

    GoogleMap mMap;
    private boolean mLocationPermissionGranted = false;

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        androidx.appcompat.widget.Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Log.d(TAG, "onCreate: Called");

        getLocationPermission();


        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();


        mFirebaseAuthLister = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();

                if(currentUser == null){
                    Intent intent = new Intent(MainActivity.this, GoogleSignInActivity.class);
                    startActivity(intent);
                }else{
                    setUserDataOnStart(currentUser);
                }
            }
        };


    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    /***********************************************************************************************
     *                                      Google Map API                                         *
     **********************************************************************************************/




    /***********************************************************************************************
     * Map Initializer
     **********************************************************************************************/
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }



    /***********************************************************************************************
     * OnMapReadyCallback function Methods
     **********************************************************************************************/

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady: Called");

        Toast.makeText(this, "Map is ready to use", Toast.LENGTH_LONG).show();

        mMap = googleMap;

        if(mLocationPermissionGranted){
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "getDeviceLocation: Not Permitted for in the getDeviceLocation");
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
            mMap.setOnMyLocationClickListener(this);

        }



    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Log.println(Log.INFO, "MAP", "OnPointerCaptureChanged is called.........!");
    }


    /***********************************************************************************************
     * Map Animation Related Methods
     **********************************************************************************************/

    private void showLocationWithAnimation(LatLng sydney) {

        Log.println(Log.INFO, TAG, "********************************* OnPointerCaptureChanged is called *********************************");

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(sydney)      // Sets the center of the map to Mountain View
                .zoom(17)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }


    /***********************************************************************************************
     * Current Location Related Methods
     **********************************************************************************************/

    @Override
    public boolean onMyLocationButtonClick() {
        Log.d(TAG, "onMyLocationButtonClick: Called");

        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Log.d(TAG, "onMyLocationClick: Called");

    }


    /***********************************************************************************************
     * Check the Google Play Service
     **********************************************************************************************/
    public boolean isServiceOk() {
        Log.d(TAG, "isServiceOk: Check the correct version of the GooglePlayService");

        int googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
            Log.d(TAG, "isService Ok: Google Play Service is Working fine");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(googlePlayServicesAvailable)) {

            Log.d(TAG, "isService Ok: Google Play Service is having some error");
            Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, googlePlayServicesAvailable, ERROR_DIALOG_REQUEST);
            errorDialog.show();

        } else {
            Toast.makeText(this, "We cannot make map requests.", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    /**
     * Get the Center location of the marker
     * */
    private LatLng getCenterMarkerLocation(){
        LatLng target = mMap.getCameraPosition().target;
        return target;
    }




    /***********************************************************************************************
     * Get the Location Related Permissions
     **********************************************************************************************/

    /**
     * Request the permission
     */
    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    /**
     * Permission Request and checking
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult: Called");
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (permissions.length > 1) {


                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        mLocationPermissionGranted = false;
                        return;
                    }
                }
                mLocationPermissionGranted = true;
                initMap();
                Log.d(TAG, "onRequestPermissionsResult: Map is initialized");
            } else {
                // Permission was denied. Display an error message.
                Log.d(TAG, "onRequestPermissionsResult: Permission Denied");
                Toast.makeText(this, "Cannot load the map the permission was denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the device location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionGranted) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG, "getDeviceLocation: Not Permitted for in the getDeviceLocation");
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Task<Location> location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {

                        Location currentLocation = task.getResult();
                        if(currentLocation != null){
                            Log.d(TAG, "onComplete: found the location " + currentLocation.getLatitude() + "  " + currentLocation.getLongitude());
                            showLocationWithAnimation(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                        }



                    }
                });

            }
        }catch (Exception e){

        }

    }

    /***********************************************************************************************
     *                                    Firebase Connection                                      *
     **********************************************************************************************/

    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mFirebaseAuthLister;
    private DatabaseReference mRef;


    public void setUserDataOnStart(FirebaseUser user){
        ImageView imageView = findViewById(R.id.imageView);
        TextView userName = findViewById(R.id.txtUserName);
        TextView email = findViewById(R.id.txtEmail);

        imageView.setImageURI(user.getPhotoUrl());
        userName.setText(user.getDisplayName());
        email.setText(user.getEmail());

        Log.d(TAG, "setUserDataOnStart: Called, User details are " + user.getDisplayName() + "  " + user.getPhotoUrl());

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mFirebaseAuthLister);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mFirebaseAuthLister != null){
            mAuth.removeAuthStateListener(mFirebaseAuthLister);
        }
    }

    /**
    * Share the location to the Firebase
    * */
    private void setMyCurrentLocationToDatabsae(LatLng target) {
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: Called");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: Called");

            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LatLng target = mMap.getCameraPosition().target;
                setMyCurrentLocationToDatabsae(target);

                Log.d(TAG, "onClick: Called to store the data to the database name : "+ FIREBASE_USER.getDisplayName() + " email : " + FIREBASE_USER.getEmail() + "location "+ "latlag" + target.latitude + "  " + target.longitude);

//                Toast.makeText(MainActivity.this,"My Location 6.691928, 79.918058" ,Toast.LENGTH_LONG).show();
//                Snackbar.make(view, "latlag" + target.latitude + "  " + target.longitude, Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                if(FIREBASE_USER != null && target != null){
                    User user = new User(FIREBASE_USER.getUid(),FIREBASE_USER.getDisplayName(),target.latitude + " ," + target.longitude);
                    mRef.child("Users").child(FIREBASE_USER.getUid()).setValue(user);
                }else{
                    Log.d(TAG, "onClick: Called Error when Storing the data");
                    Snackbar.make(view, "You have to log in first", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }

            }
        });

    }

}




//        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
//            Log.println(Log.INFO,"PERMISSION", "********************************* ACCESS_FINE_LOCATION Permission Granted *********************************");
////            mMap.setMyLocationEnabled(true);
//        } else {
//            Log.println(Log.INFO,"PERMISSION", "********************************* ACCESS_FINE_LOCATION Permission Not Granted *********************************");
//            // Show rationale and request permission.
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
//        }
//
//        if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)){
//            Log.println(Log.INFO,"PERMISSION", "********************************* ACCESS_FINE_LOCATION Permission Granted *********************************");
//        }else{
//            Log.println(Log.INFO,"PERMISSION", "********************************* ACCESS_COARSE_LOCATION Permission Not Granted *********************************");
//            // Show rationale and request permission.
//
//            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE_ACCESS_COARSE_LOCATION);
//        }

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            mMap.setMyLocationEnabled(true);
//            mMap.setOnMyLocationButtonClickListener(this);
//            mMap.setOnMyLocationClickListener(this);
//        } else {
//            // Show rationale and request permission.
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
//        }
