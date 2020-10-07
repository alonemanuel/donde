package com.example.donde.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.donde.BuildConfig;
import com.example.donde.R;
import com.example.donde.models.EventModel;
import com.example.donde.models.InvitedUserModel;
import com.example.donde.utils.map_utils.CustomMapTileProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CreateEventActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    SearchView searchView;


    LocationCallback mLocationCallback;
    private EditText editTextEventName;
    private EditText editTextEventDescription;
    private EditText editTextLocationName;
    private EditText editTextLongitude;
    private EditText editTextLatitude;
    private EditText editTextEventDay;
    private EditText editTextEventMonth;
    private EditText editTextEventYear;
    private EditText editTextEventHour;
    private EditText editTextEventMinute;
    private EditText editTextInvitedUser1;
    private EditText editTextInvitedUser2;
    private EditText editTextInvitedUser3;
    private Button buttonCreateEvent;
    private Button buttonDebugAutofill;
    private ProgressBar progressBar;
    private FirebaseFirestore firebaseFirestore;
    private SearchView searchViewLocationSearch;

    // Authentication
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;


    // Fields for storing data to push to FirebaseFirestore (ff)
    private String ffEventName;
    private String ffEventDescription;
    private String ffEventLocationName;
    private GeoPoint ffEventLocation;
    private String ffEventCreatorUID;
    private String ffEventCreatorName;
    private Date ffEventTimeCreated;
    private Date ffEventTimeStarting;

    private List<InvitedUserModel> ffInvitedUserModels;


    void checkForPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                // This part I didn't implement,because for my case it isn't needed
                Log.i("TAG", "Unexpected flow");
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_LOCATION);

                // MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(CreateEventActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkForPermissions();
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void initializeFields() {
        editTextEventName = findViewById(R.id.create_editText_event_name);
        editTextEventDescription = findViewById(R.id.create_editText_event_description);
//        editTextLocationName = findViewById(R.id.create_editText_location_name);
//        editTextLongitude = findViewById(R.id.create_editText_longitude);
//        editTextLatitude = findViewById(R.id.create_editText_latitude);
        buttonCreateEvent = findViewById(R.id.create_button_create);
        buttonDebugAutofill = findViewById(R.id.create_button_debug_fill_default);
        progressBar = findViewById(R.id.create_progressBar);
        editTextEventDay = findViewById(R.id.create_editText_event_day);
        editTextEventMonth = findViewById(R.id.create_editText_event_month);
        editTextEventYear = findViewById(R.id.create_editText_event_year);
        editTextEventHour = findViewById(R.id.create_editText_event_hour);
        editTextEventMinute = findViewById(R.id.create_editText_event_minute);
        editTextInvitedUser1 = findViewById(R.id.create_editText_invited_user1);
        editTextInvitedUser2 = findViewById(R.id.create_editText_invited_user2);
        editTextInvitedUser3 = findViewById(R.id.create_editText_invited_user3);

        searchViewLocationSearch = findViewById(R.id.create_searchView_location_search);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (BuildConfig.DEBUG && !(firebaseUser != null)) {
            throw new AssertionError("No user is logged in");
        }
        firebaseFirestore = FirebaseFirestore.getInstance();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFrag =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.create_mapView);

        mapFrag.getMapAsync(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                List<Location> locationList = locationResult.getLocations();
                if (locationList.size() > 0) {
                    //The last location in the list is the newest
                    Location location = locationList.get(locationList.size() - 1);
                    Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                    mLastLocation = location;
                    if (mCurrLocationMarker != null) {
                        mCurrLocationMarker.remove();
                    }

                    //Place current location marker
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title("Current Position");

//                markerOptions.icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.shani_getz));

//                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.shani_getz));

                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                    mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
                    //move map camera
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
                }
            }
        };
    }

    private Timestamp getEventStartTime() {
        int sEventDay = Integer.parseInt(editTextEventDay.getText().toString());
        int sEventMonth = Integer.parseInt(editTextEventMonth.getText().toString());
        int sEventYear = Integer.parseInt(editTextEventYear.getText().toString());
        int sEventHour = Integer.parseInt(editTextEventHour.getText().toString());
        int sEventMinute = Integer.parseInt(editTextEventMinute.getText().toString());
        Timestamp startTime = new Timestamp(new Date(sEventYear, sEventMonth, sEventDay,
                sEventHour, sEventMinute));
        return startTime;
    }

//    private List<String> getEventInvitedUserIDs() {
//        String invitedEmail0 = firebaseAuth.getCurrentUser().getUid();
//        String invitedEmail1 = editTextInvitedUser1.getText().toString();
//        String invitedEmail2 = editTextInvitedUser2.getText().toString();
//        String invitedEmail3 = editTextInvitedUser3.getText().toString();
//
////        FirebaseFirestore.getInstance().collection(getString(R.string.ff_users_collection)).get().addOnCompleteListener();
//        List<String> invitedUserEmails = Arrays.asList(invitedEmail0, invitedEmail1,
//                invitedEmail2, invitedEmail3);
//
//        List<String> invitedUserIDs = new ArrayList<>();
//        return invitedUserIDs;
//    }

    private void initializeSearchQuery() {
//        GoogleMap mGoogleMap =
        searchViewLocationSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchViewLocationSearch.getQuery().toString();
                Toast.makeText(CreateEventActivity.this, "Search query is: " + location,
                        Toast.LENGTH_SHORT).show();
                List<Address> addressList = null;
                if (location != null || !location.equals("")) {
                    Geocoder geocoder = new Geocoder(CreateEventActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Address address = addressList.get(0);
                    LatLng latling = new LatLng(address.getLatitude(), address.getLongitude());
//                    TileOverlay offlineTileOverlay = mGoogleMap.addTileOverlay(new TileOverlayOptions()
//                            .tileProvider(new OfflineTileProvider()));
                    TileOverlay onlineTileOverlay =
                            mGoogleMap.addTileOverlay(new TileOverlayOptions()
                                    .tileProvider(new CustomMapTileProvider(getFilesDir().getAbsolutePath())));
                    mGoogleMap.addMarker(new MarkerOptions().position(latling).title(location));
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latling, 15));
                    setEventLocation(latling.latitude, latling.longitude);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void initializeListeners() {
        initializeSearchQuery();


        buttonDebugAutofill.setOnClickListener(v -> {
            editTextEventName.setText("A Debug Event Name");
            editTextEventDescription.setText("A debug description for an event. This text is kind of long but" +
                    " also not too long.");
            searchViewLocationSearch.setQuery("Ein Bokek", true);
            editTextEventDay.setText("16");
            editTextEventMonth.setText("10");
            editTextEventYear.setText("2020");
            editTextEventHour.setText("20");
            editTextEventMinute.setText("30");
            editTextInvitedUser1.setText("debug1@gmail.com");
            editTextInvitedUser2.setText("debug2@gmail.com");
            editTextInvitedUser3.setText("debug3@gmail.com");
        });

        initializeCreateEventListener();
//        buttonCreateEvent.setOnClickListener(v -> {
//
//
//            String sEventName = editTextEventName.getText().toString();
//            String sEventDescription = editTextEventDescription.getText().toString();
//            String sEventLocationName = editTextLocationName.getText().toString();
//
//            double sLongitude = Double.parseDouble(editTextLongitude.getText().toString());
//            double sLatitude = Double.parseDouble(editTextLatitude.getText().toString());
//            GeoPoint eventLocation = new GeoPoint(sLatitude, sLongitude);
//
//
//            Timestamp eventStartTime = getEventStartTime();
//
//            String currentUserID = firebaseAuth.getCurrentUser().getUid();
//
//            List<String> sInvitedUserIDs = Arrays.asList(currentUserID);
//            Timestamp stamp = Timestamp.now();
//            Timestamp timeCreated = Timestamp.now();
//
//            if (isEventFieldsValid(sEventName, sEventLocationName, sLongitude, sLatitude)) {
//                progressBar.setVisibility(View.VISIBLE);
//
//                Map<String, Object> newEventMap = new HashMap<>();
//                newEventMap.put(getString(R.string.ff_event_name), sEventName);
//                newEventMap.put(getString(R.string.ff_event_description), sEventDescription);
//                newEventMap.put(getString(R.string.ff_event_creator_uid), currentUserID);
//                newEventMap.put(getString(R.string.ff_event_time_created), timeCreated);
//                newEventMap.put(getString(R.string.ff_event_location_name), sEventLocationName);
//                newEventMap.put(getString(R.string.ff_event_location), eventLocation);
//                newEventMap.put(getString(R.string.ff_event_start_time), eventStartTime);
//                newEventMap.put(getString(R.string.ff_event_invited_users), sInvitedUserIDs);
//
//                firebaseFirestore.collection(getString(R.string.ff_events_collection)).add(newEventMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
//                    @Override
//                    public void onComplete(@NonNull Task<DocumentReference> task) {
//                        progressBar.setVisibility(View.INVISIBLE);
//                    }
//                })
//                        .addOnSuccessListener(documentReference -> {
//                            Toast.makeText(CreateEventActivity.this, "Event created " +
//                                    "successfully", Toast.LENGTH_LONG).show();
//                            gotoEvents();
//
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//
//                                Toast.makeText(CreateEventActivity.this, "Error while trying " +
//                                                "to create event",
//                                        Toast.LENGTH_LONG).show();
//                            }
//                        });
//
//            } else {
//                Toast.makeText(CreateEventActivity.this, "Some fields are missing",
//                        Toast.LENGTH_LONG).show();
//
//            }
//
//        });
    }

    private void initializeCreateEventListener() {
        buttonCreateEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createEvent();
            }
        });
    }

//    private String getEventName() throws Exception {
//        String sEditTextEventName = editTextEventName.getText().toString();
//        if (TextUtils.isEmpty(sEditTextEventName)) {
//            throw new Exception("event cannot be empty");
//        } else {
//            return sEditTextEventName;
//        }
//
//    }

//    private String getEventLocationName() {
//
//    }

    private void gotoEvents() {
        Intent eventsIntent = new Intent(CreateEventActivity.this, MainActivity.class);
        startActivity(eventsIntent);
        // don't allow going back to creating event
        finish();

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        initializeFields();
        initializeListeners();
    }


//    private boolean isEventFieldsValid(String eventName, String locationName, double longitude,
//                                       double latitude) {
//        boolean isEventNameValid = !TextUtils.isEmpty(eventName);
//        boolean isLocationNameValid = !TextUtils.isEmpty(locationName);
//        boolean isLongitudeValid = true;
//        boolean isLatitudeValid = true;
//
//        return isEventNameValid && isLocationNameValid && isLatitudeValid && isLongitudeValid;
//    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Called onmapready", Toast.LENGTH_SHORT).show();

        Log.d("OnMapReady", "called onMapReady");
        mGoogleMap = googleMap;
//        mGoogleMap =(GoogleMap) "dis.jfif";
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(120000); // two minute interval
        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    private void createEvent() {
        boolean didSetFields = retrieveAndSetEventFields();
        if (didSetFields) {
            progressBar.setVisibility(View.VISIBLE);
            EventModel createdEvent = new EventModel();
            createdEvent.setEventName(ffEventName);
            createdEvent.setEventDescription(ffEventDescription);
            createdEvent.setEventLocationName(ffEventLocationName);
            createdEvent.setEventLocation(ffEventLocation);
            createdEvent.setEventTimeCreated(ffEventTimeCreated);
            createdEvent.setEventTimeStarting(ffEventTimeStarting);
            createdEvent.setEventCreatorUID(ffEventCreatorUID);
            createdEvent.setEventCreatorName(ffEventCreatorName);

            firebaseFirestore.collection(getString(R.string.ff_events_collection)).add(createdEvent).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    // add invited users
                    CollectionReference invitedUsersRef =
                            documentReference.collection(getString(R.string.ff_events_eventInvitedUsers));
                    for (InvitedUserModel invitedUserModel : ffInvitedUserModels) {
                        invitedUsersRef.add(invitedUserModel).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(CreateEventActivity.this, String.format("Error " +
                                                "adding invited user %s: %s",
                                        invitedUserModel.getEventInvitedUserEmail(), e.getMessage()),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    Toast.makeText(CreateEventActivity.this, "Event created successfully",
                            Toast.LENGTH_SHORT).show();
                    gotoEvents();

                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(CreateEventActivity.this,
                            "Error while creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            progressBar.setVisibility(View.INVISIBLE);
        }
    }


    private boolean retrieveAndSetEventFields() {
        String toastErrorMessage = "";


        if (!setEventName(editTextEventName.getText().toString())) {
            toastErrorMessage = "Fix event name";
        } else if (!setEventDescription(editTextEventDescription.getText().toString())) {
            toastErrorMessage = "Fix event description";
        } else if (!setEventLocationName(searchViewLocationSearch.getQuery().toString())) {
            toastErrorMessage = "Fix event location name";
            // ffEventLocation should be full since it is retrieved from the search query
        } else if (this.ffEventLocation == null) {
            toastErrorMessage = "Fix event location";
        } else if (!setEventCreatorUID()) {
            toastErrorMessage = "Error getting creator UID";
            // TODO: Creator name is null
        } else if (!setEventCreatorName()) {
            toastErrorMessage = "Error getting creator name";
        } else if (!setEventTimeCreated()) {
            toastErrorMessage = "Error setting creation time";
            // TODO: Time starting not working (giving weird times)
        } else if (!setEventTimeStarting(
                Integer.parseInt(editTextEventDay.getText().toString()),
                Integer.parseInt(editTextEventMonth.getText().toString()),
                Integer.parseInt(editTextEventYear.getText().toString()),
                Integer.parseInt(editTextEventHour.getText().toString()),
                Integer.parseInt(editTextEventMinute.getText().toString()))) {
            toastErrorMessage = "Fix time starting";
        } else if (!setInvitedUsers(new ArrayList<>(Arrays.asList(
                editTextInvitedUser1.getText().toString(),
                editTextInvitedUser2.getText().toString(),
                editTextInvitedUser3.getText().toString())))) {

            toastErrorMessage = "Error inviting guests";
        }
        if (!TextUtils.isEmpty(toastErrorMessage)) {
            Toast.makeText(this, toastErrorMessage, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }


    /*
 Return true iff location is valid
  */
    private boolean setEventLocation(double latitude, double longitude) {
        boolean validLatitude = (latitude >= -90) && (latitude <= 90);
        boolean validLongitude = (longitude >= -180) && (longitude <= 180);
        if (validLatitude && validLongitude) {
            this.ffEventLocation = new GeoPoint(latitude, longitude);
            return true;
        } else {
            return false;
        }
    }

    private boolean setEventName(String eventName) {
        boolean nameNotEmpty = !TextUtils.isEmpty(eventName);
        if (nameNotEmpty) {
            this.ffEventName = eventName;
            return true;
        } else {
            return false;
        }
    }

    private boolean setEventDescription(String eventDescription) {
        boolean descriptionNotEmpty = !TextUtils.isEmpty(eventDescription);
        if (descriptionNotEmpty) {
            this.ffEventDescription = eventDescription;
            return true;
        } else {
            return false;
        }
    }

    private boolean setEventLocationName(String eventLocationName) {
        boolean nameNotEmpty = !TextUtils.isEmpty(eventLocationName);
        if (nameNotEmpty) {
            this.ffEventLocationName = eventLocationName;
            return true;
        } else {
            return false;
        }
    }

    private boolean setEventCreatorUID() {
        this.ffEventCreatorUID = this.firebaseUser.getUid();
        return true;
    }

    // TODO: creator name not working
    private boolean setEventCreatorName() {
        String creatorUID = this.firebaseUser.getUid();
        CollectionReference usersCollectionRef =
                firebaseFirestore.collection(getString(R.string.ff_users_collection));
        Query creatorUserQuery =
                usersCollectionRef.whereEqualTo(getString(R.string.ff_users_userID), creatorUID);
        this.progressBar.setVisibility(View.VISIBLE);
        creatorUserQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (BuildConfig.DEBUG && task.getResult().size() != 1) {
                        throw new AssertionError("Either no or more than one user with given UID " +
                                "exists.");
                    }
                    DocumentSnapshot creatorDocument = task.getResult().getDocuments().get(0);
                    ffEventCreatorName = creatorDocument.getString(getString(R.string.ff_users_userName));
                } else {
                    Toast.makeText(CreateEventActivity.this, String.format("Error getting creator" +
                                    " user name: %s", task.getException().getMessage()),
                            Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.INVISIBLE);

            }
        });


        return true;
    }

    private boolean setEventTimeCreated() {
        this.ffEventTimeCreated = Timestamp.now().toDate();
        return true;
    }

    private boolean setEventTimeStarting(int day, int month, int year, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        Date startDate = new Date(year, month, day, hour, minute);
        cal.setLenient(false);
        cal.setTime(startDate);
        try {
            cal.getTime();
            this.ffEventTimeStarting = startDate;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean setInvitedUsers(ArrayList<String> userEmails) {

        CollectionReference usersRef = firebaseFirestore.collection(getString(R.string.ff_users_collection));
        // add self to invitees
        userEmails.add(firebaseUser.getEmail());
        for (String userEmail : userEmails) {
            Query userByEmailQuery = usersRef.whereEqualTo(getString(R.string.ff_users_userEmail)
                    , userEmail);

            progressBar.setVisibility(View.VISIBLE);
            userByEmailQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        if (task.getResult().size() < 1) {
                            Toast.makeText(CreateEventActivity.this, String.format("Error: No " +
                                            "user with email %s", userEmail),
                                    Toast.LENGTH_SHORT).show();
                        }
                        DocumentSnapshot creatorDocument = task.getResult().getDocuments().get(0);
                        ffInvitedUserModels = new ArrayList<>();
                        String invitedUserID = creatorDocument.getId();
                        String invitedUserEmail = userEmail;
                        String invitedUserName = creatorDocument.getString(getString(R.string.ff_users_userName));
                        ffInvitedUserModels.add(new InvitedUserModel(invitedUserID, invitedUserEmail, invitedUserName));

                    } else {
                        Toast.makeText(CreateEventActivity.this, String.format("Error getting " +
                                        "user: %s", task.getException().getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.INVISIBLE);

                }
            });
        }
        return true;
    }


}

