package com.example.tripbuddy;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;



import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import com.google.maps.android.PolyUtil;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;


import androidx.core.content.ContextCompat;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;


import com.google.maps.NearbySearchRequest;
import com.google.maps.PlacesApi;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private LatLng currentLocationLatLng;

    private String stopType;

    private Integer interval;

    private List<LatLng> places;

    private List<Marker> markers = new ArrayList<>();
    private List<Polyline> polylines = new ArrayList<>();
    private List<Circle> circles = new ArrayList<>();
    private String apikey;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apikey = BuildConfig.GOOGLE_MAP_API_KEY;
        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable the "My Location" layer if the permission is granted.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            // Move camera to the user's current location
            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocationLatLng, 15f));
                }
            });
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null; // Use the default window
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Custom InfoWindow layout
                View view = getLayoutInflater().inflate(R.layout.custom_info_window, null);

                TextView titleTextView = view.findViewById(R.id.titleTextView);
                TextView snippetTextView = view.findViewById(R.id.snippetTextView);

                // Set the title and snippet
                titleTextView.setText(marker.getTitle());
                snippetTextView.setText(marker.getSnippet());

                return view;
            }
        });

        Button buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the destination from the EditText
                EditText editTextDestination =findViewById(R.id.editTextDestination);
                Spinner locationSpinner = findViewById(R.id.StopTypeSpinner);
                Spinner intervalSpinner = findViewById(R.id.IntervalSpinner);


                String selectedEnterval = intervalSpinner.getSelectedItem().toString();

                stopType = locationSpinner.getSelectedItem().toString();
                interval = Integer.parseInt(selectedEnterval.substring(0,1));


                String destination = editTextDestination.getText().toString();

                // Check if the destination is not empty and the current location is available
                if (!destination.isEmpty() && currentLocationLatLng != null) {
                    // Fetch directions from the current location to the destination
                    clearMap();
                    fetchDirections(currentLocationLatLng, destination);
//                    drawStraightLine(currentLocationLatLng, destination);

                } else {
                    Toast.makeText(MainActivity.this, "Invalid destination or current location.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void clearMap() {
        // Clear markers
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();

        // Clear polylines
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();

        // Clear circles
        for (Circle circle : circles) {
            circle.remove();
        }
        circles.clear();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, update map
                onMapReady(mMap);
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private long requestTime;
    private GeoApiContext context;
    private void fetchDirections(LatLng startLocation, String endLocation) {


        context = new GeoApiContext.Builder().apiKey(apikey).build();

        // Convert LatLng to string representations
        String startLocationStr = startLocation.latitude + "," + startLocation.longitude;
        String endLocationStr = endLocation;
        requestTime = System.currentTimeMillis() ; // Set departure time 2 hours later

        // Create an Instant object for departure time
        Instant instantDepartureTime = Instant.ofEpochMilli(requestTime);

        DirectionsApiRequest request = DirectionsApi.getDirections(context, startLocationStr, endLocationStr).departureTime(instantDepartureTime);
        try {
            DirectionsResult result = request.await();
            handleDirectionsResult(result);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle error
        }
    }

    private DirectionsResult directionsResult;
    private void handleDirectionsResult(DirectionsResult result) {
        // Process the directions result, e.g., draw the route on the map
        if (result != null && result.routes != null && result.routes.length > 0) {
            DirectionsRoute route = result.routes[0]; // Get the first route
            System.out.println(route);

            List<LatLng> stops = new ArrayList<>();
            stops.add(currentLocationLatLng);

            double proportionElapsed = 0;
            long totalDurationInMillis = route.legs[0].duration.inSeconds * 1000; // Total duration in milliseconds

            long estimatedArrivalTime = requestTime ;

            while(proportionElapsed<1){

                // Calculate the estimated location after 2 hours

                estimatedArrivalTime += interval * 60 * 60 * 1000;
                // Calculate the proportion of the total duration corresponding to the elapsed time
                proportionElapsed = (double) (estimatedArrivalTime - requestTime) / totalDurationInMillis;

                // Interpolate along the route based on the proportion
                LatLng estimatedLocation = interpolateAlongRoute(route, proportionElapsed);

                stops.add(estimatedLocation);
//                drawRouteToStops(currentLocationLatLng, estimatedLocation);
//                addDotAtLatLng(estimatedLocation);

                searchNearByPlaces(estimatedLocation,stopType);

                // Log the estimated location (you can use it as needed)
                System.out.println("Estimated location after 2 hours: " + estimatedLocation);
            }



//            drawStopLines(stops);






            List<LatLng> points = decodePolyline(route.overviewPolyline.getEncodedPath());
            drawRouteOnMap(points);
            moveCameraToShowEntireRoute(points);
        }


    }

    //search for nearby places;
    private void searchNearByPlaces(LatLng origin, String placeType) {

        try {
            NearbySearchRequest nearbySearchRequest = PlacesApi.nearbySearchQuery(context, new com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                    .radius(15000)  // Specify the radius in meters
                    .type(PlaceType.valueOf(placeType));  // Specify the place type

            PlacesSearchResult[] results = nearbySearchRequest.await().results;

            if (results != null && results.length > 0) {
                // Assuming you want to add a dot for the first nearby place found
                com.google.maps.model.LatLng placeLatLng = results[0].geometry.location;

                System.out.println("Stop Name: " + results[0].name+ "," + "Address:" + results[0].vicinity);
                // Convert the placeLatLng to LatLng from com.google.android.gms.maps.model
                LatLng convertedPlaceLatLng = new LatLng(placeLatLng.lat, placeLatLng.lng);

                // Add a dot at the specified LatLng
                addDotAtRestStop(convertedPlaceLatLng,results[0].name,results[0].vicinity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private LatLng interpolateAlongRoute(DirectionsRoute route, double proportionElapsed) {
        List<LatLng> points = decodePolyline(route.overviewPolyline.getEncodedPath());
        double totalDistance = route.legs[0].distance.inMeters;

        double accumulatedDistance = 0;
        LatLng previousPoint = null;

        for (LatLng point : points) {
            if (previousPoint != null) {
                double segmentDistance = distanceBetween(previousPoint, point);

                if (accumulatedDistance + segmentDistance >= proportionElapsed * totalDistance) {
                    // Interpolate within this segment
                    double remainingDistance = proportionElapsed * totalDistance - accumulatedDistance;
                    double fraction = remainingDistance / segmentDistance;

                    double interpolatedLat = previousPoint.latitude + fraction * (point.latitude - previousPoint.latitude);
                    double interpolatedLng = previousPoint.longitude + fraction * (point.longitude - previousPoint.longitude);

                    return new LatLng(interpolatedLat, interpolatedLng);
                }

                accumulatedDistance += segmentDistance;
            }

            previousPoint = point;
        }

        // Return the last point if something goes wrong
        return points.get(points.size() - 1);
    }

    private double distanceBetween(LatLng point1, LatLng point2) {
        // Haversine formula to calculate distance between two points on the Earth
        double R = 6371000; // Earth radius in meters
        double lat1 = Math.toRadians(point1.latitude);
        double lon1 = Math.toRadians(point1.longitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lon2 = Math.toRadians(point2.longitude);

        double dlat = lat2 - lat1;
        double dlon = lon2 - lon1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private List<LatLng> decodePolyline(String encodedPolyline) {
        List<LatLng> polyPoints = PolyUtil.decode(encodedPolyline);
        return polyPoints;
    }

    // Draw the route on the map
    private void drawRouteOnMap(List<LatLng> points) {
        if (mMap != null) {

            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(points)
                    .color(Color.BLUE)
                    .width(5);
            Polyline polyline = mMap.addPolyline(polylineOptions);
            polylines.add(polyline);

        }
    }






    private void addDotAtRestStop(LatLng latLng  ,String placeName, String vicinity) {
        // Add a dot (circle) at the specified LatLng
        CircleOptions circleOptions = new CircleOptions()
                .center(latLng)
                .radius(200)  // Set the radius of the dot (adjust as needed)
                .strokeWidth(0)  // Set the stroke width to 0 for a filled circle
                .fillColor(Color.BLUE);  // Set the fill color of the circle

        Circle circle = mMap.addCircle(circleOptions);
        circles.add(circle);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(placeName)
                .snippet(vicinity);

        Marker marker = mMap.addMarker(markerOptions);
        markers.add(marker);
        marker.showInfoWindow(); // Show the info window immediately


    }

    // Move and animate the camera to show the entire route with a smooth animation
    private void moveCameraToShowEntireRoute(List<LatLng> points) {
        if (mMap != null && points != null && points.size() > 1) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // Include all points in the bounds
            for (LatLng point : points) {
                builder.include(point);
            }

            LatLngBounds bounds = builder.build();

            // Padding is optional, adjust as needed
            int padding = 100; // in pixels

            // Create a CameraUpdate with smooth animation
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

            // Use animateCamera for smooth animation
            mMap.animateCamera(cameraUpdate);
        }
    }





}
