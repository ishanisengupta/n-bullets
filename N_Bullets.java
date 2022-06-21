import tester.*;
import javalib.worldimages.*;
import javalib.funworld.*;
import java.awt.Color;
import java.util.Random;

//represents a game piece
interface ILoGamePiece {
  ILoGamePiece moveAll();

  ILoGamePiece append(ILoGamePiece given);

  WorldScene placeAll(WorldScene base);

  boolean collidesWithGamePiece(GamePiece other);

  ILoGamePiece collidesWith(ILoGamePiece other);

  int countCollisions(ILoGamePiece other);

  ILoGamePiece removeOffScreen(int width, int height);

  public ILoGamePiece spawnedBullets(ILoGamePiece ships);
}

//represents the beginning or end of game where 
//there is no game pieces on screen
class MtLoGamePiece implements ILoGamePiece {
  MtLoGamePiece() {
  }

  // moves all game pieces to proper position
  public ILoGamePiece moveAll() {
    return new MtLoGamePiece();
  }

  // appends two lists of game pieces
  public ILoGamePiece append(ILoGamePiece given) {
    return given;
  }

  // places all elements in a world scene
  public WorldScene placeAll(WorldScene base) {
    return base;
  }

  // determines whether a game pieces collides with another
  public boolean collidesWithGamePiece(GamePiece other) {
    return false;
  }

  // removes an element from a list of game pieces if it collides
  public ILoGamePiece collidesWith(ILoGamePiece other) {
    return this;
  }

  // counts number of collisions
  public int countCollisions(ILoGamePiece other) {
    return 0;
  }

  // removes elements that go off screen
  public ILoGamePiece removeOffScreen(int width, int height) {
    return this;
  }

  // list of new bullets after explosion
  public ILoGamePiece spawnedBullets(ILoGamePiece ships) {
    return this;
  }

}

//represents a list of game pieces
class ConsLoGamePiece implements ILoGamePiece {
  GamePiece first;
  ILoGamePiece rest;

  ConsLoGamePiece(GamePiece first, ILoGamePiece rest) {
    this.first = first;
    this.rest = rest;
  }

  // moves all game pieces to proper position
  public ILoGamePiece moveAll() {
    return new ConsLoGamePiece(this.first.move(), this.rest.moveAll());
  }

  // appends two lists 
  public ILoGamePiece append(ILoGamePiece given) {
    return new ConsLoGamePiece(this.first, this.rest.append(given));
  }

  //appends two lists of game pieces
  public WorldScene placeAll(WorldScene base) {
    return this.first.place(this.rest.placeAll(base));
  }

  //determines whether a game pieces collides with another
  public boolean collidesWithGamePiece(GamePiece other) {
    return (this.first.collidesWithHelp(other) || this.rest.collidesWithGamePiece(other));
  }

  // removes any elements that collide
  public ILoGamePiece collidesWith(ILoGamePiece other) {
    if (other.collidesWithGamePiece(this.first)) {
      return this.rest.collidesWith(other);
    }
    else {
      return new ConsLoGamePiece(this.first, this.rest.collidesWith(other));
    }
  }

  // returns the amount of collisions
  public int countCollisions(ILoGamePiece other) {
    if (other.collidesWithGamePiece(this.first)) {
      return 1 + this.rest.countCollisions(other);
    }
    else {
      return this.rest.countCollisions(other);
    }
  }

  // removes off screen game pieces
  public ILoGamePiece removeOffScreen(int width, int height) {
    if (this.first.isOffScreen(width, height)) {
      return this.rest.removeOffScreen(width, height);
    }
    else {
      return new ConsLoGamePiece(this.first, this.rest.removeOffScreen(width, height));
    }
  }

  // new bullets created from bullet explosions
  public ILoGamePiece spawnedBullets(ILoGamePiece ships) {
    if (ships.collidesWithGamePiece(this.first)) {
      return this.first.explode().append(this.rest.spawnedBullets(ships));
    }
    else {
      return this.rest.spawnedBullets(ships);
    }
  }

}

// standard Posn class
class MyPosn extends Posn {

  MyPosn(int x, int y) {
    super(x, y);
  }

  MyPosn(Posn p) {
    this(p.x, p.y);
  }

  MyPosn add(MyPosn n) {
    return new MyPosn(n.x + this.x, n.y + this.y);
  }

  boolean isOffScreen(int width, int height) {
    return (this.x < 0 || this.x > width || this.y < 0 || this.y > height);
  }

}

// represents a game piece
abstract class GamePiece {
  MyPosn position;
  MyPosn velocity;
  int radius;

  GamePiece(MyPosn position, MyPosn velocity, int radius) {
    this.position = position;
    this.velocity = velocity;
    this.radius = radius;
  }

  // moves a game piece to proper location
  public abstract GamePiece move();

  // places an element on world scene
  public abstract WorldScene place(WorldScene world);

  // compares positions of game pieces
  public boolean collidesWithHelp(GamePiece other) {
    return ((this.position.x - other.position.x) * (this.position.x - other.position.x)
        + (this.position.y - other.position.y)
            * (this.position.y - other.position.y)) <= (this.radius + other.radius)
                * (this.radius + other.radius);
  }

  //determines if a game piece is off screen 
  public boolean isOffScreen(int width, int height) {
    return this.position.isOffScreen(width + this.radius, height + this.radius);
  }

  // explodes a game piece
  public ILoGamePiece explode() {
    return new MtLoGamePiece();
  }
}

// represents a bullet
class Bullet extends GamePiece {
  int n; // represents the number of bullets a bullet explodes into

  Bullet(MyPosn position, MyPosn velocity, int radius, int n) {
    super(position, velocity, Math.min(radius, 10));
    this.n = n;
  }

  // draws a bullet
  WorldImage drawBullet() {
    return new CircleImage(this.radius, OutlineMode.SOLID, Color.gray);
  }

  // places a bullet on scene
  public WorldScene place(WorldScene world) {
    return world.placeImageXY(this.drawBullet(), this.position.x, this.position.y);
  }

  // moves a bullet
  public Bullet move() {
    return new Bullet(this.position.add(velocity), this.velocity, this.radius, this.n);
  }

  //explodes this bullet into the correct amount of bullets
  public ILoGamePiece explode() {
    return explodeHelp(this.n + 1);
  }

  // helps create the correct amount of new bullets for an explosion
  public ILoGamePiece explodeHelp(int i) {
    if (i == 0) {
      return new MtLoGamePiece();
    }
    else {
      MyPosn velocity = new MyPosn((int) (8 * (Math.cos(Math.toRadians(i * 360 / (this.n + 1))))),
          (int) (8 * (Math.sin(Math.toRadians(i * 360 / (this.n + 1))))));
      return new ConsLoGamePiece(new Bullet(this.position, velocity, this.radius + 2, this.n + 1),
          this.explodeHelp(i - 1));
    }
  }

}

// represents a ship
class Ship extends GamePiece {
  boolean left;

  Ship(MyPosn position, MyPosn velocity, int radius, boolean left) {
    super(position, new MyPosn(8, 0), 20);
    this.velocity = new MyPosn(8, 0);
    this.left = left;

  }

  // draws a ship
  WorldImage drawShip() {
    return new CircleImage(this.radius, OutlineMode.SOLID, Color.green);
  }

  // places a ship on a world scene
  public WorldScene place(WorldScene world) {
    return world.placeImageXY(this.drawShip(), this.position.x, this.position.y);
  }

  // moves a ship
  public Ship move() {
    return new Ship(this.position.add(velocity), this.velocity, this.radius, this.left);
  }
}

// represents a game
class MyGame extends World {
  int width;
  int height;
  int currentTick;
  int numBullets;
  int shipsDestroyed;
  boolean welcomeScreen;
  ILoGamePiece listOfShips;
  ILoGamePiece listOfBullet;

  MyGame(int numBullets) {
    this.numBullets = numBullets;
    this.width = 500;
    this.height = 300;
    this.currentTick = 0;
    this.welcomeScreen = true;
    this.listOfShips = new MtLoGamePiece();
    this.listOfBullet = new MtLoGamePiece();
  }

  // constructor with all information
  MyGame(int width, int height, int currentTick, int numBullets, int shipsDestroyed,
      ILoGamePiece listOfShips, ILoGamePiece listOfBullet) {
    if (width < 0 || height < 0 || numBullets < 0) {
      throw new IllegalArgumentException("Invalid arguments passed to constructor.");
    }
    else {
      this.width = width;
      this.height = height;
      this.currentTick = currentTick;
      this.numBullets = numBullets;
      this.shipsDestroyed = shipsDestroyed;
      this.listOfShips = listOfShips;
      this.listOfBullet = listOfBullet;
    }
  }

  // constructor with only width height and numbullets
  MyGame(int width, int height, int numBullets) {
    this(width, height, 1, numBullets, 0, new MtLoGamePiece(), new MtLoGamePiece());
  }

  @Override

  // creates scene
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(this.width, this.height);
    scene = addInfoToScene(scene);
    scene = listOfShips.placeAll(scene);
    scene = listOfBullet.placeAll(scene);
    return scene;
  }

  // displays welcome image
  WorldScene addWelcomeMessage(WorldScene scene) {
    return scene.placeImageXY(new TextImage("Game will start shortly.", Color.green), 250, 250);
  }

  // adds info to scene
  WorldScene addInfoToScene(WorldScene scene) {
    WorldScene bulletScene = scene.placeImageXY(
        new TextImage("Bullets Remaining: " + Integer.toString(this.numBullets), Color.black), 100,
        10);
    WorldScene shipScene = bulletScene.placeImageXY(
        new TextImage("  Ships Killed: " + Integer.toString(this.shipsDestroyed), Color.black), 400,
        10);
    WorldScene sceneWithShoot = shipScene.placeImageXY(
        new HexagonImage(30, OutlineMode.SOLID, Color.PINK), this.width / 2, this.height - 20);
    return sceneWithShoot;
  }

  // moves a game piece
  public MyGame movePieces() {
    return new MyGame(this.width, this.height, this.currentTick + 1, this.numBullets,
        this.shipsDestroyed, this.listOfShips.moveAll(), this.listOfBullet.moveAll());
  }

  @Override
  // changes elements on each tick
  public MyGame onTick() {
    return this.shipsDestroyed().addships().addBullets().movePieces().removeOffScreen();
  }

  // removes any game pieces that go off screen
  public MyGame removeOffScreen() {
    return new MyGame(this.width, this.height, this.currentTick, this.numBullets,
        this.shipsDestroyed, this.listOfShips.removeOffScreen(this.width, this.height),
        this.listOfBullet.removeOffScreen(this.width, this.height));
  }

  // makes random ship
  public Ship addShip() {
    int randX = (new Random()).nextInt(this.width);
    int randY = (new Random()).nextInt(this.height);
    boolean randBool = (new Random()).nextBoolean();
    int x;
    int y;
    if (randBool = true) {
      x = 0;
      y = new Random().nextInt(this.height);
    }
    else {
      x = this.width;
      y = new Random().nextInt(this.height);
    }
    return new Ship(new MyPosn(x, y), new MyPosn(8, 0), 20, randBool);

  }

  // generate random numships
  public ILoGamePiece randomNumShips(int num) {
    if (num <= 0) {
      return new MtLoGamePiece();
    }
    else {
      return new ConsLoGamePiece(this.addShip(), this.randomNumShips(num - 1));
    }
  }

  // adds a list of ships to game
  public MyGame addships() {
    if (this.currentTick % 28 == 0) {
      int ships = new Random().nextInt(3) + 1;
      return new MyGame(this.width, this.height, this.currentTick, this.numBullets,
          this.shipsDestroyed, this.listOfShips.append(this.randomNumShips(ships)),
          this.listOfBullet);
    }
    else {
      return this;
    }

  }

  //increments the count of ships destroyed
  public MyGame shipsDestroyed() {
    int shipsDestroyed = this.listOfShips.countCollisions(this.listOfBullet);
    return new MyGame(this.width, this.height, this.currentTick, this.numBullets,
        this.shipsDestroyed + shipsDestroyed, this.listOfShips, this.listOfBullet);
  }

  // removes the ships that touch a bullet
  public MyGame removeShips() {
    return new MyGame(this.width, this.height, this.currentTick, this.numBullets,
        this.shipsDestroyed, this.listOfShips.collidesWith(this.listOfBullet), this.listOfBullet);
  }

  // removes the bullets that touch a ship
  public MyGame removeBullets() {
    return new MyGame(this.width, this.height, this.currentTick, this.numBullets,
        this.shipsDestroyed, this.listOfShips, this.listOfBullet.collidesWith(this.listOfBullet));
  }

  // displays spawned bullets to the game after collision
  public MyGame addBullets() {
    ILoGamePiece newBullets = this.listOfBullet.spawnedBullets(this.listOfShips);
    ILoGamePiece untouchedBullets = this.listOfBullet.collidesWith(this.listOfShips);
    ILoGamePiece untouchedShips = this.listOfShips.collidesWith(this.listOfBullet);
    return new MyGame(this.width, this.height, this.currentTick, this.numBullets,
        this.shipsDestroyed, untouchedShips, untouchedBullets.append(newBullets));
  }

  // adds 1 to tick
  public MyGame incrementGameTick() {
    return new MyGame(this.width, this.height, this.currentTick + 1, this.numBullets,
        this.shipsDestroyed, this.listOfShips, this.listOfBullet);
  }

  // represents what happens when user presses space bar
  public MyGame onKeyEvent(String key) {
    Bullet newBullet = new Bullet(new MyPosn(this.width / 2, this.height - 40), new MyPosn(0, -25),
        5, 1);
    if (key.equals(" ")) {
      return new MyGame(this.width, this.height, this.currentTick, this.numBullets - 1,
          this.shipsDestroyed, this.listOfShips,
          new ConsLoGamePiece(newBullet, this.listOfBullet.moveAll()));
    }
    else {
      return this;
    }
  }

  // end game conditions
  @Override
  public WorldEnd worldEnds() {
    if (this.numBullets <= 0) {
      return new WorldEnd(true, this.makeEndScene());
    }
    else {
      return new WorldEnd(false, this.makeEndScene());
    }
  }

  // end scene 
  public WorldScene makeEndScene() {
    WorldScene endScene = new WorldScene(this.width, this.height);
    WorldScene gameOver = endScene.placeImageXY(new TextImage("Game Over", Color.red), 250, 250);
    return gameOver.placeImageXY(
        new TextImage("Ships Destroyed: " + this.shipsDestroyed, Color.black), 250, 275);
  }

}

// examples
class ExamplesMyWorldProgram {
  boolean testBigBang(Tester t) {
    MyGame world = new MyGame(500, 500, 10);
    // width, height, tick rate = 0.5 means every 0.5 seconds the onTick method will
    // get called.
    return world.bigBang(500, 500, 1.0 / 28.0);
  }
}
