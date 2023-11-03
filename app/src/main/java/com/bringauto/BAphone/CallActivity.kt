package com.bringauto.BAphone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bringauto.BAphone.databinding.ActivityCallBinding
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

class CallActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()

    private lateinit var number: String

    private lateinit var binding: ActivityCallBinding

    private lateinit var url: String
    private var carId: Int = -1
    private lateinit var username: String
    private lateinit var password: String
    private lateinit var allowedNumber: String

    private var tenantId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        number = intent.data?.schemeSpecificPart?:""

        CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
    }

    override fun onStart() {
        super.onStart()
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        url = sharedPreferences.getString("url", "").toString()
        carId = sharedPreferences.getInt("carId", -1)
        username = sharedPreferences.getString("username", "").toString()
        password = sharedPreferences.getString("password", "").toString()
        allowedNumber = sharedPreferences.getString("number", "").toString()

        if (allowedNumber == "" || allowedNumber == number)
            updateCar()
        else
            binding.response.text = "Phone number not allowed"

        binding.answer.setOnClickListener {
            OngoingCall.answer()
        }

        binding.hangup.setOnClickListener {
            OngoingCall.hangup()
            finishAndRemoveTask()
        }

        OngoingCall.state
            .subscribe(::updateUi)
            .addTo(disposables)

        OngoingCall.state
            .filter { it == Call.STATE_DISCONNECTED }
            .delay(1, TimeUnit.SECONDS)
            .firstElement()
            .subscribe { finish() }
            .addTo(disposables)
    }

    private fun updateUi(state: Int) {
        binding.callInfo.text = "${state.asString().lowercase().replaceFirstChar(Char::titlecase)}\n$number"

        binding.answer.isVisible = state == Call.STATE_RINGING
        binding.hangup.isVisible = state in listOf(
            Call.STATE_DIALING,
            Call.STATE_RINGING,
            Call.STATE_ACTIVE
        )
    }

    private fun updateCar() {
        val volleyQueue = Volley.newRequestQueue(this)
        val loginQuery = JSONObject()
        loginQuery.put("query", getLoginString(username, password))

        val loginRequest = object: JsonObjectRequest(
            Method.POST,
            url,
            loginQuery,
            { response ->
                tenantId = response.getJSONObject("data").getJSONObject("UserQuery")
                    .getJSONObject("login").getJSONObject("tenants").getJSONArray("nodes")
                    .getJSONObject(0).getInt("id")
                getCarsRequest()
            },
            { error ->
                binding.response.text = error.localizedMessage
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "bringAutoPhone/1.0"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        volleyQueue.add(loginRequest)
    }

    private fun getCarsRequest() {
        val volleyQueue = Volley.newRequestQueue(this)
        val carQuery = JSONObject()
        carQuery.put("query", getCarGetString())

        val getCarRequest = object: JsonObjectRequest(
            Method.POST,
            url,
            carQuery,
            { response ->
                val cars = response.getJSONObject("data").getJSONObject("CarQuery")
                    .getJSONObject("cars").getJSONArray("nodes")
                for (i in 0 until cars.length()) {
                    val car = cars.getJSONObject(i)

                    if (car.getInt("id") == carId) {
                        updateCarRequest(
                            carId, car.getString("name"),
                            getNewCarStatus(car.getString("status")),
                            car.getBoolean("underTest")
                        )
                        break
                    }

                    if (i == cars.length() - 1)
                        binding.response.text = getString(R.string.no_car_error)
                }
            },
            { error ->
                binding.response.text = error.localizedMessage
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "bringAutoPhone/1.0"
                headers["Content-Type"] = "application/json"
                headers["tenant"] = tenantId.toString()
                return headers
            }
        }

        volleyQueue.add(getCarRequest)
    }

    private fun updateCarRequest(carId: Int, carName: String, carStatus: String, carUnderTest: Boolean) {
        val volleyQueue = Volley.newRequestQueue(this)
        val carUpdateMutation = JSONObject()
        carUpdateMutation.put("query", getCarUpdateString(carId, carName, carStatus, carUnderTest))

        val updateCarRequest = object: JsonObjectRequest(
            Method.POST,
            url,
            carUpdateMutation,
            { _ ->
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        OngoingCall.hangup()
                        finishAndRemoveTask()
                    },
                    5000
                )
            },
            { error ->
                binding.response.text = error.localizedMessage
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "bringAutoPhone/1.0"
                headers["Content-Type"] = "application/json"
                headers["tenant"] = tenantId.toString()
                return headers
            }
        }

        volleyQueue.add(updateCarRequest)
    }

    private fun getNewCarStatus(oldStatus: String): String {
        if (oldStatus == "STOPPEDBYPHONE") {
            binding.response.text = getString(R.string.status_to_waiting)
            return "WAITING"
        }
        binding.response.text = getString(R.string.status_to_stopped)
        return "STOPPEDBYPHONE"
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    companion object {
        fun start(context: Context, call: Call) {
            Intent(context, CallActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(call.details.handle)
                .let(context::startActivity)
        }
    }
}