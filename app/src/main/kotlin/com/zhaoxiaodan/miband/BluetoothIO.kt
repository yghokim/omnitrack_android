package com.zhaoxiaodan.miband

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.zhaoxiaodan.miband.listeners.NotifyListener
import com.zhaoxiaodan.miband.model.Profile
import java.util.*

internal class BluetoothIO : BluetoothGattCallback() {
    var gatt: BluetoothGatt? = null
    var currentCallback: ActionCallback? = null

    var notifyListeners = HashMap<UUID, NotifyListener?>()
    var _disconnectedListener: NotifyListener? = null

    fun connect(context: Context, device: BluetoothDevice, callback: ActionCallback) {
        this@BluetoothIO.currentCallback = callback
        device.connectGatt(context, false, this@BluetoothIO)
    }

    fun setDisconnectedListener(disconnectedListener: NotifyListener) {
        this._disconnectedListener = disconnectedListener
    }

    val device: BluetoothDevice?
        get() {
            if (null == gatt) {
                Log.e(TAG, "connect to miband first")
                return null
            }
            return gatt!!.device
        }

    fun writeAndRead(uuid: UUID, valueToWrite: ByteArray, callback: ActionCallback) {
        val readCallback = object : ActionCallback {

            override fun onSuccess(characteristic: Any?) {
                this@BluetoothIO.readCharacteristic(uuid, callback)
            }

            override fun onFail(errorCode: Int, msg: String) {
                callback.onFail(errorCode, msg)
            }
        }
        this.writeCharacteristic(uuid, valueToWrite, readCallback)
    }

    fun writeCharacteristic(characteristicUUID: UUID, value: ByteArray, callback: ActionCallback?) {
        writeCharacteristic(Profile.UUID_SERVICE_MILI, characteristicUUID, value, callback)
    }

    fun writeCharacteristic(serviceUUID: UUID, characteristicUUID: UUID, value: ByteArray, callback: ActionCallback?) {
        try {
            if (null == gatt) {
                Log.e(TAG, "connect to miband first")
                throw Exception("connect to miband first")
            }
            this.currentCallback = callback
            val chara = gatt!!.getService(serviceUUID).getCharacteristic(characteristicUUID)
            if (null == chara) {
                this.onFail(-1, "BluetoothGattCharacteristic $characteristicUUID is not exsit")
                return
            }
            chara.value = value
            if (false == this.gatt!!.writeCharacteristic(chara)) {
                this.onFail(-1, "gatt.writeCharacteristic() return false")
            }
        } catch (tr: Throwable) {
            Log.e(TAG, "writeCharacteristic", tr)
            this.onFail(-1, tr.message ?: "")
        }

    }

    fun readCharacteristic(serviceUUID: UUID, uuid: UUID, callback: ActionCallback) {
        try {
            if (null == gatt) {
                Log.e(TAG, "connect to miband first")
                throw Exception("connect to miband first")
            }
            this.currentCallback = callback
            val chara = gatt!!.getService(serviceUUID).getCharacteristic(uuid)
            if (null == chara) {
                this.onFail(-1, "BluetoothGattCharacteristic $uuid is not exsit")
                return
            }
            if (false == this.gatt!!.readCharacteristic(chara)) {
                this.onFail(-1, "gatt.readCharacteristic() return false")
            }
        } catch (tr: Throwable) {
            Log.e(TAG, "readCharacteristic", tr)
            this.onFail(-1, tr.message ?: "")
        }

    }

    fun readCharacteristic(uuid: UUID, callback: ActionCallback) {
        this.readCharacteristic(Profile.UUID_SERVICE_MILI, uuid, callback)
    }

    fun readRssi(callback: ActionCallback) {
        try {
            if (null == gatt) {
                Log.e(TAG, "connect to miband first")
                throw Exception("connect to miband first")
            }
            this.currentCallback = callback
            this.gatt!!.readRemoteRssi()
        } catch (tr: Throwable) {
            Log.e(TAG, "readRssi", tr)
            this.onFail(-1, tr.message ?: "")
        }

    }

    fun setNotifyListener(serviceUUID: UUID, characteristicId: UUID, listener: NotifyListener?) {
        if (null == gatt) {
            Log.e(TAG, "connect to miband first")
            return
        }

        val chara = gatt!!.getService(serviceUUID).getCharacteristic(characteristicId)
        if (chara == null) {
            Log.e(TAG, "characteristicId " + characteristicId.toString() + " not found in service " + serviceUUID.toString())
            return
        }


        this.gatt!!.setCharacteristicNotification(chara, true)
        val descriptor = chara.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        this.gatt!!.writeDescriptor(descriptor)
        this.notifyListeners.put(characteristicId, listener)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            gatt.close()
            if (this._disconnectedListener != null)
                this._disconnectedListener!!.onNotify(null)
        }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        if (BluetoothGatt.GATT_SUCCESS == status) {
            this.onSuccess(characteristic)
        } else {
            this.onFail(status, "onCharacteristicRead fail")
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        if (BluetoothGatt.GATT_SUCCESS == status) {
            this.onSuccess(characteristic)
        } else {
            this.onFail(status, "onCharacteristicWrite fail")
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        if (BluetoothGatt.GATT_SUCCESS == status) {
            this.onSuccess(rssi)
        } else {
            this.onFail(status, "onCharacteristicRead fail")
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            this.gatt = gatt
            this.onSuccess(null)
        } else {
            this.onFail(status, "onServicesDiscovered fail")
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)
        println(characteristic.toString())
        if (this.notifyListeners.containsKey(characteristic.uuid)) {
            this.notifyListeners[characteristic.uuid]?.onNotify(characteristic.value)
        }
    }

    private fun onSuccess(data: Any?) {
        if (this.currentCallback != null) {
            val callback = this.currentCallback
            this.currentCallback = null
            callback!!.onSuccess(data)
        }
    }

    private fun onFail(errorCode: Int, msg: String) {
        if (this.currentCallback != null) {
            val callback = this.currentCallback
            this.currentCallback = null
            callback!!.onFail(errorCode, msg)
        }
    }

    companion object {
        private val TAG = "BluetoothIO"
    }

}
