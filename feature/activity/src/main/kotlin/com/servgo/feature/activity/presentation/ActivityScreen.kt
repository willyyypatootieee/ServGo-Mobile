package com.servgo.feature.activity.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servgo.shared.database.entity.BookingEntity
import com.servgo.shared.database.entity.ServiceTypeEntity
import com.servgo.shared.database.entity.TrackingStepEntity
import com.servgo.shared.database.entity.VehicleEntity
import com.servgo.shared.designsystem.components.ServGoMetricCard
import com.servgo.shared.designsystem.components.ServGoStatusChip
import com.servgo.shared.designsystem.components.ServGoTimelineItem
import com.servgo.shared.designsystem.theme.ServGoAccent
import com.servgo.shared.designsystem.theme.ServGoBorder
import com.servgo.shared.designsystem.theme.ServGoCard
import com.servgo.shared.designsystem.theme.ServGoPrimary
import com.servgo.shared.designsystem.theme.ServGoTextPrimary
import com.servgo.shared.designsystem.theme.ServGoTextSecondary
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────
// ActivityScreen — Booking & Tracking Servis
//
// Arsitektur: MVP
//   • View    : ActivityScreen + sub-flow composables
//   • Presenter: BookingViewModel (StateFlow<BookingState>)
//   • Model   : BookingState, BookingEntity, ServiceTypeEntity
//
// Modularisasi: :feature:activity
// Bergantung pada: :shared:database, :shared:designsystem
//
// UX Laws:
//   • Fitts's Law       → CTA utama (Konfirmasi Booking) full-width tinggi 52dp
//   • Progress Visibility → Timeline tracking real-time terlihat jelas
//   • Hick's Law        → Max 4 sub-flow visible sekaligus
//   • Chunking          → Tiap flow dipecah ke halaman terpisah
//   • Visibility of Status → Status booking aktif selalu ditampilkan
// ─────────────────────────────────────────────

/**
 * Layar booking servis dan tracking kendaraan.
 *
 * Mendukung sub-flow terpisah untuk:
 * - Ganti Wiper (pemilihan tipe & merk)
 * - Cek Aki (diagnostik live + penggantian)
 * - Isi Angin ban (target PSI + jenis gas)
 * - Cuci Mobil (paket premium)
 * - Konfirmasi & pembayaran (summary + metode bayar)
 *
 * @param preselectedService Sub-flow yang langsung dibuka (opsional).
 * @param viewModel [BookingViewModel] untuk state management.
 */
@Composable
fun ActivityScreen(
    preselectedService: String? = null,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Sub-flow state: null (standard booking), "Cuci", "Angin", "Aki", "Wiper", "Konfirmasi"
    var currentFlow by remember(preselectedService) { mutableStateOf(preselectedService) }
    
    var selectedVehicleIndex by remember { mutableIntStateOf(0) }
    var selectedServiceIndex by remember { mutableIntStateOf(0) }
    var complaintText by remember { mutableStateOf("") }
    
    // Configurations for sub-flows
    var wiperType by remember { mutableStateOf("Hybrid (Durable)") }
    var wiperBrand by remember { mutableStateOf("Bosch") }
    var wiperCost by remember { mutableStateOf(120000L) }

    var batteryBrand by remember { mutableStateOf("GS Astra Gold") }
    var batteryCost by remember { mutableStateOf(925000L) }

    var tireTargetPsi by remember { mutableFloatStateOf(32f) }
    var tireAirType by remember { mutableStateOf("Nitrogen Premium") }
    var tireCost by remember { mutableStateOf(40000L) }

    var washPackageName by remember { mutableStateOf("Premium Wash") }
    var washCost by remember { mutableStateOf(120000L) }
    
    var paymentMethod by remember { mutableStateOf("Dana") }
    
    val currentVehicle = state.vehicles.getOrNull(selectedVehicleIndex)

    LaunchedEffect(state.activeBooking?.id, state.trackingSteps.count { it.completed }) {
        val bookingId = state.activeBooking?.id ?: return@LaunchedEffect
        while (state.trackingSteps.any { !it.completed }) {
            delay(4000)
            if (state.activeBooking?.id == bookingId) {
                viewModel.advanceDummyProgress()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .statusBarsPadding()
    ) {
        // ── HERO HEADER — Solid Red with rounded bottom ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(ServGoPrimary)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentFlow != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { currentFlow = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (currentFlow) {
                            "Wiper" -> "Layanan Ganti Wiper"
                            "Aki" -> "Layanan Cek Aki"
                            "Angin" -> "Layanan Isi Angin"
                            "Cuci" -> "Layanan Cuci Mobil"
                            "Konfirmasi" -> "Konfirmasi & Pembayaran"
                            else -> "Booking Servis"
                        },
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = when (currentFlow) {
                            "Wiper" -> "Pilih tipe wiper sesuai model kendaraan"
                            "Aki" -> "Diagnostik kelistrikan aki dan alternator"
                            "Angin" -> "Atur target tekanan ban dan jenis gas"
                            "Cuci" -> "Paket cuci premium dengan pembersihan mesin"
                            "Konfirmasi" -> "Rincian biaya, lokasi bengkel, dan pembayaran"
                            else -> "Buat jadwal perawatan di bengkel rekanan"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Sub-flows implementation
        when (currentFlow) {
            "Wiper" -> WiperSubFlow(
                onNext = { type, brand, cost ->
                    wiperType = type
                    wiperBrand = brand
                    wiperCost = cost
                    currentFlow = "Konfirmasi"
                }
            )
            "Aki" -> BatterySubFlow(
                onNext = { brand, cost ->
                    batteryBrand = brand
                    batteryCost = cost
                    currentFlow = "Konfirmasi"
                }
            )
            "Angin" -> TireSubFlow(
                onNext = { targetPsi, airType, cost ->
                    tireTargetPsi = targetPsi
                    tireAirType = airType
                    tireCost = cost
                    currentFlow = "Konfirmasi"
                }
            )
            "Cuci" -> WashSubFlow(
                onNext = { packageName, cost ->
                    washPackageName = packageName
                    washCost = cost
                    currentFlow = "Konfirmasi"
                }
            )
            "Konfirmasi" -> {
                val serviceTitle = when (preselectedService) {
                    "Wiper" -> "Ganti Wiper ($wiperBrand)"
                    "Aki" -> "Cek Aki ($batteryBrand)"
                    "Angin" -> "Isi Angin ($tireAirType)"
                    "Cuci" -> "Cuci Mobil ($washPackageName)"
                    else -> "Servis Berkala Lainnya"
                }
                val serviceCost = when (preselectedService) {
                    "Wiper" -> wiperCost
                    "Aki" -> batteryCost
                    "Angin" -> tireCost
                    "Cuci" -> washCost
                    else -> 250000L
                }
                val serviceDetails = when (preselectedService) {
                    "Wiper" -> "Jenis Wiper: $wiperType. Merk: $wiperBrand."
                    "Aki" -> "Inspeksi kelistrikan dan pemasangan baru aki $batteryBrand."
                    "Angin" -> "Target tekanan ban: ${tireTargetPsi.toInt()} PSI dengan $tireAirType."
                    "Cuci" -> "Paket cuci kendaraan: $washPackageName. Termasuk vakum interior."
                    else -> "Pengecekan standar berkala."
                }
                
                ConfirmationSubFlow(
                    vehicle = currentVehicle,
                    title = serviceTitle,
                    details = serviceDetails,
                    cost = serviceCost,
                    paymentMethod = paymentMethod,
                    onPaymentMethodChange = { paymentMethod = it },
                    onPayNow = {
                        if (currentVehicle != null) {
                            val dummyService = ServiceTypeEntity(
                                id = "svc_quick_${System.currentTimeMillis()}",
                                name = serviceTitle,
                                description = serviceDetails,
                                estimatedCost = serviceCost,
                                estimatedDurationMinutes = 45
                            )
                            viewModel.createBooking(
                                vehicle = currentVehicle,
                                serviceType = dummyService,
                                complaint = serviceDetails,
                                scheduledAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                            )
                            currentFlow = null
                        }
                    }
                )
            }
            else -> {
                // Standard Booking & Tracking Flow
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Vehicle Selector Card
                    item {
                        Text("Pilih Kendaraan", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (state.vehicles.isEmpty()) {
                            EmptyBookingHelpCard()
                        } else {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.vehicles.forEachIndexed { index, item ->
                                    FilterChip(
                                        selected = selectedVehicleIndex == index,
                                        onClick = { selectedVehicleIndex = index },
                                        label = { Text("${item.name} (${item.licensePlate})") }
                                    )
                                }
                            }
                        }
                    }

                    // Service Type Selector
                    item {
                        Text("Tipe Layanan", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.serviceTypes.forEachIndexed { index, item ->
                                FilterChip(
                                    selected = selectedServiceIndex == index,
                                    onClick = { selectedServiceIndex = index },
                                    label = { Text(item.name) }
                                )
                            }
                        }
                    }

                    // Complaint Text Field
                    item {
                        Text("Keluhan Utama (Opsional)", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = complaintText,
                            onValueChange = { complaintText = it },
                            placeholder = { Text("Contoh: rem terasa bergetar, AC kurang dingin, dsb.") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Metric Pricing Summary
                    item {
                        val selectedServiceObj = state.serviceTypes.getOrNull(selectedServiceIndex)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ServGoMetricCard(
                                "Estimasi Biaya",
                                "Rp${selectedServiceObj?.estimatedCost?.formatRupiah() ?: "0"}",
                                "",
                                Modifier.weight(1f)
                            )
                            ServGoMetricCard(
                                "Durasi",
                                "${selectedServiceObj?.estimatedDurationMinutes ?: 0}",
                                "menit",
                                Modifier.weight(1f)
                            )
                        }
                    }

                    // Confirm Button
                    item {
                        val selectedServiceObj = state.serviceTypes.getOrNull(selectedServiceIndex)
                        Button(
                            onClick = {
                                if (currentVehicle != null && selectedServiceObj != null) {
                                    viewModel.createBooking(
                                        currentVehicle,
                                        selectedServiceObj,
                                        complaintText,
                                        System.currentTimeMillis() + 24L * 60L * 60L * 1000L
                                    )
                                    complaintText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ServGoPrimary),
                            enabled = currentVehicle != null && selectedServiceObj != null
                        ) {
                            Text(
                                text = when {
                                    currentVehicle == null -> "Tambah Kendaraan Dulu"
                                    selectedServiceObj == null -> "Pilih Layanan Dulu"
                                    else -> "Konfirmasi Booking"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Active Booking Card & Timeline
                    state.activeBooking?.let { booking ->
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Antrean Servis Berjalan", color = ServGoTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        item {
                            ActiveBookingCard(booking)
                        }
                        item {
                            Text("Tracking Timeline (Progres Real-time)", color = ServGoTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        items(state.trackingSteps.size) { index ->
                            val step = state.trackingSteps[index]
                            ServGoTimelineItem(
                                title = when (step.title) {
                                    "Menunggu Konfirmasi" -> "Menunggu Konfirmasi"
                                    "Diterima" -> "Booking Diterima"
                                    "Antrian" -> "Antrean Teknisi"
                                    "Inspeksi" -> "Inspeksi Visual"
                                    "Sedang Dikerjakan" -> "Dalam Pengerjaan"
                                    "Quality Check" -> "Quality Check"
                                    "Siap Diambil" -> "Siap Diambil"
                                    "Selesai" -> "Layanan Selesai"
                                    else -> step.title
                                },
                                note = step.note,
                                time = if (step.completed) "Baru saja" else "",
                                completed = step.completed,
                                isLast = index == state.trackingSteps.lastIndex
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sub-flow 1: Wiper Replacement Details
@Composable
private fun WiperSubFlow(
    onNext: (String, String, Long) -> Unit
) {
    var selectedType by remember { mutableStateOf("Hybrid") }
    var selectedBrand by remember { mutableStateOf("Bosch") }

    val wiperCosts = mapOf(
        "Flat Blade" to 180000L,
        "Hybrid" to 120000L,
        "Conventional" to 60000L
    )
    val cost = wiperCosts[selectedType] ?: 120000L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("PILIH TIPE WIPER", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Flat Blade", "Premium, performa aero-dinamis tinggi", "Rp180.000"),
                    Triple("Hybrid", "Sangat tahan lama, performa segala cuaca", "Rp120.000"),
                    Triple("Conventional", "Ekonomis, efisiensi pembersihan standar", "Rp60.000")
                ).forEach { wiper ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = wiper.first },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == wiper.first) Color(0xFFFEF2F2) else ServGoCard
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedType == wiper.first) ServGoPrimary else ServGoBorder
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedType == wiper.first,
                                onClick = { selectedType = wiper.first },
                                colors = RadioButtonDefaults.colors(selectedColor = ServGoPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(wiper.first, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                                Text(wiper.second, fontSize = 12.sp, color = ServGoTextSecondary)
                            }
                            Text(wiper.third, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                        }
                    }
                }
            }
        }

        item {
            Text("PILIH BRAND", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Bosch", "Denso", "Michelin", "Hella").forEach { brand ->
                    FilterChip(
                        selected = selectedBrand == brand,
                        onClick = { selectedBrand = brand },
                        label = { Text(brand) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onNext(selectedType, selectedBrand, cost) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ServGoPrimary)
            ) {
                Text("Pesan & Pasang Sekarang • Rp${cost.formatRupiah()}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// Sub-flow 2: Battery Testing & Replacement
@Composable
private fun BatterySubFlow(
    onNext: (String, Long) -> Unit
) {
    var selectedBrand by remember { mutableStateOf("GS Astra Gold") }
    val batteryCosts = mapOf(
        "GS Astra Gold" to 925000L,
        "Amaron Hi-Life" to 1150000L
    )
    val cost = batteryCosts[selectedBrand] ?: 925000L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Battery health diagnostics overview
        item {
            Text("DIAGNOSTIK AKTIF AKI (LIVE)", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ServGoMetricCard("Voltase", "12.6V", "Status Ok", Modifier.weight(1f))
                ServGoMetricCard("Kesehatan", "85%", "Kondisi Baik", Modifier.weight(1f))
                ServGoMetricCard("Suhu", "32°C", "Normal", Modifier.weight(1f))
            }
        }

        item {
            Text("REKOMENDASI AKI PENGGANTI", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("GS Astra Gold", "Aki kering bebas perawatan standar Indonesia", "Rp925.000"),
                    Triple("Amaron Hi-Life", "Aki premium asal India, performa & garansi lama", "Rp1.150.000")
                ).forEach { battery ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBrand = battery.first },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedBrand == battery.first) Color(0xFFFEF2F2) else ServGoCard
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedBrand == battery.first) ServGoPrimary else ServGoBorder
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedBrand == battery.first,
                                onClick = { selectedBrand = battery.first },
                                colors = RadioButtonDefaults.colors(selectedColor = ServGoPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(battery.first, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                                Text(battery.second, fontSize = 12.sp, color = ServGoTextSecondary)
                            }
                            Text(battery.third, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onNext(selectedBrand, cost) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ServGoPrimary)
            ) {
                Text("Pesan & Ganti Aki • Rp${cost.formatRupiah()}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// Sub-flow 3: Tire Inflation & Nitrogen selection
@Composable
private fun TireSubFlow(
    onNext: (Float, String, Long) -> Unit
) {
    var targetPsi by remember { mutableFloatStateOf(32f) }
    var airType by remember { mutableStateOf("Nitrogen Premium") }

    val airCosts = mapOf(
        "Nitrogen Premium" to 40000L,
        "Angin Biasa" to 10000L
    )
    val cost = airCosts[airType] ?: 40000L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("TEKANAN BAN SAAT INI", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Depan Kiri\n30 PSI", "Depan Kanan\n29 PSI", "Belakang Kiri\n32 PSI", "Belakang Kanan\n31 PSI").forEach { tire ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = ServGoCard),
                        border = BorderStroke(1.dp, ServGoBorder)
                    ) {
                        Text(
                            text = tire,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServGoTextPrimary
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TARGET TEKANAN", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${targetPsi.toInt()} PSI", color = ServGoPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Slider(
                value = targetPsi,
                onValueChange = { targetPsi = it },
                valueRange = 28f..38f,
                colors = SliderDefaults.colors(
                    thumbColor = ServGoPrimary,
                    activeTrackColor = ServGoPrimary
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "Eco Mode (30)" to 30f,
                    "Comfort (32)" to 32f,
                    "Full Load (35)" to 35f
                ).forEach { preset ->
                    OutlinedButton(
                        onClick = { targetPsi = preset.second },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (targetPsi == preset.second) ServGoPrimary else ServGoBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (targetPsi == preset.second) Color(0xFFFEF2F2) else Color.Transparent
                        )
                    ) {
                        Text(preset.first, fontSize = 11.sp, color = if (targetPsi == preset.second) ServGoPrimary else ServGoTextPrimary)
                    }
                }
            }
        }

        item {
            Text("JENIS ANGIN", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Nitrogen Premium", "Gas nitrogen murni untuk kestabilan ban dan suhu dingin", "Rp40.000 / 4 Ban"),
                    Triple("Angin Biasa", "Angin kompresor standar biasa", "Rp10.000 / 4 Ban")
                ).forEach { air ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { airType = air.first },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (airType == air.first) Color(0xFFFEF2F2) else ServGoCard
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (airType == air.first) ServGoPrimary else ServGoBorder
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = airType == air.first,
                                onClick = { airType = air.first },
                                colors = RadioButtonDefaults.colors(selectedColor = ServGoPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(air.first, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                                Text(air.second, fontSize = 12.sp, color = ServGoTextSecondary)
                            }
                            Text(air.third, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onNext(targetPsi, airType, cost) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ServGoPrimary)
            ) {
                Text("Pesan & Isi Angin • Rp${cost.formatRupiah()}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// Sub-flow 4: Car Wash Packages Selection
@Composable
private fun WashSubFlow(
    onNext: (String, Long) -> Unit
) {
    var selectedPackage by remember { mutableStateOf("Premium") }
    val packageCosts = mapOf(
        "Regular" to 50000L,
        "Premium" to 120000L,
        "Ultimate" to 350000L
    )
    val cost = packageCosts[selectedPackage] ?: 120000L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ServGoPrimary)
                    .padding(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = "Layanan Premium Wash",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Kilau Sempurna & Proteksi Cat Kendaraan",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Text("PILIH PAKET CUCI", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Regular", "Cuci salju + vacuum interior standar + poles ban", "Rp50.000"),
                    Triple("Premium", "Cuci salju + pembersihan mesin + vacuum + wax cair", "Rp120.000"),
                    Triple("Ultimate", "Premium wash + poles bodi + fogging anti bakteri", "Rp350.000")
                ).forEach { washPkg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPackage = washPkg.first },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPackage == washPkg.first) Color(0xFFFEF2F2) else ServGoCard
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedPackage == washPkg.first) ServGoPrimary else ServGoBorder
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPackage == washPkg.first,
                                onClick = { selectedPackage = washPkg.first },
                                colors = RadioButtonDefaults.colors(selectedColor = ServGoPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(washPkg.first, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                                Text(washPkg.second, fontSize = 12.sp, color = ServGoTextSecondary)
                            }
                            Text(washPkg.third, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onNext(selectedPackage, cost) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ServGoPrimary)
            ) {
                Text("Pesan Paket Cuci • Rp${cost.formatRupiah()}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// Sub-flow 5: Confirmation and Payment View
@Composable
private fun ConfirmationSubFlow(
    vehicle: VehicleEntity?,
    title: String,
    details: String,
    cost: Long,
    paymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    onPayNow: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vehicle & Service info summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ServGoCard),
                border = BorderStroke(1.dp, ServGoBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "RINGKASAN PESANAN",
                        color = ServGoPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ServGoTextPrimary)
                    Text("Kendaraan: ${vehicle?.name ?: "Belum dipilih"} (${vehicle?.licensePlate ?: "-"})", color = ServGoTextSecondary, fontSize = 13.sp)
                    Text(details, color = ServGoTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }

        // Location Info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ServGoCard),
                border = BorderStroke(1.dp, ServGoBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Lokasi",
                        tint = ServGoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Lokasi Bengkel", fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                        Text("ServGo Auto Darmo, Surabaya (ETA 9 Menit)", color = ServGoTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Payment Method Select
        item {
            Text("PILIH METODE PEMBAYARAN", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "Dana" to "Saldo e-wallet digital",
                    "OVO" to "Saldo e-wallet OVO",
                    "Bank Transfer" to "Transfer Bank Virtual Account"
                ).forEach { method ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPaymentMethodChange(method.first) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (paymentMethod == method.first) Color(0xFFFEF2F2) else ServGoCard
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (paymentMethod == method.first) ServGoPrimary else ServGoBorder
                        )
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = paymentMethod == method.first,
                                onClick = { onPaymentMethodChange(method.first) },
                                colors = RadioButtonDefaults.colors(selectedColor = ServGoPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(method.first, fontWeight = FontWeight.Bold, color = ServGoTextPrimary)
                                Text(method.second, fontSize = 12.sp, color = ServGoTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Bottom payment action
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ServGoBorder)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL PEMBAYARAN", color = ServGoTextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Rp${cost.formatRupiah()}", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Button(
                        onClick = onPayNow,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ServGoPrimary),
                        enabled = vehicle != null
                    ) {
                        Text("Bayar Sekarang", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBookingCard(booking: BookingEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ServGoCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ServGoBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Booking Aktif", color = ServGoTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                ServGoStatusChip(
                    text = when (booking.status) {
                        "Waiting Confirmation" -> "Menunggu Konfirmasi"
                        "Accepted" -> "Diterima"
                        "Queue" -> "Antrian"
                        "Inspection" -> "Inspeksi"
                        "In Progress" -> "Sedang Dikerjakan"
                        "Quality Check" -> "Quality Check"
                        "Ready Pickup" -> "Siap Diambil"
                        "Completed" -> "Selesai"
                        else -> booking.status
                    },
                    color = ServGoPrimary
                )
            }
            Text(booking.title, color = ServGoTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(booking.complaint, color = ServGoTextSecondary, fontSize = 13.sp)
            Text("${booking.workshopName} • Rp${booking.estimatedCost.formatRupiah()}", color = ServGoTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyBookingHelpCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ServGoCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ServGoBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Belum ada kendaraan lokal", color = ServGoTextPrimary, fontWeight = FontWeight.Bold)
            Text(
                "Buka tab Vehicle, tambah kendaraan baru, isi model, plat, odometer, dan pilih model 3D. Setelah itu, alur booking servis akan aktif otomatis luring.",
                color = ServGoTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun Long.formatRupiah(): String = "%,d".format(this).replace(',', '.')
