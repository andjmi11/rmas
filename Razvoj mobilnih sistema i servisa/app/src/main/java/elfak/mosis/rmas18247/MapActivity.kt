package elfak.mosis.rmas18247

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView

class MapActivity : AppCompatActivity() {

    lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        var ctx: Context? = applicationContext
        Configuration.getInstance().load(ctx,
            ctx?.let { PreferenceManager.getDefaultSharedPreferences(it) })

        map = findViewById<MapView>(R.id.map)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
