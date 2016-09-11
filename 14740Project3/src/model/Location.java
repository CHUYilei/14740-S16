package model;

/**
 *
 * @Author:
 * Xiaocheng OU
 * Yilei CHU

 simulate the location condition of each node, 
 used for selecting closer nodes in peer selection
 */
public class Location {
    private double longitude,latitude;

    public Location(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public static double getDistance(Location l1,Location l2){
        return Math.pow(l1.latitude-l2.latitude,2)+Math.pow(l1.longitude-l2.longitude,2);
    }
    
    public String toString(){
        return "latitude:"+latitude+",longitude:"+longitude;
    }
}
