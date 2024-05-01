package de.md5lukas.waypoints.api.sqlite

import de.md5lukas.jdbc.select
import de.md5lukas.jdbc.selectFirst
import de.md5lukas.jdbc.selectNotNull
import de.md5lukas.jdbc.update
import de.md5lukas.waypoints.api.Folder
import de.md5lukas.waypoints.api.SearchResult
import de.md5lukas.waypoints.api.Type
import de.md5lukas.waypoints.api.Waypoint
import de.md5lukas.waypoints.api.WaypointHolder
import de.md5lukas.waypoints.api.base.DatabaseManager
import de.md5lukas.waypoints.api.event.FolderCreateEvent
import de.md5lukas.waypoints.api.event.WaypointCreateEvent
import de.md5lukas.waypoints.api.gui.GUIType
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.permissions.Permissible

internal open class WaypointHolderImpl(
    internal val dm: DatabaseManager,
    override val type: Type,
    private val owner: UUID?,
) : WaypointHolder {

  override suspend fun getFolders(): List<Folder> =
      withContext(dm.asyncDispatcher) {
        dm.connection.select(
            "SELECT * FROM folders WHERE type = ? AND owner IS ?;",
            type.name,
            owner?.toString()) {
              FolderImpl(dm, this)
            }
      }

  override suspend fun getWaypoints(): List<Waypoint> =
      withContext(dm.asyncDispatcher) {

        if (type == Type.PUBLIC) {

          dm.connection.select(
            "SELECT * FROM waypoints WHERE type = ? AND folder IS NULL;",
            type.name) {
            WaypointImpl(dm, this)
          }

        } else {

          dm.connection.select(
            "SELECT * FROM waypoints WHERE type = ? AND owner IS ? AND folder IS NULL;",
            type.name,
            owner?.toString()) {
            WaypointImpl(dm, this)
          }

        }
      }

  override suspend fun getAllWaypoints(): List<Waypoint> =
      withContext(dm.asyncDispatcher) {

        if (type == Type.PUBLIC) {

          dm.connection.select(
            "SELECT * FROM waypoints WHERE type = ? AND folder IS NULL;",
            type.name) {
            WaypointImpl(dm, this)
          }

        } else {

          dm.connection.select(
            "SELECT * FROM waypoints WHERE type = ? AND owner IS ?;",
            type.name,
            owner?.toString()) {
            WaypointImpl(dm, this)
          }

        }
      }

  override suspend fun getWaypointsAmount(): Int =
      withContext(dm.asyncDispatcher) {

        if (type == Type.PUBLIC) {

          dm.connection.selectFirst(
            "SELECT COUNT(*) FROM waypoints WHERE type = ?;",
            type.name) {
            getInt(1)
          }!!

        }else{

          dm.connection.selectFirst(
            "SELECT COUNT(*) FROM waypoints WHERE type = ? AND owner IS ?;",
            type.name,
            owner?.toString()) {
            getInt(1)
          }!!

        }

      }

  override suspend fun getFoldersAmount(): Int =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst(
            "SELECT COUNT(*) FROM folders WHERE type = ? AND owner IS ?;",
            type.name,
            owner?.toString()) {
              getInt(1)
            }!!
      }

  override suspend fun getWaypointsVisibleForPlayer(permissible: Permissible): Int =
      if (type == Type.PERMISSION) {
        withContext(dm.asyncDispatcher) {
          dm.connection
              .select("SELECT permission FROM waypoints WHERE type = ?;", type.name) {
                getString("permission")
              }
              .count { permissible.hasPermission(it) }
        }
      } else {
        getWaypointsAmount()
      }

  override suspend fun createWaypoint(name: String, location: Location): Waypoint {
    return createWaypointTyped(name, location, type)
  }

  override suspend fun createWaypoint(name: String, location: Location, coolowner: UUID): Waypoint {
    return createWaypointTyped(name, location, type, coolowner)
  }

  internal suspend fun createWaypointTyped(name: String, location: Location, type: Type): Waypoint =
      withContext(dm.asyncDispatcher) {
        val id = UUID.randomUUID()
        dm.connection.update(
            "INSERT INTO waypoints(id, createdAt, type, owner, name, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
            id.toString(),
            OffsetDateTime.now().toString(),
            type.name,
            owner?.toString(),
            name,
            location.world!!.name,
            location.x,
            location.y,
            location.z,
        )
        dm.connection
            .selectFirst("SELECT * FROM waypoints WHERE id = ?;", id.toString()) {
              WaypointImpl(dm, this)
            }!!
            .also { WaypointCreateEvent(!dm.testing, it).callEvent() }
      }

  internal suspend fun createWaypointTyped(name: String, location: Location, type: Type, coolowner: UUID): Waypoint =
      withContext(dm.asyncDispatcher) {
        val id = UUID.randomUUID()
        dm.connection.update(
            "INSERT INTO waypoints(id, createdAt, type, owner, name, world, x, y, z) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
            id.toString(),
            OffsetDateTime.now().toString(),
            type.name,
            coolowner.toString(),
            name,
            location.world!!.name,
            location.x,
            location.y,
            location.z,
        )
        dm.connection
            .selectFirst("SELECT * FROM waypoints WHERE id = ?;", id.toString()) {
              WaypointImpl(dm, this)
            }!!
            .also { WaypointCreateEvent(!dm.testing, it).callEvent() }
      }

  override suspend fun createFolder(name: String): Folder =
      withContext(dm.asyncDispatcher) {
        val id = UUID.randomUUID()
        dm.connection.update(
            "INSERT INTO folders(id, createdAt, type, owner, name) VALUES (?, ?, ?, ?, ?);",
            id.toString(),
            OffsetDateTime.now().toString(),
            type.name,
            owner?.toString(),
            name,
        )
        dm.connection
            .selectFirst("SELECT * FROM folders WHERE id = ?", id.toString()) {
              FolderImpl(dm, this)
            }!!
            .also { FolderCreateEvent(!dm.testing, it).callEvent() }
      }

  override val createdAt: OffsetDateTime = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))

  override suspend fun isDuplicateWaypointName(name: String): Boolean =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst(
            "SELECT EXISTS(SELECT 1 FROM waypoints WHERE type = ? AND owner IS ? AND name = ? COLLATE NOCASE);",
            type.name,
            owner?.toString(),
            name,
        ) {
          getInt(1) == 1
        } ?: false
      }

  override suspend fun isDuplicateFolderName(name: String): Boolean =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst(
            "SELECT EXISTS(SELECT 1 FROM folders WHERE type = ? AND owner IS ? AND name = ? COLLATE NOCASE);",
            type.name,
            owner?.toString(),
            name,
        ) {
          getInt(1) == 1
        } ?: false
      }

  override suspend fun searchFolders(
      query: String,
      permissible: Permissible?
  ): List<SearchResult<out Folder>> =
      withContext(dm.asyncDispatcher) {
        val (reducedQuery, taggedIndex) = prepareQuery(query)

        dm.connection
            .selectNotNull<Folder>(
                "SELECT * FROM folders WHERE type = ? AND owner = ? AND name LIKE ? ESCAPE '!';",
                type.name,
                owner?.toString(),
                "$reducedQuery%",
            ) {
              FolderImpl(dm, this).also {
                if (permissible !== null &&
                    this@WaypointHolderImpl.type === Type.PERMISSION &&
                    it.getAmountVisibleForPlayer(permissible) == 0)
                    return@selectNotNull null
              }
            }
            .tagDuplicateSearchResults({ it.name }, reducedQuery, taggedIndex)
      }

  override suspend fun searchWaypoints(
      query: String,
      permissible: Permissible?
  ): List<SearchResult<out Waypoint>> =
      withContext(dm.asyncDispatcher) {
        val typeString = type.name
        val ownerString = owner?.toString()

        val (reducedQuery, taggedIndex) = prepareQuery(query)

        if ('/' in reducedQuery) {
              val result = reducedQuery.split('/', limit = 2)
              val folder = "${result[0]}%"
              val waypoint = "${result[1]}%"

              dm.connection.selectNotNull<Waypoint>(
                  "SELECT * FROM waypoints WHERE type = ? AND owner IS ? AND name LIKE ? ESCAPE '!' AND folder IN (SELECT id FROM folders WHERE type = ? AND owner IS ? AND name LIKE ? ESCAPE '!');",
                  typeString,
                  ownerString,
                  waypoint,
                  typeString,
                  ownerString,
                  folder,
              ) {
                if (permissible !== null &&
                    this@WaypointHolderImpl.type === Type.PERMISSION &&
                    !permissible.hasPermission(getString("permission")))
                    return@selectNotNull null

                WaypointImpl(dm, this)
              }
            } else {
              dm.connection.selectNotNull<Waypoint>(
                  "SELECT * FROM waypoints WHERE type = ? AND owner IS ? AND name LIKE ? ESCAPE '!';",
                  typeString,
                  ownerString,
                  "$reducedQuery%",
              ) {
                if (permissible !== null &&
                    this@WaypointHolderImpl.type === Type.PERMISSION &&
                    !permissible.hasPermission(getString("permission")))
                    return@selectNotNull null

                WaypointImpl(dm, this)
              }
            }
            .tagDuplicateSearchResults(Waypoint::getFullPath, reducedQuery, taggedIndex)
      }

  private fun prepareQuery(query: String): Pair<String, Int?> {
    val taggedIndex = query.substringAfterLast('#').toIntOrNull()

    val reducedQuery =
        if (taggedIndex === null) {
              query
            } else {
              query.substringBeforeLast('#')
            }
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")

    return reducedQuery to taggedIndex
  }

  private suspend fun <T> List<T>.tagDuplicateSearchResults(
      nameSelector: suspend (T) -> String,
      reducedQuery: String,
      taggedIndex: Int?
  ): List<SearchResult<out T>> {
    val keys = mutableMapOf<T, String>()
    this.forEach { keys[it] = nameSelector(it) }

    return keys.entries
        .groupBy { it.value }
        .flatMap { (name, tList) ->
          if (taggedIndex !== null) {
            if (name != reducedQuery) {
              return@flatMap emptyList()
            }
            tList.getOrNull(taggedIndex)?.let { t ->
              return@flatMap listOf(SearchResult(t.key, "$name#$taggedIndex"))
            }
          }
          if (tList.size == 1) {
            listOf(SearchResult(tList[0].key, name))
          } else {
            tList.mapIndexed { index, t -> SearchResult(t.key, "$name#$index") }
          }
        }
  }

  override val guiType: GUIType
    get() =
        when (type) {
          Type.PUBLIC -> GUIType.PUBLIC_HOLDER
          Type.PERMISSION -> GUIType.PERMISSION_HOLDER
          else -> throw IllegalStateException("A waypoint holder for a player cannot be a GUI item")
        }

  override val name: String
    get() = guiType.name

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as WaypointHolder

    return type == other.type
  }

  override fun hashCode(): Int {
    return type.hashCode()
  }

  override fun toString(): String {
    return "WaypointHolderImpl(type=$type, owner=$owner)"
  }
}
