package org.opentripplanner.transit.raptor.rangeraptor.transit.philita;

import java.sql.*;

public class occupancyCost {

    public static TripData OccupancyCostCalc(String tripId, Connection connection) {
        TripData tripdata = new TripData();
        String sql = "SELECT business FROM public.\"RouteCapacity\" WHERE route_id = '" + tripId + "'";
        tripdata.GTFSIdSet(tripId);

        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                Integer busyness = resultSet.getInt("business"); // This will set the busyness to null if the GTFS id //
                                                                 // not found.
                tripdata.busynessSet(busyness);
            } else {
                System.err.println("Failed to return occupancy for tripId " + tripId + ", busyness set to max");
                tripdata.busynessSet(null);
            }

        } catch (SQLException ex) {
            tripdata.busynessSet(null);
            System.err.println(
                    "Failed to return occupancy for tripId " + tripId + ", busyness set to max, SQL err:" + ex);
            System.err.println(ex);
        }

        return tripdata;
    }
}
