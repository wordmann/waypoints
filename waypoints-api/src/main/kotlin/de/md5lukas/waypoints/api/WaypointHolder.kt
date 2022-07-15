package de.md5lukas.waypoints.api

import de.md5lukas.waypoints.api.gui.GUIFolder
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * The WaypointHolder provides methods and properties to accumulate waypoints and folders
 */
interface WaypointHolder : GUIFolder {

    /**
     * The folders that are contained in this WaypointHolder
     */
    override val folders: List<Folder>

    /**
     * The waypoints that are contained in this WaypointHolder that are not in a folder, i.e. the waypoints that are directly children of the holder.
     *
     * @see allWaypoints If you want every waypoint disregarding the folder it is in
     */
    override val waypoints: List<Waypoint>

    /**
     * All the waypoints that belong to this WaypointHolder disregarding the folder it is in.
     *
     * @see waypointsAmount If you just need the total amount without the overhead of creating the objects and counting the list.
     */
    val allWaypoints: List<Waypoint>

    /**
     * The total amount of waypoints that belong to this WaypointHolder disregarding the folder it is in.
     */
    val waypointsAmount: Int

    /**
     * The total amount of folders that belong to this WaypointHolder.
     */
    val foldersAmount: Int

    /**
     * The total amount of waypoints that belong to this WaypointHolder disregarding the folder it is in.
     *
     * If the type of this holder is [Type.PERMISSION], then the waypoints the player does not have the permission for are omitted from the amount.
     */
    fun getWaypointsVisibleForPlayer(player: Player): Int

    /**
     * Creates a new Waypoint in this holder with the given name and location
     *
     * @param name The name of the waypoint
     * @param location The location of the waypoint
     * @return The newly created waypoint
     */
    fun createWaypoint(name: String, location: Location): Waypoint

    /**
     * Creates a new folder in this holder with the given name
     *
     * @param name The name of the folder
     * @return The newly created folder
     */
    fun createFolder(name: String): Folder

    /**
     * Checks if a waypoint with the provided name already exists. The case of the name is ignored during the comparison.
     *
     * @param name The name to look for
     * @return `true` if a waypoint exists with the name
     */
    fun isDuplicateWaypointName(name: String): Boolean

    /**
     * Checks if a folder with the provided name already exists. The case of the name is ignored during the comparison.
     *
     * @param name The name to look for
     * @return `true` if a folder exists with the name
     */
    fun isDuplicateFolderName(name: String): Boolean
}