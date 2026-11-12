package com.personal.wallet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

/** صفحه ورود: پس‌زمینه‌ی گرادیانی، لوگوی برنامه، نقطه‌های رمز با انیمیشن، لرزش هنگام رمز اشتباه، صفحه‌کلید گرد */
@Composable
fun LockScreen(settings: AppSettings, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var errorCount by remember { mutableStateOf(0) }
    val shake = remember { Animatable(0f) }
    val bioReady = settings.fingerprint && canUseBiometric(context)

    fun tryBiometric() {
        val act = context as? FragmentActivity ?: return
        showBiometricPrompt(act, onUnlocked)
    }
    LaunchedEffect(Unit) { if (bioReady) tryBiometric() }

    // رمز اشتباه: لرزش کوتاه، سپس پاک شدن نقطه‌ها
    LaunchedEffect(errorCount) {
        if (errorCount > 0) {
            repeat(3) {
                shake.animateTo(14f, tween(45))
                shake.animateTo(-14f, tween(45))
            }
            shake.animateTo(0f, tween(45))
            delay(120)
            pin = ""
            wrong = false
        }
    }

    fun press(d: String) {
        if (wrong || pin.length >= 4) return
        pin += d
        if (pin.length == 4) {
            if (settings.checkPin(pin)) {
                onUnlocked()
            } else {
                wrong = true
                errorCount++
            }
        }
    }

    StatusBarColor(Color(0xFF0B1226))
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1226), Color(0xFF0F1B3F), Color(0xFF080C19))))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x388BE04E), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.08f), radius = size.minDimension * 0.75f
                    ),
                    radius = size.minDimension * 0.75f, center = Offset(size.width * 0.85f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x2626C6DA), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.95f), radius = size.minDimension * 0.7f
                    ),
                    radius = size.minDimension * 0.7f, center = Offset(size.width * 0.05f, size.height * 0.95f)
                )
            }
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            val logoShape = RoundedCornerShape(32.dp)
            Box(
                Modifier
                    .size(116.dp)
                    .shadow(24.dp, logoShape, ambientColor = Color(0xFF8BE04E), spotColor = Color(0xFF8BE04E))
                    .clip(logoShape)
                    .border(1.dp, Color(0x55FFFFFF), logoShape),
                contentAlignment = Alignment.Center
            ) {
                // همان آیکون برنامه؛ لایه‌ها بزرگ‌تر از قاب کشیده و برش داده می‌شوند تا نقاشی درشت دیده شود
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(172.dp))
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(172.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("کیف پول", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (wrong) "رمز اشتباه است" else "برای ورود، رمز خود را وارد کنید",
                color = if (wrong) WColors.Expense else Color(0xFF9FB0D6),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(26.dp))
            Row(
                Modifier.offset(x = shake.value.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                repeat(4) { i -> PinDot(filled = i < pin.length, wrong = wrong) }
            }
            Spacer(Modifier.weight(0.8f))

            Ltr {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("fp", "0", "del")).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            row.forEach { k ->
                                when (k) {
                                    "fp" -> {
                                        if (bioReady) {
                                            PadKey(onClick = { tryBiometric() }, filled = false) {
                                                Icon(Icons.Filled.Fingerprint, null, tint = Color(0xFF7DB0FF), modifier = Modifier.size(34.dp))
                                            }
                                        } else {
                                            Spacer(Modifier.size(76.dp))
                                        }
                                    }
                                    "del" -> PadKey(onClick = { if (!wrong && pin.isNotEmpty()) pin = pin.dropLast(1) }, filled = false) {
                                        Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = Color(0xFFB8C4E0), modifier = Modifier.size(28.dp))
                                    }
                                    else -> PadKey(onClick = { press(k) }, filled = true) {
                                        Text(k, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("داده‌ها فقط روی همین گوشی ذخیره می‌شوند", color = Color(0xFF62729C), fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PinDot(filled: Boolean, wrong: Boolean) {
    val fill by animateColorAsState(
        targetValue = when {
            wrong -> WColors.Expense
            filled -> Color(0xFF7DB0FF)
            else -> Color.Transparent
        },
        animationSpec = tween(140),
        label = "pinDot"
    )
    val border = if (wrong) WColors.Expense else if (filled) Color(0xFF7DB0FF) else Color(0xFF4A5C8C)
    Box(
        Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(fill)
            .border(BorderStroke(1.6.dp, border), CircleShape)
    )
}

@Composable
private fun PadKey(onClick: () -> Unit, filled: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(if (filled) Color(0x1AFFFFFF) else Color.Transparent)
            .then(if (filled) Modifier.border(1.dp, Color(0x1FFFFFFF), CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
