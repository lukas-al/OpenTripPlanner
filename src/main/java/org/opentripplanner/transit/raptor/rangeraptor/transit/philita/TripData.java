package org.opentripplanner.transit.raptor.rangeraptor.transit.philita;

public class TripData {

    private int busyness; // Only occupancy is implemented for now
    private String GTFSId; // The GTFS trip ID

    public String toString() {
        return "tripData[" + "GTFSId=" + GTFSId + ", busyness=" + busyness + ']';
    }

    // Method to set the busyness
    public void busynessSet(Integer busyness) {
        if (busyness != null) {
            this.busyness = busyness;
        } else {
            this.busyness = Integer.MAX_VALUE;
        }

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
