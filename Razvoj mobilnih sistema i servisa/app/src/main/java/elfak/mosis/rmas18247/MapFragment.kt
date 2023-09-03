package elfak.mosis.rmas18247

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView


class MapFragment : Fragment() {
lateinit var map: MapView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var ctx: Context? = activity?.applicationContext
        Configuration.getInstance().load(ctx,
            ctx?.let { PreferenceManager.getDefaultSharedPreferences(it) })

        map = requireView().findViewById<MapView>(R.id.map)
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