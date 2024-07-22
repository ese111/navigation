package com.example.navigationsample

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentManager
import com.example.navigationsample.databinding.TmapViewBinding
import com.example.navigationsample.ui.theme.NavigationSampleTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.skt.Tmap.TMapView
import com.skt.tmap.engine.navigation.DriveStatusChangedListener
import com.skt.tmap.engine.navigation.SDKManager
import com.skt.tmap.engine.navigation.network.ndds.CarOilType
import com.skt.tmap.engine.navigation.network.ndds.NddsDataType
import com.skt.tmap.engine.navigation.network.ndds.TollCarType
import com.skt.tmap.engine.navigation.route.RoutePlanType
import com.skt.tmap.engine.navigation.route.data.MapPoint
import com.skt.tmap.engine.navigation.route.data.WayPoint
import com.skt.tmap.vsm.coordinates.VSMCoordinates
import com.tmapmobility.tmap.tmapsdk.ui.data.CarOption
import com.tmapmobility.tmap.tmapsdk.ui.fragment.NavigationFragment
import com.tmapmobility.tmap.tmapsdk.ui.util.TmapUISDK
import vsm.route.RouteData
import java.util.UUID

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TmapUISDK.initialize(
            activity = this,
            clientId = BuildConfig.TMAP_CLIENT_ID,
            apiKey = BuildConfig.TMAP_API_KEY,
            userKey = UUID.randomUUID().toString(),
            deviceKey = UUID.randomUUID().toString(),
            initializeListener = object : TmapUISDK.InitializeListener {
                override fun onFail(errorCode: Int, errorMsg: String?) {
                    Log.d("TmapUISDK", "InitializeListener onFail: $errorCode, $errorMsg")
                }

                override fun onSuccess() {
                    Log.d("TmapUISDK", "InitializeListener onSuccess")
                    val navigationFragment = TmapUISDK.getFragment()

                    TmapUISDK.observableRouteData.observe(this@MainActivity) {
                        Log.d("TmapUISDK", "observableRouteData: $it")
                    }

                    setContent {
                        NavigationSampleTheme {

                            val permissionState = rememberMultiplePermissionsState(
                                permissions = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,

                                    ).also {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        it.add(Manifest.permission.FOREGROUND_SERVICE)
                                    }
                                },
                            )

                            LaunchedEffect(true) {
                                if (permissionState.allPermissionsGranted.not()) {
                                    permissionState.launchMultiplePermissionRequest()
                                }
                            }

                            LaunchedEffect(permissionState.shouldShowRationale) {
                                if (permissionState.shouldShowRationale) {
                                    permissionState.launchMultiplePermissionRequest()
                                }
                            }

                            Greeting(
                                onClickStartRoute = {
                                    getRoute(navigationFragment)
                                },
                                onClickStart = navigationFragment::startSafeDrive,
                                onClickStop = navigationFragment::stopDrive
                            ) {
                                TmapFragment(
                                    supportFragmentManager = supportFragmentManager,
                                    navigationFragment = navigationFragment
                                )
                            }
                        }
                    }
                }

                override fun savedRouteInfoExists(destinationName: String?) {
                    Log.d("TmapUISDK", "InitializeListener savedRouteInfoExists: $destinationName")
                }

            }
        )

    }

    private fun getRoute(
        navigationFragment: NavigationFragment
    ) {
        navigationFragment.carOption = CarOption().apply {
            carType = TollCarType.Car
            oilType = CarOilType.Gasoline
            isHipassOn = true
        }

        val currentLocation = SDKManager.getInstance().getCurrentPosition()
        val currentName = VSMCoordinates.getAddressOffline(currentLocation.longitude, currentLocation.latitude)
        val startPoint = WayPoint(currentName, MapPoint(currentLocation.longitude, currentLocation.latitude))

        val endPoint = WayPoint("강남역", MapPoint(127.027813, 37.497999), "280181", 5.toByte())

        val planList = ArrayList<RoutePlanType>()
        planList.add(RoutePlanType.Traffic_Recommend)

        navigationFragment.requestRoute(
            startPoint,
            null,
            endPoint,
            false,
            object : TmapUISDK.RouteRequestListener {
                override fun onFail(errorCode: Int, errorMsg: String?) {
                    Log.d("TmapUISDK", "RouteRequestListener onFail: $errorCode, $errorMsg")
                    Toast.makeText(this@MainActivity, "경로 탐색 실패", Toast.LENGTH_SHORT).show()
                }

                override fun onSuccess() {
                    Log.d("TmapUISDK", "RouteRequestListener onSuccess ${startPoint.mapPoint} \n ${endPoint.mapPoint}")
                }
            }, planList)


    }
}

@Composable
fun Greeting(
    onClickStartRoute: () -> Unit,
    onClickStop: () -> Unit,
    onClickStart: () -> Unit,
    mapContent: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClickStartRoute
                ) {
                    Text(text = "Get Route")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onClickStart
                    ) {
                        Text(text = "Start")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onClickStop
                    ) {
                        Text(text = "Stop")
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier
            .padding(it)
            .fillMaxSize()) {
            mapContent()
        }
    }
}

@Composable
fun TmapFragment(
    navigationFragment: NavigationFragment,
    supportFragmentManager: FragmentManager
) {
    val transaction = remember { supportFragmentManager.beginTransaction() }
    AndroidViewBinding(
        modifier = Modifier.fillMaxSize(),
        factory = TmapViewBinding::inflate
    ) {
        transaction.add(R.id.contaier, navigationFragment)
        transaction.commitAllowingStateLoss()
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NavigationSampleTheme {
        Greeting(
            onClickStartRoute = {},
            onClickStart = {},
            onClickStop = {}
        ) {
            Box(modifier = Modifier
                .background(Color.Red)
                .fillMaxSize())
        }
    }
}