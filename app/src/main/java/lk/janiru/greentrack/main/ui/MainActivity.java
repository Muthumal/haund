package lk.janiru.greentrack.main.ui;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.room.RoomDatabase;
import lk.janiru.greentrack.R;
import lk.janiru.greentrack.db.entiity.User;
import lk.janiru.greentrack.db.repository.UserRepository;
import lk.janiru.greentrack.services.LocationServices;
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

    public static LatLng currentUserLocation = new LatLng(0.0,0.0);
    public static LatLng currentBusLocation = new LatLng(0.0,0.0);

    //For markers and the distance calculations
    private Map<String, Marker> mMarkers = new HashMap<>();
    LocationServices distance = new LocationServices();

    public UserRepository userRepository;

    public static boolean resumeStatus = false;
    public static boolean isNotificationSent = false;

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


    public final static boolean[] isServiceOn = {false};

    public static boolean iaminTheBus = false;

    public static RoomDatabase.Builder<RoomDatabase> INSTANCE;


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

         userRepository = new UserRepository(this);


        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseDatabase.getInstance().getReference();

        FirebaseMessaging.getInstance().subscribeToTopic("BUS_IS_NEAR_BY");

        mFirebaseAuthLister = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                if (FIREBASE_USER == null) {
                    Log.d(TAG, "onAuthStateChanged: Use is null");
                    Intent intent = new Intent(MainActivity.this, GoogleSignInActivity.class);
                    startActivity(intent);
                } else {
                    Log.d(TAG, "onAuthStateChanged: User is alive " + FIREBASE_USER.getDisplayName());
                    setUserDataOnStart(FIREBASE_USER);
                    setMyCurrentLocationToDatabsae();
                }
            }

        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }


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


//            User statusUser = new User(FIREBASE_USER.getUid(), FIREBASE_USER.getDisplayName(), "0,0");
//            statusUser.setAlive(false);
//            userRepository.update(statusUser);
//
//            if(FIREBASE_USER != null){
//
//            }
//
//
//
//
//            if(isServiceOn[0]){
//
//            }
//
//            User statusUser = new User(FIREBASE_USER.getUid(), FIREBASE_USER.getDisplayName(), "0,0");
//            statusUser.setAlive(false);
//
//            mRef.child("User").child(FIREBASE_USER.getUid()).setValue(statusUser);
//            // Handle the camera action
//            isServiceOn[0] = false;


        } else if (id == R.id.nav_share) {

            final FirebaseUser firebaseUser = FIREBASE_USER;

            GoogleSignInActivity.mAuth.signOut();
            GoogleSignInActivity.mGoogleSignInClient.revokeAccess().addOnCompleteListener(MainActivity.this,
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mRef.child("Driver").child(FIREBASE_USER.getUid()).setValue(
                                    new User(FIREBASE_USER.getUid(), FIREBASE_USER.getDisplayName(), "0,0"));
                            isServiceOn[0] = false;
                            startActivity(new Intent(MainActivity.this, GoogleSignInActivity.class));
                            finish();
                        }
                    }
            );


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

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        Log.d(TAG, "onMapReady: Called");

        //Toast.makeText(this, "Map is ready to use", Toast.LENGTH_LONG).show();

        mMap = googleMap;

        if (mLocationPermissionGranted) {


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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


            driverIsCloseNotification();

//            User checkTheUser = userRepository.getUser();

            getDeviceLocation();

            if (userRepository.getUser() == null) {
                User user = new User(FIREBASE_USER.getUid(), FIREBASE_USER.getDisplayName(), currentUserLocation.latitude + "," + currentUserLocation.longitude);
                userRepository.insert(user);
                resumeStatus = false;
            } else {
                User userToGetTheLocation = userRepository.getUser();
                if (userToGetTheLocation.getLocation().equals("0,0")) {
                    String[] split = userToGetTheLocation.getLocation().split(",");

                    mRef.child("User").child(userToGetTheLocation.getUserId()).child("location").setValue(split[0] + "," + split[1]);

                    LatLng latLng = new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));

                    new CameraPosition.Builder().target(latLng).build();

                    resumeStatus = false;

                } else {

                    resumeStatus = true;

                    //Toast.makeText(this, "//////////////////////////////////",Toast.LENGTH_LONG).show();
                    String[] split = userToGetTheLocation.getLocation().split(",");

                    LatLng latLng = new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
                    showLocationWithAnimation(latLng);

                }

            }

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
     */
    private LatLng getCenterMarkerLocation() {
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

        mFusedLocationProviderClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionGranted) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                        if (currentLocation != null) {
                            Log.d(TAG, "onComplete: found the location " + currentLocation.getLatitude() + "  " + currentLocation.getLongitude());


                            if(! resumeStatus){
                                showLocationWithAnimation(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                            }

                        }


                    }
                });

            }
        } catch (Exception e) {

        }

    }

    /***********************************************************************************************
     *                                    Firebase Connection                                      *
     **********************************************************************************************/

    public static FirebaseDatabase mFirebaseDatabase;
    public static FirebaseAuth mAuth;
    public static FirebaseAuth.AuthStateListener mFirebaseAuthLister;
    public static DatabaseReference mRef;


//    public void setUserDataOnStart(FirebaseUser user) {
//
//        if (FIREBASE_USER != null) {
//            ImageView imageView = findViewById(R.id.imageViewUserPhoto);
//            TextView userName = findViewById(R.id.txtUserName);
//            TextView email = findViewById(R.id.txtEmail);
//
//            try {
//                imageView.setImageURI(user.getPhotoUrl());
//                userName.setText(user.getDisplayName());
//                email.setText(user.getEmail());
//            } catch (Exception e) {
//            }
//
//
//            Log.d(TAG, "setUserDataOnStart: Called, User details are " + user.getDisplayName() + "  " + user.getPhotoUrl());
//        }
//    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mFirebaseAuthLister);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFirebaseAuthLister != null) {
            mAuth.removeAuthStateListener(mFirebaseAuthLister);
        }
    }

    /**
     * Share the location to the Firebase
     */
    private void setMyCurrentLocationToDatabsae() {
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

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        Button fab =  findViewById(R.id.btnLocationSt);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                notificationStatus = true;

                currentUserLocation = mMap.getCameraPosition().target;

//                Toast.makeText(MainActivity.this,"My Location 6.691928, 79.918058" ,Toast.LENGTH_LONG).show();
//                Snackbar.make(view, "latlag" + target.latitude + "  " + target.longitude, Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                if (FIREBASE_USER != null && currentUserLocation != null) {
                    Log.d(TAG, "onClick: Called to store the data to the database name : " + FIREBASE_USER.getDisplayName() + " email : " + FIREBASE_USER.getEmail() + "location " + "latlag" + currentUserLocation.latitude + "  " + currentUserLocation.longitude);
                    User user = new User(FIREBASE_USER.getUid(), FIREBASE_USER.getDisplayName(), currentUserLocation.latitude + " ," + currentUserLocation.longitude);
                    mRef.child("User").child(FIREBASE_USER.getUid()).setValue(user);
                } else {
                    Log.d(TAG, "onClick: Called Error when Storing the data");
                    Snackbar.make(view, "You have to log in first", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }


                if(FIREBASE_USER != null){
                    if(userRepository.getUser()==null){
                        User user = new User(FIREBASE_USER.getUid(),FIREBASE_USER.getDisplayName(),currentUserLocation.latitude + "," + currentUserLocation.longitude);
                        userRepository.insert(user);
                    }else {
                        User user = userRepository.getUser();
                        user.setLocation(currentUserLocation.latitude + "," + currentUserLocation.longitude);
                        userRepository.update(user);
                    }


//
//                    System.out.println("*************************************************");
                    System.out.println(userRepository.getById(FIREBASE_USER.getUid()));
//                    System.out.println("***************************************************");



                }else{

                }



                sendNotificationCall();
                getAccessTocken();


            }
        });

    }


    // Distance count and the notificationStatus
    public static boolean notificationStatus = true;


    private void driverIsCloseNotification() {
        mRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildChanged: Called " + dataSnapshot.getKey());

                setMarker(dataSnapshot);

//                System.out.println("******************************" + dataSnapshot.getValue(). );
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved: Called");
                setMarker(dataSnapshot);

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d(TAG, "onChildMoved: Called");
                setMarker(dataSnapshot);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: Called");
            }
        });
    }

    // Set markers
    private void setMarker(DataSnapshot dataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once

        String type = dataSnapshot.getKey();
        if(FIREBASE_USER==null |type.equals("NSBM"))return;
        HashMap<String, HashMap<String, String>> value = (HashMap<String, HashMap<String, String>>) dataSnapshot.getValue();

        Map<String, LatLng> busLocationList = new HashMap<>();


        boolean isDriver = type.equals("Driver");

        for (HashMap<String, String> s : value.values()) {

            String key = s.get("name");
            String locationStr = s.get("location");
            String[] split = locationStr.split(",");
            double lat = Double.parseDouble(split[0]);
            double lng = Double.parseDouble(split[1]);

            System.out.println("INSIDE OF ADD MARKER " + lat + " " + lng + " " + key);


            if (!isDriver & !s.get("userId").equals(FIREBASE_USER.getUid())) {
//                currentUserLocation = new LatLng(lat,lng);
                continue;
            }

            LatLng location = new LatLng(lat, lng);

            Bitmap icon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), isDriver ? R.drawable.bus_marker : R.drawable.bus_marker_green), 100, 100, false);

            if (!mMarkers.containsKey(key)) {
                mMarkers.put(key, mMap.addMarker(new MarkerOptions().title(key).position(location)
                        .icon(BitmapDescriptorFactory.fromBitmap(icon))));
                currentBusLocation = location;

                busLocationList.put(s.get("userId"), currentBusLocation);
            } else {
                mMarkers.get(key).setPosition(location);
                LatLng latLng = busLocationList.get(key);
                latLng = currentBusLocation;

                currentBusLocation = location;

            }

            if (currentBusLocation != null && currentUserLocation != null) {
                if (isDriver) {
                    Log.d(TAG, "setMarker: Before the distance calculation DRIVER" + currentUserLocation + " " + currentBusLocation + " " + key);
                } else {
                    Log.d(TAG, "setMarker: Before the distance calculation USER" + currentUserLocation + " " + currentBusLocation + " " + key);

                }


                Intent intent = new Intent(this, LocationServices.class);
                startService(intent);

            }

            if (locationStr.equals("0,0")) {
                if (mMarkers.containsKey(key)) {
                    Marker marker = mMarkers.get(key);
                    marker.remove();
                    mMarkers.remove(key);
                }
                continue;
            }


        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : mMarkers.values()) {
            builder.include(marker.getPosition());
        }


    }


    void getAccessTocken() {
        // [START retrieve_current_token]
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
        // [END retrieve_current_token]
    }

    void sendNotificationCall() {
        Log.d(TAG, "Subscribing to weather topic");
        // [START subscribe_topics]
        FirebaseMessaging.getInstance().subscribeToTopic("vehicleisnearby")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = getString(R.string.msg_subscribed);
                        if (!task.isSuccessful()) {
                            msg = getString(R.string.msg_subscribe_failed);
                        }
                        Log.d(TAG, msg);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
        // [END subscribe_topics]


    }

    public void setUserDataOnStart(FirebaseUser user){
        NavigationView navigationView = findViewById(R.id.nav_view);
        View view = navigationView.getHeaderView(0);
        //getResources().get
        if(user != null){
            TextView userName = view.findViewById(R.id.txtUserName);
            TextView email = view.findViewById(R.id.txtEmail);
            new DownloadImageTask((ImageView) view.findViewById(R.id.imageViewUserPhoto))
                    .execute(user.getPhotoUrl().toString());


//        imageView.setImageURI(user.getPhotoUrl());
            userName.setText(user.getDisplayName());
            email.setText(user.getEmail());
            Log.d(TAG, "setUserDataOnStart: Called, User details are " + user.getDisplayName() + "  " + user.getPhotoUrl());
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
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
