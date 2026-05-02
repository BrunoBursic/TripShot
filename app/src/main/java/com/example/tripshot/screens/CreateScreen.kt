package com.example.tripshot.screens

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripshot.R
import com.example.tripshot.ui.theme.TripShotBgColor
import com.example.tripshot.ui.theme.TripShotDividerColor
import com.example.tripshot.ui.theme.TripShotHint
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.ui.theme.TripShotSurfaceColor
import com.example.tripshot.ui.theme.TripShotTextPrimary
import com.example.tripshot.ui.theme.TripShotTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class IntensityOption {
    CHILL,
    BALANCED,
    INTENSE
}

data class InviteeUi(
    val name: String,
    val avatar: Painter? = null
)

private const val ONE_MINUTE_MILLIS = 60_000L

@Composable
fun CreateScreen(
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {}
) {
    var tripName by rememberSaveable { mutableStateOf("") }
    var selectedCoverUri by rememberSaveable { mutableStateOf<String?>(null) }

    // Initialize with default dates (today and 3 days from now)
    val calendar = Calendar.getInstance()
    val defaultStartTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 3)
    val defaultEndTime = calendar.timeInMillis

    var startDateTimeMillis by rememberSaveable { mutableLongStateOf(defaultStartTime) }
    var endDateTimeMillis by rememberSaveable { mutableLongStateOf(defaultEndTime) }

    // Calculate duration in days
    val durationInDays = remember(startDateTimeMillis, endDateTimeMillis) {
        val diff = endDateTimeMillis - startDateTimeMillis
        (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    }

    // Calculate photo count based on AGENTS.md rules
    val photoCount = remember(durationInDays) {
        when {
            durationInDays <= 7 -> durationInDays * 2
            durationInDays <= 14 -> durationInDays + 7
            else -> durationInDays
        }
    }

    // Calculate interval in hours based on AGENTS.md rules
    val intervalHours = remember(durationInDays) {
        when {
            durationInDays <= 7 -> 12
            durationInDays <= 14 -> 16
            else -> 24
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedIntensity by rememberSaveable {
        mutableStateOf(IntensityOption.BALANCED)
    }
    var isPublicTrip by rememberSaveable { mutableStateOf(true) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedCoverUri = uri?.toString() }
    )

    val invitees = rememberSaveable(
        saver = listSaver(
            save = { inviteeList -> inviteeList.map(InviteeUi::name) },
            restore = { names -> names.mapTo(mutableStateListOf()) { InviteeUi(it) } }
        )
    ) { mutableStateListOf<InviteeUi>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TripShotBgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CoverSection(
                coverUri = selectedCoverUri?.let(Uri::parse),
                onChangeCoverClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            TripNameSection(
                value = tripName,
                onValueChange = { tripName = it }
            )

            DateRangeSection(
                startDateTimeMillis = startDateTimeMillis,
                endDateTimeMillis = endDateTimeMillis,
                onStartDateTimeChange = { updatedStart ->
                    startDateTimeMillis = updatedStart
                    if (endDateTimeMillis <= updatedStart) {
                        endDateTimeMillis = updatedStart + ONE_MINUTE_MILLIS
                    }
                },
                onEndDateTimeChange = { updatedEnd ->
                    endDateTimeMillis = maxOf(updatedEnd, startDateTimeMillis + ONE_MINUTE_MILLIS)
                }
            )

            MomentIntensitySection(
                selected = selectedIntensity,
                onSelect = { selectedIntensity = it },
                durationInDays = durationInDays,
                photoCount = photoCount,
                intervalHours = intervalHours
            )

            InviteExplorersSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                invitees = invitees,
                onAddInviteeFromSearch = {
                    val newName = searchQuery.trim()
                    if (newName.isNotEmpty() && invitees.none { it.name.equals(newName, ignoreCase = true) }) {
                        invitees.add(InviteeUi(newName))
                        searchQuery = ""
                    }
                },
                onRemoveInvitee = { invitees.remove(it) }
            )

            PublicTripSection(
                checked = isPublicTrip,
                onCheckedChange = { isPublicTrip = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSaveClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TripShotPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_trip_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun CoverSection(
    coverUri: Uri?,
    onChangeCoverClick: () -> Unit
) {
    val coverImageBitmap = rememberImageBitmapFromUri(coverUri)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF2F4F4F))
    ) {
        if (coverImageBitmap != null) {
            Image(
                bitmap = coverImageBitmap,
                contentDescription = stringResource(R.string.content_desc_trip_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = ColorPainter(Color(0xFF2F4F4F)),
                contentDescription = stringResource(R.string.content_desc_trip_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x66000000),
                            Color(0xAA000000)
                        )
                    )
                )
        )

        Surface(
            color = Color(0x66333333),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, Color(0x55FFFFFF)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .clickable { onChangeCoverClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.content_desc_camera),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.create_trip_change_cover),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun rememberImageBitmapFromUri(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val imageBitmapState by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = uri?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(contentResolver, it)
                android.graphics.ImageDecoder.decodeBitmap(source).asImageBitmap()
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, it).asImageBitmap()
            }
        }
    }

    return imageBitmapState
}

@Composable
fun TripNameSection(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.create_trip_trip_name))

        StyledTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.create_trip_trip_name_placeholder)
        )
    }
}

@Composable
fun DateRangeSection(
    startDateTimeMillis: Long,
    endDateTimeMillis: Long,
    onStartDateTimeChange: (Long) -> Unit,
    onEndDateTimeChange: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DateTimeField(
            label = stringResource(R.string.create_trip_start_date),
            dateTimeMillis = startDateTimeMillis,
            onDateTimeChange = onStartDateTimeChange,
            modifier = Modifier.weight(1f)
        )

        DateTimeField(
            label = stringResource(R.string.create_trip_end_date),
            dateTimeMillis = endDateTimeMillis,
            onDateTimeChange = onEndDateTimeChange,
            minDateTimeMillis = startDateTimeMillis + ONE_MINUTE_MILLIS,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun startOfDayMillis(dateTimeMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = dateTimeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    dateTimeMillis: Long,
    onDateTimeChange: (Long) -> Unit,
    minDateTimeMillis: Long? = null,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateTimeFormatter = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }

    val displayText = remember(dateTimeMillis) {
        dateTimeFormatter.format(dateTimeMillis)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel(label)

        Surface(
            color = Color(0xFF242424),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TripShotDividerColor),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayText,
                    color = TripShotTextPrimary,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.content_desc_calendar),
                    tint = TripShotTextSecondary
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val minSelectableDateMillis = remember(minDateTimeMillis) {
            minDateTimeMillis?.let(::startOfDayMillis)
        }
        val selectableDates = remember(minSelectableDateMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return minSelectableDateMillis?.let { utcTimeMillis >= it } ?: true
                }
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTimeMillis,
            selectableDates = selectableDates
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { newDateMillis ->
                            // Preserve the time while updating the date
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = dateTimeMillis
                            val oldHour = calendar.get(Calendar.HOUR_OF_DAY)
                            val oldMinute = calendar.get(Calendar.MINUTE)

                            val newCalendar = Calendar.getInstance()
                            newCalendar.timeInMillis = newDateMillis
                            newCalendar.set(Calendar.HOUR_OF_DAY, oldHour)
                            newCalendar.set(Calendar.MINUTE, oldMinute)

                            val updatedMillis = if (minDateTimeMillis != null) {
                                maxOf(newCalendar.timeInMillis, minDateTimeMillis)
                            } else {
                                newCalendar.timeInMillis
                            }
                            onDateTimeChange(updatedMillis)
                        }
                        showDatePicker = false
                        // Show time picker after date selection
                        showTimePicker = true
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateTimeMillis
        }
        val selectedDateStart = startOfDayMillis(dateTimeMillis)
        val minCalendar = minDateTimeMillis?.let {
            Calendar.getInstance().apply { timeInMillis = it }
        }
        val minTimeAppliesToSelectedDate = minDateTimeMillis != null &&
            selectedDateStart == startOfDayMillis(minDateTimeMillis)
        val minHour = if (minTimeAppliesToSelectedDate) {
            minCalendar?.get(Calendar.HOUR_OF_DAY) ?: 0
        } else {
            0
        }
        val minMinute = if (minTimeAppliesToSelectedDate) {
            minCalendar?.get(Calendar.MINUTE) ?: 0
        } else {
            0
        }
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)
        val clampedInitialHour = if (minTimeAppliesToSelectedDate && initialHour < minHour) {
            minHour
        } else {
            initialHour
        }
        val clampedInitialMinute = if (
            minTimeAppliesToSelectedDate &&
            clampedInitialHour == minHour &&
            initialMinute < minMinute
        ) {
            minMinute
        } else {
            initialMinute
        }
        val timePickerState = rememberTimePickerState(
            initialHour = clampedInitialHour,
            initialMinute = clampedInitialMinute,
            is24Hour = true
        )
        LaunchedEffect(
            timePickerState.hour,
            timePickerState.minute,
            minTimeAppliesToSelectedDate,
            minHour,
            minMinute
        ) {
            if (!minTimeAppliesToSelectedDate) return@LaunchedEffect

            when {
                timePickerState.hour < minHour -> {
                    timePickerState.hour = minHour
                    timePickerState.minute = minMinute
                }
                timePickerState.hour == minHour && timePickerState.minute < minMinute -> {
                    timePickerState.minute = minMinute
                }
            }
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedCalendar = Calendar.getInstance()
                        updatedCalendar.timeInMillis = dateTimeMillis
                        updatedCalendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        updatedCalendar.set(Calendar.MINUTE, timePickerState.minute)
                        val updatedMillis = if (minDateTimeMillis != null) {
                            maxOf(updatedCalendar.timeInMillis, minDateTimeMillis)
                        } else {
                            updatedCalendar.timeInMillis
                        }
                        onDateTimeChange(updatedMillis)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            title = {
                Text(stringResource(R.string.create_trip_select_time))
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
fun MomentIntensitySection(
    selected: IntensityOption,
    onSelect: (IntensityOption) -> Unit,
    durationInDays: Int,
    photoCount: Int,
    intervalHours: Int
) {
    Surface(
        color = TripShotSurfaceColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, TripShotDividerColor),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.create_trip_moment_intensity),
                        color = TripShotPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.create_trip_intensity_description),
                        color = TripShotTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                IntensityBadge(text = selected.name)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IntensityCard(
                    label = stringResource(R.string.create_trip_intensity_chill),
                    iconText = "☾",
                    selected = selected == IntensityOption.CHILL,
                    onClick = { onSelect(IntensityOption.CHILL) },
                    modifier = Modifier.weight(1f)
                )

                IntensityCard(
                    label = stringResource(R.string.create_trip_intensity_balanced),
                    iconText = "⚖",
                    selected = selected == IntensityOption.BALANCED,
                    onClick = { onSelect(IntensityOption.BALANCED) },
                    modifier = Modifier.weight(1f)
                )

                IntensityCard(
                    label = stringResource(R.string.create_trip_intensity_intense),
                    iconText = "⚡",
                    selected = selected == IntensityOption.INTENSE,
                    onClick = { onSelect(IntensityOption.INTENSE) },
                    modifier = Modifier.weight(1f)
                )
            }

            StatsRow(
                durationInDays = durationInDays,
                photoCount = photoCount
            )

            Text(
                text = stringResource(
                    R.string.create_trip_notification_note_formatted,
                    (24 / intervalHours),
                    selected.name.lowercase().replaceFirstChar { it.uppercase() }
                ),
                color = TripShotTextSecondary,
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun IntensityBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = TripShotPrimary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, TripShotPrimary.copy(alpha = 0.25f))
    ) {
        Text(
            text = text,
            color = TripShotPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun IntensityCard(
    label: String,
    iconText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) TripShotPrimary else TripShotDividerColor
    val bg = if (selected) TripShotPrimary.copy(alpha = 0.06f) else Color.Transparent
    val contentColor = if (selected) TripShotPrimary else TripShotTextPrimary

    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = iconText,
                color = contentColor,
                fontSize = 22.sp
            )

            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatsRow(
    durationInDays: Int,
    photoCount: Int
) {
    Surface(
        color = Color(0xFF242424),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TripShotDividerColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.create_trip_trip_duration),
                    color = TripShotTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$durationInDays ${if (durationInDays == 1) "Day" else "Days"}",
                    color = TripShotTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp),
                color = TripShotDividerColor
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = stringResource(R.string.create_trip_exp_memories),
                    color = TripShotTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$photoCount Photos",
                    color = TripShotPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InviteExplorersSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    invitees: List<InviteeUi>,
    onAddInviteeFromSearch: () -> Unit,
    onRemoveInvitee: (InviteeUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionLabel(stringResource(R.string.create_trip_invite_explorers))

        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            onAddFromSearch = onAddInviteeFromSearch
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            invitees.forEach { invitee ->
                InviteeChip(
                    invitee = invitee,
                    onRemoveClick = { onRemoveInvitee(invitee) }
                )
            }
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onAddFromSearch: () -> Unit
) {
    val canAddInvitee = value.isNotBlank()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.create_trip_search_placeholder),
                color = TripShotHint
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.content_desc_search),
                tint = TripShotTextSecondary
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onAddFromSearch,
                enabled = canAddInvitee
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.content_desc_add),
                    tint = if (canAddInvitee) TripShotPrimary else TripShotTextSecondary
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { onAddFromSearch() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF242424),
            unfocusedContainerColor = Color(0xFF242424),
            disabledContainerColor = Color(0xFF242424),
            focusedBorderColor = TripShotDividerColor,
            unfocusedBorderColor = TripShotDividerColor,
            focusedTextColor = TripShotTextPrimary,
            unfocusedTextColor = TripShotTextPrimary,
            cursorColor = TripShotPrimary
        ),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun InviteeChip(
    invitee: InviteeUi,
    onRemoveClick: () -> Unit
) {
    Surface(
        color = Color(0xFF242424),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, TripShotDividerColor)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9D9D9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = invitee.name.first().toString(),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = invitee.name,
                color = TripShotTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = onRemoveClick,
                modifier = Modifier.widthIn(min = 24.dp)
            ) {
                Text(
                    text = "×",
                    color = TripShotTextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PublicTripSection(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Color(0xFF242424),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, TripShotDividerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TripShotPrimary.copy(alpha = 0.1f))
                    .border(
                        width = 1.dp,
                        color = TripShotPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.content_desc_public),
                    tint = TripShotPrimary
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.create_trip_public_trip),
                    color = TripShotTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.create_trip_public_trip_description),
                    color = TripShotTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TripShotPrimary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF555555)
                )
            )
        }
    }
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = TripShotTextSecondary.copy(alpha = 0.4f),
                fontSize = 20.sp
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF242424),
            unfocusedContainerColor = Color(0xFF242424),
            disabledContainerColor = Color(0xFF242424),
            focusedBorderColor = TripShotDividerColor,
            unfocusedBorderColor = TripShotDividerColor,
            focusedTextColor = TripShotTextPrimary,
            unfocusedTextColor = TripShotTextPrimary,
            cursorColor = TripShotPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TripShotTextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CreateScreenPreview() {
    MaterialTheme {
        CreateScreen()
    }
}
