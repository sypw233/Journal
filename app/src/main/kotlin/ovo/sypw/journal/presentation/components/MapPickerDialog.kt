package ovo.sypw.journal.presentation.components

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.Marker
import com.amap.api.maps2d.model.MarkerOptions
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.common.utils.PermissionUtils
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.LocationData
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.TextStyle


/**
 * 地图选择器对话框
 * 用于在地图上选择位置
 * @param isVisible 是否显示对话框
 * @param initialLocation 初始位置
 * @param onLocationSelected 位置选择回调
 * @param onDismiss 取消回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    isVisible: Boolean,
    initialLocation: LocationData? = null,
    onLocationSelected: (LocationData) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 地图状态
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    
    // 位置状态
    var currentLatitude by remember { 
        mutableStateOf(initialLocation?.latitude ?: 39.908823) 
    }
    var currentLongitude by remember { 
        mutableStateOf(initialLocation?.longitude ?: 116.397470) 
    }
    var currentLocationName by remember { 
        mutableStateOf(initialLocation?.name ?: "") 
    }
    
    // 搜索状态
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    // 地理编码搜索器
    val geocodeSearch = remember { GeocodeSearch(context) }
    
    // 地理编码结果监听器
    val geocodeListener = remember {
        object : GeocodeSearch.OnGeocodeSearchListener {
            override fun onRegeocodeSearched(result: RegeocodeResult?, rCode: Int) {
                // 逆地理编码查询结果回调
                isSearching = false
                if (rCode == 1000) {
                    result?.regeocodeAddress?.let { address ->
                        currentLocationName = address.formatAddress ?: ""
                        SnackBarUtils.showSnackBar("获取位置信息: $currentLocationName")
                    }
                } else {
                    SnackBarUtils.showSnackBar("获取位置信息失败: $rCode")
                }
            }

            override fun onGeocodeSearched(result: GeocodeResult?, rCode: Int) {
                // 地理编码查询结果回调
                isSearching = false
                if (rCode == 1000 && result?.geocodeAddressList?.isNotEmpty() == true) {
                    result.geocodeAddressList?.get(0)?.let { address ->
                        currentLatitude = address.latLonPoint?.latitude ?: currentLatitude
                        currentLongitude = address.latLonPoint?.longitude ?: currentLongitude
                        currentLocationName = address.formatAddress ?: searchText
                        
                        // 移动到搜索的位置
                        aMap?.let { map ->
                            val latLng = LatLng(currentLatitude, currentLongitude)
                            
                            // 更新标记点
                            if (marker == null) {
                                val markerOptions = MarkerOptions()
                                    .position(latLng)
                                    .title(currentLocationName)
                                    .draggable(true)
                                marker = map.addMarker(markerOptions)
                            } else {
                                marker?.position = latLng
                                marker?.title = currentLocationName
                            }
                            
                            // 移动到指定位置
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                        }
                        
                        SnackBarUtils.showSnackBar("已定位到: $currentLocationName")
                    }
                } else {
                    SnackBarUtils.showSnackBar("搜索位置失败: $rCode")
                }
            }
        }
    }
    
    // 设置地理编码监听器
    LaunchedEffect(Unit) {
        geocodeSearch.setOnGeocodeSearchListener(geocodeListener)
    }
    
    // 更新标记点位置并获取地址信息
    fun updateMarkerAndGetAddress(latitude: Double, longitude: Double) {
        aMap?.let { map ->
            val latLng = LatLng(latitude, longitude)
            
            // 更新标记点
            if (marker == null) {
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title("选择的位置")
                    .draggable(true)
                marker = map.addMarker(markerOptions)
            } else {
                marker?.position = latLng
            }
            
            // 移动到当前位置
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            
            // 使用逆地理编码获取位置信息
            val query = RegeocodeQuery(
                LatLonPoint(latitude, longitude),
                200f, // 搜索半径，单位：米
                GeocodeSearch.AMAP // 使用高德地图
            )
            
            isSearching = true
            geocodeSearch.getFromLocationAsyn(query)
        }
    }
    
    // 移动到指定位置并更新标记点
    fun moveToLocation(map: AMap?, latitude: Double, longitude: Double, name: String?) {
        map?.let {
            val latLng = LatLng(latitude, longitude)
            
            // 更新标记点
            if (marker == null) {
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title(name ?: "选择的位置")
                    .draggable(true)
                marker = it.addMarker(markerOptions)
            } else {
                marker?.position = latLng
                marker?.title = name ?: "选择的位置"
            }
            
            // 移动到指定位置
            it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
        }
    }
    
    // 获取当前位置
    fun getCurrentLocation() {
        if (PermissionUtils.hasPermissions(context, PermissionUtils.LOCATION_PERMISSIONS)) {
            SnackBarUtils.showSnackBar("正在获取当前位置...")
            AMapLocationUtils.getCurrentLocation(
                context = context,
                onSuccess = { location ->
                    location.latitude?.let { lat ->
                        location.longitude?.let { lng ->
                            currentLatitude = lat
                            currentLongitude = lng
                            // 使用逆地理编码获取位置信息
                            val query = RegeocodeQuery(
                                LatLonPoint(lat, lng),
                                200f, // 搜索半径，单位：米
                                GeocodeSearch.AMAP // 使用高德地图
                            )
                            isSearching = true
                            geocodeSearch.getFromLocationAsyn(query)
                            
                            // 移动到当前位置
                            moveToLocation(aMap, lat, lng, location.name)
                        }
                    } ?: run {
                        SnackBarUtils.showSnackBar("获取到的位置信息不完整")
                    }
                },
                onError = { errorMsg ->
                    SnackBarUtils.showSnackBar("获取位置失败: $errorMsg")
                }
            )
        } else {
            SnackBarUtils.showSnackBar("需要定位权限才能获取当前位置")
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
//                .padding(16.dp),  // 添加外边距
            shape = RoundedCornerShape(24.dp),  // 更大的圆角
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 关闭按钮
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        // 标题
                        Text(
                            text = "选择位置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        // 确认按钮
                        IconButton(
                            onClick = {
                                val locationData = LocationData(
                                    name = currentLocationName,
                                    latitude = currentLatitude,
                                    longitude = currentLongitude
                                )
                                onLocationSelected(locationData)
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "确认",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                // 搜索栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("搜索位置...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .height(56.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "搜索",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    // 搜索按钮
                    FilledIconButton(
                        onClick = {
                            if (searchText.isNotEmpty()) {
                                isSearching = true
                                SnackBarUtils.showSnackBar("正在搜索: $searchText")
                                // 使用地理编码搜索位置
                                val query = GeocodeQuery(
                                    searchText, // 地址
                                    "" // 城市，空字符串表示全国范围
                                )
                                geocodeSearch.getFromLocationNameAsyn(query)
                            } else {
                                SnackBarUtils.showSnackBar("请输入搜索内容")
                            }
                        },
                        enabled = !isSearching && searchText.isNotEmpty(),
                        shape = CircleShape,
                        modifier = Modifier
                            .width(80.dp)
                            .height(56.dp)
                            .padding(start = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                // 地图视图
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    // 地图容器 - 使用Surface添加圆角和阴影
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        // 添加内边距，使地图内容不会紧贴边缘
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(14.dp)) // 内容也需要裁剪成圆角
                        ) {
                            // 地图视图
                            AndroidView(
                                factory = { ctx ->
                                    MapView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        
                                        mapView = this
                                        
                                        // 创建地图
                                        onCreate(null)
                                        
                                        // 获取AMap对象
                                        aMap = this.map.apply {
                                            // 启用定位层
                                            isMyLocationEnabled = true
                                            
                                            // 设置地图点击监听
                                            setOnMapClickListener { latLng ->
                                                currentLatitude = latLng.latitude
                                                currentLongitude = latLng.longitude
                                                updateMarkerAndGetAddress(latLng.latitude, latLng.longitude)
                                            }
                                            
                                            // 设置标记拖动监听
                                            setOnMarkerDragListener(object : AMap.OnMarkerDragListener {
                                                override fun onMarkerDragStart(marker: Marker) {}
                                                
                                                override fun onMarkerDrag(marker: Marker) {}
                                                
                                                override fun onMarkerDragEnd(marker: Marker) {
                                                    currentLatitude = marker.position.latitude
                                                    currentLongitude = marker.position.longitude
                                                    updateMarkerAndGetAddress(
                                                        marker.position.latitude,
                                                        marker.position.longitude
                                                    )
                                                }
                                            })
                                        }
                                        
                                        // 初始位置
                                        if (initialLocation?.latitude != null && initialLocation.longitude != null) {
                                            moveToLocation(
                                                aMap,
                                                initialLocation.latitude,
                                                initialLocation.longitude,
                                                initialLocation.name
                                            )
                                        } else {
                                            // 如果没有初始位置，尝试获取当前位置
                                            coroutineScope.launch {
                                                getCurrentLocation()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    // 当前位置按钮
                    IconButton(
                        onClick = { getCurrentLocation() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = "我的位置",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // 底部区域 - 显示当前选择的位置
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "当前选择位置",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 位置名称
                        Text(
                            text = currentLocationName.ifEmpty { "尚未选择位置" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (currentLocationName.isNotEmpty()) 
                                MaterialTheme.colorScheme.onSurface 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "坐标: ${String.format("%.6f", currentLatitude)}, ${String.format("%.6f", currentLongitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 处理地图生命周期
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDestroy()
        }
    }
}

// 地图生命周期管理扩展函数
private fun MapView.onCreate(savedInstanceState: Bundle?) {
    try {
        this.onCreate(savedInstanceState)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun MapView.onDestroy() {
    try {
        this.onDestroy()
    } catch (e: Exception) {
        e.printStackTrace()
    }
} 