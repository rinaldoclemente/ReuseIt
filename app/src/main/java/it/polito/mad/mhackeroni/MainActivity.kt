package it.polito.mad.mhackeroni

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import it.polito.mad.mhackeroni.utilities.ImageUtils
import it.polito.mad.mhackeroni.utilities.StorageHelper


class MainActivity : AppCompatActivity(), ShowProfileFragment.OnCompleteListener{

    private val USER_ID = "user_id"
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navView: NavigationView
    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uid = intent.extras?.getString(USER_ID)!!
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
                R.id.nav_showProfile, R.id.nav_itemList), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        updateHeader(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //TODO:
        // menuInflater.inflate(R.menu., menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun updateHeader(context: Context) {
        //val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.shared_pref), Context.MODE_PRIVATE)
        db = FirebaseFirestore.getInstance()

        val storageHelper = StorageHelper(context)
        val profile = storageHelper.loadProfile(db, uid)

        Log.d("KKK", "PROFILE: " + profile.toString())

        val headerView = navView.getHeaderView(0)
        val navUsername = headerView.findViewById(R.id.drawable_name) as TextView
        val navEmail = headerView.findViewById(R.id.drawable_mail) as TextView
        val navImage = headerView.findViewById(R.id.drawable_pic) as ImageView

        if(profile != null) {
            if(profile.fullName.isNullOrEmpty())
                navUsername.text = resources.getString(R.string.defaultFullName)
            else
                navUsername.text = profile.fullName

            if(profile.email.isNullOrEmpty())
                navEmail.text = resources.getString(R.string.defaultEmail)
            else
                navEmail.text = profile.email

            navImage.setImageBitmap(profile.image?.let { ImageUtils.getBitmap(it, this) })
        } else {
            navUsername.text = resources.getString(R.string.defaultFullName)
            navEmail.text = resources.getString(R.string.defaultEmail)
        }
    }

    override fun onComplete() {
        updateHeader(this)
    }
}
