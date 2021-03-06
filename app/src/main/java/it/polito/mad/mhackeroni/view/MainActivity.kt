package it.polito.mad.mhackeroni.view

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import it.polito.mad.mhackeroni.R
import it.polito.mad.mhackeroni.model.Item
import it.polito.mad.mhackeroni.model.Profile
import it.polito.mad.mhackeroni.utilities.FirebaseRepo
import it.polito.mad.mhackeroni.viewmodel.ProfileFragmentViewModel
import java.util.logging.Level
import java.util.logging.Logger


class MainActivity : AppCompatActivity(), ShowProfileFragment.OnCompleteListener{

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navView: NavigationView
    private lateinit var uid: String
    val logger: Logger = Logger.getLogger(MainActivity::class.java.name)
    private lateinit var vm : ProfileFragmentViewModel
    private lateinit var sharedPref: SharedPreferences
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = applicationContext.getSharedPreferences(getString(R.string.shared_pref), Context.MODE_PRIVATE)
        uid = sharedPref.getString(getString(R.string.uid), "")!!
        vm = ViewModelProvider(this).get(ProfileFragmentViewModel::class.java)
        vm.uid = uid

        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.nav_showProfile,
            R.id.nav_itemList,
            R.id.nav_itemListSale,
            R.id.nav_itemOfInterestList,
            R.id.nav_boughtItemList
        ), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.nav_logout -> logout()
            }
            NavigationUI.onNavDestinationSelected(menuItem, navController)
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        })
        initialHeader()

        vm.getProfile().observe(this, Observer {
            updateHeader(it)
        })

        val imageView = navView.getHeaderView(0).findViewById(R.id.drawable_pic) as ImageView

        imageView.setOnClickListener {
            val navHostFragment : NavHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            if(navHostFragment != null) {
                val currentFrag = navHostFragment.childFragmentManager.fragments[0]
                if(currentFrag is ShowProfileFragment) {

                    // navigate only if you are watching another profile
                    val emailTextView: TextView = findViewById(R.id.mail)
                    val navEmail = navView.getHeaderView(0).findViewById(R.id.drawable_mail) as TextView
                    if((emailTextView.text != navEmail.text))
                        navController.navigate(R.id.nav_showProfile)

                    drawerLayout.closeDrawers()
                } else {
                    navController.navigate(R.id.nav_showProfile)
                    drawerLayout.closeDrawers()
                }
            }
        }

        handleExtra(navController)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun handleExtra(navController: NavController){
        var itemID = intent.getStringExtra("goto")

        if(itemID == null){
            // Check if the notification was receveid in background
        }

        if(!itemID.isNullOrEmpty()){
            FirebaseRepo.INSTANCE.getItemRef(itemID).get().addOnCompleteListener {doc ->
                if(doc.isSuccessful){
                    val item =  doc.result!!.toObject(Item::class.java)
                    if(item != null) {
                        val bundle = Bundle()

                        bundle.putString("item", Item.toJSON(item).toString())
                        bundle.putBoolean("fromList", false)

                        navController.navigate(R.id.nav_ItemDetail, bundle)
                    }
                }
            }

        }
    }

    private fun logout() {
        val auth = FirebaseAuth.getInstance()

        // Delete the token from db
        FirebaseRepo.INSTANCE.logout(this).addOnCompleteListener {
            auth.signOut()
            with (sharedPref.edit()) {
                putString(getString(R.string.uid), "")
                commit()
            }
            val i = Intent(this, GoogleSignInActivity::class.java)
            i.putExtra(getString(R.string.logout_title), true)
            startActivity(i)
        }
    }

    private fun updateHeader(profile: Profile) {
        val headerView = navView.getHeaderView(0)
        val navUsername = headerView.findViewById(R.id.drawable_name) as TextView
        val navEmail = headerView.findViewById(R.id.drawable_mail) as TextView
        val navImage = headerView.findViewById(R.id.drawable_pic) as ImageView
        val navProgressbar = headerView.findViewById(R.id.drawer_progressbar) as ProgressBar

        if(profile.fullName.isNullOrEmpty())
            navUsername.text = resources.getString(R.string.defaultFullName)
        else
            navUsername.text = profile.fullName

        if(profile.email.isNullOrEmpty())
            navEmail.text = resources.getString(R.string.defaultEmail)
        else
            navEmail.text = profile.email

        if(!profile.image.isNullOrEmpty()) {
            if(drawerLayout.isDrawerOpen(GravityCompat.START))
                navProgressbar.visibility = View.VISIBLE // Show only if drawer is open

            val imagePath: String = profile.image!!

            val ref = Firebase.storage.reference
                .child("profiles_images")
                .child(imagePath)

            ref.downloadUrl.addOnCompleteListener {
                if (it.isSuccessful) {
                    try {
                        Glide.with(this)
                            .load(it.result)
                            .into(navImage)
                    } catch(ex: IllegalStateException) {
                        logger.log(Level.WARNING, "context not attached", ex)
                    }
                }
                if(drawerLayout.isDrawerOpen(GravityCompat.START))
                    navProgressbar?.visibility = View.INVISIBLE
            }
        } else {
            navImage.setImageResource(R.drawable.ic_avatar)
        }
    }

    private fun initialHeader() {
        val headerView = navView.getHeaderView(0)
        val navUsername = headerView.findViewById(R.id.drawable_name) as TextView
        val navEmail = headerView.findViewById(R.id.drawable_mail) as TextView
        navUsername.text = resources.getString(R.string.defaultFullName)
        navEmail.text = resources.getString(R.string.defaultEmail)
    }

    override fun onComplete(profile: Profile) {
        updateHeader(profile)
    }
}
