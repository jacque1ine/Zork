import java.util.ArrayList;

public class Inventory{
  //collection of Item objects
	private ArrayList<Item> items; 

	public Inventory(){
		  items = new ArrayList<Item>(); 
  }


	public boolean addItem(Item item){
    	return items.add(item); 
	}
	
	public void justAddItem(Item item){
    	items.add(item); 
	}

  /* returns the items based on the name given 
  if the item is not in inventory, return null 
   */
	public Item removeItem(String name){
    	for(int i=0; i<items.size(); i++){
      		if (name.equalsIgnoreCase(items.get(i).getName())){
        		return items.remove(i);
      		}
		}
    	return null; 
  	}

	public String toString(){
    	if(items.size() ==0){
     		 return "nothing\n";
    	}
   		String msg = ""; 
    	for(Item i: items){
      		msg+=i.getName() + "\n";
    	}
    	return msg;
  }

	public Item contains(String name){
    	for (int i=0; i<items.size(); i++){
      		if (name.equalsIgnoreCase(items.get(i).getName())){
        		return items.get(i);
      		}
    	}
		return null;
  }

	public boolean hasItem(String itemName) {
		for(Item i : items){
            if(i.getName().equalsIgnoreCase(itemName)){
                return true;
            }
        }
        return false;
	}

	public ArrayList<Item> getInventory(){
        return items;
	}
	
	public int getNumItems(){
		return items.size();
	}

	public int getInvWeight(){
        int total = 0;
        for (int i=0; i<items.size(); i++){
            total+= items.get(i).getWeight();
        }
        return total;
	}
	
	public boolean canHold(int num){
	  int weight = getInvWeight() + num; 
	  if (weight>15){
		  return false;
	  }
	  return true; 
    }
}