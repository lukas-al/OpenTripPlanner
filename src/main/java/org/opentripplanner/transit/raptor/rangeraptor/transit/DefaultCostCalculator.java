package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

//@PHilita
// Imports
import java.sql.*;
import org.opentripplanner.transit.raptor.rangeraptor.transit.philita.*;

// import javax.validation.constraints.Null;
// import org.opentripplanner.routing.algorithm.raptor.transit.request.TripScheduleWithOffset;
// import org.opentripplanner.model.Route;
// import org.opentripplanner.model.Trip; // USE THIS
// import org.opentripplanner.model.TripPattern;
// import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer; // Used to map from RaptorStopIndex to original
// import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
// import org.opentripplanner.routing.core.TraverseMode;
// import org.opentripplanner.routing.graphfinder.StopAtDistance;
// import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
// import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
// import org.opentripplanner.transit.raptor.api.path.Path;
// import org.opentripplanner.transit.raptor.api.path.PathLeg;
// import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
// import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;

/**
 * The responsibility for the cost calculator is to calculate the default
 * multi-criteria cost.
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
     * We only apply the wait factor between transits, not between access and
     * transit; Hence we start with 0 (zero) and after the first round we set this
     * to the provided {@link #waitFactor}. We assume we can time-shift the access
     * to get rid of the wait time.
     */
    private int waitFactorApplied = 0;

    // Busyness de-preferencing. Felix K.
    private final double limit = 0.7;
    private final int standArea = 134; // [m/2], avg val from data
    private final int seatCapacity = 243; // avg val from data
    private final String[] option = { "sit", "stand" }; // option between sitting and standing for condition two
    private static int currentOccupancy = 210; // holder for the otp number
    private static int numStand; // num of people standing

    // ////////////////////////////////////////////////////////////////////////////////////////
    // // @PHilita
    // // Create the Connection method.
    // public static Connection getConnection() {
    // Connection connection = null;
    // if (connection == null) {
    // String url = "jdbc:postgresql://127.0.0.1/Philita";
    // String user = "Philita";
    // String password = "HP2310i";

    // try {
    // connection = DriverManager.getConnection(url, user, password);
    // } catch (SQLException ex) {
    // System.err.println("Big oof in JDBC connection" + ex);
    // System.err.println("Terminating Philita run");
    // System.exit(3);
    // /**
    // * OK just exiting when we are knee deep in the code is gonna hard crash the
    // * Java VM TODO: Improve the error handling by retrying the connection, moving
    // * to non-Philita mode for the current journey & logging the error.
    // */
    // }
    // }
    // return connection;
    // }

    // // Create the occupancyCost method
    // public static tripData occupancyCost(String tripId, Connection connection) {
    // var tripdata = new tripData(); // Create a tripData holder
    // String sql = "SELECT business FROM public.\"RouteCapacity\" WHERE route_id =
    // '" + tripId + "'";
    // System.out.println("Method instantiated");

    // try (Statement statement = connection.createStatement(); ResultSet resultSet
    // = statement.executeQuery(sql)) {

    // if (resultSet.next()) {
    // Integer busyness = resultSet.getInt("business"); // This will set the
    // busyness to null if the GTFS id is
    // // not found.
    // String GTFSId = tripId;

    // tripdata.GTFSIdSet(GTFSId);
    // tripdata.busynessSet(busyness);

    // }

    // } catch (SQLException ex) {
    // tripdata.busynessSet(null);
    // System.err.println("Failed to return occupancy for tripId " + tripId + ",
    // busyness set to null");
    // System.err.println(ex);
    // }

    // return tripdata;

    // };

    // ////////////////////////////////////////////////////////////////////////////////////////

    public DefaultCostCalculator(int[] stopVisitCost, int boardCost, double walkReluctanceFactor,
            double waitReluctanceFactor, WorkerLifeCycle lifeCycle) {
        this.stopVisitCost = stopVisitCost;
        this.boardCost = RaptorCostConverter.toRaptorCost(boardCost);
        this.walkFactor = RaptorCostConverter.toRaptorCost(walkReluctanceFactor);
        this.waitFactor = RaptorCostConverter.toRaptorCost(waitReluctanceFactor);
        this.transitFactor = RaptorCostConverter.toRaptorCost(1.0);
        lifeCycle.onPrepareForNextRound(this::initWaitFactor);
    }

    @Override
    public int onTripRidingCost(ArrivalView<T> previousArrival, int waitTime, int boardTime, T trip) {
        // The relative-transit-time is time spent on transit. We do not know the
        // alight-stop, so
        // it is impossible to calculate the "correct" time. But the only thing that
        // maters is that
        // the relative difference between to boardings are correct, assuming riding the
        // same trip.
        // So, we can use the negative board time as relative-transit-time.
        final int relativeTransitTime = -boardTime;

        int cost = previousArrival.cost() + waitFactorApplied * waitTime + transitFactor * relativeTransitTime
                + boardCost;

        if (stopVisitCost != null) {
            cost += stopVisitCost[previousArrival.stop()];
        }

        ////////////////////////////////////////////////////////////////////////////////////////
        // Trip Busyness Add PHilita
        Connection connection = POSTGRESconnection.PGRSconnection(); // Create connection. Checks if null on entry so
                                                                     // can be looped.
        TripData tripData = new TripData(); // Create TripData holder
        tripData.GTFSIdSet("A"); // Set the tripID to be considered.

        tripData = occupancyCost.OccupancyCostCalc(tripData.GTFSIdRead(), connection); // Set the correct busyness.

        ////////////////////////////////////////////////////////////////////////////////////////

        return cost;
    }

    @Override
    public int transitArrivalCost(ArrivalView<T> previousArrival, int waitTime, int transitTime, int toStop, T trip) {
        int cost = waitFactorApplied * waitTime + transitFactor * transitTime + boardCost;
        if (stopVisitCost != null) {
            cost += stopVisitCost[previousArrival.stop()] + stopVisitCost[toStop];
        }

        ////////////////////////////////////////////////////////////////////////////////////////
        // Stop Busyness Add PHilita

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
        return boardCost * (minNumTransfers + 1) + transitFactor * minTravelTime;
    }

    private void initWaitFactor(int round) {
        // For access(round 0) and the first transit round(1) skip adding a cost for
        // waiting,
        // we assume we can time-shift the access leg.
        this.waitFactorApplied = round < 2 ? 0 : waitFactor;
    }
}
