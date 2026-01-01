package me.prskid1000.craftagent.memory

import me.prskid1000.craftagent.config.BaseConfig
import me.prskid1000.craftagent.database.repositories.ContactRepository
import me.prskid1000.craftagent.database.repositories.LocationMemoryRepository
import me.prskid1000.craftagent.model.database.Contact
import me.prskid1000.craftagent.model.database.LocationMemory
import me.prskid1000.craftagent.util.LogUtil
import net.minecraft.util.math.BlockPos
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages NPC memory: locations and contacts
 */
class MemoryManager(
    private val locationRepository: LocationMemoryRepository,
    private val contactRepository: ContactRepository,
    private val npcUuid: UUID,
    private val config: BaseConfig
) {
    
    private val cachedLocations = ConcurrentHashMap<String, LocationMemory>()
    private val cachedContacts = ConcurrentHashMap<UUID, Contact>()

    init {
        loadFromDatabase()
    }

    private fun loadFromDatabase() {
        try {
            // Load locations
            locationRepository.selectByUuid(npcUuid, config.maxLocations).forEach {
                cachedLocations[it.name] = it
            }

            // Load contacts
            contactRepository.selectByNpcUuid(npcUuid, config.maxContacts).forEach {
                cachedContacts[it.contactUuid] = it
            }
        } catch (e: Exception) {
            LogUtil.error("Error loading memory for NPC: $npcUuid", e)
        }
    }

    // Location Memory Methods
    fun saveLocation(name: String, position: BlockPos, description: String) {
        val location = LocationMemory(
            npcUuid,
            name,
            position.x,
            position.y,
            position.z,
            description
        )
        cachedLocations[name] = location
        try {
            locationRepository.insert(location, config.maxLocations)
        } catch (e: Exception) {
            LogUtil.error("Error saving location: $name", e)
        }
    }

    fun getLocations(): List<LocationMemory> {
        return cachedLocations.values.sortedByDescending { it.timestamp }.take(config.maxLocations)
    }

    fun getLocation(name: String): LocationMemory? {
        return cachedLocations[name]
    }

    fun deleteLocation(name: String) {
        cachedLocations.remove(name)
        try {
            locationRepository.delete(npcUuid, name)
        } catch (e: Exception) {
            LogUtil.error("Error deleting location: $name", e)
        }
    }

    // Contact Methods
    fun addOrUpdateContact(contactUuid: UUID, contactName: String, contactType: String, relationship: String = "neutral", notes: String = "", enmityLevel: Double = 0.0, friendshipLevel: Double = 0.0) {
        // Delete oldest contact if at limit (only for new contacts, not updates)
        if (!cachedContacts.containsKey(contactUuid)) {
            val existing = getContacts()
            if (existing.size >= config.maxContacts) {
                val oldest = existing.minByOrNull { it.lastSeen }
                oldest?.let { 
                    removeContact(it.contactUuid)
                }
            }
        }
        
        val contact = Contact(
            npcUuid,
            contactUuid,
            contactName,
            contactType,
            relationship,
            System.currentTimeMillis(),
            notes,
            enmityLevel.coerceIn(0.0, 1.0),
            friendshipLevel.coerceIn(0.0, 1.0)
        )
        cachedContacts[contactUuid] = contact
        try {
            contactRepository.insertOrUpdate(contact, config.maxContacts)
        } catch (e: Exception) {
            LogUtil.error("Error saving contact: $contactName", e)
        }
    }

    fun getContacts(): List<Contact> {
        return cachedContacts.values.sortedByDescending { it.lastSeen }.take(config.maxContacts)
    }

    fun getContact(contactUuid: UUID): Contact? {
        return cachedContacts[contactUuid]
    }

    fun updateContactLastSeen(contactUuid: UUID) {
        val contact = cachedContacts[contactUuid] ?: return
        val updated = contact.copy(lastSeen = System.currentTimeMillis())
        cachedContacts[contactUuid] = updated
        try {
            contactRepository.insertOrUpdate(updated, config.maxContacts)
        } catch (e: Exception) {
            LogUtil.error("Error updating contact last seen: $contactUuid", e)
        }
    }

    fun removeContact(contactUuid: UUID) {
        cachedContacts.remove(contactUuid)
        try {
            contactRepository.delete(npcUuid, contactUuid)
        } catch (e: Exception) {
            LogUtil.error("Error removing contact: $contactUuid", e)
        }
    }

    /**
     * Updates the relationship type for a contact
     */
    fun updateContactRelationship(contactUuid: UUID, relationship: String) {
        val contact = cachedContacts[contactUuid] ?: return
        val updated = contact.copy(relationship = relationship)
        cachedContacts[contactUuid] = updated
        try {
            contactRepository.insertOrUpdate(updated, config.maxContacts)
        } catch (e: Exception) {
            LogUtil.error("Error updating contact relationship: $contactUuid", e)
        }
    }

    /**
     * Updates the enmity level for a contact (0.0 to 1.0)
     * Increases enmity when negative interactions occur (e.g., being hit)
     */
    fun updateContactEnmity(contactUuid: UUID, enmityChange: Double) {
        val contact = cachedContacts[contactUuid] ?: return
        val newEnmity = (contact.enmityLevel + enmityChange).coerceIn(0.0, 1.0)
        val updated = contact.copy(enmityLevel = newEnmity)
        cachedContacts[contactUuid] = updated
        try {
            contactRepository.insertOrUpdate(updated, config.maxContacts)
        } catch (e: Exception) {
            LogUtil.error("Error updating contact enmity: $contactUuid", e)
        }
    }

    /**
     * Updates the friendship level for a contact (0.0 to 1.0)
     * Increases friendship when positive interactions occur
     */
    fun updateContactFriendship(contactUuid: UUID, friendshipChange: Double) {
        val contact = cachedContacts[contactUuid] ?: return
        val newFriendship = (contact.friendshipLevel + friendshipChange).coerceIn(0.0, 1.0)
        val updated = contact.copy(friendshipLevel = newFriendship)
        cachedContacts[contactUuid] = updated
        try {
            contactRepository.insertOrUpdate(updated, config.maxContacts)
        } catch (e: Exception) {
            LogUtil.error("Error updating contact friendship: $contactUuid", e)
        }
    }

    /**
     * Sets enmity and friendship levels directly
     */
    fun setContactEnmityAndFriendship(contactUuid: UUID, enmityLevel: Double, friendshipLevel: Double) {
        val contact = cachedContacts[contactUuid] ?: return
        val updated = contact.copy(
            enmityLevel = enmityLevel.coerceIn(0.0, 1.0),
            friendshipLevel = friendshipLevel.coerceIn(0.0, 1.0)
        )
        cachedContacts[contactUuid] = updated
        try {
            contactRepository.insertOrUpdate(updated, config.maxContacts)
        } catch (e: Exception) {
            LogUtil.error("Error setting contact enmity/friendship: $contactUuid", e)
        }
    }

    fun cleanup() {
        // Cleanup is handled by repository deletion in NPCService
    }
}

