package de.md5lukas.waypoints.api.sqlite

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import de.md5lukas.waypoints.api.*
import de.md5lukas.waypoints.api.event.FolderPostDeleteEvent
import de.md5lukas.waypoints.api.event.FolderPreDeleteEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.bukkit.Material
import org.junit.jupiter.api.assertAll

class FolderTest {

  private lateinit var server: ServerMock
  private lateinit var api: WaypointsAPI

  @BeforeTest
  fun createAPI() {
    server = MockBukkit.mock()
    val manager =
        SQLiteManager(MockBukkit.createMockPlugin(), DummyDatabaseConfiguration, null, true)
    manager.initDatabase()
    api = manager.api
  }

  @AfterTest
  fun unmock() {
    MockBukkit.unmock()
  }

  @TypesNoDeath
  fun deleteFolder(type: Type) = runBlocking {
    val holder = api.holderOfType(type)

    val folder = holder.createFolder("Test")

    assertEquals(1, holder.getFoldersAmount())

    folder.delete()

    server.pluginManager.assertEventFired(FolderPreDeleteEvent::class.java)
    server.pluginManager.assertEventFired(FolderPostDeleteEvent::class.java)

    assertEquals(0, holder.getFoldersAmount())
  }

  @TypesNoDeath
  fun propertiesSaved(type: Type) = runBlocking {
    val holder = api.holderOfType(type)

    var folder = holder.createFolder("Test")

    folder.setName("Other name")
    folder.setDescription("Some description")
    folder.setMaterial(Material.GRASS_BLOCK)

    folder = holder.getFolders()[0]

    assertAll(
        { assertEquals("Other name", folder.name) },
        { assertEquals("Some description", folder.description) },
        { assertEquals(Material.GRASS_BLOCK, folder.material) })
  }
}
