package com.example.myyandexmapkit

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myyandexmapkit.databinding.ActivitySecondBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*


class SecondActivity : AppCompatActivity(), UserLocationObjectListener {
    private val PERMISSIONS_REQUEST_FINE_LOCATION = 1

    lateinit var bindingClass: ActivitySecondBinding
    lateinit var placeMark: PlacemarkMapObject
    lateinit var userLocationLayer: UserLocationLayer

    private val compositeDisposable = CompositeDisposable()

    private var pointL: String? = "Точка не задана"

    //Слушатель тапов по карте
    private val inputListener: InputListener = object : InputListener {
        override fun onMapTap(map: Map, point: Point) {
            //Создаем или добавляем метку на карте
            if (this@SecondActivity::placeMark.isInitialized) {
                placeMark.geometry = point
            } else {
                placeMark = bindingClass.mapview.map.mapObjects
                    .addPlacemark(
                        point,
                        ImageProvider.fromResource(this@SecondActivity, R.drawable.search_result)
                    )
                placeMark.isDraggable = true
            }
            //Переключение на получение адреса в фоновом потоке
            startGeoStream(point)
        }
        override fun onMapLongTap(map: Map, point: Point) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingClass = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

        bindingClass.button.setOnClickListener {
            val intent = Intent().putExtra("key", pointL)
            setResult(RESULT_OK, intent)
            finish()
        }

        MapKitFactory.initialize(this)

        //Начальная точка для камеры - Москва
        bindingClass.mapview.map.move(
            CameraPosition(Point(55.751574, 37.573856), 11.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0F),
            null
        )

        bindingClass.mapview.map.addInputListener(inputListener)

        //Разрешение доступа к геоданным
        requestLocationPermission()

        //Инициализация текущей геопозиции
        val mapKit = MapKitFactory.getInstance()
        mapKit.resetLocationManagerToDefault()
        userLocationLayer = mapKit.createUserLocationLayer(bindingClass.mapview.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true

        userLocationLayer.setObjectListener(this)
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                "android.permission.ACCESS_FINE_LOCATION"
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf("android.permission.ACCESS_FINE_LOCATION"),
                PERMISSIONS_REQUEST_FINE_LOCATION
            )
        }
    }

    override fun onStop() {
        bindingClass.mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        bindingClass.mapview.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(
            "My",
            "Dispose objects"
        )
        compositeDisposable.dispose()

    }

    //Перевод камеры на текущую локацию
    override fun onObjectAdded(userLocationView: UserLocationView) {
        userLocationLayer.setAnchor(
            PointF(
                (bindingClass.mapview.width * 0.5).toFloat(),
                (bindingClass.mapview.height * 0.5).toFloat()
            ),
            PointF(
                (bindingClass.mapview.width * 0.5).toFloat(),
                (bindingClass.mapview.height * 0.83).toFloat()
            )
        )
    }

    override fun onObjectRemoved(view: UserLocationView) {
    }

    override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {
    }

    private fun startGeoStream(point: Point) {
        val disposable = Observable.just(point)
            .map {
                Geocoder(this@SecondActivity, Locale.getDefault()).getFromLocation(
                    it.latitude,
                    it.longitude,
                    1
                )[0].getAddressLine(0).toString()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                pointL = "$result \n(${point.latitude} , ${point.longitude})"
                Log.w(
                    "My",
                    "Location - $result"
                )
            },
                {
                    Log.w(
                        "My",
                        "Error"
                    )
                })
        compositeDisposable.add(disposable)
    }
}