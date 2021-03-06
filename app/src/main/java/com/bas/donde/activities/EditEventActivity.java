package com.bas.donde.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.bas.donde.R;
import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bas.donde.R;
import com.bas.donde.models.EventModel;
import com.bas.donde.models.InvitedInEventUserModel;
import com.bas.donde.models.InvitedInUserEventModel;
import com.bas.donde.models.UserModel;
import com.bas.donde.utils.map_utils.CustomMapTileProvider;
import com.bas.donde.utils.map_utils.OfflineTileProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditEventActivity extends AppCompatActivity implements OnMapReadyCallback{

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private final String TAG = "tagCreateEventActivity";
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    LocationCallback mLocationCallback;
    // all the data
    private EventModel eventModel;
    // Views
    private EditText editTextEventName;
    private EditText editTextEventDescription;
    private Button buttonEditEvent;
    private ProgressBar progressBar;
    private SearchView searchViewLocationSearch;
    private ListView listViewInvitedUsers;
    private AutoCompleteTextView autoCompleteInvitedUsers;

    private TextView textViewEventTime;
    private TextView textViewEventDate;
    private TimePicker timePicker;
    private DatePicker datePicker;

    // Utils
    private ArrayAdapter<String> autoCompleteInvitedUsersAdapter;
    private ArrayList<String> autoCompleteInvitedUsersList;
    private ArrayAdapter<String> listViewInvitedUsersAdapter;
    private ArrayList<String> listViewInvitedUsersList;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DocumentReference currUserDocumentRef;
    private FirebaseFirestore firebaseFirestore;
    private CollectionReference usersCollectionRef;
    private CollectionReference eventsCollectionRef;

    // Fields for storing data to push to FirebaseFirestore (ff)
    private String ffEventName; //yes
    private String ffEventDescription; //yes
    private String ffEventLocationName; //yes
    private GeoPoint ffEventLocation; //yes
    private String ffEventCreatorUID; //dont need
    private String ffEventCreatorName; //dont need
    private Date ffEventTimeCreated; //
    private Date ffEventTimeStarting;
    private List<InvitedInEventUserModel> ffInvitedUserInEventModels;

    //bools
    private boolean didFinishSettingCreatorName;
    private boolean didFinishSettingUsers;

    void checkForPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.i("TAG", "Unexpected flow");
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_LOCATION);

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
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(EditEventActivity.this,
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
                    Log.d("EDIT EVENT ACTIVTY", "permission denied");
                }
            }

        }
    }
    private void initializeListViewInvitedUsers() {
        listViewInvitedUsers = findViewById(R.id.create_listView_invited_users);
        listViewInvitedUsersList = new ArrayList<>();

        listViewInvitedUsersAdapter = new ArrayAdapter<>(this,
                R.layout.listview_invited_users_single_item, listViewInvitedUsersList);
        listViewInvitedUsers.setAdapter(listViewInvitedUsersAdapter);
    }
    private void addInvitedUsersToAutocomplete() {
        ArrayList<String> invitedUsersAutocomplete;
        currUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                ArrayList<String> interactedUsersArray = (ArrayList<String>) documentSnapshot.get(getString(R.string.ff_Users_userInteractedUserEmails));
                if (!(interactedUsersArray == null)) {
                    autoCompleteInvitedUsersList.addAll(interactedUsersArray);
                    autoCompleteInvitedUsersAdapter.notifyDataSetChanged();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "adding interacted users to autocomplete failed");
            }
        });
    }
    private void addInvitedUserToList(String invitedUser) {
        autoCompleteInvitedUsers.setText("");
        listViewInvitedUsersList.add(invitedUser);
        listViewInvitedUsersAdapter.notifyDataSetChanged();
    }
    private void initializeAutocompleteInvitedUsers() {
        autoCompleteInvitedUsers = findViewById(R.id.create_autoComplete_invited_users);
        autoCompleteInvitedUsersList = new ArrayList<String>();
        addInvitedUsersToAutocomplete();
        autoCompleteInvitedUsersAdapter = new ArrayAdapter<String>(this,
                R.layout.autocomplete_invited_user_single_item, autoCompleteInvitedUsersList);
        autoCompleteInvitedUsers.setAdapter(autoCompleteInvitedUsersAdapter);
        autoCompleteInvitedUsers.setThreshold(1);
        autoCompleteInvitedUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String invitedUser = parent.getItemAtPosition(position).toString();
                addInvitedUserToList(invitedUser);
            }
        });
        autoCompleteInvitedUsers.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String invitedUser = v.getText().toString();
                    addInvitedUserToList(invitedUser);
                }
                return false;
            }
        });

    }
    private void initializeFields() {

        // Views
        editTextEventName = findViewById(R.id.create_editText_event_name);
        editTextEventName.setText(eventModel.getEventName());
        Log.d( "editTextEventName", editTextEventName.getText().toString());

        editTextEventDescription = findViewById(R.id.create_editText_event_description);
        editTextEventDescription.setText(eventModel.getEventDescription());

        buttonEditEvent = findViewById(R.id.Update);
        progressBar = findViewById(R.id.create_progressBar);

        searchViewLocationSearch = findViewById(R.id.create_searchView_location_search);
        searchViewLocationSearch.setQuery(eventModel.getEventLocationName(), true);

        timePicker = findViewById(R.id.create_timePicker);
        datePicker = findViewById(R.id.create_datePicker);
        timePicker.setIs24HourView(true);

        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();
        usersCollectionRef = firebaseFirestore.collection(getString(R.string.ff_Users));
        eventsCollectionRef = firebaseFirestore.collection(getString(R.string.ff_Events));
        currUserDocumentRef = usersCollectionRef.document(firebaseUser.getUid());

        initializeTimeAndDate();
        initializeListViewInvitedUsers();
        initializeAutocompleteInvitedUsers();

        // Location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.create_mapView);
        mapFrag.getMapAsync(this);
        // Location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.create_mapView);
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
                }
            }
        };
        setEventCreatorUID();
        setEventCreatorName();

    }

    private void initializeTimeAndDate() {
        Context createContext = this;
        initializeTimePicker(createContext);
        initializeDatePicker(createContext);
    }

    private void updateCurrentDate(){
        textViewEventDate = findViewById(R.id.create_textView_event_date);
        Date currDate = Calendar.getInstance().getTime();
        String day          = (String) DateFormat.format("dd",   currDate); // 20
        String monthNumber  = (String) DateFormat.format("MM",   currDate); // 06
        String year         = (String) DateFormat.format("yyyy", currDate); // 2013
        textViewEventDate.setText(day + "/" + monthNumber + "/" + year);
    }
    private void initializeDatePicker(Context createContext) {
        updateCurrentDate();
        textViewEventDate.setOnClickListener(new View.OnClickListener() {
            //TODO: Hide soft keyboard when choosing items in craeteevent
            @Override
            public void onClick(View v) {
                String date= textViewEventDate.getText().toString();
                int day = -1;
                int month =-1;
                int year = -1;
                int idx = 0;
                for(int i =0; i <date.length(); i++){
                    if(date.charAt(i) == '/'){
                        if(day ==-1){
                            day = Integer.parseInt(date.substring(0, i));
                            idx = i;
                        }else if(month == -1){
                            month = Integer.parseInt(date.substring(idx + 1, i));
                            year = Integer.parseInt(date.substring(i +1));
                        }
                    }
                }
                textViewEventDate.setText(day + "/" + month + "/" + year);
                DatePickerDialog mDatePicker;
//                int resTheme = R.style.SpinnerTimePicker;
                int resTheme = DatePickerDialog.THEME_HOLO_DARK;
                mDatePicker = new DatePickerDialog(createContext, resTheme,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDayOfMonth) {
//                                int correctMonth = selectedMonth + 1;
                                textViewEventDate.setText(selectedDayOfMonth + "/" + selectedMonth + "/" + selectedYear);
                            }
                        },  year, month, day);//Yes 24 hour time
                mDatePicker.setTitle("Select Date");
                mDatePicker.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                mDatePicker.show();


            }

        });
    }
    private void updateTime(){
        textViewEventTime = findViewById(R.id.create_textView_event_time);
        Calendar cal  = Calendar.getInstance();
        String time = cal.getTime().toString();
        String hour = "";
        String minute = "";
        for(int i = 0; i < time.length(); i++){
            if(time.charAt(i)== ':'){
                hour = time.substring(i-2,i);
                minute = time.substring(i+1, i + 3);
                break;
            }
        }
        textViewEventTime.setText(hour +":"+ minute);
    }
    private void initializeTimePicker(Context createContext) {
        updateTime();
        textViewEventTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = -1;
                int minute = -1;
                String time = textViewEventTime.getText().toString();
                for(int i = 0; i < time.length(); i++){
                    if(time.charAt(i)== ':'){
                        hour = Integer.parseInt(time.substring(i-2,i));
                        minute = Integer.parseInt(time.substring(i+1, i + 3));
                        break;
                    }
                }
                TimePickerDialog mTimePicker;
                int resTheme = TimePickerDialog.THEME_HOLO_DARK;
                mTimePicker = new TimePickerDialog(createContext, resTheme,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                                String viewMinute = String.format("%s%s", selectedMinute < 10 ? "0" : "",
                                        selectedMinute);

                                textViewEventTime.setText(selectedHour + ":" + viewMinute);
                            }
                        }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                mTimePicker.show();

            }

        });
    }
    private void initializeSearchQuery() {
        searchViewLocationSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(searchViewLocationSearch.getQuery()=="") {
                    searchViewLocationSearch.onActionViewExpanded();
                }else{
                    searchQuerry(searchViewLocationSearch.getQuery().toString());
                }
            }
        });
        searchViewLocationSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String location = searchViewLocationSearch.getQuery().toString();
                searchQuerry(location);
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
        buttonEditEvent.findViewById(R.id.Update);
        initializeCreateEventListener();
    }
    private void initializeCreateEventListener() {
        buttonEditEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean didSetFields = retrieveAndSetEventFields();
                if (didSetFields) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            {
                                update();
                                Intent eventsIntent = new Intent(EditEventActivity.this, MainActivity.class);
                                startActivity(eventsIntent);
                            }
                        }
                    }, 2000);
                } else {
                }// 2000 milliseconds = 2seconds

            }
        });
    }
    private void update(){
        CollectionReference invitedInEventUsersRef = eventsCollectionRef.document(eventModel.getEventID()).collection("InvitedInEventUsers");
        for (InvitedInEventUserModel invitedInEventUserModel : ffInvitedUserInEventModels) {
            Log.d(TAG, "Entering addInvitedInEventUser"); //yep
            addInvitedInEventUser(invitedInEventUsersRef, invitedInEventUserModel);
            // add event to invited users
            String invitedInEventUserId = invitedInEventUserModel.getInvitedInEventUserID();
            CollectionReference invitedInUserEventsRef = usersCollectionRef.document(invitedInEventUserId).collection(getString(R.string.ff_InvitedInUserEvents));
            Log.d(TAG, "Entering addInvitedInUserEvent");
            addInteractedEmailsToUser(usersCollectionRef.document(invitedInEventUserId)); //works
            addInvitedInUserEvent(eventModel.getEventID(), eventModel, invitedInUserEventsRef); //works
        }
    }

    private void gotoEvents() {
        Intent eventsIntent = new Intent(EditEventActivity.this, MainActivity.class);
        startActivity(eventsIntent);
        // don't allow going back to creating event
        finish();

    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isOnline()) {
            // get event object from intent
            Gson gson = new Gson();
            eventModel = gson.fromJson(getIntent().getStringExtra(getString(R.string.arg_event_model)), EventModel.class);
            //set all ff:
            ffEventName = eventModel.getEventName();
            ffEventDescription = eventModel.getEventDescription();
            ffEventLocationName = eventModel.getEventLocationName();
            ffEventLocation = eventModel.getEventLocation();
            ffEventCreatorUID = eventModel.getEventCreatorUID();
            ffEventCreatorName = eventModel.getEventCreatorName();
            ffEventTimeCreated = eventModel.getEventTimeCreated();
            ffEventTimeStarting = eventModel.getEventTimeStarting();
            setContentView(R.layout.activity_edit_event);
            initializeFields();
            initializeListeners();
        } else {
            Log.d("editActivity", "Go online in order to create an event");
            gotoEvents();
        }
    }

    // ICMP
    public boolean isOnline() {
        return true;
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
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

        LatLng latLng = new LatLng(eventModel.getEventLocation().getLatitude(), eventModel.getEventLocation().getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
    }

    private void addInvitedInEventUser(CollectionReference invitedInEventUsersRef,
                                       InvitedInEventUserModel invitedInEventUserModel) {
        invitedInEventUsersRef.document(invitedInEventUserModel.getInvitedInEventUserID()).set(invitedInEventUserModel).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, String.format("Added user %s to event",
                        invitedInEventUserModel.getInvitedInEventUserName()));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, String.format("Failed to add user %s to event, error: %s",
                        invitedInEventUserModel.getInvitedInEventUserName(), e.getMessage()));

            }
        });

    }
    private void addInvitedInUserEvent(String newEventId, EventModel newEventModel,
                                       CollectionReference invitedInUserEventsRef) {
        InvitedInUserEventModel newInvitedInUserEventModel = new InvitedInUserEventModel(newEventId, newEventModel.getEventName(),
                newEventModel.getEventLocationName(), newEventModel.getEventCreatorName()
                , newEventModel.getEventTimeStarting());
        invitedInUserEventsRef.document(newEventId).set(newInvitedInUserEventModel).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, String.format("Added event %s to user", newEventModel.getEventName()));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, String.format("Failed to add event %s to user, error: %s",
                        newEventModel.getEventName(), e.getMessage()));

            }
        });
    }
    private void addInteractedEmailsToUser(DocumentReference userRef) {

        userRef.update(getString(R.string.ff_Users_userInteractedUserEmails),
                FieldValue.arrayUnion((Object[])
                        listViewInvitedUsersList.toArray(new
                                String[listViewInvitedUsersList.size()])));
    }

    private boolean retrieveAndSetEventFields() {
        String toastErrorMessage = "";
        if (!setEventName(editTextEventName.getText().toString())) {
            toastErrorMessage = "Fix event name";
        } else if (!setInvitedUsers(listViewInvitedUsersList)) {
            toastErrorMessage = "Error inviting guests";
        } else if (!setEventDescription(editTextEventDescription.getText().toString())) {
            toastErrorMessage = "Fix event description";
        } else if (!setEventLocationName(searchViewLocationSearch.getQuery().toString())) {
            toastErrorMessage = "Fix event location name";
            // ffEventLocation should be full since it is retrieved from the search query
        } else if (this.ffEventLocation == null) {
            toastErrorMessage = "Fix event location";
//        } else if (!setEventCreatorUID()) {
//            toastErrorMessage = "Error getting creator UID";
        } else if (!setEventTimeCreated()) {
            toastErrorMessage = "Error setting creation time";
        } else if (!setEventTimeStarting()) {
            toastErrorMessage = "Fix time starting";
        }
        if (!TextUtils.isEmpty(toastErrorMessage)) {
            Log.d("editActivty", toastErrorMessage);
            return false;
        } else {
            return true;
        }
    }

    void searchQuerry(String location){
        Log.d("EditEventActivity", "Search query is: " + location);
        List<Address> addressList = new ArrayList<>();
        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(EditEventActivity.this);
            LatLng latling = new LatLng(0, 0);

            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (addressList.size() == 0) {
                Log.d("EditEventActivity", "Search query failed");
            } else {

                Address address = addressList.get(0);
                latling = new LatLng(address.getLatitude(), address.getLongitude());
                Log.d("latLng ", latling.latitude + " " + latling.longitude);
            }
            // if we add these line it actually finds the location but not large enough
            //what does v do? crazy zoom when gets bigger
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latling, 17));
            setEventLocation(latling.latitude, latling.longitude);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
            TileOverlay onlineTileOverlay = mGoogleMap.addTileOverlay(new TileOverlayOptions()
                    .tileProvider(new OfflineTileProvider(getBaseContext())));

            // add marker
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latling);
            markerOptions.title("Event Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
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
            eventModel.setEventLocation(this.ffEventLocation);
            eventsCollectionRef.document(eventModel.getEventID()).update("eventLocation", this.ffEventLocation);
            return true;
        } else {
            return false;
        }
    }
    private boolean setEventName(String eventName) {
        boolean nameNotEmpty = !TextUtils.isEmpty(eventName);
        if (nameNotEmpty) {
            this.ffEventName = eventName;
            eventModel.setEventName(eventName);
            eventsCollectionRef.document(eventModel.getEventID()).update("eventName", this.ffEventName);
            return true;
        } else {
            return false;
        }
    }
    private boolean setEventDescription(String eventDescription) {
        boolean descriptionNotEmpty = !TextUtils.isEmpty(eventDescription);
        if (descriptionNotEmpty) {
            this.ffEventDescription = eventDescription;
            eventModel.setEventDescription(eventDescription);
            eventsCollectionRef.document(eventModel.getEventID()).update("eventDescription", this.ffEventDescription);
            return true;
        } else {
            return false;
        }
    }
    private boolean setEventLocationName(String eventLocationName) {
        boolean nameNotEmpty = !TextUtils.isEmpty(eventLocationName);
        if (nameNotEmpty) {
            this.ffEventLocationName = eventLocationName;
            eventModel.setEventLocationName(eventLocationName);
            eventsCollectionRef.document(eventModel.getEventID()).update("eventLocationName", this.ffEventLocationName);
            return true;
        } else {
            return false;
        }
    }
    private boolean setEventCreatorUID() {
        this.ffEventCreatorUID = this.firebaseUser.getUid();
        return true;
    }
    private boolean setEventCreatorName() {
        this.progressBar.setVisibility(View.VISIBLE);
        currUserDocumentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                ffEventCreatorName =
                        documentSnapshot.getString(getString(R.string.ff_Users_userName));
                Log.d("create", "event creator name is " + ffEventCreatorName);
                didFinishSettingCreatorName = true;
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Log.d("EditEventActivity", String.format("Error getting creator" + " user name: %s", e.getMessage()));
            }

        }).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                progressBar.setVisibility(View.INVISIBLE);

            }
        });
        return true;
    }
    private boolean setEventTimeCreated() {
        this.ffEventTimeCreated = Timestamp.now().toDate();
        eventsCollectionRef.document(eventModel.getEventID()).update("eventTimeCreated", this.ffEventTimeCreated);
        return true;
    }
    private boolean setEventTimeStarting() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy,HH:mm", Locale.ENGLISH);
        Date date;
        try {
            date = formatter.parse(String.format("%s,%s", textViewEventDate.getText(),
                    textViewEventTime.getText()));
            this.ffEventTimeStarting = date;
            eventModel.setEventTimeStarting(date);
            eventsCollectionRef.document(eventModel.getEventID()).update("eventTimeStarting", this.ffEventTimeStarting);

            return true;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

    }
    private boolean setInvitedUsers(ArrayList<String> userEmails) {
        ffInvitedUserInEventModels = new ArrayList<>();
        CollectionReference usersRef = firebaseFirestore.collection(getString(R.string.ff_Users));
        // add self to invitees
        userEmails.add(firebaseUser.getEmail());
        for (String userEmail : userEmails) {
            if (TextUtils.isEmpty(userEmail)) {
                continue;
            }
            Query userByEmailQuery = usersRef.whereEqualTo(getString(R.string.ff_Users_userEmail)
                    , userEmail);

            progressBar.setVisibility(View.VISIBLE);
            userByEmailQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        if (task.getResult().size() == 1) {
                            DocumentSnapshot invitedUserDoc = task.getResult().getDocuments().get(0);
                            UserModel userModel = invitedUserDoc.toObject(UserModel.class);
                            Log.d(TAG, String.format("adding user to ffInvited: %s",
                                    userModel.getUserName()));
                            ffInvitedUserInEventModels.add(new InvitedInEventUserModel(userModel.getUserID(),
                                    userModel.getUserName(), userModel.getUserEmail(), userModel.getUserStatus(), userModel.getUserLastLocation(), userModel.getUserProfilePicURL()));

                            eventsCollectionRef.document(eventModel.getEventID()).update("InvitedInEventUsers", ffInvitedUserInEventModels);

                        } else if (task.getResult().size() == 0) {
                            Log.d("EditEventActivity", String.format("No user found" + " with email %s", userEmail));
                        } else if (task.getResult().size() > 1) {
                            Log.d("EditEventActivity", String.format("Found more " + "than one user with email %s", userEmail));
                        }
                        progressBar.setVisibility(View.INVISIBLE);

                    } else {
                        Log.d("EditEventActivity", String.format("Error processing " + "email %s, error: %s", userEmail, task.getException().getMessage()));
                    }
                    didFinishSettingUsers = true;
                }
            });
        }
        return true;


    }

}
