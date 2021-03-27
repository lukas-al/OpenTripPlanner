package org.opentripplanner.transit.raptor.rangeraptor.transit;


import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

//@PHilita
// Imports
import java.sql.*;
import org.opentripplanner.transit.raptor.transit.tripData;

/**
 * The responsibility for the cost calculator is to calculate the default  multi-criteria cost.
 * <P/>
 * This class is immutable and thread safe.
 */
public class DefaultCostCalculator<T extends RaptorTripSchedule> implements CostCalculator<T> {
    private final int boardCost;
    private final int walkFactor;
    private final int waitFactor;
    private final int transitFactor;
    private final int[] stopVisitCost;

    /**
     * We only apply the wait factor between transits, not between access and transit;
     * Hence we start with 0 (zero) and after the first round we set this to the
     * provided {@link #waitFactor}. We assume we can time-shift the access to get rid
     * of the wait time.
     */
    private int waitFactorApplied = 0;

////////////////////////////////////////////////////////////////////////////////////////
    // @PHilita
    // Create the Connection method. 
    public static Connection getConnection() {
        Connection connection = null;
        if (connection == null) {
            String url = "jdbc:postgresql://127.0.0.1/Philita";
            String user = "Philita";
            String password = "HP2310i";

            try {
                connection = DriverManager.getConnection(url, user, password);
            } catch (SQLException ex) {
                System.err.println("Big oof in JDBC connection" + ex);
                System.err.println("Terminating Philita run");
                System.exit(3);
                /**
                 * OK just exiting when we are knee deep in the code is gonna hard crash the
                 * Java VM TODO: Improve the error handling by retrying the connection, moving
                 * to non-Philita mode for the current journey & logging the error.
                 */
            }
        }

        return connection;
    }


    //  Create the occupancyCost method
    public static tripData occupancyCost(String tripId, Connection connection) {
        var tripdata = new tripData(); // Create a tripData holder
        String sql = "SELECT business FROM public.\"RouteCapacity\" WHERE route_id = '" + tripId + "'";
        System.out.println("Method instantiated");

        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                Integer busyness = resultSet.getInt("business"); // This will set the busyness to null if the GTFS id is
                                                                 // not found.
                String GTFSId = tripId;

                tripdata.GTFSIdSet(GTFSId);
                tripdata.busynessSet(busyness);

            }

        } catch (SQLException ex) {
            tripdata.busynessSet(null);
            System.err.println("Failed to return occupancy for tripId " + tripId + ", busyness set to null");
            System.err.println(ex);
        }

        return tripdata;

    };

////////////////////////////////////////////////////////////////////////////////////////
    public DefaultCostCalculator(
            int[] stopVisitCost,
            int boardCost,
            double walkReluctanceFactor,
            double waitReluctanceFactor,
            WorkerLifeCycle lifeCycle
    ) {
        this.stopVisitCost = stopVisitCost;
        this.boardCost = RaptorCostConverter.toRaptorCost(boardCost);
        this.walkFactor = RaptorCostConverter.toRaptorCost(walkReluctanceFactor);
        this.waitFactor = RaptorCostConverter.toRaptorCost(waitReluctanceFactor);
        this.transitFactor = RaptorCostConverter.toRaptorCost(1.0);
        lifeCycle.onPrepareForNextRound(this::initWaitFactor);
    }

    @Override
    public int onTripRidingCost(
        ArrivalView<T> previousArrival,
        int waitTime,
        int boardTime,
        T trip
    ) {
        // The relative-transit-ime is time spent on transit. We do not know the alight-stop, so
        // it is impossible to calculate the "correct" time. But the only thing that maters is that
        // the relative difference between to boardings are correct, assuming riding the same trip.
        // So, we can use the negative board time as relative-transit-time.
        final int relativeTransitTime =  - boardTime;

        int cost = previousArrival.cost()
            + waitFactorApplied * waitTime
            + transitFactor * relativeTransitTime
            + boardCost;

        if(stopVisitCost != null) {
            cost += stopVisitCost[previousArrival.stop()];
        }
////////////////////////////////////////////////////////////////////////////////////////
        
        // @PHilita 
        // Activate the connection
        Connection connection = getConnection();

        if (connection != null) {
            System.out.println("Connection achieved");
        } else {
            System.err.println("Connection NOT working");
            System.exit(3);
        }
        // Get cost due to occupancy on trip
        String tripId = "XXX";
        tripData tripdata1 = occupancyCost(tripId, connection);

        // Add cost due to occupancy on trip if not null. Print for debugging. 
        if (tripdata1.busynessRead() != null) {
            cost += tripdata1.busynessRead();
            System.out.println("The trip of GTFSId " + tripId + " has occupancy " + tripdata1.busynessRead() + ".");
        } else {
            System.err.println("The trip of GTFSId " + tripId + " has no entry in the database. Returning occupancy as "
            + tripdata1.busynessRead() + ".");
        }

////////////////////////////////////////////////////////////////////////////////////////        s
        return cost;
    }

    @Override
    public int transitArrivalCost(
        ArrivalView<T> previousArrival,
        int waitTime,
        int transitTime,
        int toStop,
        T trip
    ) {
        int cost = waitFactorApplied * waitTime + transitFactor * transitTime + boardCost;
        if(stopVisitCost != null) {
            cost += stopVisitCost[previousArrival.stop()] + stopVisitCost[toStop];
        }
        return cost;
    }

    @Override
    public int walkCost(int walkTimeInSeconds) {
        return walkFactor * walkTimeInSeconds;
    }

    @Override
    public int waitCost(int waitTimeInSeconds) {
        return waitFactor * waitTimeInSeconds;
    }

    @Override
    public int calculateMinCost(int minTravelTime, int minNumTransfers) {
        return  boardCost * (minNumTransfers + 1) + transitFactor * minTravelTime;
    }

    private void initWaitFactor(int round) {
        // For access(round 0) and the first transit round(1) skip adding a cost for waiting,
        // we assume we can time-shift the access leg.
        this.waitFactorApplied = round < 2 ? 0 : waitFactor;
    }
}
