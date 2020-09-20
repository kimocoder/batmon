package dev.kdrag0n.batterymonitor.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.templates.ControlTemplate
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.StatelessTemplate
import androidx.annotation.RequiresApi
import dev.kdrag0n.batterymonitor.R
import dev.kdrag0n.batterymonitor.ui.MainActivity
import dev.kdrag0n.batterymonitor.utils.TextUtils
import dev.kdrag0n.batterymonitor.utils.setColoredStatusText
import dev.kdrag0n.batterymonitor.utils.setColoredTitle
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import org.reactivestreams.FlowAdapters
import timber.log.Timber
import java.util.concurrent.Flow
import java.util.function.Consumer

private const val ACTIVE_CONTROL_REQUEST_CODE = 100
private const val ACTIVE_CONTROL_ID = "active_drain"

private const val IDLE_CONTROL_REQUEST_CODE = 101
private const val IDLE_CONTROL_ID = "idle_drain"

private const val BATTERY_CONTROL_REQUEST_CODE = 103
private const val BATTERY_CONTROL_ID = "battery_level"

private const val CLEAR_CONTROL_REQUEST_CODE = 102
private const val CLEAR_CONTROL_ID = "clear_stats"

@RequiresApi(Build.VERSION_CODES.R)
class ControlService : ControlsProviderService() {
    private lateinit var updatePublisher: ReplayProcessor<Control>

    private val pi by lazy {
        PendingIntent.getActivity(
            baseContext, ACTIVE_CONTROL_REQUEST_CODE,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val activeControl by lazy {
        Control.StatefulBuilder(ACTIVE_CONTROL_ID, pi)
            .setTitle(getString(R.string.control_active_drain_title))
            .setDeviceType(DeviceTypes.TYPE_LIGHT)
            .setZone("Status")
            .setStatus(Control.STATUS_OK)
            .setColoredStatusText(getString(R.string.percent_per_hour, 7.926), getColor(R.color.pastel_yellow))
            .setCustomIcon(Icon.createWithResource(this, R.drawable.ic_outline_brightness_high_24))
            .setSubtitle("in 10h 48m")
            .setControlTemplate(ControlTemplate.getNoTemplateObject())
            .build()
    }

    private val idleControl by lazy {
        Control.StatefulBuilder(IDLE_CONTROL_ID, pi)
            .setTitle(getString(R.string.control_idle_drain_title))
            .setDeviceType(DeviceTypes.TYPE_CURTAIN)
            .setZone("Status")
            .setStatus(Control.STATUS_OK)
            .setColoredStatusText(getString(R.string.percent_per_hour, 0.317), getColor(R.color.pastel_blue))
            .setCustomIcon(Icon.createWithResource(this, R.drawable.ic_outline_bedtime_24))
            .setSubtitle("in 2d 3h")
            .build()
    }

    private val batteryControl by lazy {
        Control.StatefulBuilder(BATTERY_CONTROL_ID, pi)
            .setColoredTitle(getString(R.string.control_battery_title), getColor(R.color.pastel_orange))
            .setDeviceType(DeviceTypes.TYPE_REMOTE_CONTROL)
            .setZone("Status")
            .setStatus(Control.STATUS_OK)
            .setSubtitle(getString(R.string.control_battery_subtitle, 0.0015))
            .setControlTemplate(RangeTemplate(BATTERY_CONTROL_ID, 0f, 100f, 44183f/65535f*100f, 1/65535f, TextUtils.createColoredSpan("%.3f%%", getColor(R.color.pastel_orange))))
            .setCustomIcon(Icon.createWithResource(this, R.drawable.ic_outline_battery_full_24))
            .build()
    }

    private val clearControl by lazy {
        Control.StatefulBuilder(CLEAR_CONTROL_ID, pi)
            .setColoredTitle(getString(R.string.control_clear_title), getColor(R.color.pastel_red))
            .setDeviceType(DeviceTypes.TYPE_SPRINKLER)
            .setZone("Actions")
            .setStatus(Control.STATUS_OK)
            .setSubtitle(getString(R.string.control_clear_subtitle))
            .setControlTemplate(StatelessTemplate(CLEAR_CONTROL_ID))
            .setCustomIcon(Icon.createWithResource(this, R.drawable.ic_outline_delete_24))
            .build()
    }

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        Timber.i("create all pubs")
        val controls = listOf(
            activeControl,
            idleControl,
            batteryControl,
            clearControl
        )

        return FlowAdapters.toFlowPublisher(Flowable.fromIterable(controls))
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        Timber.i("create pub for $controlIds")
        updatePublisher = ReplayProcessor.create()

        if (controlIds.contains(ACTIVE_CONTROL_ID)) {
            updatePublisher.onNext(activeControl)
        }

        if (controlIds.contains(IDLE_CONTROL_ID)) {
            updatePublisher.onNext(idleControl)
        }

        if (controlIds.contains(BATTERY_CONTROL_ID)) {
            updatePublisher.onNext(batteryControl)
        }

        if (controlIds.contains(CLEAR_CONTROL_ID)) {
            updatePublisher.onNext(clearControl)
        }

        return FlowAdapters.toFlowPublisher(updatePublisher)
    }

    override fun performControlAction(controlId: String, action: ControlAction, consumer: Consumer<Int>) {
        consumer.accept(ControlAction.RESPONSE_OK)

        if (action is BooleanAction) {
        }
    }
}