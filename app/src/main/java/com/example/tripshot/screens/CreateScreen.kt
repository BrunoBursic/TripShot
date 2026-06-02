package com.example.tripshot.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tripshot.R
import com.example.tripshot.data.CreateTripRequest
import com.example.tripshot.data.TripRepository
import com.example.tripshot.model.User
import com.example.tripshot.ui.theme.TripShotBgColor
import com.example.tripshot.ui.theme.TripShotDividerColor
import com.example.tripshot.ui.theme.TripShotHint
import com.example.tripshot.ui.theme.TripShotPrimary
import com.example.tripshot.ui.theme.TripShotTextPrimary
import com.example.tripshot.ui.theme.TripShotTextSecondary
import com.example.tripshot.util.TripNotificationCalculator
import com.example.tripshot.util.rememberImageBitmapFromUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class InviteeUi(
    val uid: String,
    val name: String
)

private const val ONE_MINUTE_MILLIS = 60_000L

@Composable
fun CreateScreen(
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val tripRepository = remember { TripRepository() }
    val currentUserId = auth.currentUser?.uid

    var tripName by rememberSaveable { mutableStateOf("") }
    var selectedCoverUri by rememberSaveable { mutableStateOf<String?>(null) }
    var isSavingTrip by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var availableUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    val calendar = Calendar.getInstance()
    val defaultStartTime = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 3)
    val defaultEndTime = calendar.timeInMillis

    var startDateTimeMillis by rememberSaveable { mutableLongStateOf(defaultStartTime) }
    var endDateTimeMillis by rememberSaveable { mutableLongStateOf(defaultEndTime) }

    val schedule = remember(startDateTimeMillis, endDateTimeMillis) {
        TripNotificationCalculator.calculate(
            startDateTimeMillis = startDateTimeMillis,
            endDateTimeMillis = endDateTimeMillis
        )
    }
    val durationInDays = schedule.durationInDays
    val dailyPhotoNotificationRate = schedule.dailyPhotoNotificationRate
    val totalPhotoNotifications = schedule.totalPhotoNotifications
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedCoverUri = uri?.toString() }
    )

    val invitees = rememberSaveable(
        saver = listSaver(
            save = { inviteeList ->
                inviteeList.flatMap { invitee -> listOf(invitee.uid, invitee.name) }
            },
            restore = { rawValues ->
                rawValues.chunked(2)
                    .mapNotNull { chunk ->
                        val uid = chunk.getOrNull(0) as? String ?: return@mapNotNull null
                        val name = chunk.getOrNull(1) as? String ?: return@mapNotNull null
                        InviteeUi(uid = uid, name = name)
                    }
                    .mapTo(mutableStateListOf()) { it }
            }
        )
    ) { mutableStateListOf<InviteeUi>() }
    val selectedInviteeIds = invitees.map(InviteeUi::uid).toSet()
    val normalizedSearchQuery = searchQuery.trim()
    val inviteSearchResults = remember(availableUsers, normalizedSearchQuery, selectedInviteeIds) {
        if (normalizedSearchQuery.isBlank()) {
            emptyList()
        } else {
            availableUsers
                .filter { user ->
                    user.uid.isNotBlank() &&
                        user.name.contains(normalizedSearchQuery, ignoreCase = true) &&
                        !selectedInviteeIds.contains(user.uid)
                }
                .take(8)
        }
    }

    DisposableEffect(currentUserId) {
        if (currentUserId == null) {
            availableUsers = emptyList()
            onDispose { }
        } else {
            val registration = firestore.collection("users")
                .addSnapshotListener { snapshot, _ ->
                    availableUsers = snapshot?.documents
                        ?.mapNotNull { it.toObject(User::class.java) }
                        ?.filter { user ->
                            user.uid.isNotBlank() && user.uid != currentUserId
                        }
                        ?: emptyList()
                }

            onDispose { registration.remove() }
        }
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

            TripScheduleSection(
                durationInDays = durationInDays,
                totalPhotoNotifications = totalPhotoNotifications,
                dailyPhotoNotificationRate = dailyPhotoNotificationRate
            )

            InviteExplorersSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                invitees = invitees,
                searchResults = inviteSearchResults,
                onSelectInvitee = { selectedUser ->
                    if (invitees.none { it.uid == selectedUser.uid }) {
                        invitees.add(InviteeUi(uid = selectedUser.uid, name = selectedUser.name))
                    }
                    searchQuery = ""
                },
                onRemoveInvitee = { invitees.remove(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (tripName.isBlank()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.create_trip_error_trip_name_required),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    if (currentUserId == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.create_trip_error_auth_required),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    isSavingTrip = true
                    tripRepository.createTrip(
                        request = CreateTripRequest(
                            name = tripName.trim(),
                            coverImageUri = selectedCoverUri?.let(Uri::parse),
                            startDateTimeMillis = startDateTimeMillis,
                            endDateTimeMillis = endDateTimeMillis,
                            invitedUserIds = invitees.map(InviteeUi::uid),
                            dailyPhotoNotificationRate = dailyPhotoNotificationRate,
                            totalPhotoNotifications = totalPhotoNotifications
                        ),
                        onSuccess = {
                            isSavingTrip = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.create_trip_save_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            onSaveClick()
                        },
                        onFailure = { throwable ->
                            isSavingTrip = false
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.create_trip_error_save_failed,
                                    throwable.message ?: ""
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TripShotPrimary,
                    contentColor = Color.White
                ),
                enabled = !isSavingTrip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isSavingTrip) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(R.string.create_trip_create_in_progress),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.create_trip_button),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
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
fun TripScheduleSection(
    durationInDays: Int,
    totalPhotoNotifications: Int,
    dailyPhotoNotificationRate: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatsRow(
            durationInDays = durationInDays,
            photoCount = totalPhotoNotifications
        )
        Text(
            text = stringResource(
                R.string.create_trip_notification_note_formatted,
                dailyPhotoNotificationRate
            ),
            color = TripShotTextSecondary,
            fontSize = 12.sp,
            lineHeight = 20.sp
        )
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
    searchResults: List<User>,
    onSelectInvitee: (User) -> Unit,
    onRemoveInvitee: (InviteeUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionLabel(stringResource(R.string.create_trip_invite_explorers))

        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange
        )

        if (searchQuery.trim().isNotBlank()) {
            if (searchResults.isEmpty()) {
                Text(
                    text = stringResource(R.string.create_trip_search_no_users),
                    color = TripShotTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    searchResults.forEach { user ->
                        InviteSearchResultItem(
                            user = user,
                            onAddClick = { onSelectInvitee(user) }
                        )
                    }
                }
            }
        }

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
fun InviteSearchResultItem(
    user: User,
    onAddClick: () -> Unit
) {
    Surface(
        color = Color(0xFF242424),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, TripShotDividerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = user.name,
                color = TripShotTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onAddClick) {
                Text(
                    text = stringResource(R.string.create_trip_add_invitee),
                    color = TripShotPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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
