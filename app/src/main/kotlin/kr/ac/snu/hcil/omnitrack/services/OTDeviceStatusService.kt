package kr.ac.snu.hcil.omnitrack.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.system.OTExternalSettingsPrompter


class OTDeviceStatusService : Service() {

    companion object {

        private val batteryStatusIntentFilter: IntentFilter by lazy {
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        }

        fun getBatteryStatus(context: Context): Intent = context.registerReceiver(null, batteryStatusIntentFilter)

        fun isBatteryCharging(batteryStatus: Intent): Boolean {
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }

        fun isBatteryCharging(context: Context): Boolean {
            return isBatteryCharging(getBatteryStatus(context))
        }

        fun getBatteryPercentage(batteryStatus: Intent): Float {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            return level / scale.toFloat()
        }
    }

    private val receiver = PowerReceiver()
    private val intentFilter = IntentFilter()
            .apply {
                addAction(Intent.ACTION_BATTERY_OKAY)
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiver, intentFilter)

        OTApp.logger.writeSystemLog("BatteryStatusService created", PowerReceiver.TAG)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)

        OTApp.logger.writeSystemLog("BatteryStatusService destroyed", PowerReceiver.TAG)

        if (OTExternalSettingsPrompter.isBatteryOptimizationWhiteListed(this)) {
            this.startService(Intent(this, OTDeviceStatusService::class.java))
        } else {
            OTApp.logger.writeSystemLog("reserve starting batteryStatusService for next foreground enter.", PowerReceiver.TAG)
            (application as OTApp).isDeviceStateServiceStartReserved = true
        }
    }

    class PowerReceiver : BroadcastReceiver() {
        companion object {
            const val TAG = "PowerReceiver"
        }

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_LOW -> {
                    OTApp.logger.writeSystemLog("Battery Low", TAG)
                    //Toast.makeText(context, "Battery is low", Toast.LENGTH_SHORT).show()
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    OTApp.logger.writeSystemLog("Battery Okay", TAG)
                    //Toast.makeText(context, "Battery is okay", Toast.LENGTH_SHORT).show()
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    OTApp.logger.writeSystemLog("Power connected", TAG)
                    onDeviceActive(context)
                    //Toast.makeText(context, "Power connected", Toast.LENGTH_SHORT).show()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    OTApp.logger.writeSystemLog("Power disconnected", TAG)
                    //Toast.makeText(context, "Power disconnected", Toast.LENGTH_SHORT).show()
                }
                Intent.ACTION_SCREEN_ON -> {
                    OTApp.logger.writeSystemLog("Screen on", TAG)
                    onDeviceActive(context)
                    //Toast.makeText(context, "Screen on", Toast.LENGTH_SHORT).show()
                }

                Intent.ACTION_SCREEN_OFF -> {
                    OTApp.logger.writeSystemLog("Screen off", TAG)
                    //Toast.makeText(context, "Screen off", Toast.LENGTH_SHORT).show()
                }
            }

            when (intent.action) {
                Intent.ACTION_BATTERY_LOW, Intent.ACTION_BATTERY_OKAY, Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> {
                    val eventLogger = (context.applicationContext as OTAndroidApp).applicationComponent.getEventLogger()
                    val batteryStatus = getBatteryStatus(context)

                    val batteryPercentage = getBatteryPercentage(batteryStatus)
                    val isCharging = isBatteryCharging(batteryStatus)

                    OTApp.logger.writeSystemLog("Battery percentage: $batteryPercentage, isCharging: $isCharging", TAG)
                    eventLogger.logDeviceStatusChangeEvent(
                            when (intent.action) {
                                Intent.ACTION_BATTERY_LOW -> IEventLogger.SUB_DEVICE_BATTERY_LOW
                                Intent.ACTION_BATTERY_OKAY -> IEventLogger.SUB_DEVICE_BATTERY_OKAY
                                Intent.ACTION_POWER_CONNECTED -> IEventLogger.SUB_DEVICE_PLUGGED
                                Intent.ACTION_POWER_DISCONNECTED -> IEventLogger.SUB_DEVICE_UNPLUGGED
                                else -> intent.action
                            }, batteryPercentage)
                }
            }
        }

        private fun onDeviceActive(context: Context) {
            //Do Samsung-specific optimization tweaking
            if (Build.MANUFACTURER == "samsung") {
                val triggerSystemManager = (context.applicationContext as OTAndroidApp).applicationComponent.getTriggerSystemManager()
                triggerSystemManager.get().refreshReservedAlarms()
            }
        }
    }

}