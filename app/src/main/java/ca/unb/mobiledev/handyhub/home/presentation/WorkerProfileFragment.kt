package ca.unb.mobiledev.handyhub.home.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import ca.unb.mobiledev.handyhub.databinding.FragmentWorkerProfileBinding
import ca.unb.mobiledev.handyhub.home.domain.model.TimeSlot
import ca.unb.mobiledev.handyhub.home.domain.model.WorkerDetail
import ca.unb.mobiledev.handyhub.home.domain.viewmodel.WorkerProfileViewModel
import ca.unb.mobiledev.handyhub.home.presentation.adapter.AvailableTimeSlot
import ca.unb.mobiledev.handyhub.home.presentation.adapter.TimeSlotAdapter
import ca.unb.mobiledev.handyhub.util.Resource
import com.bumptech.glide.Glide
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.os.Parcel

@AndroidEntryPoint
class WorkerProfileFragment : Fragment() {

    private var _binding: FragmentWorkerProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkerProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    
    private var scrollToAvailability = false
    private var workerDetail: WorkerDetail? = null
    private var workerId: String = ""
    private var selectedDate: String = ""
    private var selectedTimeSlot: TimeSlot? = null
    private lateinit var timeSlotAdapter: TimeSlotAdapter
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollToAvailability = arguments?.getBoolean("scrollToAvailability") ?: false
        workerId = arguments?.getString("workerId") ?: ""
        
        val categoryName = arguments?.getString("categoryName") ?: "Service"
        binding.textViewTitle.text = "Get $categoryName Service"

        setupRecyclerView()
        setupClickListeners()
        loadWorkerProfile()
        observeWorkerProfile()
        observeBookings()
        observeBookingResult()
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
    }

    private fun setupRecyclerView() {
        timeSlotAdapter = TimeSlotAdapter { timeSlot ->
            selectedTimeSlot = timeSlot
            updateConfirmButton()
        }
        
        binding.recyclerViewTimeSlots.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = timeSlotAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.buttonSelectDate.setOnClickListener {
            showDatePicker()
        }
        
        binding.buttonConfirmBooking.setOnClickListener {
            confirmBooking()
        }
    }

    private fun showDatePicker() {
        val worker = workerDetail
        if (worker == null) {
            android.widget.Toast.makeText(requireContext(), "Worker details not loaded", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get available dates from worker's schedule
        val availableDates = worker.schedule.keys
            .filter { dateStr ->
                try {
                    val scheduleDate = dateFormat.parse(dateStr)
                    val today = Calendar.getInstance()
                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)
                    today.set(Calendar.MILLISECOND, 0)
                    
                    // Only include dates from today onwards
                    scheduleDate != null && !scheduleDate.before(today.time)
                } catch (e: Exception) {
                    false
                }
            }
            .sorted()
        
        if (availableDates.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "No available dates found", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Set constraints to only allow available dates
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val constraintsBuilder = com.google.android.material.datepicker.CalendarConstraints.Builder()
            .setStart(today)
            .setValidator(object : com.google.android.material.datepicker.CalendarConstraints.DateValidator {
                override fun isValid(date: Long): Boolean {
                    val dateStr = dateFormat.format(Date(date))
                    return availableDates.contains(dateStr)
                }
                
                override fun describeContents(): Int = 0
                
                override fun writeToParcel(dest: android.os.Parcel, flags: Int) {}
            })
        
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Available Date")
            .setSelection(today)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = dateFormat.format(Date(selection))
            selectedDate = date
            binding.textViewSelectedDate.text = displayDateFormat.format(Date(selection))
            binding.textViewSelectedDate.visibility = View.VISIBLE
            binding.textViewTimeSlotsLabel.visibility = View.VISIBLE
            
            // Load bookings for selected date and update time slots
            viewModel.loadBookingsForDate(workerId, date)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun loadWorkerProfile() {
        if (workerId.isNotEmpty()) {
            viewModel.loadWorkerDetail(workerId)
        }
    }

    private fun observeWorkerProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.workerDetail.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                        showError(false)
                        showContent(false)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        showError(false)
                        showContent(true)
                        val detail = resource.data
                        if (detail != null) {
                            workerDetail = detail
                            displayWorkerDetail(detail)
                            if (scrollToAvailability) {
                                scrollToAvailabilitySection()
                            }
                        }
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        showError(true)
                        showContent(false)
                        binding.textViewError.text = resource.message ?: "Unknown error"
                    }
                }
            }
        }
    }

    private fun observeBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookings.collect { resource ->
                if (resource is Resource.Success && selectedDate.isNotEmpty()) {
                    updateAvailableTimeSlots(resource.data ?: emptyList())
                }
            }
        }
    }

    private fun observeBookingResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookingResult.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val bookingId = resource.data
                        if (bookingId != null && bookingId.isNotEmpty()) {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Booking confirmed successfully!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            // Reload bookings to update availability
                            if (selectedDate.isNotEmpty()) {
                                viewModel.loadBookingsForDate(workerId, selectedDate)
                            }
                            // Reset selection
                            selectedTimeSlot = null
                            updateConfirmButton()
                        }
                    }
                    is Resource.Error -> {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Failed to create booking: ${resource.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun displayWorkerDetail(workerDetail: WorkerDetail) {
        binding.apply {
            textViewName.text = workerDetail.name
            textViewRating.text = String.format("%.1f", workerDetail.rating)
            textViewCategory.text = workerDetail.category
            textViewLocation.text = if (workerDetail.city.isNotEmpty() && workerDetail.state.isNotEmpty()) {
                "${workerDetail.city}, ${workerDetail.state}"
            } else if (workerDetail.city.isNotEmpty()) {
                workerDetail.city
            } else {
                "Location not specified"
            }
            
            val priceText = if (workerDetail.hourlyRate > 0) {
                "$${workerDetail.hourlyRate}/hr"
            } else {
                "Contact for pricing"
            }
            textViewPrice.text = priceText
            
            textViewEmail.text = workerDetail.email.ifEmpty { "Not provided" }
            textViewPhone.text = workerDetail.phone.ifEmpty { "Not provided" }
            
            Glide.with(requireContext())
                .load(workerDetail.profilePicture)
                .placeholder(ca.unb.mobiledev.handyhub.R.drawable.worker)
                .error(ca.unb.mobiledev.handyhub.R.drawable.worker)
                .circleCrop()
                .into(imageViewProfile)
            
            displayAvailabilitySchedule(workerDetail.schedule)
        }
    }

    private fun displayAvailabilitySchedule(schedule: Map<String, ca.unb.mobiledev.handyhub.home.domain.model.DaySchedule>) {
        val container = binding.containerSchedule
        container.removeAllViews()
        
        // Get today's date
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        // Filter and sort dates (only show future dates, limit to next 7 days)
        val futureDates = schedule.entries
            .filter { (dateStr, _) ->
                try {
                    val scheduleDate = dateFormat.parse(dateStr)
                    scheduleDate != null && !scheduleDate.before(today.time)
                } catch (e: Exception) {
                    false
                }
            }
            .sortedBy { it.key }
            .take(7) // Show only next 7 days
        
        if (futureDates.isEmpty()) {
            val emptyView = android.widget.TextView(requireContext())
            emptyView.text = "No upcoming availability"
            emptyView.setTextColor(0xFF999999.toInt())
            emptyView.textSize = 14f
            emptyView.setPadding(12, 12, 12, 12)
            container.addView(emptyView)
            return
        }
        
        for ((dateStr, daySchedule) in futureDates) {
            val dayView = android.view.LayoutInflater.from(requireContext())
                .inflate(ca.unb.mobiledev.handyhub.R.layout.item_day_schedule, container, false)
            
            val textViewDay = dayView.findViewById<android.widget.TextView>(ca.unb.mobiledev.handyhub.R.id.textViewDay)
            val textViewSchedule = dayView.findViewById<android.widget.TextView>(ca.unb.mobiledev.handyhub.R.id.textViewSchedule)
            
            // Format: "Monday, Nov 23"
            val date = dateFormat.parse(dateStr)
            val displayDate = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(date!!)
            textViewDay.text = displayDate
            
            if (daySchedule.isAvailable && daySchedule.slots.isNotEmpty()) {
                val scheduleText = daySchedule.slots.joinToString(", ") { slot ->
                    "${slot.start} - ${slot.end}"
                }
                textViewSchedule.text = scheduleText
                textViewSchedule.setTextColor(0xFF4CAF50.toInt())
            } else {
                textViewSchedule.text = "Not available"
                textViewSchedule.setTextColor(0xFF999999.toInt())
            }
            
            container.addView(dayView)
        }
    }

    private fun updateAvailableTimeSlots(bookings: List<ca.unb.mobiledev.handyhub.home.domain.model.Booking>) {
        val worker = workerDetail ?: return
        if (selectedDate.isEmpty()) return
        
        // Direct date lookup - much simpler!
        val daySchedule = worker.schedule[selectedDate]
        if (daySchedule == null || !daySchedule.isAvailable || daySchedule.slots.isEmpty()) {
            timeSlotAdapter.submitList(emptyList())
            binding.recyclerViewTimeSlots.visibility = View.GONE
            android.widget.Toast.makeText(requireContext(), "No availability for this date", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Generate time slots from working hours
        val allSlots = generateTimeSlots(daySchedule.slots, worker.slotDurationMinutes, worker.bufferTimeMinutes)
        
        // Filter out booked slots
        val availableSlots = allSlots.map { slot ->
            val isBooked = bookings.any { booking ->
                slotsOverlap(slot, booking.startTime, booking.endTime)
            }
            AvailableTimeSlot(slot, isBooked)
        }
        
        timeSlotAdapter.submitList(availableSlots)
        binding.recyclerViewTimeSlots.visibility = if (availableSlots.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun generateTimeSlots(
        workingHours: List<TimeSlot>,
        slotDuration: Int,
        bufferTime: Int
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        
        for (workingHour in workingHours) {
            var currentTime = parseTimeToMinutes(workingHour.start)
            val endTime = parseTimeToMinutes(workingHour.end)
            
            while (currentTime + slotDuration <= endTime) {
                val slotStart = formatMinutesToTime(currentTime)
                val slotEnd = formatMinutesToTime(currentTime + slotDuration)
                
                slots.add(TimeSlot(start = slotStart, end = slotEnd))
                
                currentTime += slotDuration + bufferTime
            }
        }
        
        return slots
    }

    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun formatMinutesToTime(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }

    private fun slotsOverlap(slot: TimeSlot, bookingStart: String, bookingEnd: String): Boolean {
        val slotStart = parseTimeToMinutes(slot.start)
        val slotEnd = parseTimeToMinutes(slot.end)
        val bookStart = parseTimeToMinutes(bookingStart)
        val bookEnd = parseTimeToMinutes(bookingEnd)
        
        return slotStart < bookEnd && bookStart < slotEnd
    }

    private fun updateConfirmButton() {
        if (selectedDate.isNotEmpty() && selectedTimeSlot != null) {
            val dateText = displayDateFormat.format(dateFormat.parse(selectedDate)!!)
            binding.buttonConfirmBooking.text = "Confirm Service at $dateText"
            binding.buttonConfirmBooking.visibility = View.VISIBLE
        } else {
            binding.buttonConfirmBooking.visibility = View.GONE
        }
    }

    private fun confirmBooking() {
        val worker = workerDetail ?: return
        val timeSlot = selectedTimeSlot ?: return
        if (selectedDate.isEmpty()) return
        
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "User"
        
        val totalAmount = worker.hourlyRate * (worker.slotDurationMinutes / 60)
        
        viewModel.createBooking(
            workerId = workerId,
            userId = userId,
            userName = userName,
            serviceCategory = worker.category,
            serviceName = worker.category,
            date = selectedDate,
            startTime = timeSlot.start,
            endTime = timeSlot.end,
            totalAmount = totalAmount,
            notes = ""
        )
    }

    private fun scrollToAvailabilitySection() {
        binding.scrollView.post {
            val scrollTo = binding.cardAvailability.top - 100
            binding.scrollView.smoothScrollTo(0, scrollTo)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(show: Boolean) {
        binding.errorContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showContent(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.imageViewProfile.visibility = visibility
        binding.textViewName.visibility = visibility
        binding.textViewRating.visibility = visibility
        binding.textViewCategory.visibility = visibility
        binding.textViewLocation.visibility = visibility
        binding.textViewPrice.visibility = visibility
        binding.textViewEmail.visibility = visibility
        binding.textViewPhone.visibility = visibility
        binding.cardAvailability.visibility = visibility
    }

    private fun hideBottomNavigation() {
        val bottomNav = activity?.findViewById<View>(ca.unb.mobiledev.handyhub.R.id.bottomNavigationContainer)
        bottomNav?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
