import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;


/*
 *Class Game - the main class of the "Zork" game.
 *
 *Author: Michael Kolling Version: 1.1 Date: March 2000
 *
 *This class is the main class of the "Zork" application. Zork is a very
 *simple, text based adventure game. Users can walk around some scenery. That's
 *all. It should really be extended to make it more interesting!
 *
 *To play this game, create an instance of this class and call the "play"
 *routine.
 *
 *This main class creates and initialises all the others: it creates all rooms,
 *creates the parser and starts the game. It also evaluates the commands that
 *the parser returns.
 */
class Game {
	private Parser parser;
	private Room currentRoom;
	private Inventory inventory;
	// This is a MASTER object that contains all of the rooms and is easily
	// accessible.
	// The key will be the name of the room -> no spaces (Use all caps and
	// underscore -> Great Room would have a key of GREAT_ROOM
	// In a hashmap keys are case sensitive.
	// masterRoomMap.get("GREAT_ROOM") will return the Room Object that is the Great
	// Room (assuming you have one).
	private HashMap<String, Room> masterRoomMap;
	private HashMap<String, Item> masterItemMap;

	/*
	initItems: creates items from the items.dat file, puts the items into their inventories,
	and puts items into hashmap
	*/
	private void initItems(String fileName) throws Exception{
		Scanner itemScanner;
		masterItemMap = new HashMap<String, Item>();
		
		try {
			itemScanner = new Scanner(new File(fileName));
			while (itemScanner.hasNext()) {

				//Create new item
				Item item = new Item();

				//Read and set name
				String itemName = getNextLine(itemScanner).split(":")[1].trim();
				item.setName(itemName);

				//Read and set description
				String itemDesc = getNextLine(itemScanner).split(":")[1].trim();
				item.setDescription(itemDesc.replaceAll("<br>", "\n").trim());

				//Read and set whether or not this object has its own inventory or is "openable"
				Boolean openable = Boolean.valueOf(getNextLine(itemScanner).split(":")[1].trim());
				item.setOpenable(openable);

				//Read and set weight
				int weight = Integer.parseInt(itemScanner.nextLine().split(":")[1].trim()); 
        		item.setWeight(weight); 
				
				//Puts into hashmap
				masterItemMap.put(itemName.toUpperCase().replaceAll(" ", "_"), item);
				
				//Adding items into the inventories of their respective rooms or within other items 
				String temp = getNextLine(itemScanner);
				String itemType = temp.split(":")[0].trim();
				String name = temp.split(":")[1].trim();
				if (itemType.equals("Room"))
					masterRoomMap.get(name).getInventory().addItem(item);
				else
					masterItemMap.get(name).addItem(item);
			}
		}catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*
	initRooms: creates room objects from the room.dat file, and ensures that each room has exits
	*/
	private void initRooms(String fileName) throws Exception {
		masterRoomMap = new HashMap<String, Room>();
		Scanner roomScanner;
		try {
			HashMap<String, HashMap<String, String>> exits = new HashMap<String, HashMap<String, String>>();
			roomScanner = new Scanner(new File(fileName));
			while (roomScanner.hasNext()) {
				Room room = new Room();
				
				// Read the Name
				String roomName = getNextLine(roomScanner);
				room.setRoomName(roomName.split(":")[1].trim());
				
				// Read the Description
				String roomDescription = getNextLine(roomScanner);
				room.setDescription(roomDescription.split(":")[1].replaceAll("<br>", "\n").trim());
				
				// Read the Exits
                String roomExits = getNextLine(roomScanner);
				
				//Read if room is locked 
				boolean locked = Boolean.parseBoolean(getNextLine(roomScanner).split(": ")[1].replaceAll("<br>", "\n").trim());
				room.setLocked(locked);

				//read if room kills you
				boolean isKiller = Boolean.parseBoolean(getNextLine(roomScanner).split(": ")[1].replaceAll("<br>", "\n").trim());
				room.setKill(isKiller);

				// An array of strings in the format E-RoomName
				String[] rooms = roomExits.split(":")[1].split(",");
				HashMap<String, String> temp = new HashMap<String, String>();
				for (String s : rooms) {
					temp.put(s.split("-")[0].trim(), s.split("-")[1]);
				}

				exits.put(roomName.substring(10).trim().toUpperCase().replaceAll(" ", "_"), temp);

				// This puts the room we created (Without the exits in the masterMap)
				masterRoomMap.put(roomName.toUpperCase().substring(10).trim().replaceAll(" ", "_"), room);
			}
			
			//Set the exits.
			for (String key : masterRoomMap.keySet()) {
				Room roomTemp = masterRoomMap.get(key);
				HashMap<String, String> tempExits = exits.get(key);
				for (String s : tempExits.keySet()) {// s = direction
					String roomName2 = tempExits.get(s.trim());
					Room exitRoom = masterRoomMap.get(roomName2.toUpperCase().replaceAll(" ", "_"));
                    roomTemp.setExit(s.trim().charAt(0), exitRoom); 
				}
			}
		roomScanner.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*
	getNextLine: ignores empty lines. Allows for blank lines in .dat files. 
	*/
	private String getNextLine(Scanner roomScanner){
    	String nextLine = roomScanner.nextLine(); 
    	while(nextLine != null && nextLine.trim().equals("")){
      		nextLine = roomScanner.nextLine();
    	}
    	return nextLine;
  	}

	/*
	Game Constructor: creates the game and initialise its internal map.
	*/
	public Game() {
		try {
			initRooms("data/Rooms.dat");	// creates the map from the rooms.dat file
			// initRooms is responsible for building/ initializing the masterRoomMap (private instance variable)
			currentRoom = masterRoomMap.get("SECRET_STUDY");	// the key for the masterRoomMap is the name of the room all in Upper Case (spaces replaced with _)
			inventory = new Inventory();

			//Set the kets to each room. setKey() indicates the item needed
			masterRoomMap.get("GARAGE_1").setKey("REMOTE");
			masterRoomMap.get("GARAGE_SECRET_ROOM").setKey("WATCH");
			masterRoomMap.get("SECRET_STUDY").setKey("KEYCARD");
			masterRoomMap.get("ATTIC_CONTINUED").setKey("LANTERN");
			
      		initItems("data/items.dat");
		} catch (Exception e) {
			e.printStackTrace();
		}
		parser = new Parser();
	}

	
	/*
	 Main play routine. Loops until end of play.
	 */
	public void play() {
		printWelcome();
		String message = "";
		boolean finished = false;
	
		//Repeatedly read commands and execute them until the game is over.
		while (!finished) {
			Command command = parser.getCommand();
			if(processCommand(command) || hasWon() || currentRoom.isKiller()){
				finished = true;
			}	
		}
		if (currentRoom.isKiller()){
			message=("You have died. Try again.");
		}else if(hasWon()){
			message=("CONGRATS!!! You have arrived safely back home.\nHopefully you had fun in the past even though it might have beenjust a little scary.bye");
		}
		System.out.println(message + "\nThank you for playing.  Good bye.");
	}

	/*
	 *Print out the opening message for the player.
	 */
	private void printWelcome() {
		System.out.println();
		System.out.println("Welcome to Timequest");
		System.out.println("Timequest is a fun text-based adventure game which draws from elements of Back to the Future.\n" + 
		" You will see that in this game some areas are locked, while some are not. Once you have a certain item in your inventory the areas will unlock");
		System.out.println("Type 'help' if you need help.");
		System.out.println();
		System.out.println(currentRoom.shortDescription());
	}

	/*
	 *Given a command, process (that is: execute) the command. If this command ends
	 *the game, true is returned, otherwise false is returned.
	 */
	private boolean processCommand(Command command) {
		if (command.isUnknown()) {
			System.out.println("I don't know what you mean...");
			return false;
		}
		String commandWord = command.getCommandWord();

		if (commandWord.equalsIgnoreCase("help"))
			printHelp();
		else if (commandWord.equalsIgnoreCase("go"))
			goRoom(command);
		else if (commandWord.equalsIgnoreCase("quit")) {
			if (command.hasSecondWord())
				System.out.println("Quit what?");
			else
				return true; // signal that we want to quit
		
		}else if (commandWord.equalsIgnoreCase("eat")) {
			eat(command.getSecondWord());
		} else if (commandWord.equalsIgnoreCase("talk")) {
			return talk();
		} else if (commandWord.equalsIgnoreCase("look")) {
			System.out.println(currentRoom.lookAround());
		} else if (commandWord.equalsIgnoreCase("jump")) {
			return jump();
		} else if (commandWord.equalsIgnoreCase("sit")) {
			sit();
		} else if ("udeswn".indexOf(commandWord) > -1) {
			goRoom(command);
		} else if (commandWord.equalsIgnoreCase("take")) {
			if (!command.hasSecondWord())
				System.out.println("Take what?");
			else if(command.hasSecondWord() && command.hasThirdWord())
				takeItem(command.getSecondWord() + " " + command.getThirdWord());
			else
				takeItem(command.getSecondWord());
		} else if (commandWord.equalsIgnoreCase("drop")) {
			if (!command.hasSecondWord())
				System.out.println("Drop what?");
			else if(command.hasSecondWord() && command.hasThirdWord())
				dropItem(command.getSecondWord() + " " + command.getThirdWord());
			else
				dropItem(command.getSecondWord());
		} else if (commandWord.equalsIgnoreCase("inspect") || commandWord.equalsIgnoreCase("read")){
			if (!command.hasSecondWord())
				System.out.println("c'mon you have to tell me what to specfically look at");
			else if(command.hasSecondWord() && command.hasThirdWord())
				inspectItem(command.getSecondWord() + " " + command.getThirdWord());
			else
				inspectItem(command.getSecondWord());
		}else if (commandWord.equalsIgnoreCase("wear")) {
				if (!command.hasSecondWord())
					System.out.println("Wear what?");
				else
					wearItem(command.getSecondWord());
		} else if (commandWord.equalsIgnoreCase("inventory")) {
			System.out.println("You are carrying the following:\n" + inventory);
		} else if (commandWord.equalsIgnoreCase("open")) {
			if (!command.hasSecondWord())
				System.out.println("Open what?");
			else if(command.hasSecondWord() && command.hasThirdWord())
				openItem(command.getSecondWord() + " " + command.getThirdWord());
			else
				openItem(command.getSecondWord());
		} else if (commandWord.equalsIgnoreCase("throw")){
			System.out.println("There are better ways to put things down"); 
		} else if (commandWord.equalsIgnoreCase("kill")){
			System.out.println("Great Scott!! Why are you so violent");
		} else if(commandWord.equalsIgnoreCase("sleep")){
			System.out.println("sleeping is for losers... don't you want to go back home?");
		} 
			// }else if(commandWord.equalsIgnoreCase("pull")){
		// 	//add of it has second word integrate with pull method
		// 	if (!command.hasSecondWord()){
		// 		System.out.println("pull what?");
		// 	}
		// 	else
		// 		System.out.println("theres nothing to pull");
		
		return false;
	}

//Implementations of user commands:


	private void wearItem(String itemName) {
		if(currentRoom.getRoomName().equalsIgnoreCase("Garage Secret Room") && itemName.equalsIgnoreCase("helmet")){
			System.out.println("Yay! You now are protected"); 
		}
		else{
			System.out.println("fashion star"); 
		}
	}


	private boolean talk() {
			if(currentRoom.getRoomName().equalsIgnoreCase("Bakery")){
				System.out.println("Uh oh...remember what the letter's said: 'BE CAREFUL WITH WHEN YOU INTERACT WITH THOSE FROM THE PAST'."+ 
			"\nThe game actually hasn't happened yet, or the semi-finals, but you revealed the results, and provided lots of detail."+
			"\nThese two nobodies beleived you, and through betting became super rich.\nYOU CHANGED HISTORY, AND THEREFORE YOU NO LONGER EXISTS");
			}
			else{
				System.out.println("By talking to them, you have messsed up the timeline...YOU NO LONGER EXIST");
			}
		return true;
		
	}

	private void inspectItem(String itemName) {
			Item playerItem = inventory.contains(itemName);
			Item roomItem = currentRoom.getInventory().contains(itemName);
			
			if(playerItem != null) {
				System.out.println(playerItem.getDescription());
			}else if(roomItem != null){
				System.out.println(roomItem.getDescription());
			}else{
				System.out.println("I cannot inspect what is not there.");
			}
	}

	/*
	*Open Item: opens the item requested if there is an item in the inventory.
	*/
		private void openItem(String itemName) {
			Item playerItem = inventory.contains(itemName);
			Item roomItem = currentRoom.getInventory().contains(itemName);
			
			if(playerItem != null) {
				System.out.println(playerItem.displayContents());
			}else if(roomItem != null ){
				System.out.println(roomItem.displayContents());
			}else{
				System.out.println("you can't open this item.");
			}
		}

	/*
	*Take Item: removes item from room inventory and puts it in the player's inventory.
	*/
	private void takeItem(String itemName) {
		Inventory roomInventory = currentRoom.getInventory();
		Item item = roomInventory.removeItem(itemName); 

    	//if item is in room inventory
		if (item != null){

			if(item.canPickUp()){
				if(inventory.getInvWeight()<=15){
					if(inventory.addItem(item)){
						System.out.println("You have taken the " + itemName);
					}
				}else{
					System.out.println(itemName + " is there but you do not have space. Drop some items and try again"+
					"\nYou have have " + inventory.getInvWeight() + "/15 slots filled in your inventory."); 
					currentRoom.getInventory().justAddItem(item); 
				}
			}else{
				System.out.println(itemName + " is too heavy to pickup");
				currentRoom.getInventory().justAddItem(item);
			}	
		}else{

			 //go through the player's inventory to see if requested item is within any items that can contain other items
			for (int i=0; i<roomInventory.getNumItems(); i++){

				 //accessing the items arrayList of room inventory and getting one of the indexes, and getting the contents of that item. 
        		Inventory itemInventory = roomInventory.getInventory().get(i).getContents(); 
				
				//if the current item has an invetory which contains the target item
				if (itemInventory != null && itemInventory.hasItem(itemName)){ 
					Item removedItem = itemInventory.removeItem(itemName);
					if(removedItem.canPickUp()){
						if(inventory.getInvWeight()<=15){
							if(inventory.addItem(removedItem)){
								System.out.println("You have taken the " + itemName);
							}
						}else{
							System.out.println(itemName + " is there but you do not have space. Drop some items and try again"+
							"\nYou have have " + inventory.getInvWeight() + "/15 slots filled in your inventory."); 
							itemInventory.getInventory().get(i).addItem(removedItem); 
						}
					}else{
						System.out.println(itemName + " is too heavy to pickup");
						itemInventory.getInventory().get(i).addItem(removedItem); 
					}
				}
				else{
					System.out.println("You were unable to take the " + itemName + " here.\nYou have have " + inventory.getInvWeight() + "/15 slots filled in your inventory."); 
				}
					
			}
		}
		if(itemName.equalsIgnoreCase("helmet") && currentRoom.getRoomName().equalsIgnoreCase("Garage Secret Room")){
			System.out.println("You are now protected"); 
		}else if(itemName.equalsIgnoreCase("watch") && currentRoom.getRoomName().equalsIgnoreCase("Garage 1")){
			System.out.println("You hear a click, and the locked door to your west clicks open");
		}else if(itemName.equalsIgnoreCase("keycard") && currentRoom.getRoomName().equalsIgnoreCase("Science Section")){
			System.out.println("You have unlocked something, but what is it?");
		}else if(itemName.equalsIgnoreCase("remote") && currentRoom.getRoomName().equalsIgnoreCase("Garage Secret Room")){
			System.out.println("You know that flimsy wall you were suspicious about? Well it seems like by taking the remote that wall slide open and revealed a new part of the garage");
		}
	}

	/*
	*Drop Item: removes item from player's inventory and adds it to the room they are in.
	*/
	private void dropItem(String itemName) {
		Item item = inventory.removeItem(itemName);
		
		if (item != null) {
			if (currentRoom.getInventory().addItem(item)) {
				System.out.println("You have dropped the " + itemName);
			}else {
				System.out.println("You were unable to drop the " + itemName);
			}
		}else {
			System.out.println("You are not carrying a " + itemName + ".");
		}
	}


	/*
	 *HasWon: once a player as the speciifc item, the method will return true. 
	 */
	private boolean hasWon(){
		if(inventory.hasItem("Plutonium Nuclear Reactor")){
			return true;
		}
		return false;
	}

	/*
	 *Print Help: Print out some help information. Here we print some stupid, cryptic message
	 *and a list of the command words.
	 */
	private void printHelp() {
		System.out.println(" Hello? Hello? Anybody home? Huh? Think, McFly! Think!");
		System.out.println();
		System.out.println("Your command words are:");
		parser.showCommands();
	}

	/*
	 *Go Room: Try to go to one direction. If there is an exit, enter the new room,
	 *otherwise print an error message. ]
	 */
	private void goRoom(Command command) {
		if (!command.hasSecondWord() && ("udeswn".indexOf(command.getCommandWord()) < 0)) {
			// if there is no second word, we don't know where to go...
			System.out.println("Go where?");
			return;
		}
		//Allows the player to enter short forms for the directions
		String direction = command.getSecondWord();
		if ("udeswn".indexOf(command.getCommandWord()) > -1) {
			direction = command.getCommandWord();
			if (direction.equalsIgnoreCase("u"))
				direction = "up";
			else if (direction.equalsIgnoreCase("d"))
				direction = "down";
			else if (direction.equalsIgnoreCase("e"))
				direction = "east";
			else if (direction.equalsIgnoreCase("w"))
				direction = "west";
			else if (direction.equalsIgnoreCase("n"))
				direction = "north";
			else if (direction.equalsIgnoreCase("s"))
				direction = "south";
		}
		
		// Try to leave current room.
		Room nextRoom = currentRoom.nextRoom(direction);
		if (nextRoom == null)
			System.out.println("There is something obstructing your path. You cannot go this way!");
		else if (!hasKey(nextRoom) && nextRoom.getRoomName().equalsIgnoreCase("Attic Continued")){
			System.out.println("You need something bright. It is too dark here");
		}
		else if (nextRoom.isLocked() && !hasKey(nextRoom)) {
			System.out.println("The area is locked. You do not have the neccessary items to pass.");
		} else if(currentRoom.getRoomName().equalsIgnoreCase("Garage Secret Room") && nextRoom.getRoomName().equalsIgnoreCase("oldtown square")){
			if(inventory.hasItem("helmet")){
				nextRoom.setKill(false);
				currentRoom.setLocked(true);
				System.out.println("\nYOU FEEL A SERIES OF JOLTS, COLOURS EVERYWEHRE!\nYOU ARE VERY DIZZY, WHAT IS HAPPENING?!!\nAfter a series of zaps...you open your eyes and find yourself in...");
				currentRoom = nextRoom;
				System.out.println(currentRoom.shortDescription());
			}else{
				System.out.println("You were not protected from the harmful radiation of the portal");
				currentRoom=nextRoom;
			}
		} else if(nextRoom.isKiller()){
			currentRoom = nextRoom;
			if(currentRoom.getRoomName().equalsIgnoreCase("Alleyway")){
				System.out.println("you walked straight into some thugs, and you have nothing to protect yourself. You were killed because you saw something you shouldn't have");
			}else{
				System.out.println("you walked straight into danger and made a bad decision");
			}
		
		}else {
			if (nextRoom.isLocked() && hasKey(nextRoom)){
				System.out.println("You have unlocked this area");
			}else if(currentRoom.getRoomName().equalsIgnoreCase("Garage Secret Room") && nextRoom.getRoomName().equalsIgnoreCase("oldtown square")){
				currentRoom.setLocked(true);
				System.out.println("\nYOU FEEL A SERIES OF JOLTS, COLOURS EVERYWEHRE!\nYOU ARE VERY DIZZY, WHAT IS HAPPENING?!!\nAfter a series of zaps...you open your eyes and find yourself in...");
			}
			currentRoom = nextRoom;
			System.out.println(currentRoom.shortDescription());
		}
	}


	/*
	*Has Key: gets the key from a room, and checks if the player's inventory has the corresponding key
	*/
	private boolean hasKey(Room nextRoom) {
		String key = nextRoom.getKey();
		return key != null && inventory.contains(key) != null && inventory.contains(key).getName().equalsIgnoreCase(key);
	}

	//Methods for commands that just return fun Strings: 

	private void eat(String secondWord) {
		if (secondWord.equalsIgnoreCase("steak"))
			System.out.println("YUMMY");
		else if (secondWord.equalsIgnoreCase("bread"))
			System.out.println("I don't eat carbs...");
		else 
			System.out.println("You are the " + secondWord);
	}

	private void sit() {
		System.out.println("You are now sitting. You lazy excuse for a person.");	
	}

	private boolean jump() {
		System.out.println("You jumped. Ouch you fell. You fell hard. Really hard." 
		+"You are getting sleepy. Very sleepy! Yuo are dead!");
		return true;
	}

}

//METHODS NOT IN USE: 

//READ METHOD- REPLACED WITH INSPECT 

	/*
	private void readItem(String itemName){
		Item playerItem = inventory.contains(itemName);
		Item roomItem = currentRoom.getInventory().contains(itemName);
		
		if(roomItem != null || roomItem != null) {
			if (roomItem.getName().equalsIgnoreCase("letter")||playerItem.getName().equalsIgnoreCase("letter") ){
				System.out.println("DOC BROWN"); 
			}
		}else if(playerItem != null){

		}else {
			System.out.println("You can't read items unless you have them.");
		}
		
		if(playerItem != null) {
			if (playerItem.getName().equalsIgnoreCase("letter")||playerItem.getName().equalsIgnoreCase("letter") ){
				System.out.println("DOC BROWN"); 
			}
		}else if(roomItem != null){
			System.out.println(roomItem.getDescription());
		}else{
			System.out.println("I cannot inspect what is not there.");
		}
	}

		private void read(String itemName) {
// 		if (itemName.equalsIgnoreCase("newspaper")){
// 			System.out.println("Valley Gazette\nAPRIL 7 1950\n");
// 		}
// }
*/
	
