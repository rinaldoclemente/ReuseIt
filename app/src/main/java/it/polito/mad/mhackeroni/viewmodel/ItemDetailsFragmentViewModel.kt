package it.polito.mad.mhackeroni.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import it.polito.mad.mhackeroni.model.Item
import it.polito.mad.mhackeroni.model.Profile
import it.polito.mad.mhackeroni.utilities.FirebaseRepo
import kotlinx.coroutines.*

class ItemDetailsFragmentViewModel : ViewModel() {
    var itemId : String = ""
    var owner : String = ""
    private var profile : MutableLiveData<Profile> = MutableLiveData()
    private var item : MutableLiveData<Item> = MutableLiveData()

    fun getItem(): LiveData<Item>{
        val repo = FirebaseRepo.INSTANCE
        repo.getItemRef(itemId).addSnapshotListener{snapshot, e ->
            if(e != null){
                return@addSnapshotListener
            }

            if(snapshot != null && snapshot.exists()){
                item.value = snapshot.toObject(Item::class.java)
                item.value?.id  = snapshot.id
                runBlocking {
                    launch { item.value?.user?.let { loadProfile(it)
                    owner = it} }
                }
            } else {
                item.value = Item()
            }
        }


        return item

    }

    fun getProfile(): LiveData<Profile>{
        return profile
    }

    fun loadProfile(profileID : String): LiveData<Profile>{
        val repo = FirebaseRepo.INSTANCE
        repo.getProfileRef(profileID).addSnapshotListener { snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                profile.value = snapshot.toObject(Profile::class.java)
            } else {
                profile.value = Profile()
            }
        }

        return profile
    }

    fun getInterestedUsers(item : String): LiveData<List<Profile>> {
        val repo = FirebaseRepo.INSTANCE

        return repo.getInterestedProfile(item)
    }

}

