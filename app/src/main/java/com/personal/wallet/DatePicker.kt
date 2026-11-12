package com.personal.wallet

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar

/**
 * انتخاب تاریخ؛ بسته به تنظیم برنامه یا دیالوگ شمسی (سال / ماه / روز) یا دیالوگ استاندارد میلادی.
 * ساعتِ روزِ اصلی دست‌نخورده می‌ماند.
 */
@Composable
fun DatePickerHost(
    show: Boolean,
    initialMillis: Long,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    if (CalendarPrefs.jalali) {
        JalaliDatePickerDialog(initialMillis, onPicked, onDismiss)
    } else {
        val context = LocalContext.current
        DisposableEffect(Unit) {
            val c = Calendar.getInstance().also { it.timeInMillis = initialMillis }
            val dialog = DatePickerDialog(
                context,
                { _, y, m, d ->
                    val r = Calendar.getInstance()
                    r.timeInMillis = initialMillis
                    r.set(y, m, d)
                    onPicked(r.timeInMillis)
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setOnDismissListener { onDismiss() }
            dialog.show()
            onDispose { dialog.setOnDismissListener(null); dialog.dismiss() }
        }
    }
}

@Composable
fun JalaliDatePickerDialog(initialMillis: Long, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val init = remember { Jalali.fromMillis(initialMillis) }
    var year by remember { mutableStateOf(init.year) }
    var month by remember { mutableStateOf(init.month) }
    var day by remember { mutableStateOf(init.day) }

    val maxDay = Jalali.monthLength(year, month)
    if (day > maxDay) day = maxDay

    val years = ((init.year - 40)..(init.year + 10)).map { it.toString() to it.toString().toPersianDigits() }
    val months = Jalali.monthNames.mapIndexed { i, n -> (i + 1).toString() to n }
    val days = (1..maxDay).map { it.toString() to it.toString().toPersianDigits() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WColors.Card,
        title = { Text("انتخاب تاریخ (شمسی)", color = Color.White) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField("سال", year.toString().toPersianDigits(), years, { year = it.toInt() }, Modifier.weight(1f))
                    DropdownField("ماه", Jalali.monthNames[month - 1], months, { month = it.toInt() }, Modifier.weight(1.3f))
                    DropdownField("روز", day.toString().toPersianDigits(), days, { day = it.toInt() }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "امروز: " + formatDate(System.currentTimeMillis()),
                    color = WColors.Muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPicked(Jalali.toMillis(year, month, day, initialMillis)) }) {
                Text("تأیید", color = WColors.Blue)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = WColors.Muted) } }
    )
}
