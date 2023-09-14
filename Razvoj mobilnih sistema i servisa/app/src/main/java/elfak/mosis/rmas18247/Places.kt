package elfak.mosis.rmas18247

data class Places(
    val naslov: String="",
    val slike: HashMap<String, String> = HashMap(),
    val mesto: String="",
    val kreatorID: String="",
    val opisi: HashMap<String,String> = HashMap(),
    val ocena: HashMap<String, Float> = HashMap(),
    val brOcena: Int=0,
    var longitude: Double=0.0,
    var latitude: Double=0.0,
    var dateCreated: String="",
    var timeCreated : String="",

    )
