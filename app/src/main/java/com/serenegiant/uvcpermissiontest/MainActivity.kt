package com.serenegiant.uvcpermissiontest

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import kotlin.math.min

class MainActivity : AppCompatActivity() {
	
	private lateinit var mainFrame: View
	private lateinit var mLogScrollView: ScrollView
	private lateinit var logTv: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (DEBUG) Log.v(TAG, "onCreate:")
		setContentView(R.layout.activity_main)
		this.title = "targetSDK${applicationInfo.targetSdkVersion}"
		mainFrame = findViewById(R.id.main_frame)
		mLogScrollView = findViewById(R.id.log_scrollview)
		logTv = findViewById(R.id.log_textview)

		// output information to screen
		var msg = "targetSDKVersion=${applicationInfo.targetSdkVersion}"
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			msg = "${msg}\n    minSDKVersion=${applicationInfo.minSdkVersion}"
		}
		msg = "${msg}\n    SDK_INT=${Build.VERSION.SDK_INT}\n" +
			"    BOARD=${Build.BOARD}\n" +
			"    BOOTLOADER=${Build.BOOTLOADER}\n" +
			"    BRAND=${Build.BRAND}\n" +
			"    DEVICE=${Build.DEVICE}\n" +
			"    DISPLAY=${Build.DISPLAY}\n" +
			"    HARDWARE=${Build.HARDWARE}\n" +
			"    ID=${Build.ID}\n" +
			"    MANUFACTURER=${Build.MANUFACTURER}\n" +
			"    PRODUCT=${Build.PRODUCT}\n" +
			"    TAGS=${Build.TAGS}\n" +
			"    VERSION.MODEL=${Build.MODEL}\n"
		log("Info", msg)

		// check CAMERA permission
		val hasPermission= ActivityCompat.checkSelfPermission(this,
			Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			// Android 9 and later needs CAMERA permission to access UVC devices
			if (hasPermission) {
				log("CAMERA permission", "already granted")
			} else {
				requestCameraPermission()
			}
		} else {
			log("CAMERA permission", "before Android 9,CAMERA permission=$hasPermission")
		}
	}
	
	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		if (DEBUG) Log.v(TAG, "onNewIntent:$intent")
		if (intent != null) {
			runOnUiThread {
				handleIntent(intent)
			}
		}
	}
	
	override fun onStart() {
		super.onStart()
		if (DEBUG) Log.v(TAG, "onStart:")
		// Register BroadcastReceiver to receive USB related events
		val filter = IntentFilter(ACTION_USB_PERMISSION)
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
		registerReceiver(mBroadcastReceiver, filter)
		log("BroadcastReceiver", "register")
		if (intent != null) {
			runOnUiThread {
				handleIntent(intent)
			}
		}
	}
	
	override fun onStop() {
		if (DEBUG) Log.v(TAG, "onStop:")
		unregisterReceiver(mBroadcastReceiver)
		log("BroadcastReceiver", "unregister")
		super.onStop()
	}
	
	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		super.onDestroy()
	}
	
//--------------------------------------------------------------------------------
	/**
	 * When received result of permission request
	 */
	override fun onRequestPermissionsResult(
		requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		val n = min(permissions.size, grantResults.size)
		for (i in 0 until n) {
			handlePermissionResult(permissions[i],
			grantResults[i] == PackageManager.PERMISSION_GRANTED)
		}
	}

	private fun handlePermissionResult(permission: String, result: Boolean) {
		if (result) {
			if (permission == Manifest.permission.CAMERA) {
				log("CAMERA permission", "granted")
				Snackbar.make(mainFrame,
					R.string.camera_permission_granted, Snackbar.LENGTH_SHORT
				).show()
				scanAttachedDevice()
			}
		} else {
			log("CAMERA permission", "denied")
			Snackbar.make(mainFrame,
				R.string.camera_permission_denied, Snackbar.LENGTH_SHORT
			).show()
		}
	}
	
//--------------------------------------------------------------------------------
	/**
	 * request CAMERA permission
	 * Android 9 and later needs CAMERA permission to access UVC devices
	 */
	private fun requestCameraPermission() {
		if (DEBUG) Log.v(TAG, "requestCameraPermission:")
		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
			Snackbar.make(mainFrame,
				R.string.camera_access_required, Snackbar.LENGTH_INDEFINITE)
				.setAction(android.R.string.ok) {
					// Request the permission
					log("CAMERA permission", "request")
					ActivityCompat.requestPermissions(
						this@MainActivity,
						arrayOf(Manifest.permission.CAMERA),
						REQ_PERMISSION_CAMERA
					)
				}
				.show()
		} else {
			Snackbar.make(mainFrame,
				R.string.camera_unavailable, Snackbar.LENGTH_SHORT
			).show()
			log("CAMERA permission", "request")
			ActivityCompat.requestPermissions(
				this@MainActivity,
				arrayOf(Manifest.permission.CAMERA),
				REQ_PERMISSION_CAMERA
			)
		}
	}
	
//--------------------------------------------------------------------------------
	/**
	 * BroadcastReceiver to receive USB related events
	 */
	private val mBroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent != null) {
				handleIntent(intent)
			}
		}
	}
	
	/**
	 * Handle received Intent
	 */
	private fun handleIntent(intent: Intent) {
		if (DEBUG) Log.v(TAG, "handleIntent:$intent")
		when (intent.action) {
			UsbManager.ACTION_USB_DEVICE_ATTACHED
				-> handleActionOnAttachDevice(intent)
			UsbManager.ACTION_USB_DEVICE_DETACHED
				-> handleActionOnDetachDevice(intent)
			ACTION_USB_PERMISSION
				-> handleActionUsbPermission(intent)
			Intent.ACTION_MAIN
				-> scanAttachedDevice()	// app launched by user
			else -> {
				log("Unknown intent", "action=" + intent.action)
			}
		}
	}
	
	/**
	 * when app received attach event of USB device
	 */
	private fun handleActionOnAttachDevice(intent: Intent) {
		if (DEBUG) Log.v(TAG, "handleActionOnAttachDevice:$intent")
		val manager = getSystemService(Context.USB_SERVICE) as UsbManager
		val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
			} else {
				intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
			}
		var hasPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
		if (device != null) {
			hasPermission = hasPermission || manager.hasPermission(device)
			log("USB_DEVICE_ATTACHED", "hasPermission=$hasPermission\n    ${deviceName(device)}")
			if (!hasPermission) {
				requestUsbPermission(manager, device)
			} else {
				log("USB permission", "already has permission:\n    ${deviceName(device)}")
			}
		} else {
			log("USB_DEVICE_ATTACHED", "device is null")
		}
	}
	
	/**
	 * when app received detach event of USB device
	 */
	private fun handleActionOnDetachDevice(intent: Intent) {
		if (DEBUG) Log.v(TAG, "handleActionOnDetachDevice:$intent")
		val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
			} else {
				intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
			}
		log("USB_DEVICE_DETACHED", deviceName(device))
	}
	
	/**
	 * when app received the result of requesting USB access permission.
	 */
	private fun handleActionUsbPermission(intent: Intent) {
		if (DEBUG) Log.v(TAG, "handleActionUsbPermission:$intent")
		val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
			} else {
				intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
			}
		val hasPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
		log("Result", "hasPermission=$hasPermission\n    ${deviceName(device)}(${device})")
	}

//--------------------------------------------------------------------------------
	/**
	 * scan attached usb devices and request permission for first found device that has no permission
	 */
	private fun scanAttachedDevice() {
		if (DEBUG) Log.v(TAG, "scanAttachedDevice:")
		log("SCAN", "start")
		val manager = getSystemService(Context.USB_SERVICE) as UsbManager
		val devices = manager.deviceList
		run loop@ {
			devices.values.forEach {
				if (isUVC(it)) {
					if (!manager.hasPermission(it)) {
						requestUsbPermission(manager, it)
						// XXX only request permission for first found device now
						return@loop
					} else {
						log("USB permission", "already has permission:\n    ${deviceName(it)}")
					}
				}
			}
		}
		log("SCAN", "finished")
	}
	
	/**
	 * request USB permission for specific device
	 */
	private fun requestUsbPermission(manager: UsbManager, device: UsbDevice) {
		log("USB permission", "request\n    ${deviceName(device)}")
		runOnUiThread {
			var flags = 0
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				flags = flags or PendingIntent.FLAG_MUTABLE
			}
			val permissionIntent = PendingIntent.getBroadcast(this, REQ_PERMISSION_USB,
				 Intent(ACTION_USB_PERMISSION)
				 	.setPackage(packageName),
				flags)
			manager.requestPermission(device, permissionIntent)
		}
	}
	
	/**
	 * check whether the specific device or one of its interfaces is VIDEO class
	 */
	private fun isUVC(device: UsbDevice?): Boolean {
		var result = false
		if (device != null) {
			if (device.deviceClass == UsbConstants.USB_CLASS_VIDEO) {
				result = true
			} else {
				loop@ for (i in 0..device.interfaceCount) {
					val iface = device.getInterface(i)
					if (iface.interfaceClass == UsbConstants.USB_CLASS_VIDEO) {
						result = true
						break@loop
					}
				}
			}
		}
		return result
	}

	/**
	 * get device name of specific UsbDevice
	 * return productName if it is available else return deviceName
	 */
	private fun deviceName(device: UsbDevice?): String {
		var result = "device is null"
		if (device != null) {
			result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				if (!TextUtils.isEmpty(device.productName)) device.productName!! else device.deviceName
			} else {
				device.deviceName
			}
		}
		return result
	}
	
//--------------------------------------------------------------------------------
	/**
	 * add message to TextView
	 */
	private fun log(tag: String, msg: String) {
		runOnUiThread {
			logTv.append("$tag:\n    $msg\n")
			mLogScrollView.scrollTo(0, logTv.bottom)
		}
	}

//--------------------------------------------------------------------------------
	companion object {
		private const val DEBUG = true
		private val TAG = MainActivity::class.simpleName
		private const val ACTION_USB_PERMISSION = "ACTION_USB_PERMISSION"
		private const val REQ_PERMISSION_USB = 16
		private const val REQ_PERMISSION_CAMERA = 18
	}
}
