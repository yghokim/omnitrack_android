package com.zhaoxiaodan.miband

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.util.Log
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener
import com.zhaoxiaodan.miband.listeners.NotifyListener
import com.zhaoxiaodan.miband.listeners.RealtimeStepsNotifyListener
import com.zhaoxiaodan.miband.model.*
import java.util.*

class MiBand(private val context: Context) {
    private val io: BluetoothIO

    init {
        this.io = BluetoothIO()
    }

    /**
     * 连接指定的手环

     * @param callback
     */
    fun connect(device: BluetoothDevice, callback: ActionCallback) {
        this.io.connect(context, device, callback)
    }

    fun setDisconnectedListener(disconnectedListener: NotifyListener) {
        this.io.setDisconnectedListener(disconnectedListener)
    }

    /**
     * 和手环配对, 实际用途未知, 不配对也可以做其他的操作

     * @return data = null
     */
    fun pair(callback: ActionCallback) {
        val ioCallback = object : ActionCallback {

            override fun onSuccess(data: Any?) {
                val characteristic = data as BluetoothGattCharacteristic
                Log.d(TAG, "pair result " + Arrays.toString(characteristic.value))
                if (characteristic.value.size == 1 && characteristic.value[0].toInt() == 2) {
                    callback.onSuccess(null)
                } else {
                    callback.onFail(-1, "respone values no succ!")
                }
            }

            override fun onFail(errorCode: Int, msg: String) {
                callback.onFail(errorCode, msg)
            }
        }
        this.io.writeAndRead(Profile.UUID_CHAR_PAIR, Protocol.PAIR, ioCallback)
    }

    val device: BluetoothDevice?
        get() = this.io.device

    /**
     * 读取和连接设备的信号强度RSSI值

     * @param callback
     * *
     * @return data : int, rssi值
     */
    fun readRssi(callback: ActionCallback) {
        this.io.readRssi(callback)
    }

    /**
     * 读取手环电池信息

     * @return [BatteryInfo]
     */
    fun getBatteryInfo(callback: ActionCallback) {
        val ioCallback = object : ActionCallback {

            override fun onSuccess(data: Any?) {
                val characteristic = data as BluetoothGattCharacteristic
                Log.d(TAG, "getBatteryInfo result " + Arrays.toString(characteristic.value))
                if (characteristic.value.size == 10) {
                    val info = BatteryInfo.fromByteData(characteristic.value)
                    callback.onSuccess(info)
                } else {
                    callback.onFail(-1, "result format wrong!")
                }
            }

            override fun onFail(errorCode: Int, msg: String) {
                callback.onFail(errorCode, msg)
            }
        }
        this.io.readCharacteristic(Profile.UUID_CHAR_BATTERY, ioCallback)
    }

    /**
     * 让手环震动
     */
    fun startVibration(mode: VibrationMode) {
        val protocal: ByteArray
        when (mode) {
            VibrationMode.VIBRATION_WITH_LED -> protocal = Protocol.VIBRATION_WITH_LED
            VibrationMode.VIBRATION_10_TIMES_WITH_LED -> protocal = Protocol.VIBRATION_10_TIMES_WITH_LED
            VibrationMode.VIBRATION_WITHOUT_LED -> protocal = Protocol.VIBRATION_WITHOUT_LED
        }
        this.io.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, protocal, null)
    }

    /**
     * 停止以模式Protocol.VIBRATION_10_TIMES_WITH_LED 开始的震动
     */
    fun stopVibration() {
        this.io.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, Protocol.STOP_VIBRATION, null)
    }

    fun setNormalNotifyListener(listener: NotifyListener) {
        this.io.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_NOTIFICATION, listener)
    }

    /**
     * 重力感应器数据通知监听, 设置完之后需要另外使用 [MiBand.enableRealtimeStepsNotify] 开启 和
     * [MiBand] 关闭通知
     * @param listener
     */
    fun setSensorDataNotifyListener(listener: NotifyListener) {
        this.io.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_SENSOR_DATA, object : NotifyListener {
            override fun onNotify(data: ByteArray?) {
                listener.onNotify(data)
            }
        })
    }

    /**
     * 开启重力感应器数据通知
     */
    fun enableSensorDataNotify() {
        this.io.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, Protocol.ENABLE_SENSOR_DATA_NOTIFY, null)
    }

    /**
     * 关闭重力感应器数据通知
     */
    fun disableSensorDataNotify() {
        this.io.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, Protocol.DISABLE_SENSOR_DATA_NOTIFY, null)
    }

    /**
     * 实时步数通知监听器, 设置完之后需要另外使用 [MiBand.enableRealtimeStepsNotify] 开启 和
     *
     * @param listener
     */
    fun setRealtimeStepsNotifyListener(listener: RealtimeStepsNotifyListener) {
        this.io.setNotifyListener(Profile.UUID_SERVICE_MILI, Profile.UUID_CHAR_REALTIME_STEPS, object : NotifyListener {
            override fun onNotify(data: ByteArray?) {
                Log.d(TAG, Arrays.toString(data))
                if (data?.size == 4) {
                    val steps = data[3].toInt() shl 24 or (data[2].toInt() and 0xFF) shl 16 or (data[1].toInt() and 0xFF) shl 8 or (data[0].toInt() and 0xFF)
                    listener.onNotify(steps)
                }
            }
        })
    }

    /**
     * 开启实时步数通知
     */
    fun enableRealtimeStepsNotify() {
        this.io.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, Protocol.ENABLE_REALTIME_STEPS_NOTIFY, null)
    }

    /**
     * 关闭实时步数通知
     */
    fun disableRealtimeStepsNotify() {
        this.io.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, Protocol.DISABLE_REALTIME_STEPS_NOTIFY, null)
    }

    /**
     * 设置led灯颜色
     */
    fun setLedColor(color: LedColor) {
        val protocal: ByteArray
        when (color) {
            LedColor.RED -> protocal = Protocol.SET_COLOR_RED
            LedColor.BLUE -> protocal = Protocol.SET_COLOR_BLUE
            LedColor.GREEN -> protocal = Protocol.SET_COLOR_GREEN
            LedColor.ORANGE -> protocal = Protocol.SET_COLOR_ORANGE
        }
        this.io.writeCharacteristic(Profile.UUID_CHAR_CONTROL_POINT, protocal, null)
    }

    /**
     * 设置用户信息

     * @param userInfo
     */
    fun setUserInfo(userInfo: UserInfo) {
        val device = this.io.device
        val data = userInfo.getBytes(device!!.address)
        this.io.writeCharacteristic(Profile.UUID_CHAR_USER_INFO, data, null)
    }

    fun showServicesAndCharacteristics() {
        for (service in this.io.gatt!!.services) {
            Log.d(TAG, "onServicesDiscovered:" + service.uuid)

            for (characteristic in service.characteristics) {
                Log.d(TAG, "  char:" + characteristic.uuid)

                for (descriptor in characteristic.descriptors) {
                    Log.d(TAG, "    descriptor:" + descriptor.uuid)
                }
            }
        }
    }

    fun setHeartRateScanListener(listener: HeartRateNotifyListener) {
        this.io.setNotifyListener(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_NOTIFICATION_HEARTRATE,
                object : NotifyListener {
                    override fun onNotify(data: ByteArray?) {
                        Log.d(TAG, Arrays.toString(data))
                        if (data != null) {
                            if (data.size == 2 && data[0].toInt() == 6) {
                                val heartRate = data[1].toInt() and 0xFF
                                listener.onNotify(heartRate)
                            }
                        }
                    }

                })
    }

    fun startHeartRateScan() {

        this@MiBand.io.writeCharacteristic(Profile.UUID_SERVICE_HEARTRATE, Profile.UUID_CHAR_HEARTRATE, Protocol.START_HEART_RATE_SCAN, null)
    }

    companion object {

        private val TAG = "miband-android"

        fun startScan(callback: ScanCallback) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (null == adapter) {
                Log.e(TAG, "BluetoothAdapter is null")
                return
            }
            val scanner = adapter.bluetoothLeScanner
            if (null == scanner) {
                Log.e(TAG, "BluetoothLeScanner is null")
                return
            }
            scanner.startScan(callback)
        }

        fun stopScan(callback: ScanCallback) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (null == adapter) {
                Log.e(TAG, "BluetoothAdapter is null")
                return
            }
            val scanner = adapter.bluetoothLeScanner
            if (null == scanner) {
                Log.e(TAG, "BluetoothLeScanner is null")
                return
            }
            scanner.stopScan(callback)
        }
    }

}
