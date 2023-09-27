package elfak.mosis.rmas18247

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    private lateinit var currentLocation: GeoPoint
    private lateinit var currentUser: String

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseRefPlaces: DatabaseReference
    private lateinit var firebaseRefReviews: DatabaseReference
    private lateinit var firebaseRefUsers: DatabaseReference

    private lateinit var reviewRecyclerView: RecyclerView
    private lateinit var reviewArray: ArrayList<ReviewsList>

    var selectedIme: String? = null
    var selectedPrezime: String? = null
    var selectedTip: String? = null
    var selectedOcena: Float = -1.0f
    var selectedDatumOd: String? = null
    var selectedDatumDo: String? = null
    var selectedVremeOd: String? = null
    var selectedVremeDo: String? = null
    var selectedRadius: Double = 0.0

    private lateinit var spinnerV: Spinner
    private lateinit var spinnerList: ArrayList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var spinnerRef: DatabaseReference


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseRefPlaces = FirebaseDatabase.getInstance().getReference("places")
        firebaseRefReviews = FirebaseDatabase.getInstance().getReference("reviews")
        firebaseRefUsers = FirebaseDatabase.getInstance().getReference("users")


        currentUser = firebaseAuth.currentUser?.uid.toString()

        val ctx = requireContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        map = view.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.controller.zoomTo(18.8)

        val permissions = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.CHANGE_NETWORK_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        requestPermissionsIfNecessary(permissions)

        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.setMultiTouchControls(true)

        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            if (isLocationEnabled()) {
                setMyLocationOverlay()
                setOnMapClickOverlay()
            } else {
                showLocationDisabledDialog()
            }
        }

        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.setMultiTouchControls(true)

        loadPlacesFromFirebase();

        //najbliziMarker(currentLocation.latitude, currentLocation.longitude)
        val buttonRefresh = view.findViewById<Button>(R.id.refreshButton)
        buttonRefresh.setOnClickListener{
            loadPlacesFromFirebase()
        }

        val filter = view.findViewById<TextView>(R.id.filterButton)
        filter.setOnClickListener {
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_filter, null)

            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            alertDialogBuilder.setView(dialogView)
            val alertDialog = alertDialogBuilder.create()

            spinnerV = dialogView.findViewById(R.id.kreatorSpinner)
            spinnerRef = FirebaseDatabase.getInstance().getReference("users")
            spinnerList = ArrayList<String>()
            adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, spinnerList)
            spinnerV.adapter = adapter

            showKreatori()


            spinnerV.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedNameSurname = spinnerList[position]

                    val parts = selectedNameSurname.split(" ")
                    if (parts.size >= 2) {
                        selectedIme = parts[0]
                        selectedPrezime = parts[1]
                    }
                    else {
                        selectedIme = null
                        selectedPrezime = null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedIme = null
                    selectedPrezime = null
                }
            }

            val tipMestaSpinner = dialogView.findViewById<Spinner>(R.id.tipSpinner)
            val adapterTip = ArrayAdapter.createFromResource(requireContext(), R.array.opcije_filter, android.R.layout.simple_spinner_item)
            adapterTip.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            tipMestaSpinner.adapter = adapterTip
            selectedTip = tipMestaSpinner.selectedItem.toString()
            tipMestaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedTip = adapterTip.getItem(position).toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // nista nije izabrano
                }
            }

            val ocenaEdit = dialogView.findViewById<EditText>(R.id.ocenaEdit)
            ocenaEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }
                override fun afterTextChanged(s: Editable?) {
                    val ocenaText = s.toString().trim()
                    if (ocenaText.isNotEmpty()) {
                        try {
                            selectedOcena = ocenaText.toFloat()
                            Log.d("tag", "Vrednost ocene je $selectedOcena")
                        } catch (e: NumberFormatException) {
                            Log.e("tag", "Nije moguće pretvoriti tekst u float: $ocenaText")
                        }
                    } else {
                        //prazno polje
                    }
                }
            })

            val radius = dialogView.findViewById<EditText>(R.id.radijus)
            radius.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    val radiusTxt = s.toString().trim()
                    if (radiusTxt.isNotEmpty()) {
                        try {
                            selectedRadius = radiusTxt.toDouble()
                            Log.d("tag", "Vrednost radijusa je $selectedRadius")
                        } catch (e: NumberFormatException) {
                            Log.e("tag", "Greska kod radijusa")
                        }
                    } else {
                        //prazno polje
                    }
                }
            })

            val datumOdPicker = dialogView.findViewById<EditText>(R.id.datumOd)
            val datumDoPicker = dialogView.findViewById<EditText>(R.id.datumDo)
            val vremeOdPicker = dialogView.findViewById<EditText>(R.id.vremeOd)
            val vremeDoPicker = dialogView.findViewById<EditText>(R.id.vremeDo)

            datumOdPicker.setOnClickListener {
                showDatePickerDialog(datumOdPicker, "datumOd")
                datumOdPicker.setText(selectedDatumOd)
            }

            datumDoPicker.setOnClickListener {
                showDatePickerDialog(datumDoPicker, "datumDo")
                datumDoPicker.setText(selectedDatumDo)
            }

            vremeOdPicker.setOnClickListener {
                showTimePickerDialog(vremeOdPicker, "vremeOd")
                vremeOdPicker.setText(selectedVremeOd)
            }

            vremeDoPicker.setOnClickListener {
                showTimePickerDialog(vremeDoPicker, "vremeDo")
                vremeDoPicker.setText(selectedVremeDo)
            }

          /*  val poslInterakcija = dialogView.findViewById<CheckBox>(R.id.poslInterakcija)
            if(poslInterakcija.isChecked){
                selectedPoslInterakcija = true
            }*/

            val buttonFilter = dialogView.findViewById<Button>(R.id.buttonFilter)
            buttonFilter.setOnClickListener {
                filtering()
            }


            alertDialog.show()

        }

        return view
    }

    private fun filtering() {

        if (selectedIme != null && selectedPrezime != null) {
            map.overlays.clear()
            setMyLocationOverlay()
            map.invalidate()
            filterKreator(selectedIme.toString(), selectedPrezime.toString()) }
        if(selectedTip != "Oba"){
            map.overlays.clear()
            setMyLocationOverlay()
            map.invalidate()
            filterTip(selectedTip.toString())
        }
        if (selectedOcena != -1.0f) {
            map.overlays.clear()
            setMyLocationOverlay()
            map.invalidate()
            filterOcena(selectedOcena) }
        if (selectedRadius != 0.0) {
            getMyLocation()
        }
        if(selectedVremeOd!="" && selectedVremeDo!=""){
            map.overlays.clear()
            setMyLocationOverlay()
            map.invalidate()
            filterVremeDatum(selectedVremeOd.toString(), selectedVremeDo.toString(), "timeCreated")
        }
        if(selectedDatumOd!="" &&selectedDatumDo!=""){
            map.overlays.clear()
            setMyLocationOverlay()
            map.invalidate()
            filterVremeDatum(selectedVremeOd.toString(), selectedVremeDo.toString(), "dateCreated")
        }

        /*if(selectedPoslInterakcija == true){
            map.overlays.clear()
            setMyLocationOverlay()
            map.invalidate()
            filterLastInteraction()
        }*/
    }

   /* private fun filterLastInteraction() {
        val trenutnoVremeString = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val upit = firebaseRefPlaces.orderByChild("lastInteraction")

        upit.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    var najblizaInterakcija: DataSnapshot? = null
                    var najmanjaRazlika: Long = Long.MAX_VALUE

                    for (placeSnapshot in dataSnapshot.children) {
                        val placeInfo = placeSnapshot.getValue(Places::class.java)
                        if (placeInfo != null) {
                            val poslInter = placeInfo.lastInteraction

                            if (poslInter != null) {
                                val razlika = izracunajRazlikuVremena(trenutnoVremeString, poslInter)
                                if (razlika < najmanjaRazlika) {
                                    najmanjaRazlika = razlika
                                    najblizaInterakcija = placeSnapshot
                                }
                            }
                        }
                    }

                    if (najblizaInterakcija != null) {
                        val placeInfo = najblizaInterakcija.getValue(Places::class.java)
                        if (placeInfo != null) {
                            val geoPoint = GeoPoint(placeInfo.latitude, placeInfo.longitude)
                            val naslov = placeInfo.naslov
                            val kreator = placeInfo.kreatorID.toString()
                            val noviTip = placeInfo.mesto
                            val noviMestoID = najblizaInterakcija.key

                            if (noviMestoID != null) {
                                addMarker(geoPoint, naslov, kreator, noviTip, noviMestoID)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Greška pri filtriranju najbliže interakcije.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun izracunajRazlikuVremena(vreme1: String, vreme2: String): Long {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date1 = dateFormat.parse(vreme1)
        val date2 = dateFormat.parse(vreme2)
        return Math.abs(date1.time - date2.time)
    }*/

    private fun filterVremeDatum(from: String, to: String,dateOrTIme: String) {
        val query = firebaseRefPlaces.orderByChild(dateOrTIme).startAt(from).endAt(to)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (placeSnapshot in dataSnapshot.children) {
                        val placeInfo = placeSnapshot.getValue(Places::class.java)
                        if (placeInfo != null) {
                            val geoPoint = GeoPoint(placeInfo.latitude, placeInfo.longitude)
                            val naslov = placeInfo.naslov
                            val kreator = placeInfo.kreatorID.toString()
                            val noviTip = placeInfo.mesto
                            val noviMestoID = placeSnapshot.key


                            if (noviMestoID != null) {
                                addMarker(geoPoint, naslov, kreator, noviTip, noviMestoID)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Greška pri filtriranju datuma/vremena.",Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterTip(tip: String) {
        firebaseRefPlaces.orderByChild("mesto").equalTo(tip)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(placeSnapshot: DataSnapshot) {
                    if (placeSnapshot.exists()) {
                        for (place in placeSnapshot.children) {
                            val placeInfo = place.getValue(Places::class.java)
                            if (placeInfo != null) {
                                val geoPoint = GeoPoint(placeInfo.latitude, placeInfo.longitude)
                                val naslov = placeInfo.naslov
                                val kreator = placeInfo.kreatorID.toString()
                                val noviTip = placeInfo.mesto
                                val noviMestoID = place.key
                                Log.d("filter", "Vrednosti $geoPoint, $naslov, $kreator, $noviTip, $noviMestoID")

                                // Dodajte marker na mapu koristeći ove podatke
                                if (noviMestoID != null) {
                                    addMarker(geoPoint, naslov, kreator, noviTip, noviMestoID)
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Greška pri filtriranju tipa!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filterRadius(myLatitude: Double, myLongitude: Double, radius: Double) {
        firebaseRefPlaces.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(placesSnapshot: DataSnapshot) {
                if (placesSnapshot.exists()) {
                    for (place in placesSnapshot.children) {
                        val placeInfo = place.getValue(Places::class.java)
                        if (placeInfo != null) {
                            val objekatLatitude = placeInfo.latitude
                            val objekatLongitude = placeInfo.longitude

                            val udaljenost = calculateDistance(
                                myLatitude,
                                myLongitude,
                                objekatLatitude,
                                objekatLongitude
                            )

                            if (udaljenost <= radius) {
                                val geoPoint = GeoPoint(placeInfo.latitude, placeInfo.longitude)
                                val noviNaslov = placeInfo.naslov
                                val noviKreator = placeInfo.kreatorID.toString()
                                val noviTip = placeInfo.mesto
                                val noviMestoID = place.key

                                if (noviMestoID != null) {
                                    addMarker(
                                        geoPoint,
                                        noviNaslov,
                                        noviKreator,
                                        noviTip,
                                        noviMestoID
                                    )
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Greška pri filtriranju po radijusu",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Zemljin srednji radijus u km

        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)))

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c //udaljenost u km
    }


    private fun getMyLocation() {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = android.location.LocationListener { location ->
            val myLatitude = location.latitude
            val myLongitude = location.longitude

            filterRadius(myLatitude, myLongitude, selectedRadius)
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener
            )
        }
    }

    private fun filterOcena(ocena: Float) {
        firebaseRefReviews.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(reviewsSnapshot: DataSnapshot) {
                if (reviewsSnapshot.exists()) {
                    for (review in reviewsSnapshot.children) {
                        val reviewInfo = review.getValue(Reviews::class.java)
                        if (reviewInfo != null && reviewInfo.ocena == ocena) {
                            val mestoID = reviewInfo.mestoID

                            firebaseRefPlaces.child(mestoID).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(placeSnapshot: DataSnapshot) {
                                    if (placeSnapshot.exists()) {
                                        val placeInfo = placeSnapshot.getValue(Places::class.java)
                                        if (placeInfo != null) {
                                            val geoPoint = GeoPoint(placeInfo.latitude, placeInfo.longitude)
                                            val noviNaslov = placeInfo.naslov
                                            val noviKreator = placeInfo.kreatorID.toString()
                                            val noviTip = placeInfo.mesto
                                            val noviMestoID = placeSnapshot.key
                                            if (noviMestoID != null) {
                                                addMarker(geoPoint, noviNaslov, noviKreator, noviTip, noviMestoID)
                                            }
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                   Toast.makeText(requireContext(), "Greška pri filtriranju ocene", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Greška pri filtriranju ocene", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun filterKreator(ime: String, prezime: String) {
        firebaseRefUsers.orderByChild("name").equalTo(ime)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    if (userSnapshot.exists()) {
                        for (user in userSnapshot.children) {
                            val userInfo = user.getValue(Users::class.java)
                            if (userInfo?.surname == prezime) {
                                val userId = user.key

                                firebaseRefPlaces.orderByChild("kreatorID").equalTo(userId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(placeSnapshot: DataSnapshot) {
                                            if (placeSnapshot.exists()) {
                                                for (place in placeSnapshot.children) {
                                                    val placeInfo = place.getValue(Places::class.java)
                                                    if (placeInfo != null) {
                                                        val geoPoint = GeoPoint(placeInfo.latitude, placeInfo.longitude)
                                                        val noviNaslov = placeInfo.naslov
                                                        val noviKreator = placeInfo.kreatorID.toString()
                                                        val noviTip = placeInfo.mesto
                                                        val noviMestoID = place.key
                                                        if (noviMestoID != null) {
                                                            addMarker(geoPoint, noviNaslov, noviKreator, noviTip, noviMestoID)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(requireContext(), "Greška pri filtriranju kreatora", Toast.LENGTH_SHORT).show()

                                        }
                                    })
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Greška pri filtriranju kreatora", Toast.LENGTH_SHORT).show()

                }
            })
    }
    private fun showDatePickerDialog(editText: EditText, field: String) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                when (field) {
                    "datumOd" -> selectedDatumOd = formattedDate
                    "datumDo" -> selectedDatumDo = formattedDate
                }
                editText.setText(formattedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
    private fun showTimePickerDialog(editText: EditText, field: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
                )
                when (field) {
                    "vremeOd" -> selectedVremeOd = formattedTime
                    "vremeDo" -> selectedVremeDo = formattedTime
                }
                editText.setText(formattedTime)
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    private fun showKreatori() {
        spinnerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    spinnerList.clear()

                    for (userSnapshot in snapshot.children) {
                        val name = userSnapshot.getValue(Users::class.java)
                        if (name != null) {
                            val ime = name.name
                            val prezime = name.surname
                            spinnerList.add("$ime $prezime").toString()
                        }
                    }
                    spinnerList.add("Svi").toString()
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private lateinit var naslov: String
    private lateinit var mesto: String
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0

    private fun setOnMapClickOverlay() {

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // menadzer za dobijanje lokacije
            val locationManager =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                val myLatitude = location.latitude
                val myLongitude = location.longitude

                map.overlays.add(object : Overlay() {
                    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                        // dobijemo geografske koordinate tacke na koju je korisnik kliknuo
                        val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())

                        longitude = geoPoint.longitude.toDouble()
                        latitude = geoPoint.latitude.toDouble()

                        val distance = calculateDistance(myLatitude, myLongitude, geoPoint.latitude, geoPoint.longitude)

                        val maxDistanceMeters = 0.1
                     //   if (distance <= maxDistanceMeters) {
                          //  najbliziMarker(myLatitude, myLongitude)
                            //pre nego kreiram marker, otvaram dijalog za unos podataka o mestu
                            val inflater = requireActivity().layoutInflater
                            val dialogView = inflater.inflate(R.layout.dialog_newplace, null)

                            val alertDialogBuilder = AlertDialog.Builder(requireContext())
                            alertDialogBuilder.setView(dialogView)


                            val imeMestaEditText =
                                dialogView.findViewById<EditText>(R.id.imeMestaEditText)
                            val tipMestaSpinner =
                                dialogView.findViewById<Spinner>(R.id.tipMestaSpinner)


                            val adapter = ArrayAdapter.createFromResource(requireContext(), R.array.opcije_mesta, android.R.layout.simple_spinner_item)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            tipMestaSpinner.adapter = adapter

                            AlertDialog.Builder(requireContext())
                                .setView(dialogView)
                                .setTitle("Unos mesta")
                                .setPositiveButton(
                                    "Potvrdi",
                                    DialogInterface.OnClickListener { dialog, which ->
                                        naslov = imeMestaEditText.text.toString()
                                        mesto = tipMestaSpinner.selectedItem.toString()
                                        saveData()
                                    })
                                .setNegativeButton("Otkaži", null)
                                .create()
                                .show()


                            return true
                     /*   }
                        else{
                            return false
                        }*/
                    }
                })
            }
        }
    }

    private fun addPointsForCurrentUser(d: Double, uid: String) {
        val u:String = if(uid == "") { currentUser } else { uid }
        firebaseRefUsers.child(u).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user10 = snapshot.getValue(Users::class.java)
                    if (user10 != null) {
                        val p = user10.points + d
                        user10.points = p
                        snapshot.ref.setValue(user10).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if(uid == "") {
                                    Toast.makeText(
                                        requireContext(),
                                        "Dobili ste $d poena!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Greška pri dodavanju poena korisniku!", Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun saveData() {

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentDate = Date()


        val place = Places(
            naslov.toString(), mesto.toString(), "", currentUser,
            longitude, latitude, dateFormat.format(currentDate),timeFormat.format(currentDate)
        )

        if (currentUser != null) {
            val newPlaceRef = firebaseRefPlaces.push()
            val newPlaceId = newPlaceRef.key

            if (newPlaceId != null)
                place.pid = newPlaceId
            newPlaceRef.setValue(place).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), "Uspešno unešeni podaci o mestu!", Toast.LENGTH_SHORT).show()
                    addPointsForCurrentUser(10.0, "")
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Neupešno unešeni podaci o mestu!",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        }

    }

    private lateinit var getNaslov: String
    private lateinit var getkreatorID: String
    private lateinit var getkreatorIme: String
    private lateinit var getkreatorPrezime: String
    private lateinit var getTipMesta: String
    private lateinit var getMestoID: String
    private var getOcena: Float = 0.0f
    private lateinit var getOpis: String

    private fun loadPlacesFromFirebase() {
            firebaseRefPlaces.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (markerSnapshot in snapshot.children) {
                            val placeId = markerSnapshot.key
                            val placesInfo = markerSnapshot.getValue(Places::class.java)
                            if (placesInfo != null) {
                                //informacije iz firebase
                                val geoPoint = GeoPoint(placesInfo.latitude, placesInfo.longitude)
                                getNaslov = placesInfo.naslov
                                getkreatorID = placesInfo.kreatorID.toString();
                                getTipMesta = placesInfo.mesto
                                getMestoID = placeId.toString()
                                addMarker(geoPoint, getNaslov, getkreatorID, getTipMesta, getMestoID)

                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Greška pri učitavanju mesta na mapi!", Toast.LENGTH_SHORT).show()

                }
            })
    }

    private fun addMarker(
        geoPoint: GeoPoint, naslov: String, kreator: String,
        mesto: String, mestoID: String
    ) {

        val marker = Marker(map)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(marker)
        map.invalidate() // Osvežite mapu da biste videli promene

        val customMarkerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_pin_drop_24)
        customMarkerDrawable?.mutate()
        marker.icon = customMarkerDrawable

        marker.setOnMarkerClickListener { marker, mapView ->
            showRecenzije(naslov, kreator, mesto, mestoID)
            true
        }

    }

    private lateinit var alertDialog : AlertDialog
    private fun showRecenzije(
        naslov: String, kreator: String,
        mesto: String, mestoID: String
    ) {
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_recenzija, null)

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setView(dialogView)
        alertDialog = alertDialogBuilder.create()


        val textNaslov = dialogView.findViewById<TextView>(R.id.imeMesta)
        val textKreator = dialogView.findViewById<TextView>(R.id.id_kreator)

        val submitAddReview = dialogView.findViewById<Button>(R.id.submitAddReview)
        val ocena = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val opis = dialogView.findViewById<EditText>(R.id.opisMesta)


        val n = "Naziv:"
        val op = "["
        val cl = "]"
        textNaslov.text = "$n $naslov $op $mesto $cl"

        firebaseRefUsers.child(kreator)
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(Users::class.java)
                        if (user != null) {
                            getkreatorIme = user.name.toString()
                            getkreatorPrezime = user.surname.toString()
                            textKreator.text = " $getkreatorIme $getkreatorPrezime"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Greška pri pribavljanju podataka o korisnicima!", Toast.LENGTH_SHORT).show()

                }
            })

        submitAddReview.setOnClickListener { dialogView ->
           /* val currentDate = Date()
            firebaseRefPlaces.child(mestoID).child("lastInteraction").setValue(currentDate).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Ovo je poslednja interakcija",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Greska pri update-ovanju poslednje interakcije",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }*/

            getOcena = ocena.rating.toFloat()
            getOpis = opis.text.toString()

            if(getOcena != -1.0f){
                addPointsForCurrentUser(3.0,"")
            }

            if(getOpis != ""){
                addPointsForCurrentUser(5.0,"")
            }

            if (currentUser != null) {
                val review = Reviews(
                    " ",
                    currentUser.toString(), mestoID,
                    getOcena.toFloat(), getOpis.toString(), 0.0
                )

                val newReviewRef = firebaseRefReviews.push()
                val pid = newReviewRef.key

                if (pid != null) {
                    review.pid = pid
                    newReviewRef.setValue(review).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                "Uspešno unešena recenzija o mestu!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Neupešno unešena recenzija o mestu!",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }

                } else {
                    Toast.makeText(requireContext(), "uid null", Toast.LENGTH_SHORT).show()
                }
            }
            alertDialog.dismiss()

        }

        reviewRecyclerView = dialogView.findViewById(R.id.reviewList)
        reviewRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        reviewRecyclerView.setHasFixedSize(true)

        reviewArray = arrayListOf<ReviewsList>()

        getReviewsData(mestoID)
        alertDialog.show()
    }
    private fun getReviewsData(placeId: String) {
        firebaseRefReviews.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userRef = FirebaseDatabase.getInstance().getReference("users")
                    for (reviewSnapshot in snapshot.children) {
                        val review = reviewSnapshot.getValue(ReviewsList::class.java)
                        val reviewID = reviewSnapshot.key
                        if (review != null) {
                            val korisnikID = review.korisnikid
                            val mestoID = review.mestoID
                          if (mestoID == placeId) {
                                userRef.child(korisnikID)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (snapshot.exists()) {
                                                val user = snapshot.getValue(Users::class.java)
                                                if (user != null) {
                                                    reviewArray.add(
                                                        ReviewsList(
                                                            user.name + " " + user.surname,
                                                            reviewID.toString(),
                                                            review.ocena,
                                                            review.opis,
                                                            " ",
                                                            review.numOfLikes
                                                        )
                                                    )
                                                    val currentUserUid = firebaseAuth.currentUser?.uid ?: ""
                                                    userRef.child(currentUserUid)
                                                        .addListenerForSingleValueEvent(object: ValueEventListener{
                                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                                if(snapshot.exists()){
                                                                    val cu = snapshot.getValue(Users::class.java)
                                                                    if(cu!= null){
                                                                        reviewRecyclerView.adapter =
                                                                            ReviewsAdapter(reviewArray, cu.name + " " + cu.surname,
                                                                             { reviewID ->
                                                                                deleteReview(
                                                                                    reviewID
                                                                                )
                                                                                alertDialog.dismiss()

                                                                            },{reviewID ->
                                                                                    addLikeOnReview(reviewID)
                                                                                    alertDialog.dismiss()
                                                                                })
                                                                    }
                                                                }
                                                            }

                                                            override fun onCancelled(error: DatabaseError) {
                                                                TODO("Not yet implemented")
                                                            }

                                                        })
                                                }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {

                                        }

                                    })

                            }
                        }

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun addLikeOnReview(reviewID: String) {
        firebaseRefReviews.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (reviewSnapshot in snapshot.children) {
                        val revInfo = reviewSnapshot.getValue(Reviews::class.java)
                        if (revInfo != null && revInfo.pid == reviewID) {
                            val likes = revInfo.numOfLikes + 1 // Povećaj broj lajkova za 1
                            val korisnik = revInfo.korisnikid

                            revInfo.numOfLikes = likes

                            reviewSnapshot.ref.setValue(revInfo)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(requireContext(), "Lajkovali ste recenziju",
                                            Toast.LENGTH_SHORT).show()
                                        addPointsForCurrentUser(1.0, korisnik)
                                    } else {
                                        Toast.makeText(requireContext(), "Neuspešno ažuriranje lajka!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Greška pri obradi lajkova", Toast.LENGTH_SHORT).show()

            }
        })
    }
    private fun deleteReview(reviewID: String) {
        val reviewRef = firebaseRefReviews.child(reviewID)

        reviewRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(
                    requireContext(),
                    "Recenzija je uspešno obrisana.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Greška prilikom brisanja recenzije
                Toast.makeText(
                    requireContext(),
                    "Greška prilikom brisanja recenzije.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun showLocationDisabledDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Lokacija nije omogućena")
        alertDialog.setMessage("Za korišćenje ove funkcije, molimo omogućite lokaciju na vašem uređaju.")
        alertDialog.setPositiveButton("Otvori postavke") { _, _ ->
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        alertDialog.setNegativeButton("Otkaži") { _, _ ->
        }
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }

    private fun setMyLocationOverlay() {
        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

        myLocationOverlay.enableFollowLocation()

        myLocationOverlay.runOnFirstFix {
            val location = myLocationOverlay.myLocation
            if (location != null) {
                currentLocation = GeoPoint(location.latitude, location.longitude)
                activity?.runOnUiThread {
                    map.controller.animateTo(currentLocation)

                   // najbliziMarker(currentLocation.latitude, currentLocation.longitude)
                }
            }
        }



    }

    private fun najbliziMarker(latitude: Double, longitude: Double) {
        for (marker in map.overlays) {
            if (marker is Marker) {
                val markerLatitude = marker.position.latitude
                val markerLongitude = marker.position.longitude

                val udaljenost = calculateDistance(latitude, longitude, markerLatitude, markerLongitude)

                val zeljeniRadijus = 1.0

                if (udaljenost <= zeljeniRadijus) {
                    val customMarkerDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_pin_drop_24_rozi)
                    customMarkerDrawable?.mutate()
                    marker.icon = customMarkerDrawable

                }
            }
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                setMyLocationOverlay()
                map.invalidate()

            }
        }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val missingPermissions = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add(permission)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                missingPermissions.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }



}