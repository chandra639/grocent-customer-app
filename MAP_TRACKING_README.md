# Map Tracking Implementation Guide

## Overview

This document provides comprehensive instructions for configuring and testing the map tracking system in the Grocent delivery app. The implementation uses OSMDroid for map tiles, OSRM for route calculation and map-matching, with GraphHopper as a fallback.

## Features

- ✅ **OSMDroid Map Tiles**: OpenStreetMap-based street map display
- ✅ **OSRM Route Calculation**: Curved road-following routes with 30-200 waypoints
- ✅ **Map-Matching**: GPS coordinates snapped to nearest roads using OSRM /match API
- ✅ **GraphHopper Fallback**: Automatic fallback when OSRM is unavailable
- ✅ **Animated Marker**: Smooth bike icon animation with rotation based on movement direction
- ✅ **Auto-Follow Mode**: Camera follows delivery person while preserving user zoom level
- ✅ **Blue Route Polyline**: Clear route visualization on map

## Configuration

### 1. OSRM Endpoint

The app uses the public OSRM demo server by default:
- **URL**: `https://router.project-osrm.org`
- **No API key required** for basic usage
- **Rate limits**: Public server has rate limits; for production, consider self-hosting

To use a custom OSRM server, modify `RouteHelper.kt`:
```kotlin
val urlString = "https://your-osrm-server.com/route/v1/driving/" +
        "$startLng,$startLat;$endLng,$endLat" +
        "?overview=full&geometries=polyline"
```

### 2. GraphHopper API Key (Optional)

GraphHopper is used as a fallback when OSRM fails. To use GraphHopper:

1. **Get Free API Key**: Visit https://www.graphhopper.com/ and sign up
2. **Update RouteHelper.kt**: Replace `demo_key` with your API key:
```kotlin
val apiKey = "YOUR_GRAPHHOPPER_API_KEY" // In getRouteFromGraphHopper()
```

**Note**: The demo key works for testing but has strict rate limits. For production, use your own API key.

### 3. Bike Icon Setup

The app uses bike icons stored in Android resource folders. Place PNG files in the following locations:

#### Icon Naming Convention
- **File name**: `ic_bike.png` (or `ic_bike_topdown_{size}_bag_green.png` for variants)
- **Sizes**: 48px, 72px, 96px

#### Folder Structure
```
app/src/main/res/
├── drawable-mdpi/
│   └── ic_bike.png (48px)
├── drawable-hdpi/
│   └── ic_bike.png (72px)
└── drawable-xhdpi/
    └── ic_bike.png (96px)
```

#### Icon Variants (Optional)
If you have multiple icon variants, use the naming pattern:
- `ic_bike_topdown_48_base_green.png` - Base bike (48px, green)
- `ic_bike_topdown_72_bag_green.png` - Bike with bag (72px, green)
- `ic_bike_topdown_96_helmet_yellow.png` - Bike with helmet (96px, yellow)

The app currently uses `R.drawable.ic_bike` - ensure this resource exists.

## Code Examples

### 1. Network Call (Route Fetching)

The route fetching is handled in `RouteHelper.kt`:

```kotlin
suspend fun getRoute(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double
): List<GeoPoint>? {
    // Tries OSRM first, falls back to GraphHopper
    val osrmRoute = getRouteFromOSRM(...)
    if (osrmRoute != null) return osrmRoute
    
    return getRouteFromGraphHopper(...)
}
```

### 2. JSON Parsing (Polyline5 Decoding)

Polyline5 decoding is implemented in `RouteHelper.kt` and `MapMatchingHelper.kt`:

```kotlin
private fun decodePolyline5(encoded: String): List<Pair<Double, Double>> {
    // Decodes OSRM/GraphHopper polyline5 string to coordinates
    // Returns list of (lat, lng) pairs
}
```

### 3. Polyline Drawing (OSMDroid)

Route drawing in `OrderTrackingScreen.kt`:

```kotlin
val routeLine = Polyline()
routeLine.setPoints(currentRouteWaypoints) // List<GeoPoint>
routeLine.color = Color(0xFF2196F3).hashCode() // Blue color
routeLine.width = 12f // Thickness
mapView.overlays.add(routeLine)
mapView.invalidate()
```

### 4. Marker Creation and Animation

Marker animation using `AnimatedMarker` class:

```kotlin
val bikeIcon = ContextCompat.getDrawable(context, R.drawable.ic_bike)
val animatedMarker = AnimatedMarker(
    mapView = mapView,
    initialPosition = GeoPoint(lat, lng),
    title = "Delivery Person",
    icon = bikeIcon
)

// Animate to new position with rotation
animatedMarker.animateTo(
    newPosition = GeoPoint(newLat, newLng),
    duration = 500, // milliseconds
    rotate = true // Rotate based on movement direction
)
```

### 5. Map-Matching Example

GPS location map-matching in `MapMatchingHelper.kt`:

```kotlin
val mapMatchingHelper = MapMatchingHelper()
val matchedPoint = mapMatchingHelper.matchGPSLocation(
    latitude = gpsLatitude,
    longitude = gpsLongitude
)
// Returns GeoPoint snapped to nearest road
```

### 6. Auto-Follow Camera

Camera auto-follow logic in `OrderTrackingScreen.kt`:

```kotlin
var autoFollowEnabled by remember { mutableStateOf(true) }

if (autoFollowEnabled) {
    val currentZoom = mapView.zoomLevelDouble
    mapView.controller.animateTo(markerPosition)
    mapView.controller.setZoom(currentZoom) // Preserve zoom
}
```

## Testing Instructions

### Prerequisites

1. **Android Device/Emulator**: With location services enabled
2. **Internet Connection**: Required for OSRM/GraphHopper API calls
3. **Location Permissions**: Grant location permissions to the app

### Test Steps

#### 1. Basic Route Display Test

1. Open the app and navigate to order tracking screen
2. Ensure order has valid `storeLocation` and `customerLocation`
3. **Expected**: Blue curved route line appears on map connecting store to customer
4. **Verify**: Route has 30+ waypoints (not just 2 points)

#### 2. Map-Matching Test

1. Enable GPS location in device settings
2. Grant location permissions to app
3. Start order tracking with real GPS enabled
4. **Expected**: Delivery person marker snaps to nearest road
5. **Verify**: Check logs for "GPS matched" messages

#### 3. Auto-Follow Mode Test

1. Start order tracking
2. **Test 1**: With `autoFollowEnabled = true`
   - **Expected**: Camera follows delivery person marker
   - **Verify**: Zoom level is preserved when camera moves
3. **Test 2**: Set `autoFollowEnabled = false`
   - **Expected**: User can zoom/pan freely without camera reset
   - **Verify**: Camera doesn't jump back when marker moves

#### 4. Marker Animation Test

1. Start order tracking
2. **Expected**: Bike icon smoothly animates along route
3. **Verify**: 
   - Icon rotates based on movement direction
   - Animation is smooth (no jumping)
   - Icon size is appropriate (48dp)

#### 5. GraphHopper Fallback Test

1. Temporarily disable internet or block OSRM domain
2. Start order tracking
3. **Expected**: App falls back to GraphHopper
4. **Verify**: Check logs for "Route fetched from GraphHopper" message

#### 6. Route Color Test

1. Display route on map
2. **Expected**: Route line is blue (#2196F3)
3. **Verify**: Route is clearly visible against map background

### Expected Screenshots

When testing, you should see:

1. **Map View**: 
   - OpenStreetMap street tiles (OSMDroid MAPNIK)
   - Blue curved route line from store to customer
   - Customer marker (blue dot) at destination
   - Store marker (if within 5km)
   - Bike icon marker at delivery person location

2. **Route Details**:
   - Route follows actual roads (not straight line)
   - Route has smooth curves showing turns
   - Route color is blue and clearly visible

3. **Marker Animation**:
   - Bike icon moves smoothly along route
   - Icon rotates to face movement direction
   - Icon size is appropriate for zoom level

4. **Camera Behavior**:
   - Auto-follow: Camera centers on marker while preserving zoom
   - Manual mode: User can zoom/pan without interference

## Troubleshooting

### Route Not Showing

**Symptoms**: No route line appears on map

**Solutions**:
1. Check internet connection
2. Verify OSRM API is accessible: `https://router.project-osrm.org`
3. Check logs for "Route fetched" messages
4. Verify `storeLocation` and `customerLocation` are valid
5. Check if route has >= 5 waypoints (validation requirement)

### Map-Matching Not Working

**Symptoms**: GPS coordinates not snapping to roads

**Solutions**:
1. Check internet connection
2. Verify OSRM /match API is accessible
3. Check logs for "Map-matching failed" messages
4. Ensure GPS location is valid (not null)
5. Verify location permissions are granted

### GraphHopper Fallback Not Working

**Symptoms**: Route fails when OSRM is unavailable

**Solutions**:
1. Check GraphHopper API key (if using custom key)
2. Verify internet connection
3. Check logs for GraphHopper API errors
4. Test GraphHopper API directly: `https://graphhopper.com/api/1/route?...`

### Camera Auto-Reset Issue

**Symptoms**: Camera resets zoom/position when user manually zooms

**Solutions**:
1. Verify `autoFollowEnabled` state is working
2. Check that camera reset code is removed from update block
3. Ensure camera is only set in factory block (initial setup)

### Icon Not Displaying

**Symptoms**: Bike icon doesn't appear or appears too large

**Solutions**:
1. Verify `ic_bike.png` exists in `res/drawable-mdpi/`, `drawable-hdpi/`, `drawable-xhdpi/`
2. Check icon file names match `R.drawable.ic_bike`
3. Verify icon scaling in `scaleDrawableForMap()` function
4. Check icon size is 48px, 72px, 96px for respective folders

## File Structure

```
app/src/main/java/com/codewithchandra/grocent/
├── util/
│   ├── RouteHelper.kt          # OSRM/GraphHopper route fetching
│   └── MapMatchingHelper.kt    # OSRM map-matching
├── ui/
│   ├── screens/
│   │   └── OrderTrackingScreen.kt  # Map display and route drawing
│   └── components/
│       └── AnimatedMarker.kt       # Marker animation with rotation
└── viewmodel/
    └── OrderTrackingViewModel.kt   # Tracking logic and map-matching integration

app/src/main/res/
├── drawable-mdpi/
│   └── ic_bike.png (48px)
├── drawable-hdpi/
│   └── ic_bike.png (72px)
└── drawable-xhdpi/
    └── ic_bike.png (96px)
```

## API Endpoints Used

### OSRM Route API
```
GET https://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=full&geometries=polyline
```

### OSRM Match API
```
GET https://router.project-osrm.org/match/v1/driving/{lon},{lat}?geometries=polyline&radiuses=50
```

### GraphHopper Route API
```
GET https://graphhopper.com/api/1/route?point={lat1},{lng1}&point={lat2},{lng2}&vehicle=car&key={KEY}&points_encoded=true
```

## Dependencies

The implementation uses:
- **OSMDroid**: Map tiles and overlays
- **Kotlin Coroutines**: Async API calls
- **AndroidX Compose**: UI framework
- **Standard Android APIs**: HTTP connections, JSON parsing

No additional third-party libraries required for basic functionality.

## Production Considerations

1. **OSRM Server**: Consider self-hosting OSRM for production to avoid rate limits
2. **GraphHopper API Key**: Use your own API key for production (free tier available)
3. **Error Handling**: Add user-friendly error messages for API failures
4. **Caching**: Consider caching routes to reduce API calls
5. **Offline Support**: For offline scenarios, implement route caching or offline routing

## Support

For issues or questions:
1. Check logs using `adb logcat | grep -E "RouteHelper|MapMatchingHelper|OrderTracking"`
2. Verify API endpoints are accessible
3. Check network connectivity
4. Review this README for configuration steps

---

**Last Updated**: Implementation complete with OSRM route following, map-matching, GraphHopper fallback, and auto-follow camera mode.



































