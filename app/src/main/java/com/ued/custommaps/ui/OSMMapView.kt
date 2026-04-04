package com.ued.custommaps.ui

import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ued.custommaps.models.CustomMarker
import com.ued.custommaps.models.GeoPointData
import com.ued.custommaps.models.MapStyle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OSMMapView(
    modifier: Modifier = Modifier,
    markers: List<CustomMarker> = emptyList(),
    polyline: List<GeoPointData> = emptyList(),
    mapStyle: MapStyle = MapStyle.NORMAL,
    onMapLongClick: (GeoPoint) -> Unit = {},
    onMarkerClick: (CustomMarker) -> Unit = {},
    initialCenter: GeoPoint = GeoPoint(10.762622, 106.660172),
    initialZoom: Double = 15.0,
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current

    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(getTileSource(mapStyle))
                setMultiTouchControls(true)

                // CHỈNH NÚT ZOOM: Chuyển sang chế độ ẩn hoặc hiển thị bên phải
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)

                controller.setZoom(initialZoom)
                controller.setCenter(initialCenter)

                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()
                overlays.add(locationOverlay)

                overlays.add(object : Overlay() {
                    override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
                        val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                        onMapLongClick(geoPoint)
                        return true
                    }
                })
                onMapReady(this)
            }
        },
        update = { mapView ->
            mapView.setTileSource(getTileSource(mapStyle))
            mapView.overlays.removeAll { it is Marker || it is Polyline }

            if (polyline.isNotEmpty()) {
                val osmdroidPolyline = Polyline(mapView)
                osmdroidPolyline.outlinePaint.color = android.graphics.Color.RED
                osmdroidPolyline.outlinePaint.strokeWidth = 8f
                osmdroidPolyline.setPoints(polyline.map { GeoPoint(it.latitude, it.longitude) })
                mapView.overlays.add(osmdroidPolyline)
            }

            markers.forEach { customMarker ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(customMarker.latitude, customMarker.longitude)
                    title = customMarker.title
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> onMarkerClick(customMarker); true }
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}

private fun getTileSource(mapStyle: MapStyle) = when (mapStyle) {
    MapStyle.NORMAL -> TileSourceFactory.MAPNIK
    MapStyle.SATELLITE -> TileSourceFactory.USGS_SAT
    MapStyle.TERRAIN -> TileSourceFactory.DEFAULT_TILE_SOURCE
}