package ovo.sypw.journal.presentation.components

import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
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
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.poisearch.PoiSearch.OnPoiSearchListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovo.sypw.journal.common.utils.AMapLocationUtils
import ovo.sypw.journal.common.utils.PermissionUtils
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.LocationData


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
    var isTextFieldFocused by remember { mutableStateOf(false) }

    // POI搜索结果
    var poiItems by remember { mutableStateOf<List<com.amap.api.services.core.PoiItem>>(emptyList()) }
    var isShowingSuggestions by remember { mutableStateOf(false) }

    // 地理编码搜索器
    val geocodeSearch = remember { GeocodeSearch(context) }

    // POI搜索监听器
    val poiSearchListener = remember {
        object : OnPoiSearchListener {
            override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                isSearching = false
                if (rCode == 1000) {
                    poiItems = result?.pois ?: emptyList()
                    isShowingSuggestions = poiItems.isNotEmpty() && isTextFieldFocused
                } else {
                    poiItems = emptyList()
                    isShowingSuggestions = false
                }
            }

            override fun onPoiItemSearched(
                poiItem: com.amap.api.services.core.PoiItem?,
                rCode: Int
            ) {
                // 单个POI项查询回调，这里不需要处理
            }
        }
    }

    // 设置定时器以防止每次按键都进行查询
    LaunchedEffect(searchText) {
        if (searchText.length >= 2) {
            delay(500) // 延迟500毫秒，等待用户输入完成

            // 如果文本框有焦点，才自动触发搜索
            if (isTextFieldFocused) {
                isSearching = true

                // 执行POI搜索
                val query = PoiSearch.Query(searchText, "", "")
                query.pageSize = 10 // 限制结果数量为10
                query.pageNum = 1

                // 执行POI搜索
                val poiSearch = PoiSearch(context, query)
                poiSearch.bound = PoiSearch.SearchBound(
                    LatLonPoint(currentLatitude, currentLongitude),
                    5000 // 搜索半径5公里
                )
                poiSearch.setOnPoiSearchListener(poiSearchListener)
                poiSearch.searchPOIAsyn()
            }
        } else {
            // 如果文本太短，清空候选项（但不影响按钮点击后的显示）
            if (isTextFieldFocused) {
                poiItems = emptyList()
                isShowingSuggestions = false
            }
        }
    }

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

    // 从POI项移动到位置
    fun moveToPoiLocation(poiItem: com.amap.api.services.core.PoiItem) {
        poiItem.latLonPoint?.let { point ->
            currentLatitude = point.latitude
            currentLongitude = point.longitude
            currentLocationName = poiItem.title ?: ""
            searchText = poiItem.title ?: "" // 更新搜索框文本为选中的位置名称

            // 移动到选中的位置
            moveToLocation(aMap, point.latitude, point.longitude, poiItem.title)

            // 关闭候选列表
            isShowingSuggestions = false
            isTextFieldFocused = false

            SnackBarUtils.showSnackBar("已定位到: ${poiItem.title}")
        }
    }

    // 执行地理编码搜索
    fun executeGeocodeSearch() {
        if (searchText.isNotEmpty()) {
            isSearching = true
            isShowingSuggestions = false

            // 使用地理编码搜索位置
            val query = GeocodeQuery(
                searchText, // 地址
                "" // 城市，空字符串表示全国范围
            )
            geocodeSearch.getFromLocationNameAsyn(query)
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

                // 搜索栏和候选列表
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    // 搜索栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .zIndex(1f), // 确保搜索栏在候选列表上方
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("搜索位置...", fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                                .height(56.dp)
                                .onFocusChanged {
                                    isTextFieldFocused = it.isFocused
                                    if (it.isFocused) {
                                        isShowingSuggestions = poiItems.isNotEmpty()
                                    }
                                },
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

                                    // 执行POI搜索来显示候选列表
                                    val query = PoiSearch.Query(searchText, "", "")
                                    query.pageSize = 10 // 限制结果数量为10
                                    query.pageNum = 1

                                    val poiSearch = PoiSearch(context, query)
                                    poiSearch.bound = PoiSearch.SearchBound(
                                        LatLonPoint(currentLatitude, currentLongitude),
                                        5000 // 搜索半径5公里
                                    )
                                    poiSearch.setOnPoiSearchListener(object : OnPoiSearchListener {
                                        override fun onPoiSearched(result: PoiResult?, rCode: Int) {
                                            isSearching = false
                                            if (rCode == 1000) {
                                                poiItems = result?.pois ?: emptyList()
                                                // 强制显示候选列表，不管文本框是否有焦点
                                                isShowingSuggestions = poiItems.isNotEmpty()

                                                if (poiItems.isEmpty()) {
                                                    // 如果没有候选项，回退到地理编码搜索
                                                    executeGeocodeSearch()
                                                }
                                            } else {
                                                // 搜索失败，回退到地理编码搜索
                                                executeGeocodeSearch()
                                            }
                                        }

                                        override fun onPoiItemSearched(
                                            poiItem: com.amap.api.services.core.PoiItem?,
                                            rCode: Int
                                        ) {
                                            // 不需要处理
                                        }
                                    })
                                    poiSearch.searchPOIAsyn()

                                    SnackBarUtils.showSnackBar("正在搜索: $searchText")
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

                    // 候选地点列表
                    if (isShowingSuggestions && poiItems.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp) // 位于搜索栏下方
                                .zIndex(0.9f),
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp) // 最大高度
                            ) {
                                items(poiItems) { poiItem ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                moveToPoiLocation(poiItem)
                                                // 选择后自动执行地理编码搜索
                                                isShowingSuggestions = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) { // 添加weight确保文本可以适当换行
                                            Text(
                                                text = poiItem.title ?: "",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (!poiItem.snippet.isNullOrEmpty()) {
                                                Text(
                                                    text = poiItem.snippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1 // 限制地址只显示一行
                                                )
                                            }

                                            // 显示所属区域信息
                                            Text(
                                                text = "${poiItem.cityName ?: ""}${poiItem.adName ?: ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 地图视图
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp) // 添加垂直间距
                        .clickable(
                            // 点击地图区域时关闭候选列表
                            onClick = { isShowingSuggestions = false }
                        )
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
                            /*
                             * AndroidView 用于在 Compose 中嵌入 Android View。
                             * factory 用于创建 MapView 实例。
                             * MapView 初始化时，设置其布局参数为 MATCH_PARENT，使其充满父布局。
                             * 将创建的 MapView 实例赋值给 mapView 变量，以便在其他地方引用。
                             */
                            AndroidView(
                                factory = { ctx ->
                                    MapView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )

                                        mapView = this

                                        // 创建地图
                                        /*
                                         * 调用 MapView 的 onCreate 方法，传入 null 作为 Bundle。
                                         * 这是高德地图 SDK 的要求，用于初始化地图。
                                         */
                                        onCreate(null)

                                        // 获取AMap对象
                                        /*
                                         * 获取 MapView 中的 AMap 对象，AMap 是高德地图的核心类，用于操作地图。
                                         * 在 apply 块中对 AMap 对象进行配置。
                                         */
                                        aMap = this.map.apply {
                                            // 自定义UI设置
                                            uiSettings.apply {
                                                // 禁用默认的缩放控件（放大缩小按钮）
                                                isZoomControlsEnabled = false
                                                // 禁用比例尺控件
                                                isScaleControlsEnabled = false
                                                // 启用指南针
                                                isCompassEnabled = true
                                                // 启用缩放手势
                                                isZoomGesturesEnabled = true
                                                // 启用滑动手势
                                                isScrollGesturesEnabled = true
                                            }

                                            // 启用定位层
                                            /*
                                             * 启用地图的定位图层。
                                             * 如果设置为 true，地图上会显示当前位置的蓝色圆点，并且可以获取定位信息。
                                             */
                                            isMyLocationEnabled = true

                                            // 设置地图点击监听
                                            /*
                                             * 设置地图的点击事件监听器。
                                             * 当用户点击地图时，会回调此监听器，并传入点击位置的经纬度 (LatLng)。
                                             * 在回调中，更新当前的经纬度状态，并调用 updateMarkerAndGetAddress 方法更新标记点和获取地址信息。
                                             */
                                            setOnMapClickListener { latLng ->
                                                currentLatitude = latLng.latitude
                                                currentLongitude = latLng.longitude
                                                updateMarkerAndGetAddress(
                                                    latLng.latitude,
                                                    latLng.longitude
                                                )
                                            }

                                            // 设置标记拖动监听
                                            /*
                                             * 设置地图标记的拖动事件监听器。
                                             * onMarkerDragStart: 标记开始拖动时回调。
                                             * onMarkerDrag: 标记拖动过程中回调。
                                             * onMarkerDragEnd: 标记拖动结束时回调。
                                             * 在 onMarkerDragEnd 回调中，获取拖动结束后标记的位置，更新当前的经纬度状态，
                                             * 并调用 updateMarkerAndGetAddress 方法更新标记点和获取地址信息。
                                             */
                                            setOnMarkerDragListener(object :
                                                AMap.OnMarkerDragListener {
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
                                        /*
                                         * 判断是否有初始位置信息 (initialLocation)。
                                         * 如果有，则调用 moveToLocation 方法将地图移动到初始位置，并显示标记点。
                                         * 如果没有初始位置信息，则启动一个协程 (coroutineScope.launch) 来异步获取当前设备的位置，
                                         * 并将地图移动到当前位置。
                                         *
                                         * 这一部分确保了地图在打开时会显示一个有意义的初始位置。
                                         */
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
                                    text = "坐标: ${
                                        String.format(
                                            "%.6f",
                                            currentLatitude
                                        )
                                    }, ${String.format("%.6f", currentLongitude)}",
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