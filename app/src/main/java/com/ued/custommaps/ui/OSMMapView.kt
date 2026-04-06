package com.ued.custommaps.ui

import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    initialCenter: GeoPoint = GeoPoint(16.0, 108.0), // Mặc định ở Việt Nam
    initialZoom: Double = 15.0,
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Khởi tạo cấu hình OSMDroid (Chỉ chạy 1 lần)
    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            setTileSource(getTileSource(mapStyle))
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(initialZoom)
            controller.setCenter(initialCenter)

            // Thêm lớp hiển thị vị trí hiện tại
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
            locationOverlay.enableMyLocation()
            overlays.add(locationOverlay)

            // Xử lý nhấn giữ trên bản đồ
            overlays.add(object : Overlay() {
                override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
                    val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                    onMapLongClick(geoPoint)
                    return true
                }
            })
            onMapReady(this)
        }
    }

    // QUẢN LÝ VÒNG ĐỜI: Giúp bản đồ dừng chạy khi bạn ẩn App để tiết kiệm Pin
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Cập nhật bản đồ khi dữ liệu (markers, polyline) thay đổi
    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { m ->
            m.setTileSource(getTileSource(mapStyle))

            // Xóa các lớp cũ để vẽ lại lớp mới (tránh bị chồng đè)
            m.overlays.removeAll { it is Marker || it is Polyline }

            // Vẽ đường đi
            if (polyline.isNotEmpty()) {
                val osmdroidPolyline = Polyline(m).apply {
                    outlinePaint.color = android.graphics.Color.RED
                    outlinePaint.strokeWidth = 8f
                    setPoints(polyline.map { GeoPoint(it.latitude, it.longitude) })
                }
                m.overlays.add(osmdroidPolyline)
            }

            // Vẽ các dấu mốc
            markers.forEach { customMarker ->
                val marker = Marker(m).apply {
                    position = GeoPoint(customMarker.latitude, customMarker.longitude)
                    title = customMarker.title
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> onMarkerClick(customMarker); true }
                }
                m.overlays.add(marker)
            }
            m.invalidate() // Vẽ lại bản đồ
        }
    )
}

private fun getTileSource(mapStyle: MapStyle) = when (mapStyle) {
    MapStyle.NORMAL -> TileSourceFactory.MAPNIK
    MapStyle.SATELLITE -> TileSourceFactory.USGS_SAT
    MapStyle.TERRAIN -> TileSourceFactory.DEFAULT_TILE_SOURCE
}