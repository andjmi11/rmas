package elfak.mosis.rmas18247

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import coil.load
import coil.transform.CircleCropTransformation
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var currentLocation: GeoPoint

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseRef: DatabaseReference //za places
    private lateinit var firebaseRef2: DatabaseReference //za reviews
    private lateinit var storageRef: StorageReference

    private lateinit var reviewRecyclerView: RecyclerView
    private lateinit var reviewArray: ArrayList<ReviewsList>


    private lateinit var spinnerV : Spinner
    private lateinit var spinnerList: ArrayList<String>
    private lateinit var adapter:ArrayAdapter<String>
    private lateinit var spinnerRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseRef = FirebaseDatabase.getInstance().getReference("places")
        firebaseRef2 = FirebaseDatabase.getInstance().getReference("reviews")
        storageRef = FirebaseStorage.getInstance().getReference("placesImages")


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

        val filter = view.findViewById<TextView>(R.id.filterButton)
        filter.setOnClickListener{
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

            var selectedIme: String? = null
            var selectedPrezime: String? = null
            var selectedTip: String? = null
            var selectedOcena: Float=-1.0f
            var selectedDatumOd: String? = null
            var selectedDatumDo: String? = null
            var selectedVremeOd: String? = null
            var selectedVremeDo: String? = null
            spinnerV.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedNameSurname = spinnerList[position]

                    val parts = selectedNameSurname.split(" ")
                    if (parts.size >= 2) {
                        selectedIme = parts[0]
                        selectedPrezime = parts[1]

                        Log.d("tag", "Vrednost imena $selectedIme, vrednost prezimena $selectedPrezime")
                        // Sada možete koristiti ime i prezime za dalje radnje
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedIme = null
                    selectedPrezime = null
                }
            }

            val tipMestaSpinner = dialogView.findViewById<Spinner>(R.id.tipSpinner)
            val adapterTip = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.opcije_filter,
                android.R.layout.simple_spinner_item
            )
            adapterTip.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            tipMestaSpinner.adapter = adapterTip
            selectedTip = tipMestaSpinner.selectedItem.toString()
            tipMestaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedTip = adapterTip.getItem(position).toString()
                    Log.d("tag", "Odabrani tip mesta je $selectedTip")
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // nista nije izabrano
                }
            }
            val ocenaEdit = dialogView.findViewById<EditText>(R.id.ocenaEdit)
            ocenaEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                   //pre promene tekst
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Implementacija tokom promene teksta
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

            val datumOdPicker = dialogView.findViewById<EditText>(R.id.datumOd)
            val datumDoPicker = dialogView.findViewById<EditText>(R.id.datumDo)
            val vremeOdPicker = dialogView.findViewById<EditText>(R.id.vremeOd)
            val vremeDoPicker = dialogView.findViewById<EditText>(R.id.vremeDo)

            datumOdPicker.setOnClickListener{
                showDatePickerDialog(datumOdPicker)
            }
            datumDoPicker.setOnClickListener{
                showDatePickerDialog(datumDoPicker)
            }
            vremeOdPicker.setOnClickListener{
                showTimePickerDialog(vremeOdPicker)
            }
            vremeDoPicker.setOnClickListener{
                showTimePickerDialog(vremeDoPicker)
            }

            val buttonFilter = dialogView.findViewById<Button>(R.id.buttonFilter)
            buttonFilter.setOnClickListener {
                filtering(selectedIme, selectedPrezime, selectedTip!!, selectedOcena)
            }

            alertDialog.show()

        }

        return view
    }

    var prikazanaMesta = mutableListOf<Places>()
    private lateinit var flagKreator: Number
    private lateinit var flagOcena: Number
    private fun filtering(ime: String?, prezime: String?, tip: String, ocena: Float) {
        map.overlays.clear()
        setMyLocationOverlay()
        map.invalidate()


        if (ime != null && prezime != null) {
            filterKreator(ime, prezime)
        }

        if(ocena != -1.0f){
            filterOcena(ocena)
        }
    }


    private fun filterOcena(ocena: Float) {
        val reviewsRef = FirebaseDatabase.getInstance().getReference("reviews")
        val placesRef = FirebaseDatabase.getInstance().getReference("places")

        reviewsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(reviewsSnapshot: DataSnapshot) {
                if (reviewsSnapshot.exists()) {
                    for (review in reviewsSnapshot.children) {
                        val reviewInfo = review.getValue(Reviews::class.java)
                        if (reviewInfo != null && reviewInfo.ocena == ocena) {
                            val mestoID = reviewInfo.mestoID

                            placesRef.child(mestoID).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(placeSnapshot: DataSnapshot) {
                                    if (placeSnapshot.exists()) {
                                        val placeInfo = placeSnapshot.getValue(Places::class.java)
                                        if (placeInfo != null) {
                                            val geoPoint = GeoPoint(placeInfo.latitude, placeInfo.longitude)
                                            val noviNaslov = placeInfo.naslov
                                            val noviKreator = placeInfo.kreatorID.toString()
                                            val noviTip = placeInfo.mesto
                                            val noviMestoID = placeSnapshot.key
                                            Log.d("filter", "Vrednosti $geoPoint, $noviNaslov, $noviKreator, $noviTip, $noviMestoID")
                                            if (noviMestoID != null) {
                                                addMarker(geoPoint, noviNaslov, noviKreator, noviTip, noviMestoID)
                                            }
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Handle error
                                }
                            })
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }


    private fun filterKreator(ime: String, prezime: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users")
        val placesRef = FirebaseDatabase.getInstance().getReference("places")

        userRef.orderByChild("name").equalTo(ime)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    if (userSnapshot.exists()) {
                        for (user in userSnapshot.children) {
                            val userInfo = user.getValue(Users::class.java)
                            if (userInfo?.surname == prezime) {
                                val userId = user.key

                                placesRef.orderByChild("kreatorID").equalTo(userId)
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
                                                        Log.d("filter", "Vrednosti $geoPoint, $noviNaslov, $noviKreator, $noviTip, $noviMestoID")
                                                        if (noviMestoID != null) {
                                                            addMarker(geoPoint, noviNaslov, noviKreator, noviTip, noviMestoID)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle error
                                        }
                                    })
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }


    private fun showDatePickerDialog(editText: EditText) {
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
                editText.setText(formattedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showTimePickerDialog(editText: EditText) {
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
                    // Očistite prethodne podatke u listi kako biste izbegli dupliciranje
                    spinnerList.clear()

                    for (userSnapshot in snapshot.children) {
                        val name = userSnapshot.getValue(Users::class.java)
                        if (name != null) {
                            val ime = name.name
                            val prezime = name.surname
                            spinnerList.add("$ime $prezime").toString()
                        }
                    }

                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ovde rukujte greškom ako se dogodi
            }
        })
    }

    private lateinit var naslov: String
    private lateinit var mesto: String
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private lateinit var yourBitmap: Bitmap
    private lateinit var image: ImageView

    private fun setOnMapClickOverlay() {
        map.overlays.add(object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                // dobijemo geografske koordinate tačke na koju je korisnik kliknuo
                val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())

                longitude = geoPoint.longitude.toDouble()
                latitude = geoPoint.latitude.toDouble()

                //pre nego kreiram marker, otvaram dijalog za unos podataka o mestu
                val inflater = requireActivity().layoutInflater
                val dialogView = inflater.inflate(R.layout.dialog_newplace, null)

                val alertDialogBuilder = AlertDialog.Builder(requireContext())
                alertDialogBuilder.setView(dialogView)


                val imeMestaEditText = dialogView.findViewById<EditText>(R.id.imeMestaEditText)
                val tipMestaSpinner = dialogView.findViewById<Spinner>(R.id.tipMestaSpinner)


                val adapter = ArrayAdapter.createFromResource(
                    requireContext(),
                    R.array.opcije_mesta,
                    android.R.layout.simple_spinner_item
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                // Postavite adapter za Spinner
                tipMestaSpinner.adapter = adapter

                AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setTitle("Unos mesta")
                    .setPositiveButton("Potvrdi", DialogInterface.OnClickListener { dialog, which ->
                        naslov = imeMestaEditText.text.toString()
                        mesto = tipMestaSpinner.selectedItem.toString()
                        saveData()
                    })
                    .setNegativeButton("Otkaži", null)
                    .create()
                    .show()


                return true
            }
        })
    }

    private fun saveData() {
        val uid = firebaseAuth.currentUser?.uid

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentDate = Date()


        val place = Places(
            naslov.toString(), mesto.toString(), "", uid.toString(),
            longitude, latitude, dateFormat.format(currentDate), timeFormat.format(currentDate)
        )

        if (uid != null) {
            val newPlaceRef = firebaseRef.push()
            val newPlaceId = newPlaceRef.key

            if (newPlaceId != null)
                place.pid = newPlaceId
            newPlaceRef.setValue(place).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Uspešno unešeni podaci o mestu!",
                        Toast.LENGTH_SHORT
                    ).show()
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

    /*private fun saveImageToStorage(placeId: String) {
        val timestamp = System.currentTimeMillis()
        val imageFileName = "Places/place_image_${placeId}_$timestamp.jpg"
        val imageRef = FirebaseStorage.getInstance().getReference(imageFileName)
        val imageBytes = convertBitmapToByteArray(yourBitmap)

        imageRef.putBytes(imageBytes).addOnSuccessListener {
            val imageInfo = HashMap<String, String>()
            imageInfo[(firebaseAuth.currentUser?.uid).toString()] = imageFileName

            val imageInfoRef = firebaseRef.child(placeId).child("slike")
            imageInfoRef.setValue(imageInfo).addOnSuccessListener {
                Toast.makeText(requireContext(), "Uspešno sačuvana slika!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Neuspešno sačuvana slika!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Neuspešno sačuvana slika!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }*/

    private val CAMERA_REQUEST_CODE = 1
    private val GALLERY_REQUEST_CODE = 2
    private fun gallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun camera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val bitmap = data?.extras?.get("data") as Bitmap
                    yourBitmap = bitmap
                    image.load(bitmap) {
                        crossfade(true)
                        crossfade(1000)
                        transformations(CircleCropTransformation())
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    val selectedImage = data?.data
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver,
                        selectedImage
                    )
                    yourBitmap = bitmap
                    image.load(bitmap) {
                        crossfade(true)
                        crossfade(1000)
                        transformations(CircleCropTransformation())
                    }
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
        FirebaseDatabase.getInstance().getReference("places")
            ?.addValueEventListener(object : ValueEventListener {
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
                                addMarker(
                                    geoPoint,
                                    getNaslov,
                                    getkreatorID,
                                    getTipMesta,
                                    getMestoID
                                )

                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Greška pri čitanju markera iz Firebase
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

        marker.setOnMarkerClickListener { marker, mapView ->
            showRecenzije(naslov, kreator, mesto, mestoID)
            true
        }

    }

    private var imeKorisnika: String = ""
    private var prezimeKorisnika: String = ""
    private fun showRecenzije(
        naslov: String, kreator: String,
        mesto: String, mestoID: String
    ) {
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_recenzija, null)

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setView(dialogView)
        val alertDialog = alertDialogBuilder.create()


        val textNaslov = dialogView.findViewById<TextView>(R.id.imeMesta)
        val textKreator = dialogView.findViewById<TextView>(R.id.id_kreator)

        val submitAddReview = dialogView.findViewById<Button>(R.id.submitAddReview)
        val ocena = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val opis = dialogView.findViewById<EditText>(R.id.opisMesta)


        val n = "Naziv:"
        val op = "["
        val cl = "]"
        textNaslov.text = "$n $naslov $op $mesto $cl"

        FirebaseDatabase.getInstance().getReference("users")?.child(kreator)
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(Users::class.java)
                        if (user != null) {
                            getkreatorIme = user.name.toString()
                            getkreatorPrezime = user.surname.toString()
                            val s = "Kreator:"
                            textKreator.text = "$s $getkreatorIme $getkreatorPrezime"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })


        submitAddReview.setOnClickListener { dialogView ->
            val uid = firebaseAuth.currentUser?.uid
            getOcena = ocena.rating.toFloat()
            getOpis = opis.text.toString()
            if (uid != null) {

                val review = Reviews(
                    uid.toString(), mestoID,
                    getOcena.toFloat(), getOpis.toString()
                )

                if (review == null)
                    Toast.makeText(requireContext(), "Review je null", Toast.LENGTH_SHORT).show()

                val newReviewRef = firebaseRef2.push()
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
        firebaseRef2 = FirebaseDatabase.getInstance().getReference("reviews")
        firebaseRef2.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userRef = FirebaseDatabase.getInstance().getReference("users")

                    for (reviewSnapshot in snapshot.children) {
                        val review = reviewSnapshot.getValue(ReviewsList::class.java)
                        if (review != null) {
                            val korisnikID = review.korisnikid
                            val mestoID = review.mestoID
                            Log.d("TAG", "Vrednost promenljive: $mestoID")
                            Log.d("TAG", "Vrednost promenljive: $placeId")
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
                                                            review.ocena,
                                                            review.opis,
                                                            " "
                                                        )
                                                    )
                                                    reviewRecyclerView.adapter =
                                                        ReviewsAdapter(reviewArray)
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


    private fun showLocationDisabledDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Lokacija nije omogućena")
        alertDialog.setMessage("Za korišćenje ove funkcije, molimo omogućite lokaciju na vašem uređaju.")
        alertDialog.setPositiveButton("Otvori postavke") { _, _ ->
// Otvori postavke lokacije na uređaju kako bi korisnik mogao omogućiti lokaciju
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        alertDialog.setNegativeButton("Otkaži") { _, _ ->
/*val latitude =  43.320902
val longitude = 21.895759
val point = GeoPoint(latitude, longitude)

val startMarker = Marker(map)
startMarker.position = point
startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
map.overlays.add(startMarker)

map.controller.setCenter(point)
koordinate nisa da se postavi bilo gde mada nema smisla*/
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