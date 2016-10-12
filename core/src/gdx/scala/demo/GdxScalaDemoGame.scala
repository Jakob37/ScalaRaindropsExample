package gdx.scala.demo

import com.badlogic.gdx.{ApplicationAdapter, Gdx, Input}
import com.badlogic.gdx.audio.{Music, Sound}
import com.badlogic.gdx.graphics.{GL20, OrthographicCamera, Texture}
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.{MathUtils, Rectangle, Vector3}
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.utils.TimeUtils

import scala.collection.mutable.ArrayBuffer

class GdxScalaDemoGame extends ApplicationAdapter {

  private var camera: OrthographicCamera = _
  private var batch: SpriteBatch = _

  private val levelWidth: Int = 800
  private val levelHeight: Int = 480

  private var dropImage: Texture = _
  private var bucketImage: Texture = _
  private var dropSound: Sound = _
  private var rainMusic: Music = _

  private var bucket: Rectangle = _
  // ArrayBuffer seems to be recommended mutable Array-like collection
  private var raindrops: ArrayBuffer[Rectangle] = _
  // Saved in nanoseconds, thus long
  private var lastDropTime: Long = _

  protected override def create() {

    camera = new OrthographicCamera()
    // Will make sure camera always shows game area of 800 x 480
    camera.setToOrtho(false, levelWidth, levelHeight)

    Bullet.init()
    batch = new SpriteBatch

    // Textures are created from file handles
    // Only graphical representations here
    dropImage = new Texture(Gdx.files.internal("assets/droplet.png"))
    bucketImage = new Texture(Gdx.files.internal("assets/bucket.png"))

    // Sounds are loaded directly to memory
    dropSound = Gdx.audio.newSound(Gdx.files.internal("assets/drop.wav"))

    // Music is streamed from its file location
    rainMusic = Gdx.audio.newMusic(Gdx.files.internal("assets/rain.mp3"))

    rainMusic.setLooping(true)
    rainMusic.play()

    // Setting up actual game representation of the bucket
    bucket = new Rectangle()
    bucket.x = levelWidth / 2 - bucketImage.getWidth / 2
    bucket.y = 20
    bucket.width = bucketImage.getWidth
    bucket.height = bucketImage.getHeight

    raindrops = ArrayBuffer[Rectangle]()
    spawnRaindrop()
  }

  protected override def render() {

    updateMovement()
    updateRaindrops()
    performRendering()
  }

  private def updateMovement(): Unit = {

    // Manage input logic for moving bucket

    // Assigning position to bucket on touch / mouse click
    if (Gdx.input.isTouched()) {
      val touchPos = new Vector3()
      // Get position for touch
      touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0)

      // Transform touch coordinate to game coordinates
      camera.unproject(touchPos)
      bucket.x = touchPos.x - bucketImage.getWidth / 2
    }

    // Check for key input, and adjust for moving 'speed' pixels per second
    val bucketSpeed = 200
    if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
      bucket.x -= bucketSpeed * Gdx.graphics.getDeltaTime
    }
    if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
      bucket.x += bucketSpeed * Gdx.graphics.getDeltaTime
    }

    if (bucket.x < 0) {
      bucket.x = 0
    }
    else if (bucket.x > levelWidth - bucket.getWidth) {
      bucket.x = levelWidth - bucket.getWidth
    }
  }

  private def updateRaindrops(): Unit = {

    val oneBillion = 1000000000
    if (TimeUtils.nanoTime() - lastDropTime > oneBillion) {
      spawnRaindrop()
    }

    val dropSpeed = 200
    var dropsToRemove = ArrayBuffer[Rectangle]()
    for (drop <- raindrops) {
      drop.y -= dropSpeed * Gdx.graphics.getDeltaTime
      if (drop.y + drop.getHeight < 0) {
        dropsToRemove += drop
      }
      else if (drop.overlaps(bucket)) {
        dropSound.play()
        dropsToRemove += drop
      }
    }

    for (drop <- dropsToRemove)  {
      raindrops -= drop
    }
  }

  private def spawnRaindrop(): Unit = {

    // Generate new raindrop

    val raindrop = new Rectangle()
    raindrop.x = MathUtils.random(0, levelWidth - bucket.getWidth)
    raindrop.y = levelHeight
    raindrop.width = dropImage.getWidth
    raindrop.height = dropImage.getHeight
    raindrops += raindrop
    lastDropTime = TimeUtils.nanoTime()
  }

  private def performRendering(): Unit = {

    // Assign dark blue color to background
    Gdx.gl.glClearColor(0, 0, 0.3f, 1)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    // Good practice to update camera properties once per frame
    camera.update()

    // Like nice old XNA!
    batch.begin()
    // Rendering performed with y-axis pointing upwards (what!)
    batch.draw(bucketImage, bucket.x, bucket.y)
    for (drop <- raindrops) {
      batch.draw(dropImage, drop.x, drop.y)
    }
    batch.end()
  }

  protected override def dispose(): Unit = {

    // Clearing native resources not handled by Java garbage collector

    dropImage.dispose()
    bucketImage.dispose()
    dropSound.dispose()
    rainMusic.dispose()
    batch.dispose()
  }
}
