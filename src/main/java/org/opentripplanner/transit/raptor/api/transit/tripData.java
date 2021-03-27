/**
 * A class which to hold the information extracted from the tripId table.
 */

// Packaging the class to use in testing
package mypack;

public class tripData {
    private Integer busyness; // Only occupancy is implemented for now
    private String GTFSId; // The GTFS

    @Override
    public String toString() {
        return "tripData[" + "GTFSId=" + GTFSId + ", busyness=" + busyness + ']';
    }

    // Method to set the busyness
    public void busynessSet(Integer busyness) {
        this.busyness = busyness;
    }

    // Method to read the busyness
    public Integer busynessRead() {
        return this.busyness;
    }

    // Method to set the GTFSId
    public void GTFSIdSet(String GTFSId) {
        this.GTFSId = GTFSId;
    }

    // Method to read the GTFSId
    public String GTFSIdRead() {
        return this.GTFSId;
    }

}
