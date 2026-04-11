package com.example.tripshot.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun CreateScreen(
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {},
    coverPainter: Painter? = null
) {
    var tripName by rememberSaveable { mutableStateOf("") }

    // Initialize with default dates (today and 3 days from now)
    val calendar = Calendar.getInstance()
    val defaultStartTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 3)
    val defaultEndTime = calendar.timeInMillis

    var startDateTimeMillis by rememberSaveable { mutableStateOf(defaultStartTime) }
    var endDateTimeMillis by rememberSaveable { mutableStateOf(defaultEndTime) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedIntensity by rememberSaveable {
        mutableStateOf(IntensityOption.BALANCED)
    }
    var isPublicTrip by rememberSaveable { mutableStateOf(true) }

    val invitees = remember {
        listOf(
            InviteeUi("Alex M."),
            InviteeUi("Sarah J.")
        )
    }

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
                coverPainter = coverPainter,
                onChangeCoverClick = {}
            )

            TripNameSection(
                value = tripName,
                onValueChange = { tripName = it }
            )

            DateRangeSection(
                startDateTimeMillis = startDateTimeMillis,
                endDateTimeMillis = endDateTimeMillis,
                onStartDateTimeChange = { startDateTimeMillis = it },
                onEndDateTimeChange = { endDateTimeMillis = it }
            )

            MomentIntensitySection(
                selected = selectedIntensity,
                onSelect = { selectedIntensity = it }
            )

            InviteExplorersSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                invitees = invitees,
                onViewContactsClick = {},
                onRemoveInvitee = {}
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
    coverPainter: Painter?,
    onChangeCoverClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF2F4F4F))
    ) {
        Image(
            painter = coverPainter ?: ColorPainter(Color(0xFF2F4F4F)),
            contentDescription = "Trip cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

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
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    dateTimeMillis: Long,
    onDateTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateTimeFormatter = remember { SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault()) }

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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTimeMillis
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

                            onDateTimeChange(newCalendar.timeInMillis)
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
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateTimeMillis
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedCalendar = Calendar.getInstance()
                        updatedCalendar.timeInMillis = dateTimeMillis
                        updatedCalendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        updatedCalendar.set(Calendar.MINUTE, timePickerState.minute)
                        onDateTimeChange(updatedCalendar.timeInMillis)
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
    onSelect: (IntensityOption) -> Unit
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

            StatsRow()

            Text(
                text = stringResource(R.string.create_trip_notification_note),
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
fun StatsRow() {
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
                    text = stringResource(R.string.create_trip_duration_value),
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
                    text = stringResource(R.string.create_trip_memories_value),
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
    onViewContactsClick: () -> Unit,
    onRemoveInvitee: (InviteeUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(stringResource(R.string.create_trip_invite_explorers))

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = onViewContactsClick) {
                Text(
                    text = stringResource(R.string.create_trip_view_contacts),
                    color = TripShotPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }

        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange
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

            AddInviteeChip(onClick = {})
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
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
fun AddInviteeChip(
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(2.dp, Color(0xFF404040)),
        color = Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = Color(0xFF404040),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
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