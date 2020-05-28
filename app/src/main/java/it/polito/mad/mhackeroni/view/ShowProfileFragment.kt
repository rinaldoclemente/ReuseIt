package it.polito.mad.mhackeroni.view

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import it.polito.mad.mhackeroni.model.Profile
import it.polito.mad.mhackeroni.viewmodel.ProfileFragmentViewModel
import it.polito.mad.mhackeroni.R
import it.polito.mad.mhackeroni.utilities.FirebaseRepo
import kotlinx.android.synthetic.main.fragment_show_profile.*
import java.util.logging.Level
import java.util.logging.Logger


class ShowProfileFragment : Fragment(), OnMapReadyCallback {
    private var mListener: OnCompleteListener? = null
    private lateinit var vm : ProfileFragmentViewModel
    private var canEdit = true
    val logger: Logger = Logger.getLogger(ShowProfileFragment::class.java.name)
    private var profile : Profile = Profile()
    private var googleMap : GoogleMap? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_show_profile, container, false)
        setHasOptionsMenu(true)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lateinit var uid : String

        val mapFragment = childFragmentManager.findFragmentById(R.id.user_map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        val passingUID= arguments?.getString(getString(R.string.uid), "") ?: ""
        val fromItem: Boolean = arguments?.getBoolean("fromItem", false) ?: false
        getNavigationInfo()
        arguments?.clear()

        vm = ViewModelProvider(this).get(ProfileFragmentViewModel::class.java)

        // Show personal profile
        if(passingUID.isNullOrEmpty() || passingUID.equals("null") && vm.uid.isEmpty()) {
            val repo = FirebaseRepo.INSTANCE
            uid = repo.getID(requireContext())
        } else { // Show another profile
            uid = passingUID
            canEdit = false
            requireActivity().invalidateOptionsMenu()
            if(!fromItem){
                phone_number.visibility = View.GONE
                mail.visibility = View.GONE
            }
            else{
                phone_number.visibility = View.VISIBLE
                mail.visibility = View.VISIBLE
            }
        }


        if(vm.uid.isNullOrEmpty())
            vm.uid = uid

        vm.getProfile().observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            profile = it
            try {
                if(it.image.isNullOrEmpty()) {
                    imageProfile.setImageResource(R.drawable.ic_avatar)
                } else {

                    profile_progress_bar.visibility = View.VISIBLE
                    val imagePath: String = it.image!!

                    val ref = Firebase.storage.reference
                        .child("profiles_images")
                        .child(imagePath)

                    ref.downloadUrl.addOnCompleteListener {
                        if (it.isSuccessful) {
                            try {
                                Glide.with(requireContext())
                                    .load(it.result)
                                    .into(imageProfile)
                            } catch(ex: IllegalStateException) {
                                logger.log(Level.WARNING, "context not attached", ex)
                            }
                        }
                        profile_progress_bar?.visibility = View.INVISIBLE
                     }
                }
            } catch (e: Exception) {
                Snackbar.make(view,R.string.image_not_found, Snackbar.LENGTH_SHORT).show()
            }

            fullname.text = it?.fullName ?: resources.getString(R.string.defaultFullName)
            bio.text = it?.bio ?: resources.getString(R.string.defaultBio)
            nickname.text = it?.nickname ?: resources.getString(R.string.defaultNickname)
            mail.text = it?.email ?: resources.getString(R.string.defaultEmail)
            phone_number.text = it?.phoneNumber ?: resources.getString(R.string.defaultPhoneNumber)
            location.text = it?.location ?: resources.getString(R.string.defaultLocation)

            if(it.lat != null && it.lng != null){
                val pos = LatLng(it.lat!!, it.lng!!)

                if(googleMap != null) {
                    googleMap!!.addMarker(
                        MarkerOptions()
                            .position(pos)
                    )
                    googleMap!!.moveCamera(CameraUpdateFactory.newLatLng(pos))
                }
            }
        })

        val scroll: ScrollView = view.findViewById(R.id.detailProfile_scrollview)
        val transparent: ImageView = view.findViewById(R.id.imagetransparent)

        transparent.setOnTouchListener { v, event ->
            val action = event.action
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    // Disallow ScrollView to intercept touch events.
                    scroll.requestDisallowInterceptTouchEvent(true)
                    // Disable touch on transparent view
                    false
                }
                MotionEvent.ACTION_UP -> {
                    // Allow ScrollView to intercept touch events.
                    scroll.requestDisallowInterceptTouchEvent(false)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    scroll.requestDisallowInterceptTouchEvent(true)
                    false
                }
                else -> true
            }
        }

        imageProfile.setOnClickListener {
            val bundle=Bundle()
            try {
                if(!profile.image.isNullOrEmpty()) {
                    bundle.putString("uri", profile.image.toString())
                    bundle.putBoolean("profile_image", true)

                    view.findNavController()
                        .navigate(R.id.action_nav_showProfile_to_showImageFragment, bundle)
                }
            } catch (e: Exception) {
                Snackbar.make(view,
                    R.string.image_not_found, Snackbar.LENGTH_SHORT).show()
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if(canEdit)
            inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle menu item selection
        return when (item.itemId) {
            R.id.menu_edit -> {
                editProfile()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun editProfile() {
        val bundle = Bundle()
        bundle.putString("profile", profile.let { Profile.toJSON(it).toString()})
        try {
            view?.findNavController()?.navigate(R.id.action_nav_showProfile_to_nav_editProfile, bundle)
        } catch (ex: IllegalArgumentException) {
            logger.log(Level.WARNING, "multiple navigation not allowed", ex)
        }
    }

    private fun getNavigationInfo(){
        val oldProfileJSON = arguments?.getString("old_profile") ?: ""
        val newProfileJSON = arguments?.getString("new_profile") ?: ""

        if(!newProfileJSON.isEmpty() && !oldProfileJSON.isEmpty() && oldProfileJSON != newProfileJSON){
            val snackbar = view?.let { Snackbar.make(it, getString(R.string.profile_update), Snackbar.LENGTH_LONG) }
            if (snackbar != null) {
                snackbar.setAction(getString(R.string.undo), View.OnClickListener {

                    val repo : FirebaseRepo = FirebaseRepo.INSTANCE
                    val prevProfile = Profile.fromStringJSON(
                        oldProfileJSON
                    )!!

                    if(prevProfile != null)
                        repo.updateProfile(prevProfile, repo.getID(requireContext()))

                })
                snackbar.show()
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if(vm != null && !vm.uid.isNullOrEmpty()) {
            val image = vm.getProfile().value?.image

            if(!image.isNullOrEmpty()) {
                profile_progress_bar.visibility = View.VISIBLE

                val ref = Firebase.storage.reference
                    .child("profiles_images")
                    .child(image)

                ref.downloadUrl.addOnCompleteListener {
                    if (it.isSuccessful) {
                        try {
                            Glide.with(requireContext())
                                .load(it.result)
                                .into(imageProfile)
                        } catch (ex: IllegalStateException) {
                            logger.log(Level.WARNING, "context not attached", ex)
                        }
                    }
                    profile_progress_bar?.visibility = View.INVISIBLE
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as OnCompleteListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnCompleteListener")
        }
    }

    interface OnCompleteListener {
        fun onComplete(profile: Profile)
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map
    }
}